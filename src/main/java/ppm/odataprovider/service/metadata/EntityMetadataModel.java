package ppm.odataprovider.service.metadata;

import java.util.Map;

public class EntityMetadataModel {
	private Map<String, String> entityset;
	private Map<String, EntityTypeMetadata> entities;
	
	public Map<String, String> getEntityset() {
		return entityset;
	}
	public void setEntityset(Map<String, String> entityset) {
		this.entityset = entityset;
	}
	public Map<String, EntityTypeMetadata> getEntities() {
		return entities;
	}
	public void setEntities(Map<String, EntityTypeMetadata> entities) {
		this.entities = entities;
	}
	
	
	
}
