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
import org.eclipse.jetty.servlet.DefaultServlet;

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
                    } catch (IndexOutOfBoundsException e) {
                        LOG.warning("Servers not provided in annotation OpenAPIDefinition, will use default server url: " + serverUrl);
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
                            LOG.warning("Server URL defined in annotation OpenAPIDefinition is invalid: " + e.getMessage());
                        }
                    }
                }

                applicationPath = StringUtils.strip(applicationPath, "/");

                Map<String, String> swaggerUiParams = new HashMap<>();

                URL webApp = ResourceUtils.class.getClassLoader().getResource("swagger-ui");

                if (webApp != null && configurationUtil.getBoolean("kumuluzee.openapi.ui.enabled").orElse(true) && configurationUtil
                        .getBoolean("kumuluzee.openapi.spec.enabled").orElse(true)) {
                    swaggerUiParams.put("resourceBase", webApp.toString());
                    server.registerServlet(DefaultServlet.class, "/api-specs/ui/*", swaggerUiParams, 1);

                    Map<String, String> swaggerUiFilterParams = new HashMap<>();

                    swaggerUiFilterParams.put("url", serverUrl + "/api-specs/" + applicationPath + "/openapi.json");
                    server.registerFilter(SwaggerUIFilter.class, "/api-specs/ui/*", swaggerUiFilterParams);

                } else {
                    LOG.severe("OpenAPI UI or OpenAPI Spec is disabled, will not initialize UI.");
                }
            }
        }
    }

    private boolean targetClassIsProxied(Class targetClass) {
        return targetClass.getCanonicalName().contains("$Proxy");
    }
}
