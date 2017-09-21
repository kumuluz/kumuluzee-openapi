package com.kumuluz.ee.openapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kumuluz.ee.common.Extension;
import com.kumuluz.ee.common.config.EeConfig;
import com.kumuluz.ee.common.dependencies.EeComponentDependency;
import com.kumuluz.ee.common.dependencies.EeComponentType;
import com.kumuluz.ee.common.dependencies.EeExtensionDef;
import com.kumuluz.ee.common.wrapper.KumuluzServerWrapper;
import com.kumuluz.ee.jetty.JettyServletServer;
import com.kumuluz.ee.openapi.models.OpenApiConfiguration;
import io.swagger.jaxrs2.integration.OpenApiServlet;
import io.swagger.oas.integration.api.OpenAPIConfiguration;
import io.swagger.oas.models.OpenAPI;
import org.glassfish.jersey.servlet.ServletContainer;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * OpenApiExtension class.
 *
 * @author Zvone Gazvoda
 * @since 1.0.0
 */
@EeExtensionDef(name = "OpenAPI", group = "OPEN_API")
@EeComponentDependency(EeComponentType.JAX_RS)
public class OpenApiExtension implements Extension {

    private static final Logger LOG = Logger.getLogger(OpenApiExtension.class.getName());

    @Override
    public void load() {

    }

    @Override
    public void init(KumuluzServerWrapper kumuluzServerWrapper, EeConfig eeConfig) {
        if (kumuluzServerWrapper.getServer() instanceof JettyServletServer) {

            LOG.info("Initializing OpenAPI extension.");

            JettyServletServer server = (JettyServletServer) kumuluzServerWrapper.getServer();

            Map<String, String> parameters = new HashMap<>();
            parameters.put("jersey.config.server.wadl.disableWadl", "true");
            parameters.put("jersey.config.server.provider.packages", "io.swagger.jaxrs2.integration.resources");

            server.registerServlet(ServletContainer.class, "/api/*", parameters);

            LOG.info("OpenAPI extension initialized.");
        }
    }
}
