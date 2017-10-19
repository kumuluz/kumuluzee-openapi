package com.kumuluz.ee.openapi;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kumuluz.ee.common.Extension;
import com.kumuluz.ee.common.config.EeConfig;
import com.kumuluz.ee.common.dependencies.EeComponentDependency;
import com.kumuluz.ee.common.dependencies.EeComponentType;
import com.kumuluz.ee.common.dependencies.EeExtensionDef;
import com.kumuluz.ee.common.wrapper.KumuluzServerWrapper;
import com.kumuluz.ee.jetty.JettyServletServer;
import io.swagger.jaxrs2.integration.OpenApiServlet;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
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

            Map<String, String> parameters = new HashMap<>();

            List<Application> applications = new ArrayList<>();
            ServiceLoader.load(Application.class).forEach(applications::add);

            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);

            for (Application application : applications) {

                Map<String, String> specParams = new HashMap<>(parameters);

                Class<?> applicationClass = application.getClass();
                if (targetClassIsProxied(applicationClass)) {
                    applicationClass = applicationClass.getSuperclass();
                }

                String applicationPath = "";
                ApplicationPath applicationPathAnnotation = applicationClass.getAnnotation(ApplicationPath.class);
                applicationPath = applicationPathAnnotation.value();

                applicationPath = StringUtils.strip(applicationPath, "/");

                specParams.put("openApi.configuration.location", "api-specs/" + applicationPath + "/openapi-configuration.json");

                server.registerServlet(OpenApiServlet.class, "/api-specs/" + applicationPath + "/*", specParams, 1);
            }

            LOG.info("OpenAPI extension initialized.");
        }
    }

    private boolean targetClassIsProxied(Class targetClass) {
        return targetClass.getCanonicalName().contains("$Proxy");
    }
}
