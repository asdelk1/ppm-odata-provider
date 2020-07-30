package ppm.odataprovider.data;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.hibernate.criterion.Criterion;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;


public abstract class PpmODataGenericService {

    public List<ApplicationEntity> getAll(Class<?> entityClazz) {
        return EntityRepository.findAll(entityClazz);
    }

    public Optional<List<ApplicationEntity>> find(Class<?> entityClazz, Criterion filter){
        return EntityRepository.find(entityClazz, filter);
    }

    public Optional<ApplicationEntity> getEntity(Class<?> entityClazz, Map<String, Object> params) {
        Optional<List<ApplicationEntity>> resultSet = EntityRepository.find(entityClazz, params);
        if (resultSet.isPresent()) {
            List<ApplicationEntity> result = resultSet.get();
            return Optional.of(result.get(0));
        } else {
            return Optional.empty();
        }
    }

    public ApplicationEntity saveEntity(ApplicationEntity newAppEntity) throws ODataApplicationException {
        try {
            newAppEntity.init();
            if (EntityRepository.get(newAppEntity.getClass(), newAppEntity.getEntityId()).isPresent()) {
                throw new ODataApplicationException("Entity already exists", HttpStatusCode.CONFLICT.getStatusCode(), Locale.ENGLISH);
            }
            return EntityRepository.save(newAppEntity);
        } catch (Exception e) {
            throw new ODataApplicationException(e.getMessage(), HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);
        }
    }

    public void updateEntity(ApplicationEntity entity) {
        EntityRepository.update(entity);
    }

    public void deleteEntity(ApplicationEntity entity) {
        EntityRepository.delete(entity);
    }
}
