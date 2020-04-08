package com.kumuluz.ee.openapi.ui;

import com.kumuluz.ee.common.Extension;
import com.kumuluz.ee.common.config.EeConfig;
import com.kumuluz.ee.common.dependencies.EeExtensionDef;
import com.kumuluz.ee.common.utils.ResourceUtils;
import com.kumuluz.ee.common.wrapper.KumuluzServerWrapper;
import com.kumuluz.ee.configuration.utils.ConfigurationUtil;
import com.kumuluz.ee.jetty.JettyServletServer;
import com.kumuluz.ee.openapi.ui.filters.SwaggerUIFilter;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import org.apache.commons.lang3.StringUtils;
import com.kumuluz.ee.openapi.ui.servlets.UiServlet;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.logging.Logger;

/**
 * OpenApiUiExtension class - hook-up OpenApiServlets to expose api specifications using SwaggerUI.
 *
 * @author Zvone Gazvoda
 * @since 1.0.0
 */
@EeExtensionDef(name = "OpenAPI-UI", group = "OPEN_API_UI")
public class OpenApiUiExtension implements Extension {

    private static final Logger LOG = Logger.getLogger(OpenApiUiExtension.class.getName());

    @Override
    public void load() {
    }

    @Override
    public void init(KumuluzServerWrapper kumuluzServerWrapper, EeConfig eeConfig) {

        ConfigurationUtil configurationUtil = ConfigurationUtil.getInstance();

        if (kumuluzServerWrapper.getServer() instanceof JettyServletServer) {

            LOG.info("Initializing OpenAPI UI extension.");

            try {
                Class.forName("com.kumuluz.ee.openapi.OpenApiExtension");
            } catch (ClassNotFoundException e) {
                LOG.severe("Unable to find OpenApi extension, OpenAPI UI will not be initialized: " + e.getMessage());
                return;
            }

            JettyServletServer server = (JettyServletServer) kumuluzServerWrapper.getServer();

            List<Application> applications = new ArrayList<>();
            ServiceLoader.load(Application.class).forEach(applications::add);

            if (applications.size() == 1) {
                Application application = applications.get(0);

                Class<?> applicationClass = application.getClass();
                if (targetClassIsProxied(applicationClass)) {
                    applicationClass = applicationClass.getSuperclass();
                }

                Integer port = null;
                String serverUrl = "localhost";

                if (eeConfig.getServer().getHttp() != null) {
                    port = eeConfig.getServer().getHttp().getPort();
                    serverUrl = "http://" + serverUrl;
                } else {
                    port = eeConfig.getServer().getHttps().getPort();
                    serverUrl = "https://" + serverUrl;
                }

                serverUrl += (port != null ? ":" + port.toString() : "");

                OpenAPIDefinition openAPIDefinitionAnnotation = applicationClass.getAnnotation(OpenAPIDefinition.class);
                if (openAPIDefinitionAnnotation != null) {
                    try {
                        URL url = new URL(openAPIDefinitionAnnotation.servers()[0].url());
                        serverUrl = url.getProtocol() + "://" + url.getHost() + ":" + url.getPort();
                    } catch (MalformedURLException e) {
                        LOG.warning("Server URL invalid: " + e.getMessage());
                    } catch (IndexOutOfBoundsException e) {
                        LOG.warning("Servers not provided in annotation OpenAPIDefinition, will use default server url: " + serverUrl);
                    }
                }

                String applicationPath = "";
                ApplicationPath applicationPathAnnotation = applicationClass.getAnnotation(ApplicationPath.class);
                if (applicationPathAnnotation != null) {
                    applicationPath = applicationPathAnnotation.value();
                } else {
                    if (openAPIDefinitionAnnotation != null) {
                        try {
                            URL url = new URL(openAPIDefinitionAnnotation.servers()[0].url());
                            applicationPath = url.getPath();
                        } catch (MalformedURLException e) {
                            LOG.warning("Server URL defined in annotation OpenAPIDefinition is invalid: " + e.getMessage());
                        }
                    }
                }
                if (applicationPath.endsWith("/")) {
                    applicationPath = applicationPath.substring(0, applicationPath.length() - 1);
                }
                if (!applicationPath.startsWith("/")) {
                    applicationPath = "/"+applicationPath;
                }

                URL webApp = ResourceUtils.class.getClassLoader().getResource("swagger-ui/api-specs/ui");

                // ui path
                String uiPath = configurationUtil.get("kumuluzee.openapi.ui.mapping").orElse("/api-specs/ui");
                if (uiPath.endsWith("*")) {
                    uiPath = uiPath.substring(0, uiPath.length() - 1);
                }
                if (uiPath.endsWith("/")) {
                    uiPath = uiPath.substring(0, uiPath.length() - 1);
                }

                if (uiPath.isEmpty()) {
                    // not supported as of yet, probably could be done by very strict redirects in SwaggerUIFilter
                    throw new IllegalArgumentException("UI cannot be served from root. Please change " +
                            "kumuluzee.openapi.ui.mapping configuration value accordingly.");
                }

                // spec path
                String specPath = configurationUtil.get("kumuluzee.openapi.servlet.mapping").orElse("/api-specs");
                if ("".equals(applicationPath) || "/".equals(applicationPath)) {
                    specPath = specPath+"/openapi.json";
                }
                else {
                    specPath = specPath+applicationPath+"/openapi.json";
                }

                // context path
                String contextPath = configurationUtil.get("kumuluzee.server.context-path").orElse("");
                if (contextPath.endsWith("/")) {
                    contextPath = contextPath.substring(0, contextPath.length() - 1);
                }

                if (webApp != null && configurationUtil.getBoolean("kumuluzee.openapi.ui.enabled").orElse(true) && configurationUtil
                        .getBoolean("kumuluzee.openapi.enabled").orElse(true)) {

                    LOG.info("Swagger UI servlet registered on "+uiPath+ " (servlet context is implied)");
                    LOG.info("Swagger UI can be accessed at "+serverUrl + contextPath + uiPath);

                    // create servlet that will serve static files
                    Map<String, String> swaggerUiParams = new HashMap<>();
                    swaggerUiParams.put("resourceBase", webApp.toString());
                    swaggerUiParams.put("uiPath", uiPath); //context already included in servlet resolution
                    server.registerServlet(UiServlet.class, uiPath + "/*", swaggerUiParams, 1);

                    String specUrl = serverUrl + contextPath + specPath;
                    String oauth2RedirectUrl = serverUrl + contextPath + uiPath;
                    String redirUiPath = contextPath+uiPath;

                    LOG.info("Swagger UI spec URL resolved to "+specUrl);

                    // create filter that will redirect to Swagger UI with appropriate parameters
                    Map<String, String> swaggerUiFilterParams = new HashMap<>();
                    swaggerUiFilterParams.put("specUrl", specUrl);
                    swaggerUiFilterParams.put("uiPath", redirUiPath);
                    swaggerUiFilterParams.put("oauth2RedirectUrl", oauth2RedirectUrl + "/oauth2-redirect.html");
                    server.registerFilter(SwaggerUIFilter.class, uiPath + "/*", swaggerUiFilterParams);

                } else {
                    LOG.severe("Swagger UI not found. Try cleaning and rebuilding project.");
                }
            }
        }
    }

    private boolean targetClassIsProxied(Class targetClass) {
        return targetClass.getCanonicalName().contains("$Proxy");
    }
}
