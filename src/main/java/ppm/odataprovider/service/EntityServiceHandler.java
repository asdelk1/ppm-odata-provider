package ppm.odataprovider.service;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriParameter;
import ppm.odataprovider.data.PpmODataGenericService;
import ppm.odataprovider.service.metadata.EntityMetadataHelper;
import ppm.odataprovider.service.metadata.EntityMetadataModel;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;


public class EntityServiceHandler {

    EntityMetadataModel[] edm;

    public EntityServiceHandler() {
        try {
            this.edm = EntityMetadataHelper.readMetadataJSON();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public EntityCollection readEntitySetData(EdmEntitySet edmEntitySet) throws ODataApplicationException {
        EntityCollection entityCollection;
        try {
            PpmODataGenericService service = getServiceClass(edmEntitySet);
            entityCollection = service.getAll(edmEntitySet.getName());
// if there is no data need to raise an error, not sure this is the correct way might need to change it later.
            if (entityCollection.getEntities().isEmpty()) {
                throw new ODataApplicationException("", HttpStatusCode.NO_CONTENT.getStatusCode(), Locale.ENGLISH);
            }
        } catch (Exception e) {
            throw new ODataApplicationException(e.getMessage(), HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);
        }
        return entityCollection;
    }

    public Entity readEntityData(EdmEntitySet edmEntitySet, List<UriParameter> keyParams) throws ODataApplicationException {
        Entity entity;
        try {
            PpmODataGenericService service = getServiceClass(edmEntitySet);
            entity = service.getEntity(edmEntitySet.getEntityType(), edmEntitySet, keyParams);
            if (entity.getProperties().isEmpty()) {
                throw new ODataApplicationException("", HttpStatusCode.NO_CONTENT.getStatusCode(), Locale.ENGLISH);
            }
        } catch (Exception e) {
            throw new ODataApplicationException(e.getMessage(), HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);
        }
        return entity;
    }

    public Entity saveEntity(EdmEntitySet edmEntitySet, Entity entity) throws ODataApplicationException{
        Entity createdEntity;
        try{
            PpmODataGenericService service = getServiceClass(edmEntitySet);
            createdEntity = service.saveEntity(edmEntitySet.getEntityType(), edmEntitySet, entity);
        }catch(Exception ex){
            throw new ODataApplicationException(ex.getMessage(), HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);
        }
        return createdEntity;
    }

    private PpmODataGenericService getServiceClass(EdmEntitySet entitySet) throws Exception {
        PpmODataGenericService service = null;
        Optional<EntityMetadataModel> dataModelOptional = Arrays.stream(this.edm).filter(m -> m.getEntitySetName().equals(entitySet.getName())).findFirst();
        try {
            if (dataModelOptional.isPresent()) {
                EntityMetadataModel model = dataModelOptional.get();
                Class serviceClass = Thread.currentThread().getContextClassLoader().loadClass(model.getServiceClass());
                service = (PpmODataGenericService) serviceClass.getConstructor().newInstance();
            }
        } catch (NoClassDefFoundError | ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            throw new Exception(e.getMessage());
        }
        return service;
    }


}
