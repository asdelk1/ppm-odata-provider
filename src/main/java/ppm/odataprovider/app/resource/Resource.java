package ppm.odataprovider.app.resource;

import ppm.odataprovider.data.ApplicationEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.UUID;

@Entity()
@Table(name = "resource")
public class Resource implements ApplicationEntity {

    @Id
    @Column(name = "entity_id")
    private String entityId;

    @Column(name = "resource_id")
    private String resourceId;

    @Column(name = "type")
    private String type;

    @Column(name = "quantity")
    private double quantity;

    @Column(name = "uom")
    private String uom;

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

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public String getUom() {
        return uom;
    }

    public void setUom(String uom) {
        this.uom = uom;
    }
}
