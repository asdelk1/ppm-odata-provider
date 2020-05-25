package ppm.odataprovider.service.metadata;

import com.google.gson.Gson;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import ppm.odataprovider.data.PpmODataGenericService;

import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.util.*;

public class EntityMetadataHelper {

    private static EntityMetadataHelper instance = null;

    private EntityMetadataModel edm;

    private EntityMetadataHelper() throws IOException, URISyntaxException {
        Gson gson = new Gson();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        File jsonFile = new File(Objects.requireNonNull(classLoader.getResource("es.json")).toURI());
        try (FileReader reader = new FileReader(jsonFile)) {
            this.edm = gson.fromJson(reader, EntityMetadataModel.class);
        }

    }

    public static EntityMetadataHelper getInstance() throws IOException, URISyntaxException {
        if (instance == null) {
            instance = new EntityMetadataHelper();
        }
        return instance;
    }

    public static boolean isNavigationProperty(Field field) {
        Annotation[] fieldAnnotations = field.getDeclaredAnnotations();
        return Arrays.stream(fieldAnnotations).anyMatch(a -> {
            Class annotationClass = a.annotationType();
            return annotationClass.equals(OneToMany.class) || annotationClass.equals(ManyToOne.class)
                    || annotationClass.equals(OneToOne.class) || annotationClass.equals(ManyToMany.class);
        });
    }

    public static boolean isCollectionType(Class type) {
        return type.equals(List.class) || type.equals(Map.class);
    }

    public static Type getParameterizedType(Field field) {
        Type parameterizedType = null;
        Type type = field.getGenericType();
        if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;
            parameterizedType = pt.getActualTypeArguments()[0];
        }
        return parameterizedType;
    }

    public Map<String, String> getEntitySets() {
        return this.edm.getEntityset();
    }

    public boolean isEntitySetExists(String entitySetName) {
        return this.edm.getEntityset().containsKey(entitySetName);
    }

    public String getEntitySetTypeName(String entitySetName) {
        String typeName = null;
        if (this.edm.getEntityset().containsKey(entitySetName) && this.edm.getEntities().containsKey(this.edm.getEntityset().get(entitySetName))) {
            typeName = this.edm.getEntityset().get(entitySetName);
        }
        return typeName;
    }

    public Optional<EntityTypeMetadata> getEntitySetType(String entitySetName) {
        if (this.edm.getEntityset().containsKey(entitySetName) && this.edm.getEntities().containsKey(this.edm.getEntityset().get(entitySetName))) {
            EntityTypeMetadata metadata = this.edm.getEntities().get(this.edm.getEntityset().get(entitySetName));
            return Optional.of(metadata);
        }
        return Optional.empty();
    }

    public Map<String, EntityTypeMetadata> getEntityTypes() {
        return this.edm.getEntities();
    }

    public Optional<EntityTypeMetadata> getEntity(String entityName) {
        Map<String, EntityTypeMetadata> entities = this.edm.getEntities();
        if (entities.containsKey(entityName)) {
            return Optional.of(entities.get(entityName));
        } else {
            return Optional.empty();
        }
    }

    public FullQualifiedName getODataPrimitiveDataType(String type) {
        EdmPrimitiveTypeKind edmPrimitiveTypeKind = EdmPrimitiveTypeKind.String;

        if (type.equals(Integer.class.getName()) || type.equals("int") || type.equals(Long.class.getName())
                || type.equals("long")) {
            edmPrimitiveTypeKind = EdmPrimitiveTypeKind.Int32;
        } else if (type.equals(Double.class.getName()) || type.equals("double")) {
            edmPrimitiveTypeKind = EdmPrimitiveTypeKind.Double;
        } else if (type.equals(Date.class.getName())) {
            edmPrimitiveTypeKind = EdmPrimitiveTypeKind.DateTimeOffset;
        }
        return edmPrimitiveTypeKind.getFullQualifiedName();
    }


    public boolean isPrimitiveType(Class type) {
        boolean isPrimitive = false;
        if (type.equals(Integer.TYPE) || type.equals(Long.TYPE) || type.equals(Double.TYPE) || type.equals(Date.class)
                || type.equals(String.class)) {
            isPrimitive = true;
        }
        return isPrimitive;
    }

    public Optional<String> getEntitySetForEntityClass(String className) {
        String entityTypeName = null;
        for (Map.Entry<String, EntityTypeMetadata> edmEntry : this.edm.getEntities().entrySet()) {

            EntityTypeMetadata metadata = edmEntry.getValue();
            if (metadata.getEntityClass().equals(className)) {
                entityTypeName = edmEntry.getKey();
                break;
            }
        }

        if (entityTypeName != null) {
            for (Map.Entry<String, String> esEntry : this.edm.getEntityset().entrySet()) {
                if (esEntry.getValue().equals(entityTypeName)) {
                    return Optional.of(esEntry.getKey());
                }
            }
            return Optional.empty();
        }
        return Optional.empty();
    }

    public Class getEntityClass(EdmEntityType edmEntityType) throws ClassNotFoundException {
        Class entityClazz = null;
        Optional<EntityTypeMetadata> entityMetadata = this.getEntity(edmEntityType.getName());
        if (entityMetadata.isPresent()) {
            EntityTypeMetadata metadataModel = entityMetadata.get();
            entityClazz = Thread.currentThread().getContextClassLoader().loadClass(metadataModel.getEntityClass());
        }
        return entityClazz;
    }

    public PpmODataGenericService getServiceClass(EdmEntitySet entitySet) throws Exception {
        PpmODataGenericService service = null;
        Optional<EntityTypeMetadata> entityTypeMetadataOptional = this.getEntitySetType(entitySet.getName());
        if (entityTypeMetadataOptional.isPresent()) {
            EntityTypeMetadata entityTypeMetadata = entityTypeMetadataOptional.get();
            Class serviceClass = Thread.currentThread().getContextClassLoader().loadClass(entityTypeMetadata.getServiceClass());
            service = (PpmODataGenericService) serviceClass.getConstructor().newInstance();
        }
        return service;
    }
}
