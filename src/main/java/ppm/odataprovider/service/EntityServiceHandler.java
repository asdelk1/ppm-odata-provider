package ppm.odataprovider.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.olingo.commons.api.data.*;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmKeyPropertyRef;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.queryoption.ExpandOption;
import org.apache.olingo.server.api.uri.queryoption.FilterOption;
import org.hibernate.criterion.Criterion;
import ppm.odataprovider.data.ApplicationEntity;
import ppm.odataprovider.data.EntityDataHelper;
import ppm.odataprovider.data.PpmODataGenericService;
import ppm.odataprovider.service.metadata.EntityMetadataHelper;
import ppm.odataprovider.service.metadata.EntityOperationMetadataModel;
import ppm.odataprovider.service.metadata.OperationParameterModel;

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

    public EntityCollection readEntitySetData(EdmEntitySet edmEntitySet, FilterOption filterOption, ExpandOption expandOption) throws ODataApplicationException {
        EntityCollection entityCollection = new EntityCollection();
        try {
            PpmODataGenericService service = this.entityMetadata.getServiceClass(edmEntitySet);
            Class<?> entityClazz = this.entityMetadata.getEntityClass(edmEntitySet.getEntityType());
            List<ApplicationEntity> resultSet;
            if (filterOption != null) {
                Criterion filter = (Criterion) filterOption.getExpression().accept(new FilterExpressionVisitor());
                Optional<List<ApplicationEntity>> result = service.find(entityClazz, filter);
                resultSet = result.isPresent() ? result.get() : new ArrayList<>();
            } else {
                resultSet = service.getAll(entityClazz);
            }
            EntityDataHelper.addEntitiesToCollection(entityCollection, resultSet, entityClazz, edmEntitySet, expandOption);
        } catch (Exception e) {
            throw new ODataApplicationException(e.getMessage(), HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);
        }
        return entityCollection;
    }

    public EntityCollection readRelatedEntitySetData(EdmEntitySet sourceEntitySet, EdmEntitySet targetEntitySet, List<UriParameter> keyParams, String navProperty) throws ODataApplicationException {
        EntityCollection entityCollection = new EntityCollection();
        try {
            PpmODataGenericService service = this.entityMetadata.getServiceClass(sourceEntitySet);
            Class<?> entityClazz = this.entityMetadata.getEntityClass(sourceEntitySet.getEntityType());
            Map<String, Object> params = EntityDataHelper.getKeyParamValue(entityClazz, keyParams);
            Optional<ApplicationEntity> resultSet = service.getEntity(entityClazz, params);
            if (resultSet.isPresent()) {
                ApplicationEntity sourceResult = resultSet.get();
                List<ApplicationEntity> list = (List<ApplicationEntity>) getNavEntity(navProperty, entityClazz, sourceResult);
                EntityDataHelper.addEntitiesToCollection(entityCollection, list, this.entityMetadata.getEntityClass(targetEntitySet.getEntityType()), targetEntitySet, null);
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
            Class<?> entityClazz = this.entityMetadata.getEntityClass(edmEntitySet.getEntityType());
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
            Class<?> sourceEntityClazz = this.entityMetadata.getEntityClass(sourceEntitySet.getEntityType());
            Class<?> targetEntityClazz = this.entityMetadata.getEntityClass(targetEntitySet.getEntityType());
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
            Class<?> entityClazz = this.entityMetadata.getEntityClass(sourceEntitySet.getEntityType());
            Class<?> targetEntityClazz = this.entityMetadata.getEntityClass(targetEntitySet.getEntityType());
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
            ApplicationEntity newObject = EntityDataHelper.fromEntity(this.entityMetadata.getEntityClass(edmEntitySet.getEntityType()), entity);
            ApplicationEntity createdObject = service.saveEntity(newObject);
            createdEntity = EntityDataHelper.toEntity(newObject.getClass(), createdObject, edmEntitySet, null);
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
            Class<?> entityClazz = this.entityMetadata.getEntityClass(edmEntitySet.getEntityType());
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

                for (Link navLink : entity.getNavigationLinks()) {
                    existingEntity.getNavigationLinks().add(navLink);
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
            Class<?> entityClazz = this.entityMetadata.getEntityClass(edmEntitySet.getEntityType());
            Map<String, Object> params = EntityDataHelper.getKeyParamValue(entityClazz, keyParams);
            Optional<ApplicationEntity> result = service.getEntity(entityClazz, params);
            if (result.isPresent()) {
                service.deleteEntity(result.get());
            } else {
                throw new ODataApplicationException(String.format("Entity with id(%s) not found", params.toString()), HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);
            }
        } catch (Exception ex) {
            throw new ODataApplicationException(ex.getMessage(), HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);
        }
    }

    public EntityOperationResult executeEntityOperation(EntityOperationType operationType, String name, Object params) throws ODataApplicationException {
        List<ApplicationEntity> entityList = null;
        Class entityClazz = null;
        Optional<EntityOperationMetadataModel> operationMetadata = operationType == EntityOperationType.Action ? this.entityMetadata.getAction(name) : this.entityMetadata.getFunction(name);
        if (operationMetadata.isPresent()) {
            try {
                Method entityMethod = EntityDataHelper.getStaticMethod(operationMetadata.get().getEntityClass(), operationMetadata.get().getMethod());
                Object[] methodArgs = this.getMethodParameters(operationType, operationMetadata.get().getParams(), params);
                Object functionReturnResult = entityMethod.invoke(null, methodArgs);
                if (functionReturnResult != null) {
                    if (EntityDataHelper.isCollectionType(entityMethod.getReturnType())) {
                        entityList = (List<ApplicationEntity>) functionReturnResult;
                        String typeName = EntityDataHelper.getParameterizedType(entityMethod.getGenericReturnType()).getTypeName();
                        entityClazz = EntityDataHelper.loadClass(typeName);
                    } else {
                        ApplicationEntity entity = (ApplicationEntity) functionReturnResult;
                        entityList = new ArrayList<>();
                        entityList.add(entity);
                        entityClazz = EntityDataHelper.loadClass(entityMethod.getGenericReturnType().getTypeName());
                    }
                }

                return new EntityOperationResult(functionReturnResult == null, entityList, entityClazz);

            } catch (Exception e) {
                throw new ODataApplicationException(e.getMessage(), HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);
            }
        }else{
            throw new ODataApplicationException("Function not found!",  HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);
        }
    }

    private Object[] getMethodParameters(EntityOperationType type, Map<String, OperationParameterModel> paramMetadata, Object params) {
        List<Object> argList = new ArrayList<>();
        if (paramMetadata != null && params != null) {
            for (Map.Entry<String, OperationParameterModel> param : paramMetadata.entrySet()) {
                if (type == EntityOperationType.Function) {
                    OperationParameterModel meta = param.getValue();
                    List<UriParameter> uriParameters = (List<UriParameter>) params;
                    Optional<UriParameter> uriParameter = uriParameters.stream().filter((p) -> p.getName().equals(meta.getName())).findFirst();
                    if (uriParameter.isPresent()) {
                        if (meta.getType().equals("Integer") || meta.getType().equals("int")) {
                            argList.add(Integer.parseInt(uriParameter.get().getText()));
                        } else if (meta.getType().equals("String")) {
                            String value = uriParameter.get().getText().substring(1, uriParameter.get().getText().length() - 1);
                            argList.add(value);
                        }
                    }
                } else if(type == EntityOperationType.Action) {
                    Map<String, Parameter> actionParameters = (Map<String, Parameter>) params;
                    if(actionParameters.containsKey(param.getKey())){
                        argList.add(actionParameters.get(param.getKey()).getValue());
                    }
                }
            }
        }
        return argList.toArray();
    }

    private Object getNavEntity(String navProperty, Class<?> entityClazz, ApplicationEntity resultObject) throws NoSuchFieldException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Field navField = entityClazz.getDeclaredField(navProperty);
        String getterMethodName = "get" + StringUtils.capitalize(navField.getName());
        Method method = entityClazz.getMethod(getterMethodName);
        return method.invoke(resultObject);
    }

}
