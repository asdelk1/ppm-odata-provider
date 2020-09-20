package ppm.odataprovider.service.metadata;


import java.util.Map;

public class EntityOperationMetadataModel {
    private String name;
    private String entityClass;
    private String method;
    private Map<String, OperationParameterModel> params;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEntityClass() {
        return entityClass;
    }

    public void setEntityClass(String clazz) {
        this.entityClass = clazz;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Map<String, OperationParameterModel> getParams() {
        return params;
    }

    public void setParams(Map<String, OperationParameterModel> params) {
        this.params = params;
    }
}

