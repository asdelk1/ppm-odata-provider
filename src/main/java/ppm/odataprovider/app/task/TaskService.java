package ppm.odataprovider.app.task;

import org.apache.olingo.server.api.ODataApplicationException;
import ppm.odataprovider.data.ApplicationEntity;
import ppm.odataprovider.data.PpmODataGenericService;

public class TaskService extends PpmODataGenericService {

    @Override
    public ApplicationEntity saveEntity(ApplicationEntity newAppEntity) throws ODataApplicationException {
        Task task = (Task) newAppEntity;

        task.setLateStart(task.getEarlyStart());
        task.setLateFinish(task.getEarlyFinish());
        task.setFreeFloat(0);
        task.setTotalFloat(0);

        return super.saveEntity(task);
    }
}
