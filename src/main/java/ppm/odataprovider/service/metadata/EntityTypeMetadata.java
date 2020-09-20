package ppm.odataprovider.service.metadata;

import ppm.odataprovider.data.EntityDataHelper;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class EntityTypeMetadata {

    private String entityClass;
    private String serviceClass;
    private String[] keys;


    public String getEntityClass() {
        return entityClass;
    }

    public void setEntityClass(String entityClass) {
        this.entityClass = entityClass;
    }

    public String[] getKeys() {
        return keys;
    }

    public void setKeys(String[] keys) {
        this.keys = keys;
    }

    public String getServiceClass() {
        return this.serviceClass;
    }

    public void setServiceClass(String serviceClass) {
        this.serviceClass = serviceClass;
    }

    public Map<String, Type> getNavigationFields() {
        Map<String, Type> navFields = new HashMap<>();

        try {
            Class entityClass = Thread.currentThread().getContextClassLoader().loadClass(this.getEntityClass());
            for (Field field : entityClass.getDeclaredFields()) {
                if (EntityMetadataHelper.isNavigationProperty(field)) {
                    if (EntityDataHelper.isCollectionType(field.getType())) {
                        navFields.put(field.getName(), EntityDataHelper.getParameterizedType(field.getGenericType()));
                    } else {
                        navFields.put(field.getName(), field.getType());
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return navFields;
    }

}
