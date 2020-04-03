package ppm.odataprovider.data;

import org.apache.commons.lang3.StringUtils;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriInfoResource;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;

import javax.persistence.Id;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class EntityDataHelper {

    public static Entity toEntity(Class dataClass, Object object, String entitySetName) {
        Entity entity = new Entity();
        URI id = null;
        for (Field field : dataClass.getDeclaredFields()) {
            String getterName = "get" + StringUtils.capitalize(field.getName());
            try {
                Annotation keyAnnotation = field.getAnnotation(Id.class);

                Method getter = dataClass.getMethod(getterName);
                Object value = getter.invoke(object);
                Property property = new Property(null, field.getName(), ValueType.PRIMITIVE, value);
                entity.addProperty(property);
                if (keyAnnotation != null) {
                    id = createId(entitySetName, value);
                }
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        entity.setId(id);

        return entity;
    }

    public static <T> T fromEntity(Class entityClass, Entity entity) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchFieldException {
        Constructor constructor = entityClass.getConstructor();
        T obj = (T) constructor.newInstance();

        for (Property property : entity.getProperties()) {
            String setterName = "set" + StringUtils.capitalize(property.getName());
            Field field = entityClass.getDeclaredField(property.getName());
            Class fieldType = field.getType();
            Method setter = entityClass.getMethod(setterName, fieldType);
            if (setter != null) {
//                throw new NoSuchMethodException(String.format("%s method not found", setterName));
                setter.invoke(obj, property.getValue());
            }
        }
        return obj;
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
                if (type.equals(Integer.class.getName()) || type.equals("int") || type.equals(Long.class.getName()) || type.equals("long")) {
                    params.put(keyParam.getName(), Long.parseLong(valueAsString));
                    continue;
                } else if (type.equals(Double.class.getName()) || type.equals("double")) {
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

    private static URI createId(String entitySetName, Object id) {
        try {
            return new URI(entitySetName + "(" + String.valueOf(id) + ")");
        } catch (URISyntaxException e) {
            throw new ODataRuntimeException("Unable to create id for entity: " + entitySetName, e);
        }
    }
}
