package ppm.odataprovider.service;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriParameter;
import ppm.odataprovider.data.PpmODataGenericService;
import ppm.odataprovider.service.metadata.EntityMetadataHelper;

import java.util.List;
import java.util.Locale;


public class EntityServiceHandler {

    private EntityMetadataHelper entityMetadata;

    public EntityServiceHandler() {
        try {
            this.entityMetadata = new EntityMetadataHelper();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public EntityCollection readEntitySetData(EdmEntitySet edmEntitySet) throws ODataApplicationException {
        EntityCollection entityCollection;
        try {
            PpmODataGenericService service = this.entityMetadata.getServiceClass(edmEntitySet);
            entityCollection = service.getAll(edmEntitySet, this.entityMetadata.getEntityClass(edmEntitySet.getEntityType()));
// if there is no data need to raise an error, not sure this is the correct way might need to change it later.
//            if (entityCollection.getEntities().isEmpty()) {
//                throw new ODataApplicationException("", HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
//            }
        } catch (Exception e) {
            throw new ODataApplicationException(e.getMessage(), HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);
        }
        return entityCollection;
    }

    public EntityCollection readRelatedEntitySetData(EdmEntitySet sourceEntitySet, List<UriParameter> keyParams, String property) {

    }

    public Entity readEntityData(EdmEntitySet edmEntitySet, List<UriParameter> keyParams) throws ODataApplicationException {
        Entity entity;
        try {
            PpmODataGenericService service = this.entityMetadata.getServiceClass(edmEntitySet);
            entity = service.getEntity(edmEntitySet, this.entityMetadata.getEntityClass(edmEntitySet.getEntityType()), keyParams);

        } catch (Exception e) {
            throw new ODataApplicationException(e.getMessage(), HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);
        }
        return entity;
    }

    public Entity saveEntity(EdmEntitySet edmEntitySet, Entity entity) throws ODataApplicationException {
        Entity createdEntity;
        try {
            PpmODataGenericService service = this.entityMetadata.getServiceClass(edmEntitySet);
            createdEntity = service.saveEntity(edmEntitySet, this.entityMetadata.getEntityClass(edmEntitySet.getEntityType()), entity);
        } catch (Exception ex) {
            throw new ODataApplicationException(ex.getMessage(), HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);
        }
        return createdEntity;
    }

    public void updateEntity(EdmEntitySet edmEntitySet, List<UriParameter> keyParams, Entity entity, HttpMethod httpMethod) throws ODataApplicationException {
        try {
            PpmODataGenericService service = this.entityMetadata.getServiceClass(edmEntitySet);
            service.updateEntity(edmEntitySet, this.entityMetadata.getEntityClass(edmEntitySet.getEntityType()), entity, keyParams, httpMethod);
        } catch (Exception ex) {
            throw new ODataApplicationException(ex.getMessage(), HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);
        }
    }

    public void deleteEntity(EdmEntitySet edmEntitySet, List<UriParameter> keyParams) throws ODataApplicationException {
        try {
            PpmODataGenericService service = this.entityMetadata.getServiceClass(edmEntitySet);
            service.deleteEntity(edmEntitySet, this.entityMetadata.getEntityClass(edmEntitySet.getEntityType()), keyParams);
        } catch (Exception ex) {
            throw new ODataApplicationException(ex.getMessage(), HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);
        }
    }

}
