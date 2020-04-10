package ppm.odataprovider.service;

import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.*;
import org.apache.olingo.commons.api.ex.ODataException;
import ppm.odataprovider.service.metadata.EntityMetadataHelper;
import ppm.odataprovider.service.metadata.EntityMetadataModel;

import java.lang.reflect.Field;
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
            this.entityMetadata = new EntityMetadataHelper();
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
            for (EntityMetadataModel model : this.entityMetadata.getEntityMetadata()) {
                FullQualifiedName modelFqn = new FullQualifiedName(PpmEdmProvider.NAMESPACE, model.getEntityType());
                if (modelFqn.equals(entityTypeName)) {
                    Class entityClass = Thread.currentThread().getContextClassLoader().loadClass(model.getEntityClass());
                    Field[] classFields = entityClass.getDeclaredFields();
                    for (Field field : classFields) {
                        if (this.entityMetadata.isPrimitiveType(field.getType())) {

                            properties.add(new CsdlProperty()
                                    .setName(field.getName())
                                    .setType(this.entityMetadata.getODataPrimitiveDataType(field.getType().getName())));

                            Optional<String> keyProperty = Arrays.stream(model.getKeys())
                                    .filter(k -> k.equals(field.getName()))
                                    .findFirst();

                            keyProperty.ifPresent(s -> keyref.add(new CsdlPropertyRef().setName(s)));

                        } else if (this.entityMetadata.isNavigationProperty(field)) {
                            boolean isCollection = this.entityMetadata.isCollectionType(field.getType());
                            Optional<EntityMetadataModel> edmOptional = isCollection ?
                                    this.entityMetadata.getEntityMetadataModelByEntityClass(this.entityMetadata.getParameterizedType(field).getTypeName())
                                    : this.entityMetadata.getEntityMetadataModelByEntityClass(field.getType().getName());
                            edmOptional.ifPresent(navModel -> {
                                CsdlNavigationProperty navProp = new CsdlNavigationProperty()
                                        .setName(field.getName())
                                        .setType(new FullQualifiedName(PpmEdmProvider.NAMESPACE, navModel.getEntityType()))
                                        .setCollection(isCollection)
                                        .setNullable(false);
//                                        .setPartner(model.getEntitySetName());
                                navPropList.add(navProp);
                            });
                        }
                    }
                    entityType.setName(model.getEntityType());
                    entityType.setProperties(properties);
                    entityType.setKey(keyref);
                    entityType.setNavigationProperties(navPropList);
                }
            }
            return entityType;
        } catch (Exception e) {
            throw new ODataException(e.getMessage());
        }
    }

    @Override
    public CsdlEntitySet getEntitySet(FullQualifiedName entityContainer, String entitySetName) throws ODataException {
        if (entityContainer.equals(CONTAINER)) {
            Optional<EntityMetadataModel> edmModelFound = this.entityMetadata.getEntityMetadataModelByEntitySet(entitySetName);
            if (edmModelFound.isPresent()) {
                EntityMetadataModel edm = edmModelFound.get();
                CsdlEntitySet entitySet = new CsdlEntitySet();
                entitySet.setName(edm.getEntitySetName());
                entitySet.setType(new FullQualifiedName(NAMESPACE, edm.getEntityType()));

                List<CsdlNavigationPropertyBinding> navPropBindingList = new ArrayList<>();
                Map<String, Class> navFields = this.entityMetadata.getEntitySetNavigationField(entitySetName);
                for (Map.Entry<String, Class> entry : navFields.entrySet()) {
                    Optional<String> entitySetOptional = this.entityMetadata.getEntitySetForEntityClass(entry.getValue().getName());
                    entitySetOptional.ifPresent(esName -> {
                        CsdlNavigationPropertyBinding navPropBinding = new CsdlNavigationPropertyBinding();
                        navPropBinding.setPath(entry.getKey());
                        navPropBinding.setTarget(esName);
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
    public CsdlEntityContainerInfo getEntityContainerInfo(FullQualifiedName entityContainerName) throws ODataException {
        CsdlEntityContainerInfo entityContainerInfo = new CsdlEntityContainerInfo();
        entityContainerInfo.setContainerName(CONTAINER);
        return entityContainerInfo;
    }

    @Override
    public List<CsdlSchema> getSchemas() throws ODataException {
        CsdlSchema schema = new CsdlSchema();
        schema.setNamespace(NAMESPACE);
        List<CsdlEntityType> entityTypes = new ArrayList<>();
        for (EntityMetadataModel metadataModel : this.entityMetadata.getEntityMetadata()) {
            entityTypes.add(getEntityType(new FullQualifiedName(NAMESPACE, metadataModel.getEntityType())));
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
        for (EntityMetadataModel metadataModel : this.entityMetadata.getEntityMetadata()) {
            entitySets.add(this.getEntitySet(CONTAINER, metadataModel.getEntitySetName()));
        }
        CsdlEntityContainer entityContainer = new CsdlEntityContainer();
        entityContainer.setName(CONTAINER_NAME);
        entityContainer.setEntitySets(entitySets);
        return entityContainer;
    }
}
