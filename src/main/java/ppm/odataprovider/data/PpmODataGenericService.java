package ppm.odataprovider.data;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeException;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriParameter;

import java.util.List;


public interface PpmODataGenericService {

    EntityCollection getAll(String entitySetName);

    Entity getEntity(EdmEntityType entityType, EdmEntitySet edmEntitySet, List<UriParameter> keyParams) throws EdmPrimitiveTypeException;

    Entity saveEntity(EdmEntityType entityType, EdmEntitySet edmEntitySet, Entity entity) throws ODataApplicationException;
}
