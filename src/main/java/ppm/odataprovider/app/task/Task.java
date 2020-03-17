package ppm.odataprovider.app.task;


import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "task")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="TASK_ID")
    private long taskId;
    @Column(name = "NAME", nullable = false)
    private String name;
    @Column(name = "DATE_CREATED", nullable = false, updatable = false, insertable = false)
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
    @Column(name = "TOTAL_FLOAT")
    private double totalFloat;
    @Column(name = "ASSIGNEE", nullable = false)
    private String assignee;

    public Task(){
        this.dateCreated = new Date();
    }

//    public Task(String name, Date dateCreated, Date earlyStart, Date earlyFinish, Date lateStart, Date lateFinish, double freeFloat, double totalFloat, String assignee) {
//        this.name = name;
//        this.dateCreated = dateCreated;
//        this.earlyStart = earlyStart;
//        this.earlyFinish = earlyFinish;
//        this.lateStart = lateStart;
//        this.lateFinish = lateFinish;
//        this.freeFloat = freeFloat;
//        this.totalFloat = totalFloat;
//        this.assignee = assignee;
//    }

    public long getTaskId() {
        return taskId;
    }

    public void setTaskId(long taskId) {
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
        if(dateCreated == null){
            dateCreated = new Date();
        }else {
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

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }
}
