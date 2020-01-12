package ppm.odataprovider.service;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.*;
import org.apache.olingo.commons.api.ex.ODataException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PpmEdmProvider extends CsdlAbstractEdmProvider {

    // Service Namespace
    public static final String NAMESPACE = "OData.Ppm";

    // EDM Container
    public static final String CONTAINER_NAME = "Project Container";
    public static final FullQualifiedName CONTAINER = new FullQualifiedName(NAMESPACE, CONTAINER_NAME);

    // Entity Types Names
    public static final String ET_TASK_NAME = "Task";
    public static final FullQualifiedName ET_TASK_FQN = new FullQualifiedName(NAMESPACE, ET_TASK_NAME);

    // Entity Set Names
    public static final String ES_TASKS_NAME = "Tasks";

    @Override
    public CsdlEntityType getEntityType(FullQualifiedName entityTypeName) throws ODataException {
        if (entityTypeName.equals(ET_TASK_FQN)) {

            // creating entity type properties
            CsdlProperty taskId = new CsdlProperty().setName("TaskId").setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName());
            CsdlProperty name = new CsdlProperty().setName("Name").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
            CsdlProperty dateCreated = new CsdlProperty().setName("DateCreated").setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName());
            CsdlProperty earlyStart = new CsdlProperty().setName("EarlyStart").setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName());
            CsdlProperty earlyFinish = new CsdlProperty().setName("EarlyFinish").setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName());
            CsdlProperty lateStart = new CsdlProperty().setName("LateStart").setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName());
            CsdlProperty lateFinish = new CsdlProperty().setName("LateFinish").setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName());
            CsdlProperty totalFloat = new CsdlProperty().setName("TotalFloat").setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName());
            CsdlProperty freeFloat = new CsdlProperty().setName("FreeFloat").setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName());
            CsdlProperty assignee = new CsdlProperty().setName("Assignee").setType((EdmPrimitiveTypeKind.String).getFullQualifiedName());

            //creating key element
            CsdlPropertyRef taskRef = new CsdlPropertyRef();
            taskRef.setName("TaskId");

            // entity type
            CsdlEntityType taskEntityType = new CsdlEntityType();
            taskEntityType.setName(ET_TASK_NAME);
            taskEntityType.setProperties(Arrays.asList(taskId, name, dateCreated, earlyStart, earlyFinish, lateStart, lateFinish, totalFloat, freeFloat, assignee));
            taskEntityType.setKey(Collections.singletonList(taskRef));

            return taskEntityType;
        }
        return null;
    }

    @Override
    public CsdlEntitySet getEntitySet(FullQualifiedName entityContainer, String entitySetName) throws ODataException {
        if(entityContainer.equals(CONTAINER) && entitySetName.equals(ES_TASKS_NAME)){
            CsdlEntitySet entitySet = new CsdlEntitySet();
            entitySet.setName(ES_TASKS_NAME);
            entitySet.setType(ET_TASK_FQN);

            return entitySet;
        }
        return null;
    }

    @Override
    public CsdlEntityContainerInfo getEntityContainerInfo(FullQualifiedName entityContainerName) throws ODataException {
        if(entityContainerName == null || entityContainerName.equals(entityContainerName)){
            CsdlEntityContainerInfo entityContainerInfo = new CsdlEntityContainerInfo();
            entityContainerInfo.setContainerName(CONTAINER);
            return entityContainerInfo;
        }

        return null;
    }

    @Override
    public List<CsdlSchema> getSchemas() throws ODataException {
        CsdlSchema schema = new CsdlSchema();
        schema.setNamespace(NAMESPACE);

        List<CsdlEntityType> entityTypes = new ArrayList<CsdlEntityType>();
        entityTypes.add(getEntityType(ET_TASK_FQN));
        schema.setEntityTypes(entityTypes);

        schema.setEntityContainer(getEntityContainer());

        List<CsdlSchema> schemaList = new ArrayList<CsdlSchema>();
        schemaList.add(schema);

        return schemaList;
    }

    @Override
    public CsdlEntityContainer getEntityContainer() throws ODataException {
        List<CsdlEntitySet> entitySets = new ArrayList<CsdlEntitySet>();
        entitySets.add(this.getEntitySet(CONTAINER, ES_TASKS_NAME));

        CsdlEntityContainer entityContainer = new CsdlEntityContainer();
        entityContainer.setName(CONTAINER_NAME);
        entityContainer.setEntitySets(entitySets);

        return entityContainer;
    }
}
