package ppm.odataprovider.app.task;


import ppm.odataprovider.app.person.Person;
import ppm.odataprovider.data.ApplicationEntity;

import javax.persistence.*;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "tasks")
public class Task implements ApplicationEntity {

    @Id
    @Column(name="entity_id", nullable = false, insertable = false, updatable = false)
    private String entityId;

    @Column(name = "TASK_ID")
    private String taskId;
    @Column(name = "NAME", nullable = false)
    private String name;
    @Column(name = "DATE_CREATED", nullable = false, updatable = false)
    private Date dateCreated;
    @Column(name = "EARLY_START", nullable = false)
    private Date earlyStart;
    @Column(name = "EARLY_FINISH", nullable = false)
    private Date earlyFinish;
    @Column(name = "LATE_START", nullable = false)
    private Date lateStart;
    @Column(name = "LATE_FINISH", nullable = false)
    private Date lateFinish;
    @Column(name = "FREE_FLOAT", nullable = false)
    private double freeFloat;
    @Column(name = "TOTAL_FLOAT", nullable = false)
    private double totalFloat;

    @ManyToOne()
    @JoinColumn(name = "assignee", referencedColumnName = "entity_id")
    private Person assignee;

    @Override
    public void init() {
        this.entityId = UUID.randomUUID().toString();
        this.dateCreated = new Date();
    }

    public String getEntityId() {
        return entityId;
    }

    @Override
    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        if (dateCreated == null) {
            this.dateCreated = new Date();
        } else {
            this.dateCreated = dateCreated;
        }
    }

    public Date getEarlyStart() {
        return earlyStart;
    }

    public void setEarlyStart(Date earlyStart) {
        this.earlyStart = earlyStart;
    }

    public Date getEarlyFinish() {
        return earlyFinish;
    }

    public void setEarlyFinish(Date earlyFinish) {
        this.earlyFinish = earlyFinish;
    }

    public Date getLateStart() {
        return lateStart;
    }

    public void setLateStart(Date lateStart) {
        this.lateStart = lateStart;
    }

    public Date getLateFinish() {
        return lateFinish;
    }

    public void setLateFinish(Date lateFinish) {
        this.lateFinish = lateFinish;
    }

    public double getFreeFloat() {
        return freeFloat;
    }

    public void setFreeFloat(double freeFloat) {
        this.freeFloat = freeFloat;
    }

    public double getTotalFloat() {
        return totalFloat;
    }

    public void setTotalFloat(double totalFloat) {
        this.totalFloat = totalFloat;
    }

    public Person getAssignee() {
        return assignee;
    }

    public void setAssignee(Person assignee) {
        this.assignee = assignee;
    }
}
