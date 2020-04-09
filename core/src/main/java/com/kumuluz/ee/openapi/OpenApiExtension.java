package com.kumuluz.ee.openapi;

import com.kumuluz.ee.common.Extension;
import com.kumuluz.ee.common.config.EeConfig;
import com.kumuluz.ee.common.dependencies.EeComponentDependency;
import com.kumuluz.ee.common.dependencies.EeComponentType;
import com.kumuluz.ee.common.dependencies.EeExtensionDef;
import com.kumuluz.ee.common.wrapper.KumuluzServerWrapper;
import com.kumuluz.ee.configuration.utils.ConfigurationUtil;
import com.kumuluz.ee.jetty.JettyServletServer;
import io.swagger.v3.jaxrs2.integration.OpenApiServlet;
import org.apache.commons.lang3.StringUtils;

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

            ConfigurationUtil configurationUtil = ConfigurationUtil.getInstance();

            if (configurationUtil.getBoolean("kumuluzee.openapi.enabled").orElse(true)) {

                LOG.info("Initializing OpenAPI extension.");

                JettyServletServer server = (JettyServletServer) kumuluzServerWrapper.getServer();

                List<Application> applications = new ArrayList<>();
                ServiceLoader.load(Application.class).forEach(applications::add);

                if (applications.size() == 1) {

                    Map<String, String> specParams = new HashMap<>();

                    String mapping = ConfigurationUtil.getInstance().get("kumuluzee.openapi.servlet.mapping")
                            .orElse("/api-specs");
                    mapping = prependAndStripSlash(mapping);

                    specParams.put("openApi.configuration.location", mapping+"/openapi-configuration.json");
                    server.registerServlet(OpenApiServlet.class, mapping+"/*", specParams, 1);

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

    public static String prependAndStripSlash(String s) {
        if (!s.startsWith("/")) s = s+"/";
        return StringUtils.stripEnd(s, "/");
    }
}
