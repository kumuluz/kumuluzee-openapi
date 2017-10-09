package com.kumuluz.ee.openapi;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kumuluz.ee.common.Extension;
import com.kumuluz.ee.common.config.EeConfig;
import com.kumuluz.ee.common.dependencies.EeComponentDependency;
import com.kumuluz.ee.common.dependencies.EeComponentType;
import com.kumuluz.ee.common.dependencies.EeExtensionDef;
import com.kumuluz.ee.common.wrapper.KumuluzServerWrapper;
import com.kumuluz.ee.jetty.JettyServletServer;
import com.kumuluz.ee.openapi.models.OpenApiConfiguration;
import org.glassfish.jersey.servlet.ServletContainer;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
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

            InputStream is = getClass().getClassLoader().getResourceAsStream("openapi-configuration.json");
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);

            OpenApiConfiguration openApiConfigurations = null;
            try {
                openApiConfigurations = mapper.readValue(is, new TypeReference<OpenApiConfiguration>() {
                });
            } catch (IOException e) {
                LOG.warning("Unable to load OpenAPI configuration. OpenAPI definition will not be served.");
            }

            if (openApiConfigurations != null) {

                URL url = null;
                try {
                    url = new URL(openApiConfigurations.getOpenAPI().getServers().get(0).getUrl());
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }

                if (url != null) {
                    server.registerServlet(ServletContainer.class, "/api-specs" + url.getPath() + "/*", parameters, 2);
                } else {
                    server.registerServlet(ServletContainer.class, "/api-specs/*", parameters, 2);
                }

                LOG.info("OpenAPI extension initialized.");
            } else {
                LOG.warning("OpenAPI specifications will not be server.");
            }
        }
    }
}
