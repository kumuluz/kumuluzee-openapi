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
import java.net.URI;
import java.net.URISyntaxException;
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

                URL webApp = ResourceUtils.class.getClassLoader().getResource("swagger-ui/api-specs/ui");

                // ui path
                String uiPath = configurationUtil.get("kumuluzee.openapi.ui.mapping").orElse("/api-specs/ui");
                if (uiPath.endsWith("*")) {
                    uiPath = uiPath.substring(0, uiPath.length() - 1);
                }
                uiPath = prependAndStripSlash(uiPath);

                if (uiPath.isEmpty()) {
                    // not supported as of yet, probably could be done by very strict redirects in SwaggerUIFilter
                    throw new IllegalArgumentException("UI cannot be served from root. Please change " +
                            "kumuluzee.openapi.ui.mapping configuration value accordingly.");
                }

                // spec path
                String specPath = configurationUtil.get("kumuluzee.openapi.servlet.mapping")
                        .orElse("/api-specs");

                // context path
                String contextPath = configurationUtil.get("kumuluzee.server.context-path").orElse("");
                contextPath = prependAndStripSlash(contextPath);

                if (webApp != null && configurationUtil.getBoolean("kumuluzee.openapi.ui.enabled").orElse(true) && configurationUtil
                        .getBoolean("kumuluzee.openapi.enabled").orElse(true)) {

                    LOG.info("Swagger UI servlet registered on "+uiPath+ " (servlet context is implied)");

                    // create servlet that will serve static files
                    Map<String, String> swaggerUiParams = new HashMap<>();
                    swaggerUiParams.put("resourceBase", webApp.toString());
                    swaggerUiParams.put("uiPath", uiPath); //context already included in servlet resolution
                    server.registerServlet(UiServlet.class, uiPath + "/*", swaggerUiParams, 1);

                    //Used to override base URL when deploying behind reverse proxy
                    String baseUrl = configurationUtil.get("kumuluzee.openapi.base-url")
                            .orElse(serverUrl + contextPath);

                    String specUrl = baseUrl + specPath;
                    String uiBaseUrl = baseUrl + uiPath;
                    String redirUiPath = contextPath+uiPath;

                    LOG.info("Swagger UI can be accessed at "+uiBaseUrl);
                    LOG.info("Swagger UI spec URL resolved to "+specUrl+"/openapi.json");

                    // create filter that will redirect to Swagger UI with appropriate parameters
                    Map<String, String> swaggerUiFilterParams = new HashMap<>();
                    swaggerUiFilterParams.put("specUrl", specUrl);
                    swaggerUiFilterParams.put("uiPath", redirUiPath);
                    swaggerUiFilterParams.put("oauth2RedirectUrl", uiBaseUrl + "/oauth2-redirect.html");
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

    private String prependAndStripSlash(String s) {
        if (!s.startsWith("/")) s = s+"/";
        return StringUtils.stripEnd(s, "/");
    }
}
