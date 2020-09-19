package ppm.odataprovider.service;

import org.apache.olingo.commons.api.data.Parameter;
import org.apache.olingo.commons.api.edm.EdmAction;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.*;
import org.apache.olingo.server.api.deserializer.ODataDeserializer;
import org.apache.olingo.server.api.processor.ActionVoidProcessor;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriResourceAction;

import java.util.Locale;
import java.util.Map;

public class PpmActionProcessor implements ActionVoidProcessor {

    private OData oData;

    @Override
    public void init(OData oData, ServiceMetadata serviceMetadata) {
        this.oData = oData;
    }

    @Override
    public void processActionVoid(ODataRequest oDataRequest, ODataResponse oDataResponse, UriInfo uriInfo, ContentType contentType) throws ODataApplicationException, ODataLibraryException {

        final EdmAction edmAction = ((UriResourceAction) uriInfo.getUriResourceParts().get(0)).getAction();
        if (contentType == null) {
            throw new ODataApplicationException("The content type has not been set in the request.",
                    HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ROOT);
        }

        final ODataDeserializer oDataDeserializer = this.oData.createDeserializer(contentType);
        final Map<String, Parameter> actionParameters = oDataDeserializer.actionParameters(oDataRequest.getBody(), edmAction).getActionParameters();
    }
}
