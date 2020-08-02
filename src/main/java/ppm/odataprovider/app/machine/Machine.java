package ppm.odataprovider.app.machine;

import ppm.odataprovider.app.task.Task;
import ppm.odataprovider.data.ApplicationEntity;

import javax.persistence.*;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Entity()
@Table(name = "machine")
public class Machine implements ApplicationEntity {

    @Id
    @Column(name = "entity_id")
    private String entityId;

    @Column(name = "machine_id")
    private String machineId;
    @Column(name = "max_Assignments")
    private int maxAssignments;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "machine")
    private List<Task> currentAssignments;

    @Column(name = "next_service_date")
    private Date nextServiceDate;

    @Override
    public void init() {
        entityId = UUID.randomUUID().toString();
    }

    public String getMachineId() {
        return machineId;
    }

    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }

    public int getMaxAssignments() {
        return maxAssignments;
    }

    public void setMaxAssignments(int maxAssignments) {
        this.maxAssignments = maxAssignments;
    }

    public int getCurrentAssignments(){
        return this.currentAssignments != null ? this.currentAssignments.size() : 0;
    }

    public Date getNextServiceDate() {
        return nextServiceDate;
    }

    public void setNextServiceDate(Date nextServiceDate) {
        this.nextServiceDate = nextServiceDate;
    }

    @Override
    public String getEntityId() {
        return this.entityId;
    }

    @Override
    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }
}
