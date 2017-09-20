package com.kumuluz.ee.openapi;

import com.kumuluz.ee.common.Extension;
import com.kumuluz.ee.common.config.EeConfig;
import com.kumuluz.ee.common.dependencies.EeComponentDependency;
import com.kumuluz.ee.common.dependencies.EeComponentType;
import com.kumuluz.ee.common.dependencies.EeExtensionDef;
import com.kumuluz.ee.common.wrapper.KumuluzServerWrapper;
import com.kumuluz.ee.jetty.JettyServletServer;

import java.util.logging.Logger;

/**
 * SwaggerExtension class.
 *
 * @author Zvone Gazvoda
 * @since 1.0.0
 */
@EeExtensionDef(name = "OpenAPI", group = "OPEN_API")
@EeComponentDependency(EeComponentType.JAX_RS)
public class SwaggerExtension implements Extension {

    private static final Logger LOG = Logger.getLogger(SwaggerExtension.class.getName());

    @Override
    public void load() {

    }

    @Override
    public void init(KumuluzServerWrapper kumuluzServerWrapper, EeConfig eeConfig) {
        if (kumuluzServerWrapper.getServer() instanceof JettyServletServer) {

            LOG.info("Initializing OpenAPI extension.");

            JettyServletServer server = (JettyServletServer) kumuluzServerWrapper.getServer();

            //ResourceConfig resourceConfig = new ResourceConfig(ApiListingResource.class, SwaggerSerializers.class);

            //server.registerServlet(new ServletContainer(resourceConfig), "/v1", null, 1);
            //server.registerServlet(Bootstrap.class, "/swopa", 2);

            LOG.info("OpenAPI extension initialized.");
        }
    }
}
