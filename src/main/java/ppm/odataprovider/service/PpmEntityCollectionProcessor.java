package ppm.odataprovider.service;

import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmFunction;
import org.apache.olingo.commons.api.edm.EdmNavigationProperty;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.*;
import org.apache.olingo.server.api.processor.EntityCollectionProcessor;
import org.apache.olingo.server.api.serializer.EntityCollectionSerializerOptions;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.*;
import org.apache.olingo.server.api.uri.queryoption.ExpandOption;
import org.apache.olingo.server.api.uri.queryoption.FilterOption;
import ppm.odataprovider.data.ApplicationEntity;
import ppm.odataprovider.data.EntityDataHelper;

import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PpmEntityCollectionProcessor implements EntityCollectionProcessor {

    private OData odata;
    private ServiceMetadata serviceMetadata;

    public void init(OData oData, ServiceMetadata serviceMetadata) {
        this.odata = oData;
        this.serviceMetadata = serviceMetadata;
    }

    @Override
    public void readEntityCollection(ODataRequest oDataRequest, ODataResponse oDataResponse, UriInfo uriInfo, ContentType contentType) throws ODataApplicationException, ODataLibraryException {
        UriResource initResource = uriInfo.getUriResourceParts().get(0);
        // analyse the uri and delegate it to relevant internal method
        if (initResource instanceof UriResourceEntitySet) {
            this.readEntityCollectionInternal(oDataRequest, oDataResponse, uriInfo, contentType);
        } else if (initResource instanceof UriResourceFunction) {
            this.readEntityFunctionInternal(oDataRequest, oDataResponse, uriInfo, contentType);
        } else {
            throw new ODataApplicationException("Not implemented",
                    HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(),
                    Locale.ENGLISH);
        }

    }

    private void readEntityCollectionInternal(ODataRequest oDataRequest, ODataResponse oDataResponse, UriInfo uriInfo, ContentType contentType) throws ODataApplicationException, ODataLibraryException {
        EdmEntitySet responseEntitySet = null; // for building ContextURL
        EntityCollection responseEntityCollection = null; // for the response body

        List<UriResource> resourceParts = uriInfo.getUriResourceParts();
        int segmentCount = resourceParts.size();

        UriResource uriResource = resourceParts.get(0); // the first segment is the EntitySet
        if (!(uriResource instanceof UriResourceEntitySet)) {
            throw new ODataApplicationException("Only EntitySet is supported", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
        }

        UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) uriResource;
        EdmEntitySet startEntitySet = uriResourceEntitySet.getEntitySet();
        EntityServiceHandler serviceHandler = new EntityServiceHandler();
        ExpandOption expandOption = uriInfo.getExpandOption();
        if (segmentCount == 1) {
            FilterOption filterOption = uriInfo.getFilterOption();
            responseEntitySet = startEntitySet;
            responseEntityCollection = serviceHandler.readEntitySetData(startEntitySet, filterOption, expandOption);
        } else if (segmentCount == 2) {
            UriResource navResource = resourceParts.get(1);
            if (navResource instanceof UriResourceNavigation) {
                UriResourceNavigation uriResourceNavigation = (UriResourceNavigation) navResource;
                EdmNavigationProperty edmNavigationProperty = uriResourceNavigation.getProperty();
                responseEntitySet = EntityServiceUtil.getNavigationTargetEntitySet(startEntitySet, edmNavigationProperty);

                List<UriParameter> keyPredicates = uriResourceEntitySet.getKeyPredicates();
                responseEntityCollection = serviceHandler.readRelatedEntitySetData(startEntitySet, responseEntitySet, keyPredicates, edmNavigationProperty.getName());

                if (responseEntityCollection == null) {
                    throw new ODataApplicationException("Entity not found.", HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ROOT);
                }
            } else {
                throw new ODataApplicationException("Not supported", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
            }
        }

        ODataSerializer serializer = this.odata.createSerializer(contentType);
        EdmEntityType edmEntityType = responseEntitySet.getEntityType();
        ContextURL contextUrl = ContextURL.with().entitySet(responseEntitySet).build();
        final String id = oDataRequest.getRawBaseUri() + "/" + responseEntitySet.getName();

        EntityCollectionSerializerOptions opts = EntityCollectionSerializerOptions
                .with()
                .id(id)
                .contextURL(contextUrl)
                .expand(expandOption)
                .build();

        SerializerResult serializerResult = serializer.entityCollection(serviceMetadata, edmEntityType, responseEntityCollection, opts);
        InputStream serializedContent = serializerResult.getContent();
        oDataResponse.setContent(serializedContent);
        oDataResponse.setStatusCode(HttpStatusCode.OK.getStatusCode());
        oDataResponse.setHeader(HttpHeader.CONTENT_TYPE, contentType.toContentTypeString());
    }

    private void readEntityFunctionInternal(ODataRequest oDataRequest, ODataResponse oDataResponse, UriInfo uriInfo, ContentType contentType) throws ODataApplicationException, SerializerException {
        final UriResource uriFunctionSegment = uriInfo.getUriResourceParts().get(0);
        final UriResourceFunction uriResourceFunction = (UriResourceFunction) uriFunctionSegment;

        final EdmFunction edmFunction = uriResourceFunction.getFunction();
        final List<UriParameter> uriParameters = uriResourceFunction.getParameters();
        final EntityCollection entityCollection = new EntityCollection();
        final EntityServiceHandler entityServiceHandler = new EntityServiceHandler();

        EntityOperationResult functionResult = entityServiceHandler.executeEntityOperation(EntityOperationType.Function, edmFunction.getName(), uriParameters);
        Class entityClass = functionResult.getEntityClazz().get();
        EntityDataHelper.addEntitiesToCollection(entityCollection, functionResult.getData().get(), entityClass,null, null);

        final EdmEntityType edmEntityType = (EdmEntityType) uriResourceFunction.getFunction().getReturnType().getType();
        final ContextURL contextURL = ContextURL.with().asCollection().type(edmEntityType).build();
        final EntityCollectionSerializerOptions opts = EntityCollectionSerializerOptions.with().contextURL(contextURL).build();
        final ODataSerializer serializer = odata.createSerializer(contentType);
        final SerializerResult serializerResult = serializer.entityCollection(serviceMetadata, edmEntityType, entityCollection, opts);

        // 3rd configure the response object
        oDataResponse.setContent(serializerResult.getContent());
        oDataResponse.setStatusCode(HttpStatusCode.OK.getStatusCode());
        oDataResponse.setHeader(HttpHeader.CONTENT_TYPE, contentType.toContentTypeString());
    }
}
