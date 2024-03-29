package ppm.odataprovider.service.metadata;

import java.util.Map;

public class EntityMetadataModel {
    private Map<String, String> entityset;
    private Map<String, EntityTypeMetadata> entities;
    private Map<String, EntityOperationMetadataModel> functions;
    private Map<String, EntityOperationMetadataModel> actions;


    public Map<String, String> getEntityset() {
        return entityset;
    }

    public void setEntityset(Map<String, String> entityset) {
        this.entityset = entityset;
    }

    public Map<String, EntityTypeMetadata> getEntities() {
        return entities;
    }

    public void setEntities(Map<String, EntityTypeMetadata> entities) {
        this.entities = entities;
    }

    public Map<String, EntityOperationMetadataModel> getFunctions() {
        return functions;
    }

    public void setFunctions(Map<String, EntityOperationMetadataModel> functions) {
        this.functions = functions;
    }

    public Map<String, EntityOperationMetadataModel> getActions() {
        return actions;
    }

    public void setActions(Map<String, EntityOperationMetadataModel> action) {
        this.actions = actions;
    }
}
