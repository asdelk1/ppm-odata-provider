package ppm.odataprovider.service;

import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.*;
import org.apache.olingo.commons.api.ex.ODataException;
import ppm.odataprovider.service.metadata.EntityMetadataHelper;
import ppm.odataprovider.service.metadata.EntityTypeMetadata;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.*;

public class PpmEdmProvider extends CsdlAbstractEdmProvider {

    // Service Namespace
    private static final String NAMESPACE = "OData.Ppm";

    // EDM Container
    private static final String CONTAINER_NAME = "Project   Container";
    private static final FullQualifiedName CONTAINER = new FullQualifiedName(NAMESPACE, CONTAINER_NAME);

    private EntityMetadataHelper entityMetadata;

    public PpmEdmProvider() {
        try {
            this.entityMetadata = EntityMetadataHelper.getInstance();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public CsdlEntityType getEntityType(FullQualifiedName entityTypeName) throws ODataException {
        try {
            CsdlEntityType entityType = new CsdlEntityType();
            List<CsdlProperty> properties = new ArrayList<>();
            List<CsdlPropertyRef> keyref = new ArrayList<>();
            List<CsdlNavigationProperty> navPropList = new ArrayList<>();
            Optional<EntityTypeMetadata> entityTypeMetadataOptional = this.entityMetadata.getEntity(entityTypeName.getName());
            if (entityTypeMetadataOptional.isPresent()) {
                EntityTypeMetadata model = entityTypeMetadataOptional.get();
                Class entityClass = Thread.currentThread().getContextClassLoader().loadClass(model.getEntityClass());
                Field[] classFields = entityClass.getDeclaredFields();
                for (Field field : classFields) {
                    if (this.entityMetadata.isPrimitiveType(field.getType())) {

                        properties.add(new CsdlProperty()
                                .setName(field.getName())
                                .setType(this.entityMetadata.getODataPrimitiveDataType(field.getType().getName())));

                        keyref.add(new CsdlPropertyRef().setName("entityId"));

                    } else if (EntityMetadataHelper.isNavigationProperty(field)) {
                        boolean isCollection = EntityMetadataHelper.isCollectionType(field.getType());

                        Optional<String> navEntitySetOptional = isCollection ?
                                this.entityMetadata.getEntitySetForEntityClass(EntityMetadataHelper.getParameterizedType(field).getTypeName())
                                : this.entityMetadata.getEntitySetForEntityClass(field.getType().getName());

                        if (navEntitySetOptional.isPresent()) {
                            String navEntityTypeName = this.entityMetadata.getEntitySetTypeName(navEntitySetOptional.get());
                            CsdlNavigationProperty navProp = new CsdlNavigationProperty()
                                    .setName(field.getName())
                                    .setType(new FullQualifiedName(PpmEdmProvider.NAMESPACE, navEntityTypeName))
                                    .setCollection(isCollection)
                                    .setNullable(false);
//                                        .setPartner(model.getEntitySetName());
                            navPropList.add(navProp);

                        }
                    }
                }
                entityType.setName(entityTypeName.getName());
                entityType.setProperties(properties);
                entityType.setKey(keyref);
                entityType.setNavigationProperties(navPropList);
            }
            return entityType;
        } catch (Exception e) {
            throw new ODataException(e.getMessage());
        }
    }

    @Override
    public CsdlEntitySet getEntitySet(FullQualifiedName entityContainer, String entitySetName) {
        if (entityContainer.equals(CONTAINER)) {

            Optional<EntityTypeMetadata> optionalMetadata = this.entityMetadata.getEntitySetType(entitySetName);
            if (this.entityMetadata.isEntitySetExists(entitySetName) && optionalMetadata.isPresent()) {
                EntityTypeMetadata entityTypeMetadata = optionalMetadata.get();
                CsdlEntitySet entitySet = new CsdlEntitySet();
                entitySet.setName(entitySetName);
                entitySet.setType(new FullQualifiedName(NAMESPACE, this.entityMetadata.getEntitySetTypeName(entitySetName)));

                List<CsdlNavigationPropertyBinding> navPropBindingList = new ArrayList<>();
                Map<String, Type> navFields = entityTypeMetadata.getNavigationFields();
                for (Map.Entry<String, Type> entry : navFields.entrySet()) {
                    Optional<String> entitySetOptional = this.entityMetadata.getEntitySetForEntityClass(entry.getValue().getTypeName());
                    entitySetOptional.ifPresent(esName -> {
                        CsdlNavigationPropertyBinding navPropBinding = new CsdlNavigationPropertyBinding();
                        navPropBinding.setPath(entry.getKey());
                        navPropBinding.setTarget(esName);
                        navPropBindingList.add(navPropBinding);
                    });
                }
                if (!navPropBindingList.isEmpty()) {
                    entitySet.setNavigationPropertyBindings(navPropBindingList);
                }
                return entitySet;
            }
        }
        return null;
    }

    @Override
    public CsdlEntityContainerInfo getEntityContainerInfo(FullQualifiedName entityContainerName) {
        CsdlEntityContainerInfo entityContainerInfo = new CsdlEntityContainerInfo();
        entityContainerInfo.setContainerName(CONTAINER);
        return entityContainerInfo;
    }

    @Override
    public List<CsdlSchema> getSchemas() throws ODataException {
        CsdlSchema schema = new CsdlSchema();
        schema.setNamespace(NAMESPACE);
        List<CsdlEntityType> entityTypes = new ArrayList<>();
        for (Map.Entry<String, EntityTypeMetadata> entry : this.entityMetadata.getEntityTypes().entrySet()) {
            entityTypes.add(getEntityType(new FullQualifiedName(NAMESPACE, entry.getKey())));
        }
        schema.setEntityTypes(entityTypes);
        schema.setEntityContainer(getEntityContainer());
        List<CsdlSchema> schemaList = new ArrayList<>();
        schemaList.add(schema);
        return schemaList;
    }

    @Override
    public CsdlEntityContainer getEntityContainer() throws ODataException {
        List<CsdlEntitySet> entitySets = new ArrayList<>();
        for (String entityset : this.entityMetadata.getEntitySets().keySet()) {
            entitySets.add(this.getEntitySet(CONTAINER, entityset));
        }
        CsdlEntityContainer entityContainer = new CsdlEntityContainer();
        entityContainer.setName(CONTAINER_NAME);
        entityContainer.setEntitySets(entitySets);
        return entityContainer;
    }
}
