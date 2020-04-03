package ppm.odataprovider.service.metadata;

import com.google.gson.Gson;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlNavigationProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlPropertyRef;
import ppm.odataprovider.service.PpmEdmProvider;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.util.*;

public class EntityMetadataHelper {

    public static CsdlEntityType getEntityType(FullQualifiedName entityTypeName) throws IOException, ClassNotFoundException, URISyntaxException {
        EntityMetadataModel[] edm = readMetadataJSON();
        CsdlEntityType entityType = new CsdlEntityType();
        List<CsdlProperty> properties = new ArrayList<>();
        List<CsdlPropertyRef> keyrefs = new ArrayList<>();
        List<CsdlNavigationProperty> navPropList = new ArrayList<>();
        for (EntityMetadataModel model : edm) {
            FullQualifiedName modelFqn = new FullQualifiedName(PpmEdmProvider.NAMESPACE, model.getEntityType());
            if (modelFqn.equals(entityTypeName)) {
                Class entityClass = Thread.currentThread().getContextClassLoader().loadClass(model.getEntityClass());
                Field[] classFields = entityClass.getDeclaredFields();
                for (Field field : classFields) {
                    if (isPrimitiveType(field.getType())) {

                        properties.add(new CsdlProperty()
                                .setName(field.getName())
                                .setType(getODataPrimitiveDataType(field.getType().getName())));

                        Optional<String> keyProperty = Arrays.stream(model.getKeys())
                                .filter(k -> k.equals(field.getName()))
                                .findFirst();

                        if (keyProperty.isPresent()) {
                            keyrefs.add(new CsdlPropertyRef().setName(keyProperty.get()));
                        }

                    } else {
                        Optional<EntityMetadataModel> edmOptional = getEntityMetadataModel(edm, field.getType().getName());
                        edmOptional.ifPresent(navModel -> {
                            CsdlNavigationProperty navProp = new CsdlNavigationProperty()
                                    .setName(field.getName())
                                    .setType(new FullQualifiedName(PpmEdmProvider.NAMESPACE, navModel.getEntityType()))
                                    .setNullable(false)
                                    .setPartner(model.getEntitySetName());
                            navPropList.add(navProp);
                        });
                    }
                }
                entityType.setName(model.getEntityType());
                entityType.setProperties(properties);
                entityType.setKey(keyrefs);
                entityType.setNavigationProperties(navPropList);
            } else {
                continue;
            }
        }
        return entityType;
    }

    public static EntityMetadataModel[] readMetadataJSON() throws IOException, URISyntaxException {
        Gson gson = new Gson();
        EntityMetadataModel[] edm;
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        File jsonFile = new File(classLoader.getResource("es.json").toURI());
        try (FileReader reader = new FileReader(jsonFile)) {
            edm = gson.fromJson(reader, EntityMetadataModel[].class);
        }
        return edm;
    }

    private static FullQualifiedName getODataPrimitiveDataType(String type) {
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

    private static boolean isPrimitiveType(Class type) {
        boolean isPrimitive = false;
        if (type.equals(Integer.TYPE) ||
                type.equals(Long.TYPE) ||
                type.equals(Double.TYPE) ||
                type.equals(Date.class) ||
                type.equals(String.class)){
            isPrimitive = true;
        }
        return isPrimitive;
    }

    private static Optional<EntityMetadataModel> getEntityMetadataModel(EntityMetadataModel[] edm, String name){
        return Arrays.stream(edm).filter(model -> model.getEntityClass().equals(name)).findFirst();
    }
}

