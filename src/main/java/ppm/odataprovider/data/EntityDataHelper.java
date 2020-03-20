package ppm.odataprovider.data;

import org.apache.commons.lang3.StringUtils;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;

import javax.persistence.Id;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;

public class EntityDataHelper {

    public static Entity toEntity(Class<ppm.odataprovider.app.task.Task> dataClass, Object object, String entitySetName) {
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

    public static <T> T fromEntity(EdmEntityType entityType, Entity entity, Class entityClass) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchFieldException {
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

    private static URI createId(String entitySetName, Object id) {
        try {
            return new URI(entitySetName + "(" + String.valueOf(id) + ")");
        } catch (URISyntaxException e) {
            throw new ODataRuntimeException("Unable to create id for entity: " + entitySetName, e);
        }
    }
}
