package ppm.odataprovider.service.metadata;

public class EntityMetadataModel {

    private String entitySetName;
    private String entityType;
    private String entityClass;
    private String serviceClass;
    private String[] keys;

    public String getEntitySetName() {
        return entitySetName;
    }

    public void setEntitySetName(String entitySetName) {
        this.entitySetName = entitySetName;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getEntityClass() {
        return entityClass;
    }

    public void setEntityClass(String entityClass) { this.entityClass = entityClass; }

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
}
