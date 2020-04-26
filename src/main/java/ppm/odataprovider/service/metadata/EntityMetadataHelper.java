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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.util.*;

public class EntityMetadataHelper {

    private EntityMetadataModel[] entityMetadata;

    public EntityMetadataHelper() throws IOException, URISyntaxException {
        Gson gson = new Gson();
        EntityMetadataModel[] edm;
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        File jsonFile = new File(Objects.requireNonNull(classLoader.getResource("es.json")).toURI());
        try (FileReader reader = new FileReader(jsonFile)) {
            this.entityMetadata = gson.fromJson(reader, EntityMetadataModel[].class);
        }

    }

    public EntityMetadataModel[] getEntityMetadata() {
        return entityMetadata;
    }

    public Optional<EntityMetadataModel> getEntitySetMetadata(String entitySetName) {
        return Arrays.stream(this.entityMetadata).filter(entityMetadataModel -> entityMetadataModel.getEntitySetName().equals(entitySetName))
                .findFirst();
    }

    public FullQualifiedName getODataPrimitiveDataType(String type) {
        EdmPrimitiveTypeKind edmPrimitiveTypeKind = EdmPrimitiveTypeKind.String;

        if (type.equals(Integer.class.getName()) || type.equals("int") || type.equals(Long.class.getName()) || type.equals("long")) {
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
        if (type.equals(Integer.TYPE) ||
                type.equals(Long.TYPE) ||
                type.equals(Double.TYPE) ||
                type.equals(Date.class) ||
                type.equals(String.class)) {
            isPrimitive = true;
        }
        return isPrimitive;
    }

    public static boolean isNavigationProperty(Field field) {
        Annotation[] fieldAnnotations = field.getDeclaredAnnotations();
        return Arrays.stream(fieldAnnotations).anyMatch(a -> {
                    Class annotationClass = a.annotationType();
                    return annotationClass.equals(OneToMany.class) ||
                            annotationClass.equals(ManyToOne.class) ||
                            annotationClass.equals(OneToOne.class) ||
                            annotationClass.equals(ManyToMany.class);
                }
        );
    }

    public static boolean isCollectionType(Class type) {
        return type.equals(List.class) || type.equals(Map.class);
    }

    public Optional<EntityMetadataModel> getEntityMetadataModelByEntitySet(String entitySetName) {
        return Arrays.stream(this.entityMetadata).filter(model -> model.getEntitySetName().equals(entitySetName)).findFirst();
    }

    public Optional<EntityMetadataModel> getEntityMetadataModelByEntityClass(String entityClassName) {
        return Arrays.stream(this.entityMetadata).filter(model -> model.getEntityClass().equals(entityClassName)).findFirst();
    }

    public Type getParameterizedType(Field field) {
        Type parameterizedType = null;
        Type type = field.getGenericType();
        if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;
            parameterizedType = pt.getActualTypeArguments()[0];
        }
        return parameterizedType;
    }

    public Map<String, Type> getEntitySetNavigationField(String entitySetName) {
        Map<String, Type> navFields = new HashMap<>();
        this.getEntitySetMetadata(entitySetName).ifPresent(metadata -> {
            try {
                Class entityClass = Thread.currentThread().getContextClassLoader().loadClass(metadata.getEntityClass());
                for (Field field : entityClass.getDeclaredFields()) {
                    if (this.isNavigationProperty(field)) {
                        if (this.isCollectionType(field.getType())) {
                            navFields.put(field.getName(), this.getParameterizedType(field));
                        } else {
                            navFields.put(field.getName(), field.getType());
                        }
                    }
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        });
        return navFields;
    }

    public Optional<String> getEntitySetForEntityClass(String className) {
        Optional<EntityMetadataModel> entityMetadataOptional = Arrays.stream(this.entityMetadata).filter(metadata -> metadata.getEntityClass().equals(className)).findFirst();
        if (entityMetadataOptional.isPresent()) {
            return Optional.of(entityMetadataOptional.get().getEntitySetName());
        } else {
            return Optional.empty();
        }
    }

    public Class getEntityClass(EdmEntityType edmEntityType) throws ClassNotFoundException {
        Class entityClazz = null;
        Optional<EntityMetadataModel> dataModelOptional = Arrays.stream(this.entityMetadata).filter(m -> m.getEntityType().equals(edmEntityType.getName())).findFirst();
        if (dataModelOptional.isPresent()) {
            EntityMetadataModel metadataModel = dataModelOptional.get();
            entityClazz = Thread.currentThread().getContextClassLoader().loadClass(metadataModel.getEntityClass());
        }
        return entityClazz;
    }

    public PpmODataGenericService getServiceClass(EdmEntitySet entitySet) throws Exception {
        PpmODataGenericService service = null;
        Optional<EntityMetadataModel> dataModelOptional = Arrays.stream(this.entityMetadata).filter(m -> m.getEntitySetName().equals(entitySet.getName())).findFirst();
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

