package ppm.odataprovider.service;

import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmFunction;
import org.apache.olingo.commons.api.edm.EdmNavigationProperty;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.*;
import org.apache.olingo.server.api.deserializer.DeserializerResult;
import org.apache.olingo.server.api.deserializer.ODataDeserializer;
import org.apache.olingo.server.api.processor.EntityProcessor;
import org.apache.olingo.server.api.serializer.EntitySerializerOptions;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.*;
import org.apache.olingo.server.api.uri.queryoption.ExpandOption;
import ppm.odataprovider.data.ApplicationEntity;
import ppm.odataprovider.data.EntityDataHelper;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PpmEntityProcessor implements EntityProcessor {

    private OData odata;
    private ServiceMetadata serviceMetadata;

    @Override
    public void readEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
        List<UriResource> uriResources = uriInfo.getUriResourceParts();
        UriResource firstSegment = uriResources.get(0);
        if(firstSegment instanceof UriResourceEntitySet){
            this.readEntityInternal(response, uriInfo, responseFormat);
        }else if( firstSegment instanceof UriResourceFunction){
            this.readFunctionImportInternal( (UriResourceFunction) firstSegment, response,  responseFormat);
        }else {
            throw new ODataApplicationException("Only EntitySet is supported",
                    HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
        }
    }

    private void readEntityInternal(ODataResponse response, UriInfo uriInfo, ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
        EdmEntitySet responseEntitySet = null;
        Entity responseEntity = null;
        EntityServiceHandler entityServiceHandler = new EntityServiceHandler();

        // Retrieve the Entity Type
        List<UriResource> uriResources = uriInfo.getUriResourceParts();
        UriResource firstUriResource = uriResources.get(0);
        if (!(firstUriResource instanceof UriResourceEntitySet)) {
            throw new ODataApplicationException("Only EntitySet is supported", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
        }
        int segmentCount = uriResources.size();

        UriResourceEntitySet resourceEntitySet = (UriResourceEntitySet) firstUriResource;
        EdmEntitySet sourceEntitySet = resourceEntitySet.getEntitySet();
        List<UriParameter> keyPredicates = resourceEntitySet.getKeyPredicates();
        ExpandOption expandOption = uriInfo.getExpandOption();
        if (segmentCount == 1) {
            responseEntitySet = sourceEntitySet;
            responseEntity = entityServiceHandler.readEntityData(sourceEntitySet, keyPredicates, expandOption);
        } else if (segmentCount == 2) {
            UriResource secondUriResource = uriResources.get(1);
            if (secondUriResource instanceof UriResourceNavigation) {
                UriResourceNavigation uriResourceNavigation = (UriResourceNavigation) secondUriResource;
                EdmNavigationProperty navProp = uriResourceNavigation.getProperty();
                responseEntitySet = EntityServiceUtil.getNavigationTargetEntitySet(sourceEntitySet, navProp);
                List<UriParameter> navKeyPredicates = uriResourceNavigation.getKeyPredicates();
                if (navKeyPredicates.isEmpty()) {
                    // e.g. DemoService.svc/Products(1)/Category
                    responseEntity = entityServiceHandler.readRelatedEntityData(sourceEntitySet, responseEntitySet, keyPredicates, navProp.getName());
                } else { // e.g. DemoService.svc/Categories(3)/Products(5)
                    List<UriParameter> mergedParams = new ArrayList<>(keyPredicates);
                    mergedParams.addAll(navKeyPredicates);
                    responseEntity = entityServiceHandler.readRelatedEntityData(sourceEntitySet, responseEntitySet, mergedParams, navProp.getName(), navKeyPredicates);
//
                }
            }
        } else {
            throw new ODataApplicationException("Not Supported URL", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
        }

        // Serialize
        EdmEntityType entityType = responseEntitySet.getEntityType();
        ContextURL contextUrl = ContextURL.with().entitySet(responseEntitySet).build();
        // expand and select currently not supported
        EntitySerializerOptions options = EntitySerializerOptions.with()
                .contextURL(contextUrl)
                .expand(expandOption)
                .build();

        ODataSerializer serializer = odata.createSerializer(responseFormat);
        SerializerResult serializerResult = serializer.entity(serviceMetadata, entityType, responseEntity, options);
        InputStream entityStream = serializerResult.getContent();

        // Configure the response object
        response.setContent(entityStream);
        response.setStatusCode(HttpStatusCode.OK.getStatusCode());
        response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
    }

    private void readFunctionImportInternal(UriResourceFunction uriResourceFunction, ODataResponse response, ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {

        final EdmFunction edmFunction = uriResourceFunction.getFunction();
        final EntityServiceHandler entityServiceHandler = new EntityServiceHandler();
        final List<UriParameter> uriParameters = uriResourceFunction.getParameters();

        Map<Class, List<ApplicationEntity>> functionResult = entityServiceHandler.executeEntityFunction(edmFunction.getName(), uriParameters);
        Class entityClass = (Class) functionResult.keySet().toArray()[0];
        final Entity entity = EntityDataHelper.toEntity(entityClass, functionResult.get(entityClass), null, null);

        // 2nd step: Serialize the response entity
        final EdmEntityType edmEntityType = (EdmEntityType) uriResourceFunction.getFunction().getReturnType().getType();
        final ContextURL contextURL = ContextURL.with().type(edmEntityType).build();
        final EntitySerializerOptions opts = EntitySerializerOptions.with().contextURL(contextURL).build();
        final ODataSerializer serializer = odata.createSerializer(responseFormat);
        final SerializerResult serializerResult = serializer.entity(serviceMetadata, edmEntityType, entity, opts);

        // 3rd configure the response object
        response.setContent(serializerResult.getContent());
        response.setStatusCode(HttpStatusCode.OK.getStatusCode());
        response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
    }

    @Override
    public void createEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType requestFormat, ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
        // 1. Retrieve the entity type from the URI
        UriResourceEntitySet resourceEntitySet = EntityDataHelper.getUriResourceEntitySet(uriInfo);
        EdmEntitySet edmEntitySet = resourceEntitySet.getEntitySet();
        EdmEntityType edmEntityType = edmEntitySet.getEntityType();

        // 2. create the data in backend
        // 2.1. retrieve the payload from the POST request for the entity to create and deserialize it
        InputStream requestInputStream = request.getBody();
        ODataDeserializer deserializer = this.odata.createDeserializer(requestFormat);
        DeserializerResult result = deserializer.entity(requestInputStream, edmEntityType);
        Entity requestEntity = result.getEntity();
        // 2.2 do the creation in backend, which returns the newly created entity
        Entity createdEntity = new EntityServiceHandler().saveEntity(edmEntitySet, requestEntity);

        // 3. serialize the response (we have to return the created entity)
        ContextURL contextUrl = ContextURL.with().entitySet(edmEntitySet).build();
        // expand and select currently not supported
        EntitySerializerOptions options = EntitySerializerOptions.with().contextURL(contextUrl).build();

        ODataSerializer serializer = this.odata.createSerializer(responseFormat);
        SerializerResult serializedResponse = serializer.entity(serviceMetadata, edmEntityType, createdEntity, options);

        //4. configure the response object
        response.setContent(serializedResponse.getContent());
        response.setStatusCode(HttpStatusCode.CREATED.getStatusCode());
        response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
    }

    @Override
    public void updateEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType requestFormat, ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
        // 1. Retrieve the entity set which belongs to the requested entity
        UriResourceEntitySet resourceEntitySet = EntityDataHelper.getUriResourceEntitySet(uriInfo);
        EdmEntitySet edmEntitySet = resourceEntitySet.getEntitySet();
        EdmEntityType edmEntityType = edmEntitySet.getEntityType();

        // 2. update the data in backend
        // 2.1. retrieve the payload from the PUT request for the entity to be updated
        InputStream requestInputStream = request.getBody();
        ODataDeserializer deserializer = this.odata.createDeserializer(requestFormat);
        DeserializerResult result = deserializer.entity(requestInputStream, edmEntityType);
        Entity requestEntity = result.getEntity();
        // 2.2 do the modification in backend
        List<UriParameter> keyPredicates = resourceEntitySet.getKeyPredicates();
        HttpMethod httpMethod = request.getMethod();

//        storage.updateEntityData(edmEntitySet, keyPredicates, requestEntity, httpMethod);
        new EntityServiceHandler().updateEntity(edmEntitySet, keyPredicates, requestEntity, httpMethod);

        //3. configure the response object
        response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
    }

    @Override
    public void deleteEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo) throws ODataApplicationException, ODataLibraryException {

        // 1. Retrieve the entity set which belongs to the requested entity
        List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
        UriResourceEntitySet resourceEntitySet = EntityDataHelper.getUriResourceEntitySet(uriInfo);
        EdmEntitySet edmEntitySet = resourceEntitySet.getEntitySet();

        // 2. delete the data in backend
        List<UriParameter> keyPredicates = resourceEntitySet.getKeyPredicates();
        new EntityServiceHandler().deleteEntity(edmEntitySet, keyPredicates);

        //3. configure the response object
        response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
    }

    @Override
    public void init(OData odata, ServiceMetadata serviceMetadata) {
        this.odata = odata;
        this.serviceMetadata = serviceMetadata;
    }
}
