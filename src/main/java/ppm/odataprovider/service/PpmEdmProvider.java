package ppm.odataprovider.service;

import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.*;
import org.apache.olingo.commons.api.ex.ODataException;
import ppm.odataprovider.data.EntityDataHelper;
import ppm.odataprovider.service.metadata.EntityMetadataHelper;
import ppm.odataprovider.service.metadata.EntityOperationMetadataModel;
import ppm.odataprovider.service.metadata.EntityTypeMetadata;
import ppm.odataprovider.service.metadata.OperationParameterModel;

import java.lang.reflect.*;
import java.util.*;

public class PpmEdmProvider extends CsdlAbstractEdmProvider {

    // Service Namespace
    private static final String NAMESPACE = "OData.Ppm";

    // EDM Container
    private static final String CONTAINER_NAME = "Project Container";
    private static final FullQualifiedName CONTAINER = new FullQualifiedName(NAMESPACE, CONTAINER_NAME);

    private EntityMetadataHelper entityMetadata;

    public PpmEdmProvider() {
        try {
            this.entityMetadata = EntityMetadataHelper.getInstance();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public CsdlEntityType getEntityType(FullQualifiedName entityTypeName) throws ODataException {
        try {
            CsdlEntityType entityType = new CsdlEntityType();
            List<CsdlProperty> properties = new ArrayList<>();
            List<CsdlPropertyRef> keyref = new ArrayList<>();
            List<CsdlNavigationProperty> navPropList = new ArrayList<>();
//            for (EntityTypeMetadata model : this.entityMetadata.getEntityMetadata()) {
            Optional<EntityTypeMetadata> entityTypeMetadataOptional = this.entityMetadata.getEntity(entityTypeName.getName());
            if (entityTypeMetadataOptional.isPresent()) {
                EntityTypeMetadata model = entityTypeMetadataOptional.get();
                Class entityClass = Thread.currentThread().getContextClassLoader().loadClass(model.getEntityClass());
                Field[] classFields = entityClass.getDeclaredFields();
                for (Field field : classFields) {
                    if (this.entityMetadata.isPrimitiveType(field.getType())) {
                        properties.add(new CsdlProperty()
                                .setName(field.getName())
                                .setType(this.entityMetadata.getODataPrimitiveDataType(field.getType().getName())));
                        keyref.add(new CsdlPropertyRef().setName("entityId"));
                    } else if (EntityMetadataHelper.isNavigationProperty(field)) {
                        boolean isCollection = EntityDataHelper.isCollectionType(field.getType());

                        Optional<String> navEntitySetOptional = isCollection ?
                                this.entityMetadata.getEntitySetForEntityClass(EntityDataHelper.getParameterizedType(field.getGenericType()).getTypeName())
                                : this.entityMetadata.getEntitySetForEntityClass(field.getType().getName());

                        if (navEntitySetOptional.isPresent()) {
                            String navEntityTypeName = this.entityMetadata.getEntitySetTypeName(navEntitySetOptional.get());
                            CsdlNavigationProperty navProp = new CsdlNavigationProperty()
                                    .setName(field.getName())
                                    .setType(new FullQualifiedName(PpmEdmProvider.NAMESPACE, navEntityTypeName))
                                    .setCollection(isCollection)
                                    .setNullable(false);
                            navPropList.add(navProp);

                        }
                    }
                }
                entityType.setName(entityTypeName.getName());
                entityType.setProperties(properties);
                entityType.setKey(keyref);
                entityType.setNavigationProperties(navPropList);
            }
//            }
            return entityType;
        } catch (Exception e) {
            throw new ODataException(e.getMessage());
        }
    }

    @Override
    public CsdlEntitySet getEntitySet(FullQualifiedName entityContainer, String entitySetName) {
        if (entityContainer.equals(CONTAINER)) {

            Optional<EntityTypeMetadata> optionalMetadata = this.entityMetadata.getEntitySetType(entitySetName);
            if (this.entityMetadata.isEntitySetExists(entitySetName) && optionalMetadata.isPresent()) {
                EntityTypeMetadata entityTypeMetadata = optionalMetadata.get();
                CsdlEntitySet entitySet = new CsdlEntitySet();
                entitySet.setName(entitySetName);
                entitySet.setType(new FullQualifiedName(NAMESPACE, this.entityMetadata.getEntitySetTypeName(entitySetName)));

                List<CsdlNavigationPropertyBinding> navPropBindingList = new ArrayList<>();
                Map<String, Type> navFields = entityTypeMetadata.getNavigationFields();
                for (Map.Entry<String, Type> entry : navFields.entrySet()) {
                    Optional<String> entitySetOptional = this.entityMetadata.getEntitySetForEntityClass(entry.getValue().getTypeName());
                    entitySetOptional.ifPresent(esName -> {
                        CsdlNavigationPropertyBinding navPropBinding = new CsdlNavigationPropertyBinding();
                        navPropBinding.setPath(entry.getKey());
                        navPropBinding.setTarget(esName);
                        navPropBindingList.add(navPropBinding);
                    });
                }
                if (!navPropBindingList.isEmpty()) {
                    entitySet.setNavigationPropertyBindings(navPropBindingList);
                }
                return entitySet;
            }
        }
        return null;
    }

    @Override
    public CsdlEntityContainerInfo getEntityContainerInfo(FullQualifiedName entityContainerName) {
        CsdlEntityContainerInfo entityContainerInfo = new CsdlEntityContainerInfo();
        entityContainerInfo.setContainerName(CONTAINER);
        return entityContainerInfo;
    }

    @Override
    public List<CsdlSchema> getSchemas() throws ODataException {
        CsdlSchema schema = new CsdlSchema();
        schema.setNamespace(NAMESPACE);
        List<CsdlEntityType> entityTypes = new ArrayList<>();
        for (Map.Entry<String, EntityTypeMetadata> entry : this.entityMetadata.getEntityTypes().entrySet()) {
            entityTypes.add(getEntityType(new FullQualifiedName(NAMESPACE, entry.getKey())));
        }
        schema.setEntityTypes(entityTypes);
        schema.setEntityContainer(getEntityContainer());
        List<CsdlSchema> schemaList = new ArrayList<>();

        // adding functions
        if (this.entityMetadata.getActionList() != null) {
            List<CsdlFunction> functions = new ArrayList<>();
            for (EntityOperationMetadataModel funcModel : this.entityMetadata.getFunctionList()) {
                functions.addAll(this.getFunctions(new FullQualifiedName(NAMESPACE, funcModel.getName())));
            }
            schema.setFunctions(functions);
        }

        // adding actions
        if(this.entityMetadata.getActionList() != null){
            List<CsdlAction> actions = new ArrayList<>();
            for (EntityOperationMetadataModel action: this.entityMetadata.getActionList()) {
                actions.addAll(this.getActions(new FullQualifiedName(NAMESPACE, action.getName())));
            }
            schema.setActions(actions);
        }

        schemaList.add(schema);
        return schemaList;
    }

    @Override
    public CsdlEntityContainer getEntityContainer() throws ODataException {
        // adding entity sets
        List<CsdlEntitySet> entitySets = new ArrayList<>();
        for (String entityset : this.entityMetadata.getEntitySets().keySet()) {
            entitySets.add(this.getEntitySet(CONTAINER, entityset));
        }
        CsdlEntityContainer entityContainer = new CsdlEntityContainer();
        entityContainer.setName(CONTAINER_NAME);
        entityContainer.setEntitySets(entitySets);

        // adding functions
        List<CsdlFunctionImport> functionImports = new ArrayList<>();
        for (EntityOperationMetadataModel funcModel : this.entityMetadata.getFunctionList()) {
            functionImports.add(getFunctionImport(new FullQualifiedName(NAMESPACE, CONTAINER_NAME), funcModel.getName()));
        }
        entityContainer.setFunctionImports(functionImports);

        // adding actions
        if (this.entityMetadata.getActionList() != null) {
            List<CsdlActionImport> actionImports = new ArrayList<>();
            for (EntityOperationMetadataModel actionModel : this.entityMetadata.getActionList()) {
                actionImports.add(getActionImport(new FullQualifiedName(NAMESPACE, CONTAINER_NAME), actionModel.getName()));
            }
            entityContainer.setActionImports(actionImports);
        }

        return entityContainer;
    }

    @Override
    public List<CsdlFunction> getFunctions(FullQualifiedName fqnFunctionName) throws ODataException {
        List<CsdlFunction> functionList = new ArrayList<>();
        String functionName = fqnFunctionName.getName();
        Optional<EntityOperationMetadataModel> operationMaybe = this.entityMetadata.getFunction(functionName);

        if (operationMaybe.isPresent()) {
            EntityOperationMetadataModel functionMetadata = operationMaybe.get();
            try {
                Method method = EntityDataHelper.getStaticMethod(functionMetadata.getEntityClass(), functionMetadata.getMethod());
                boolean isCollection = EntityDataHelper.isCollectionType(method.getReturnType());
                String returnTypeName = getMethodReturnEntityType(functionName, method);

                CsdlReturnType returnType = new CsdlReturnType();
                returnType.setCollection(isCollection);
                returnType.setType(new FullQualifiedName(NAMESPACE, returnTypeName));

                CsdlFunction csdlFunction = new CsdlFunction();
                csdlFunction.setName(functionName);

                if (functionMetadata.getParams() != null) {
                    List<CsdlParameter> csdlParameters = getCsdlParameters(functionMetadata.getParams());
                    csdlFunction.setParameters(csdlParameters);
                }
                csdlFunction.setReturnType(returnType);

                functionList.add(csdlFunction);
            } catch (ClassNotFoundException e) {
                throw new ODataException(e.getMessage());
            }
        }
        return functionList;
    }

    private String getMethodReturnEntityType(String functionName, Method method) throws ODataException {
        Type methodReturnType;
        String returnTypeName;
        if (EntityDataHelper.isCollectionType(method.getReturnType())) {
            methodReturnType = EntityDataHelper.getParameterizedType(method.getGenericReturnType());

        } else {
            methodReturnType = method.getGenericReturnType();
        }

        Optional<String> returnEntityName = this.entityMetadata.getEntityForEntityClass(methodReturnType.getTypeName());
        if (returnEntityName.isEmpty()) {
            throw new ODataException("Invalid return type for function" + functionName);
        }
        returnTypeName = returnEntityName.get();
        return returnTypeName;
    }

    @Override
    public List<CsdlAction> getActions(FullQualifiedName actionName) throws ODataException {
        String name = actionName.getName();
        List<CsdlAction> actionList = new ArrayList<>();
        Optional<EntityOperationMetadataModel> actionMaybe = this.entityMetadata.getAction(name);

        if (actionMaybe.isPresent()) {
            EntityOperationMetadataModel actionMetadata = actionMaybe.get();
            try {
                Method actionMethod = EntityDataHelper.getStaticMethod(actionMetadata.getEntityClass(), actionMetadata.getMethod());
                boolean isCollection = EntityDataHelper.isCollectionType(actionMethod.getReturnType());
                String returnTypeName = getMethodReturnEntityType(name, actionMethod);

                CsdlReturnType returnType = new CsdlReturnType();
                returnType.setCollection(isCollection);
                returnType.setType(new FullQualifiedName(NAMESPACE, returnTypeName));

                CsdlAction csdlAction = new CsdlAction();
                csdlAction.setName(name);

                if (actionMetadata.getParams() != null) {
                    List<CsdlParameter> csdlParameters = getCsdlParameters(actionMetadata.getParams());
                    csdlAction.setParameters(csdlParameters);
                }
                csdlAction.setReturnType(returnType);
                actionList.add(csdlAction);
            } catch (ClassNotFoundException e) {
                throw new ODataException(e.getMessage());
            }
        }
        return actionList;
    }

    private List<CsdlParameter> getCsdlParameters(Map<String, OperationParameterModel> params) {
        List<CsdlParameter> csdlParameters = new ArrayList<>();
        for (Map.Entry<String, OperationParameterModel> param : params.entrySet()) {

            CsdlParameter csdlParameter = new CsdlParameter();
            csdlParameter.setName(param.getKey());
            csdlParameter.setNullable(false);
            csdlParameter.setType(this.entityMetadata.getODataPrimitiveDataType(param.getValue().getType()));
            csdlParameters.add(csdlParameter);
        }
        return csdlParameters;
    }

    @Override
    public CsdlFunctionImport getFunctionImport(FullQualifiedName entityContainer, String functionImportName) throws ODataException {
        Optional<EntityOperationMetadataModel> function = this.entityMetadata.getFunction(functionImportName);
        if (function.isPresent()) {
            EntityOperationMetadataModel model = function.get();
            return new CsdlFunctionImport().setName(model.getName())
                    .setFunction(new FullQualifiedName(NAMESPACE, model.getName()))
                    .setEntitySet("Persons")
                    .setIncludeInServiceDocument(true);
        } else {
            return null;
        }
    }

    @Override
    public CsdlActionImport getActionImport(FullQualifiedName entityContainer, String actionImportName) throws ODataException {
        Optional<EntityOperationMetadataModel> action = this.entityMetadata.getAction(actionImportName);
        if (action.isPresent()) {
            EntityOperationMetadataModel model = action.get();
            return new CsdlActionImport().setName(model.getName())
                    .setAction(new FullQualifiedName(NAMESPACE, model.getName()));
        } else {
            return null;
        }
    }
}
