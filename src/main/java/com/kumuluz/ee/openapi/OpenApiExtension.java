package com.kumuluz.ee.openapi;

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
import java.net.MalformedURLException;
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

            ConfigurationUtil configurationUtil = ConfigurationUtil.getInstance();

            if (configurationUtil.getBoolean("kumuluzee.openapi.spec.enabled").orElse(true)) {

                LOG.info("Initializing OpenAPI extension.");

                JettyServletServer server = (JettyServletServer) kumuluzServerWrapper.getServer();

                List<Application> applications = new ArrayList<>();
                ServiceLoader.load(Application.class).forEach(applications::add);

                if (applications.size() == 1) {
                    Application application = applications.get(0);

                    Map<String, String> specParams = new HashMap<>();

                    Class<?> applicationClass = application.getClass();
                    if (targetClassIsProxied(applicationClass)) {
                        applicationClass = applicationClass.getSuperclass();
                    }

                    String applicationPath = "";

                    Optional<Integer> port = ConfigurationUtil.getInstance().getInteger("kumuluzee.server.http.port");

                    String serverUrl = "http://localhost" + (port.map(Object::toString).orElse(""));

                    OpenAPIDefinition openAPIDefinitionAnnotation = applicationClass.getAnnotation(OpenAPIDefinition.class);
                    if (openAPIDefinitionAnnotation != null) {
                        try {
                            URL url = new URL(openAPIDefinitionAnnotation.servers()[0].url());
                            serverUrl = url.getProtocol() + "://" + url.getHost() + ":" + url.getPort();
                        } catch (MalformedURLException e) {
                            LOG.warning("Server URL invalid: " + e.getMessage());
                        }
                    }

                    ApplicationPath applicationPathAnnotation = applicationClass.getAnnotation(ApplicationPath.class);
                    if (applicationPathAnnotation != null) {
                        applicationPath = applicationPathAnnotation.value();
                    } else {
                        if (openAPIDefinitionAnnotation != null) {
                            try {
                                URL url = new URL(openAPIDefinitionAnnotation.servers()[0].url());
                                applicationPath = url.getPath();
                            } catch (MalformedURLException e) {
                                LOG.warning("Server URL invalid: " + e.getMessage());
                            }
                        }
                    }

                    applicationPath = StringUtils.strip(applicationPath, "/");

                    if (applicationPath.equals("")) {
                        specParams.put("openApi.configuration.location", "api-specs/openapi-configuration.json");
                        server.registerServlet(OpenApiServlet.class, "/api-specs/*", specParams, 1);
                    } else {
                        specParams.put("openApi.configuration.location", "api-specs/" + applicationPath + "/openapi-configuration.json");
                        server.registerServlet(OpenApiServlet.class, "/api-specs/" + applicationPath + "/*", specParams, 1);
                    }

                    Map<String, String> swaggerUiParams = new HashMap<>();
                    URL webApp = ResourceUtils.class.getClassLoader().getResource("swagger-ui");

                    if (webApp != null && configurationUtil.getBoolean("kumuluzee.openapi.ui.enabled").orElse(false)) {
                        swaggerUiParams.put("resourceBase", webApp.toString());
                        server.registerServlet(DefaultServlet.class, "/api-specs/ui/*", swaggerUiParams, 1);

                        Map<String, String> swaggerUiFilterParams = new HashMap<>();

                        swaggerUiFilterParams.put("url", serverUrl + "/api-specs/" + applicationPath + "/openapi.json");
                        server.registerFilter(SwaggerUIFilter.class, "/api-specs/ui/*", swaggerUiFilterParams);

                    } else {
                        LOG.severe("Unable to find Swagger-UI artifacts or Swagger UI is disabled.");
                    }

                    LOG.info("OpenAPI extension initialized.");
                } else {
                    LOG.warning("Multiple JAX-RS applications not supported. OpenAPI definitions will not be served.");
                }
            }
        }
    }

    private boolean targetClassIsProxied(Class targetClass) {
        return targetClass.getCanonicalName().contains("$Proxy");
    }
}
