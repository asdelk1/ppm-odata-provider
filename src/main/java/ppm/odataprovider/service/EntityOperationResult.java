package ppm.odataprovider.service;

import ppm.odataprovider.data.ApplicationEntity;

import java.util.List;
import java.util.Optional;

public class EntityOperationResult {

    private boolean voidReturn;
    private Optional<List<ApplicationEntity>> data;
    private Optional<Class> entityClazz;

    public EntityOperationResult(boolean voidReturn, List<ApplicationEntity> data, Class entityClazz) {
        this.voidReturn = voidReturn;
        this.data = data != null && data.size() > 0 ? Optional.of(data) : Optional.empty();
        this.entityClazz = entityClazz != null ? Optional.of(entityClazz) : Optional.empty();
    }

    public boolean isVoid(){
        return this.voidReturn;
    }

    public Optional<List<ApplicationEntity>> getData() {
        return data;
    }

    public Optional<Class> getEntityClazz() {
        return entityClazz;
    }
}
