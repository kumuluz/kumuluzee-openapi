package com.kumuluz.ee.openapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kumuluz.ee.common.Extension;
import com.kumuluz.ee.common.config.EeConfig;
import com.kumuluz.ee.common.dependencies.EeComponentDependency;
import com.kumuluz.ee.common.dependencies.EeComponentType;
import com.kumuluz.ee.common.dependencies.EeExtensionDef;
import com.kumuluz.ee.common.utils.ResourceUtils;
import com.kumuluz.ee.common.wrapper.KumuluzServerWrapper;
import com.kumuluz.ee.configuration.utils.ConfigurationUtil;
import com.kumuluz.ee.jetty.JettyServletServer;
import com.kumuluz.ee.openapi.filters.SwaggerUIFilter;
import io.swagger.jaxrs2.integration.OpenApiServlet;
import io.swagger.oas.annotations.OpenAPIDefinition;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.servlet.DefaultServlet;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.logging.Logger;

/**
 * OpenApiExtension class - hook-up OpenApiServlets to expose api specifications.
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

            List<Application> applications = new ArrayList<>();
            ServiceLoader.load(Application.class).forEach(applications::add);

            for (Application application : applications) {

                Map<String, String> specParams = new HashMap<>();

                Class<?> applicationClass = application.getClass();
                if (targetClassIsProxied(applicationClass)) {
                    applicationClass = applicationClass.getSuperclass();
                }

                String applicationPath = "";
                ApplicationPath applicationPathAnnotation = applicationClass.getAnnotation(ApplicationPath.class);
                if (applicationPathAnnotation != null) {
                    applicationPath = applicationPathAnnotation.value();
                } else {
                    OpenAPIDefinition openAPIDefinitionAnnotation = applicationClass.getAnnotation(OpenAPIDefinition.class);
                    applicationPath = openAPIDefinitionAnnotation.servers()[0].url();
                }

                applicationPath = StringUtils.strip(applicationPath, "/");

                if (applicationPath.equals("")) {
                    specParams.put("openApi.configuration.location", "api-specs/openapi-configuration.json");
                    server.registerServlet(OpenApiServlet.class, "/api-specs/*", specParams, 1);
                } else {
                    specParams.put("openApi.configuration.location", "api-specs/" + applicationPath + "/openapi-configuration.json");
                    server.registerServlet(OpenApiServlet.class, "/api-specs/" + applicationPath + "/*", specParams, 1);
                }
            }

            Map<String, String> swaggerUiParams = new HashMap<>();
            URL webApp = ResourceUtils.class.getClassLoader().getResource("swagger-ui");

            if (webApp != null) {
                swaggerUiParams.put("resourceBase", webApp.toString());
                server.registerServlet(DefaultServlet.class, "/api-specs/ui/*", swaggerUiParams, 1);

            } else {
                LOG.severe("Unable to find Swagger-UI artifacts.");
            }

            Map<String, String> swaggerUiFilterParams = new HashMap<>();

            //swaggerUiFilterParams.put("url", "http://localhost:8080/api-specs/v2/openapi.json");
            swaggerUiFilterParams.put("urls", "[{name:\"Customers - v2\",url:\"http://localhost:8080/api-specs/v2/openapi.yaml\"}]");
            server.registerFilter(SwaggerUIFilter.class, "/api-specs/*", swaggerUiFilterParams);

            LOG.info("OpenAPI extension initialized.");

        }
    }

    private boolean targetClassIsProxied(Class targetClass) {
        return targetClass.getCanonicalName().contains("$Proxy");
    }
}
