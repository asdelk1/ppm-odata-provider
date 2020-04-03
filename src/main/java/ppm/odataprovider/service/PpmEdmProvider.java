package ppm.odataprovider.service;

import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.*;
import org.apache.olingo.commons.api.ex.ODataException;
import ppm.odataprovider.service.metadata.EntityMetadataHelper;
import ppm.odataprovider.service.metadata.EntityMetadataModel;

import java.util.*;

public class PpmEdmProvider extends CsdlAbstractEdmProvider {

    // Service Namespace
    public static final String NAMESPACE = "OData.Ppm";

    // EDM Container
    public static final String CONTAINER_NAME = "Project   Container";
    public static final FullQualifiedName CONTAINER = new FullQualifiedName(NAMESPACE, CONTAINER_NAME);

    private EntityMetadataModel[] entityMetadata;

    public PpmEdmProvider() {
        try {
            this.entityMetadata = EntityMetadataHelper.readMetadataJSON();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public CsdlEntityType getEntityType(FullQualifiedName entityTypeName) throws ODataException {
        try {
            return EntityMetadataHelper.getEntityType(entityTypeName);
        } catch (Exception e) {
            throw new ODataException(e.getMessage());
        }
    }

    @Override
    public CsdlEntitySet getEntitySet(FullQualifiedName entityContainer, String entitySetName) throws ODataException {
        if (entityContainer.equals(CONTAINER)) {
            Optional<EntityMetadataModel> edmModelFound = Arrays.stream(this.entityMetadata).filter(entityMetadataModel -> entityMetadataModel.getEntitySetName().equals(entitySetName))
                    .findFirst();
            if (edmModelFound.isPresent()) {
                EntityMetadataModel edm = edmModelFound.get();
                CsdlEntitySet entitySet = new CsdlEntitySet();
                entitySet.setName(edm.getEntitySetName());
                entitySet.setType(new FullQualifiedName(NAMESPACE, edm.getEntityType()));
                return entitySet;
            }
        }
        return null;
    }

    @Override
    public CsdlEntityContainerInfo getEntityContainerInfo(FullQualifiedName entityContainerName) throws ODataException {
        if (entityContainerName == null || entityContainerName.equals(entityContainerName)) {
            CsdlEntityContainerInfo entityContainerInfo = new CsdlEntityContainerInfo();
            entityContainerInfo.setContainerName(CONTAINER);
            return entityContainerInfo;
        }
        return null;
    }

    @Override
    public List<CsdlSchema> getSchemas() throws ODataException {
        CsdlSchema schema = new CsdlSchema();
        schema.setNamespace(NAMESPACE);
        List<CsdlEntityType> entityTypes = new ArrayList<CsdlEntityType>();
        for (EntityMetadataModel metadataModel : this.entityMetadata) {
            entityTypes.add(getEntityType(new FullQualifiedName(NAMESPACE, metadataModel.getEntityType())));
        }
        schema.setEntityTypes(entityTypes);
        schema.setEntityContainer(getEntityContainer());
        List<CsdlSchema> schemaList = new ArrayList<CsdlSchema>();
        schemaList.add(schema);
        return schemaList;
    }

    @Override
    public CsdlEntityContainer getEntityContainer() throws ODataException {
        List<CsdlEntitySet> entitySets = new ArrayList<CsdlEntitySet>();
        for (EntityMetadataModel metadataModel : this.entityMetadata) {
            entitySets.add(this.getEntitySet(CONTAINER, metadataModel.getEntitySetName()));
        }
        CsdlEntityContainer entityContainer = new CsdlEntityContainer();
        entityContainer.setName(CONTAINER_NAME);
        entityContainer.setEntitySets(entitySets);
        return entityContainer;
    }
}
