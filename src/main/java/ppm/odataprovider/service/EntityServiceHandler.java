package ppm.odataprovider.service;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.List;

public class EntityServiceHandler {

    public EntityCollection getData(EdmEntitySet edmEntitySet){

        EntityCollection tasksCollection = new EntityCollection();
        // check for which EdmEntitySet the data is requested
        if(PpmEdmProvider.ES_TASKS_NAME.equals(edmEntitySet.getName())) {
            List<Entity> taskList = tasksCollection.getEntities();

            // add some sample product entities
            final Entity e1 = new Entity()
                    .addProperty(new Property(null, "TaskId", ValueType.PRIMITIVE, 1))
                    .addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "Task 1"))
                    .addProperty(new Property(null, "EarlyStart", ValueType.PRIMITIVE,  LocalDateTime.of(2020,1,11,8,0)))
                    .addProperty(new Property(null, "EarlyFinish", ValueType.PRIMITIVE,  LocalDateTime.of(2020,1,11,17,0)))
                    .addProperty(new Property(null, "LateStart", ValueType.PRIMITIVE,  LocalDateTime.of(2020,1,11,8,0)))
                    .addProperty(new Property(null, "LateFinish", ValueType.PRIMITIVE,  LocalDateTime.of(2020,1,11,17,0)))
                    .addProperty(new Property(null, "FreeFloat", ValueType.PRIMITIVE, 0))
                    .addProperty(new Property(null, "TotalFloat", ValueType.PRIMITIVE,  0))
                    .addProperty(new Property(null, "Assignee", ValueType.PRIMITIVE,  "JACKIE"));
            e1.setId(createId("Tasks", 1));
            taskList.add(e1);

            final Entity e2 = new Entity()
                    .addProperty(new Property(null, "TaskId", ValueType.PRIMITIVE, 2))
                    .addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "Task 1"))
                    .addProperty(new Property(null, "EarlyStart", ValueType.PRIMITIVE,  LocalDateTime.of(2020,1,12,8,0)))
                    .addProperty(new Property(null, "EarlyFinish", ValueType.PRIMITIVE,  LocalDateTime.of(2020,1,12,17,0)))
                    .addProperty(new Property(null, "LateStart", ValueType.PRIMITIVE,  LocalDateTime.of(2020,1,12,8,0)))
                    .addProperty(new Property(null, "LateFinish", ValueType.PRIMITIVE,  LocalDateTime.of(2020,1,12,17,0)))
                    .addProperty(new Property(null, "FreeFloat", ValueType.PRIMITIVE, 0))
                    .addProperty(new Property(null, "TotalFloat", ValueType.PRIMITIVE,  0))
                    .addProperty(new Property(null, "Assignee", ValueType.PRIMITIVE,  "JACKIE"));
            e1.setId(createId("Tasks", 2));
            taskList.add(e2);
        }

        return tasksCollection;
    }

    private URI createId(String entitySetName, Object id) {
        try {
            return new URI(entitySetName + "(" + String.valueOf(id) + ")");
        } catch (URISyntaxException e) {
            throw new ODataRuntimeException("Unable to create id for entity: " + entitySetName, e);
        }
    }

}
