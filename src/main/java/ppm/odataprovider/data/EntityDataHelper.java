package ppm.odataprovider.data;

import org.apache.commons.lang3.StringUtils;
import org.apache.olingo.commons.api.Constants;
import org.apache.olingo.commons.api.data.*;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmNavigationProperty;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.*;
import org.apache.olingo.server.api.uri.queryoption.ExpandItem;
import org.apache.olingo.server.api.uri.queryoption.ExpandOption;
import ppm.odataprovider.service.EntityServiceUtil;
import ppm.odataprovider.service.metadata.EntityMetadataHelper;

import javax.persistence.Id;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

public class EntityDataHelper {

    public static Entity toEntity(Class dataClass, Object object, EdmEntitySet entitySet, ExpandOption expandOption) {
        Entity entity = new Entity();
        URI id = null;
        try {
            EdmEntityType entityType = entitySet.getEntityType();
            for (Field field : dataClass.getDeclaredFields()) {
                EdmNavigationProperty navProperty = entityType.getNavigationProperty(field.getName());
                if (EntityMetadataHelper.isNavigationProperty(field) && expandOption != null &&
                        (navProperty != null && isExpandField(expandOption.getExpandItems(), navProperty))) {

                    EdmEntitySet targetEs = EntityServiceUtil.getNavigationTargetEntitySet(entitySet, navProperty);
                    String navPropName = navProperty.getName();
                    Link link = new Link();
                    link.setTitle(navPropName);
                    link.setType(Constants.ENTITY_NAVIGATION_LINK_TYPE);
                    link.setRel(Constants.NS_ASSOCIATION_LINK_REL + navPropName);
                    Object value = invokeGetter(dataClass, object, field);
                    if (navProperty.isCollection()) {
                        List<ApplicationEntity> list = (List<ApplicationEntity>) value;
                        EntityCollection expandEntityCollection = new EntityCollection();
                        addEntitiesToCollection(expandEntityCollection, list, field.getType(), targetEs, expandOption);
                        link.setInlineEntitySet(expandEntityCollection);
                        link.setHref(expandEntityCollection.getId().toASCIIString());
                    } else {
                        // handle single entity
                        Entity expandedEntity = toEntity(field.getType(), value, targetEs, null);
                        link.setInlineEntity(expandedEntity);
                        link.setHref(expandedEntity.getId().toASCIIString());

                    }
                    entity.getNavigationLinks().add(link);

                } else {
                    Annotation keyAnnotation = field.getAnnotation(Id.class);
                    Object value = invokeGetter(dataClass, object, field);
                    Property property = new Property(null, field.getName(), ValueType.PRIMITIVE, value);
                    entity.addProperty(property);
                    if (keyAnnotation != null) {
                        id = new URI(String.valueOf(value));
                    }
                }
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            throw new ODataRuntimeException("Unable to create id for entity: " + e);
        }
        entity.setId(id);

        return entity;
    }

    private static boolean isExpandField(List<ExpandItem> expandItems, EdmNavigationProperty navProperty) {
        if (navProperty == null) {
            return false;
        }

        boolean isExpandItemFound = expandItems.stream().anyMatch(
                (expandItem -> {
                    if (expandItem.isStar()) {
                        return true;
                    } else {
                        // Not sure about this, but for the moment leave it like this.
                        UriResource uriResource = expandItem.getResourcePath().getUriResourceParts().get(0);
                        if (uriResource instanceof UriResourceNavigation) {
                            EdmNavigationProperty expandNavProperty = ((UriResourceNavigation) uriResource).getProperty();
                            return expandNavProperty.equals(navProperty);
                        } else {
                            return false;
                        }
                    }
                })
        );
        return isExpandItemFound;
    }

    public static <T> T fromEntity(Class entityClass, Entity entity) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchFieldException {
        Constructor constructor = entityClass.getConstructor();
        T obj = (T) constructor.newInstance();

        Field[] fields = entityClass.getDeclaredFields();
        for (Field field : fields) {

            if (EntityMetadataHelper.isNavigationProperty(field)) {
                Link navigationLink = entity.getNavigationLink(field.getName());
                if (!EntityDataHelper.isCollectionType(field.getType()) && navigationLink != null) {
                    invokeSetter(entityClass, obj, field, fromEntity(field.getType(), navigationLink.getInlineEntity()));
                }
            } else {
                Property property = entity.getProperty(field.getName());
                if (property == null) {
                    continue;
                }
                invokeSetter(entityClass, obj, field, property.getValue());
            }
        }
        return obj;
    }

    public static void addEntitiesToCollection(EntityCollection collection, List<ApplicationEntity> entities, Class entityClazz, EdmEntitySet entitySet, ExpandOption expandOption) {
        List<Entity> entityList = collection.getEntities();
        for (ApplicationEntity entity : entities) {
            entityList.add(EntityDataHelper.toEntity(entityClazz, entity, entitySet, expandOption));
        }
    }

    private static <T> void invokeSetter(Class entityClass, T obj, Field field, Object value) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        String setterName = "set" + StringUtils.capitalize(field.getName());
        Class fieldType = field.getType();
        Method setter = entityClass.getMethod(setterName, fieldType);
        if (setter != null) {
            setter.invoke(obj, value);
        }
    }

    private static Object invokeGetter(Class entityClazz, Object object, Field field) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        String getterName = "get" + StringUtils.capitalize(field.getName());
        Method getter = entityClazz.getMethod(getterName);
        return getter.invoke(object);
    }

    public static UriResourceEntitySet getUriResourceEntitySet(UriInfoResource uriInfo) throws ODataApplicationException {
        List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
        // To get the entity set we have to interpret all URI segments
        if (!(resourcePaths.get(0) instanceof UriResourceEntitySet)) {
            throw new ODataApplicationException("Invalid resource type for first segment.",
                    HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
        }
        return (UriResourceEntitySet) resourcePaths.get(0);
    }

    public static HashMap<String, Object> getKeyParamValue(Class entityClazz, List<UriParameter> keyParams) throws ODataApplicationException {
        HashMap<String, Object> params = new HashMap<>();
        for (UriParameter keyParam : keyParams) {
            String keyName = keyParam.getName();
            try {
                Field field = entityClazz.getDeclaredField(keyName);
                Class type = field.getType();
                String valueAsString = keyParam.getText().replace("'", "");
                if (type.equals(Integer.class) || type.getName().equals("int") || type.equals(Long.class) || type.getName().equals("long")) {
                    params.put(keyParam.getName(), Long.parseLong(valueAsString));
                    continue;
                } else if (type.equals(Double.class) || type.equals("double")) {
                    params.put(keyParam.getName(), Double.parseDouble(valueAsString));
                    continue;
                }
                params.put(keyParam.getName(), valueAsString);
            } catch (NoSuchFieldException e) {
                throw new ODataApplicationException(e.getLocalizedMessage(), HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);
            }
        }
        return params;
    }

    public static Method getStaticMethod(String clazzName, String methodName) throws ClassNotFoundException, ODataException {
        Class clazz = Thread.currentThread().getContextClassLoader().loadClass(clazzName);
        Method[] clazzMethods = clazz.getMethods();
        Optional<Method> clazzMethod = Arrays.stream(clazzMethods).filter(m -> m.getName().equals(methodName)).findFirst();
        if (clazzMethod.isPresent() && Modifier.isStatic(clazzMethod.get().getModifiers())) {
            return clazzMethod.get();

        } else {
            throw new ODataException("No static method: " + methodName + " is found in class: " + clazzName);
        }
    }

    public static boolean isCollectionType(Class type) {
        return type.equals(List.class) || type.equals(Map.class);
    }

    public static Type getParameterizedType(Type type) {
        Type parameterizedType = null;
        if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;
            parameterizedType = pt.getActualTypeArguments()[0];
        }
        return parameterizedType;
    }

    public static Class loadClass(String clazzName) throws ClassNotFoundException {
        return Thread.currentThread().getContextClassLoader().loadClass(clazzName);
    }

}
