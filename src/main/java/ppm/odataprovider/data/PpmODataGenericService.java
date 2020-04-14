package ppm.odataprovider.data;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;


public abstract class PpmODataGenericService {

    public List<ApplicationEntity> getAll(Class entityClazz) {
        return EntityRepository.findAll(entityClazz);
    }

    public Optional<ApplicationEntity> getEntity(Class entityClazz, Map<String, Object> params) {
        Optional<List<ApplicationEntity>> resultSet = EntityRepository.find(entityClazz, params);
        if (resultSet.isPresent()) {
            List<ApplicationEntity> result = resultSet.get();
            return Optional.of(result.get(0));
        } else {
            return Optional.empty();
        }
    }

    public Entity saveEntity(EdmEntitySet edmEntitySet, Class entityClazz, Entity entity) throws ODataApplicationException {
        Entity createdEntity;
        try {
            ApplicationEntity createdObject = EntityDataHelper.fromEntity(entityClazz, entity);
            createdObject.init();
            if (EntityRepository.get(createdObject.getClass(), createdObject.getEntityId()).isPresent()) {
                throw new ODataApplicationException("Entity already exists", HttpStatusCode.CONFLICT.getStatusCode(), Locale.ENGLISH);
            }
            ApplicationEntity resultEntity = EntityRepository.save(createdObject);
            createdEntity = EntityDataHelper.toEntity(createdObject.getClass(), resultEntity, edmEntitySet.getName());
        } catch (Exception e) {
            throw new ODataApplicationException(e.getMessage(), HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);
        }
        return createdEntity;
    }

    public void updateEntity(ApplicationEntity entity) {
        EntityRepository.update(entity);
    }

    public void deleteEntity(ApplicationEntity entity) {
        EntityRepository.delete(entity);
    }
}
