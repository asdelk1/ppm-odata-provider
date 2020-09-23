package ppm.odataprovider.app.person;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import ppm.odataprovider.app.task.Task;
import ppm.odataprovider.data.ApplicationEntity;

import javax.persistence.*;
import java.util.List;
import java.util.UUID;

@Entity()
@Table(name = "person")
public class Person implements ApplicationEntity {

    @Id
    @Column(name ="entity_id")
    private String entityId;

    @Column(name = "person_id", nullable = false, updatable = false)
    private String personId;
    @Column(name = "first_name", nullable = false)
    private String firstName;
    @Column(name = "last_name", nullable = false)
    private String lastName;
    @Column(name = "department", nullable = false)
    private String department;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "assignee")
    @Fetch(FetchMode.SELECT)
    private List<Task> tasks;

    @Override
    public void init() {
        entityId = UUID.randomUUID().toString();
    }

    @Override
    public String getEntityId() {
        return this.entityId;
    }

    @Override
    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getPersonId() {
        return this. personId;
    }

    public void setUserId(String personId) {
        this.personId = personId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public void setPersonId(String personId) {
        this.personId = personId;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
    }
}
