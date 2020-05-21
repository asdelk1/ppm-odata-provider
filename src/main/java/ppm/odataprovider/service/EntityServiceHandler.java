package ppm.odataprovider.service;

import org.apache.commons.lang3.StringUtils;
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
import org.apache.olingo.server.api.uri.queryoption.ExpandOption;
import ppm.odataprovider.data.ApplicationEntity;
import ppm.odataprovider.data.EntityDataHelper;
import ppm.odataprovider.data.PpmODataGenericService;
import ppm.odataprovider.service.metadata.EntityMetadataHelper;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;


public class EntityServiceHandler {

    private EntityMetadataHelper entityMetadata;

    public EntityServiceHandler() {
        try {
            this.entityMetadata = EntityMetadataHelper.getInstance();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public EntityCollection readEntitySetData(EdmEntitySet edmEntitySet) throws ODataApplicationException {
        EntityCollection entityCollection = new EntityCollection();
        List<Entity> entityList = entityCollection.getEntities();
        try {
            PpmODataGenericService service = this.entityMetadata.getServiceClass(edmEntitySet);
            Class entityClazz = this.entityMetadata.getEntityClass(edmEntitySet.getEntityType());
            List<ApplicationEntity> resultSet = service.getAll(entityClazz);
            EntityDataHelper.addEntitiesToCollection(entityCollection, resultSet, entityClazz, edmEntitySet);

// if there is no data need to raise an error, not sure this is the correct way might need to change it later.
//            if (entityCollection.getEntities().isEmpty()) {
//                throw new ODataApplicationException("", HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
//            }
        } catch (Exception e) {
            throw new ODataApplicationException(e.getMessage(), HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);
        }
        return entityCollection;
    }

    public EntityCollection readRelatedEntitySetData(EdmEntitySet sourceEntitySet, EdmEntitySet targetEntitySet, List<UriParameter> keyParams, String navProperty) throws ODataApplicationException {
        EntityCollection entityCollection = new EntityCollection();
        try {
            PpmODataGenericService service = this.entityMetadata.getServiceClass(sourceEntitySet);
            Class entityClazz = this.entityMetadata.getEntityClass(sourceEntitySet.getEntityType());
            Map<String, Object> params = EntityDataHelper.getKeyParamValue(entityClazz, keyParams);
            Optional<ApplicationEntity> resultSet = service.getEntity(entityClazz, params);
            if (resultSet.isPresent()) {
                ApplicationEntity sourceResult = resultSet.get();
                List<ApplicationEntity> list = (List<ApplicationEntity>) getNavEntity(navProperty, entityClazz, sourceResult);
                EntityDataHelper.addEntitiesToCollection(entityCollection, list, this.entityMetadata.getEntityClass(targetEntitySet.getEntityType()), targetEntitySet);
            }
            return entityCollection;
        } catch (Exception e) {
            throw new ODataApplicationException(e.getMessage(), HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);
        }
    }


    public Entity readEntityData(EdmEntitySet edmEntitySet, List<UriParameter> keyParams, ExpandOption expandOption) throws ODataApplicationException {
        Entity entity;
        try {
            PpmODataGenericService service = this.entityMetadata.getServiceClass(edmEntitySet);
            Class entityClazz = this.entityMetadata.getEntityClass(edmEntitySet.getEntityType());
            Map<String, Object> params = EntityDataHelper.getKeyParamValue(entityClazz, keyParams);
            Optional<ApplicationEntity> resultSet = service.getEntity(entityClazz, params);
            if (resultSet.isPresent()) {
                entity = EntityDataHelper.toEntity(entityClazz, resultSet.get(), edmEntitySet, expandOption);
            } else {
                throw new ODataApplicationException("", HttpStatusCode.NO_CONTENT.getStatusCode(), Locale.ENGLISH);
            }
            return entity;

        } catch (Exception e) {
            throw new ODataApplicationException(e.getMessage(), HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);
        }
    }

    public Entity readRelatedEntityData(EdmEntitySet sourceEntitySet, EdmEntitySet targetEntitySet, List<UriParameter> keyParams, String navProperty) throws ODataApplicationException {
        Entity entity;
        try {
            PpmODataGenericService service = this.entityMetadata.getServiceClass(sourceEntitySet);
            Class sourceEntityClazz = this.entityMetadata.getEntityClass(sourceEntitySet.getEntityType());
            Class targetEntityClazz = this.entityMetadata.getEntityClass(targetEntitySet.getEntityType());
            Map<String, Object> params = EntityDataHelper.getKeyParamValue(sourceEntityClazz, keyParams);
            Optional<ApplicationEntity> resultSet = service.getEntity(sourceEntityClazz, params);
            if (resultSet.isPresent()) {
                ApplicationEntity sourceResult = resultSet.get();
                ApplicationEntity navResult = (ApplicationEntity) this.getNavEntity(navProperty, sourceEntityClazz, sourceResult);
                entity = EntityDataHelper.toEntity(targetEntityClazz, navResult, targetEntitySet, null);

            } else {
                throw new ODataApplicationException("", HttpStatusCode.NO_CONTENT.getStatusCode(), Locale.ENGLISH);
            }
            return entity;

        } catch (Exception e) {
            throw new ODataApplicationException(e.getMessage(), HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);
        }
    }

    public Entity readRelatedEntityData(EdmEntitySet sourceEntitySet, EdmEntitySet targetEntitySet, List<UriParameter> keyParams, String navProperty, List<UriParameter> navParams) throws ODataApplicationException {
        Entity entity = null;
        try {
            PpmODataGenericService service = this.entityMetadata.getServiceClass(sourceEntitySet);
            Class entityClazz = this.entityMetadata.getEntityClass(sourceEntitySet.getEntityType());
            Class targetEntityClazz = this.entityMetadata.getEntityClass(targetEntitySet.getEntityType());
            Map<String, Object> params = EntityDataHelper.getKeyParamValue(entityClazz, keyParams);
            Map<String, Object> navParamsMap = EntityDataHelper.getKeyParamValue(entityClazz, navParams);
            Optional<ApplicationEntity> resultSet = service.getEntity(entityClazz, params);
            if (resultSet.isPresent()) {
                ApplicationEntity sourceResult = resultSet.get();
                List<ApplicationEntity> list = (List<ApplicationEntity>) getNavEntity(navProperty, entityClazz, sourceResult);
                Optional<ApplicationEntity> possibleEntity = list.stream().filter(e -> {
                    boolean isFound = false;
                    Field[] fields = e.getClass().getDeclaredFields();
                    Map<String, Field> fieldMap = Arrays.asList(fields).stream()
                            .collect(Collectors.toMap(f -> f.getName(), f -> f));
                    for (Map.Entry<String, Object> param : navParamsMap.entrySet()) {
                        if (fieldMap.containsKey(param.getKey())) {
                            Object paramValue = param.getValue();
                            Field field = fieldMap.get(param.getKey());
                            try {
                                Method getterMethod = field.getClass().getMethod("get" + StringUtils.capitalize(field.getName()));
                                Object fieldValue = getterMethod.invoke(e);
                                if (!fieldValue.equals(paramValue)) {
                                    isFound = true;
                                } else {
                                    isFound = false;
                                    break;
                                }
                            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                    return isFound;
                }).findFirst();

                entity = possibleEntity.isPresent() ? EntityDataHelper.toEntity(targetEntityClazz, possibleEntity.get(), targetEntitySet, null) : null;
            }
            return entity;
        } catch (Exception e) {
            throw new ODataApplicationException(e.getMessage(), HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);
        }
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
            if (entity == null) {
                return;
            }
            Class entityClazz = this.entityMetadata.getEntityClass(edmEntitySet.getEntityType());
            Entity existingEntity = this.readEntityData(edmEntitySet, keyParams, null);
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
                service.updateEntity(modifiedEntity);
            }
        } catch (Exception ex) {
            throw new ODataApplicationException(ex.getMessage(), HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);
        }
    }

    public void deleteEntity(EdmEntitySet edmEntitySet, List<UriParameter> keyParams) throws ODataApplicationException {
        try {
            PpmODataGenericService service = this.entityMetadata.getServiceClass(edmEntitySet);
            Entity entityToDelete = this.readEntityData(edmEntitySet, keyParams, null);
            Class entityClazz = this.entityMetadata.getEntityClass(edmEntitySet.getEntityType());
            service.deleteEntity(EntityDataHelper.fromEntity(entityClazz, entityToDelete));
        } catch (Exception ex) {
            throw new ODataApplicationException(ex.getMessage(), HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);
        }
    }

    private Object getNavEntity(String navProperty, Class entityClazz, ApplicationEntity resultObject) throws NoSuchFieldException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Field navField = entityClazz.getDeclaredField(navProperty);
        String getterMethodName = "get" + StringUtils.capitalize(navField.getName());
        Method method = entityClazz.getMethod(getterMethodName);
        return method.invoke(resultObject);
    }

}
