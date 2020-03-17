package ppm.odataprovider.app.task;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.edm.*;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriParameter;
import ppm.odataprovider.data.DataRepository;
import ppm.odataprovider.data.EntityDataHelper;
import ppm.odataprovider.data.PpmODataGenericService;

import java.util.List;
import java.util.Locale;

public class TaskService implements PpmODataGenericService {


    @Override
    public EntityCollection getAll(String entitySetName) {
        EntityCollection entityCollection = new EntityCollection();
        List<Entity> entityList = entityCollection.getEntities();
        List<Task> taskList = this.getAllTasks();
        for (Task task : taskList) {
            Entity entity = EntityDataHelper.toEntity(Task.class, task, entitySetName);
            entityList.add(entity);
        }
        return entityCollection;
    }

    @Override
    public Entity getEntity(EdmEntityType edmEntityType, EdmEntitySet edmEntitySet, List<UriParameter> keyParams) throws EdmPrimitiveTypeException {
        if (keyParams.isEmpty()) {
            return null;
        }
        Entity taskEntity = new Entity();
        String keyName = keyParams.get(0).getName();
        String keyValue = keyParams.get(0).getText();

        EdmProperty keyProperty = (EdmProperty) edmEntityType.getProperty(keyName);
        EdmPrimitiveType keyType = (EdmPrimitiveType) keyProperty.getType();
        Long id = keyType.valueOfString(keyValue, keyProperty.isNullable(), keyProperty.getMaxLength(), keyProperty.getPrecision(), keyProperty.getScale(), keyProperty.isUnicode(), Long.class);
        Task task = this.getTask(id);
        if (task != null) {
            taskEntity = EntityDataHelper.toEntity(Task.class, task, edmEntitySet.getName());
        }
        return taskEntity;
    }

    @Override
    public Entity saveEntity(EdmEntityType entityType, EdmEntitySet edmEntitySet, Entity entity) throws ODataApplicationException {
        Entity createdEntity = null;
        try {
            Task newTask = EntityDataHelper.fromEntity(entityType, entity, Task.class);
            if (this.getTask(newTask.getTaskId()) != null) {
                throw new ODataApplicationException("Entity already exists", HttpStatusCode.CONFLICT.getStatusCode(), Locale.ENGLISH);
            }
            Task createdTask = this.saveTask(newTask);
            createdEntity = EntityDataHelper.toEntity(Task.class, createdTask, edmEntitySet.getName());
        } catch (Exception e) {
            throw new ODataApplicationException(e.getMessage(), HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);
        }
        return createdEntity;
    }


    public List<Task> getAllTasks() {
        return  DataRepository.getAll(Task.class);
    }

    public Task getTask(Long id) {
        return DataRepository.get(Task.class, "taskId", id);
    }

    public Task saveTask(Task newTask) {
        return DataRepository.save(Task.class, newTask);
    }
}
