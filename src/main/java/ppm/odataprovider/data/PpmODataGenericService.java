package ppm.odataprovider.data;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmKeyPropertyRef;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriParameter;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;


public abstract class PpmODataGenericService {

    public EntityCollection getAll(EdmEntitySet entitySet, Class entityClazz) {
        EntityCollection entityCollection = new EntityCollection();
        List<Entity> entityList = entityCollection.getEntities();
        List<Object> taskList = EntityRepository.findAll(entityClazz);
        for (Object task : taskList) {
            Entity entity = EntityDataHelper.toEntity(entityClazz, task, entitySet.getName());
            entityList.add(entity);
        }
        return entityCollection;
    }

    public Entity getEntity(EdmEntitySet edmEntitySet, Class entityClazz, List<UriParameter> keyParams) throws ODataApplicationException {
        if (keyParams.isEmpty()) {
            return null;
        }
        try {
            Entity entity;
            Map<String, Object> params = EntityDataHelper.getKeyParamValue(entityClazz, keyParams);
//            need to re write this using a wrapper
//            keyParams.forEach(param -> {
//                try {
//                    params.put(param.getName(), EntityDataHelper.getKeyParamValue(entityClazz, param));
//                } catch (ODataApplicationException e) {
//                   throw new Exception();
//                }
//            });
            Optional<List<ApplicationEntity>> result = EntityRepository.find(entityClazz, params);
            if (result.isPresent()) {
                entity = EntityDataHelper.toEntity(entityClazz, result.get().get(0), edmEntitySet.getName());
            } else {
                throw new ODataApplicationException("", HttpStatusCode.NO_CONTENT.getStatusCode(), Locale.ENGLISH);
            }
            return entity;
        } catch (Exception e) {
            throw new ODataApplicationException(e.getMessage(), HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);
        }
    }

    public Entity saveEntity(EdmEntitySet edmEntitySet,Class entityClazz, Entity entity) throws ODataApplicationException {
        EdmEntityType entityType = edmEntitySet.getEntityType();
        Entity createdEntity = null;
        try {
            ApplicationEntity createdObject = EntityDataHelper.fromEntity(entityClazz, entity);
            createdObject.init();
            if (EntityRepository.get(createdObject.getClass(), createdObject.getEntityId()).isPresent()) {
                throw new ODataApplicationException("Entity already exists", HttpStatusCode.CONFLICT.getStatusCode(), Locale.ENGLISH);
            }
            ApplicationEntity resultEntity = EntityRepository.save(createdObject);
            createdEntity = EntityDataHelper.toEntity(createdObject.getClass(), resultEntity, edmEntitySet.getName());
        } catch (Exception e) {
            throw new ODataApplicationException(e.getMessage(), HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);
        }
        return createdEntity;
    }

    public void updateEntity(EdmEntitySet edmEntitySet, Class entityClazz, Entity entity, List<UriParameter> keyParams, HttpMethod httpMethod) throws ODataApplicationException, InvocationTargetException, NoSuchMethodException, NoSuchFieldException, InstantiationException, IllegalAccessException {
        if (entity == null) {
            return;
        }
        Entity existingEntity = this.getEntity(edmEntitySet, entityClazz, keyParams);
        if (httpMethod == HttpMethod.PATCH) {
            EdmEntityType entityType = edmEntitySet.getEntityType();
            List<EdmKeyPropertyRef> keyRefs = entityType.getKeyPropertyRefs();
            for (Property property : entity.getProperties()) {
                if (keyRefs.stream().anyMatch(k -> !k.getProperty().equals(property))) {
                    Property exsProp = existingEntity.getProperty(property.getName());
                    if (exsProp.isPrimitive()) {
                        exsProp.setValue(property.getValueType(), property.getValue());
                    }
                }
            }
            ApplicationEntity modifiedEntity = EntityDataHelper.fromEntity(entityClazz, existingEntity);
            EntityRepository.update(modifiedEntity);
        }
        // need to add support for PUT
    }

    public void deleteEntity(EdmEntitySet edmEntitySet, Class entityClazz, List<UriParameter> keyParams) throws ODataApplicationException, InvocationTargetException, NoSuchMethodException, NoSuchFieldException, InstantiationException, IllegalAccessException {
        Entity entity = this.getEntity(edmEntitySet, entityClazz, keyParams);
        EntityRepository.delete(EntityDataHelper.fromEntity(entityClazz, entity));
    }
}
