package ppm.odataprovider.web;

import org.apache.olingo.commons.api.edmx.EdmxReference;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataHttpHandler;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.debug.DebugSupport;
import org.apache.olingo.server.api.debug.DefaultDebugSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ppm.odataprovider.service.PpmEdmProvider;
import ppm.odataprovider.service.PpmEntityCollectionProcessor;
import ppm.odataprovider.service.PpmEntityProcessor;
import ppm.odataprovider.service.PpmPrimitiveProcessor;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;


public class AppServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(AppServlet.class);

    protected void service(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        try {
            // create odata handler and configure it with CsdlEdmProvider and Processor


            OData odata = OData.newInstance();
            ServiceMetadata edm = odata.createServiceMetadata(new PpmEdmProvider(), new ArrayList<EdmxReference>());
            ODataHttpHandler handler = odata.createHandler(edm);
            handler.register(new PpmEntityCollectionProcessor());
            handler.register(new PpmEntityProcessor());
            handler.register(new PpmPrimitiveProcessor());

            //Add debug support
            DebugSupport ds = new DefaultDebugSupport();
            ds.init(odata);
            handler.register(ds);

            resp.addHeader("Access-Control-Allow-Origin", "*");
            resp.addHeader("Access-Control-Allow-Methods", "GET, POST, PATCH, PUT, DELETE, OPTIONS");
            resp.addHeader("Access-Control-Allow-Headers", "Origin, Content-Type, X-Auth-Token");

            if(req.getMethod().equals("OPTIONS")) {
                resp.setStatus(HttpServletResponse.SC_ACCEPTED);
                return;
            }

            // let the handler do the work
            handler.process(req, resp);

            if(req.getMethod().equals("OPTIONS")) {
                resp.setStatus(HttpServletResponse.SC_ACCEPTED);
            }

        } catch (RuntimeException e) {
            LOG.error("Server Error occurred in ExampleServlet", e);
            throw new ServletException(e);
        }
    }
}
