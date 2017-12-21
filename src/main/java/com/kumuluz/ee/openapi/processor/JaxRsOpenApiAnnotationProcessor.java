package com.kumuluz.ee.openapi.processor;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.kumuluz.ee.openapi.models.OpenApiConfiguration;
import com.kumuluz.ee.openapi.utils.AnnotationProcessorUtil;
import com.kumuluz.ee.openapi.utils.ApiSpecURL;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import com.kumuluz.ee.openapi.utils.ApiSpecURLs;
import io.swagger.oas.annotations.OpenAPIDefinition;
import io.swagger.oas.models.OpenAPI;
import io.swagger.oas.models.info.Contact;
import io.swagger.oas.models.info.License;
import io.swagger.oas.models.servers.Server;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * JaxRsOpenApiAnnotationProcessor class - process Jax-RS and OpenAPI annotations and build openapi configurations.
 *
 * @author Zvone Gazvoda
 * @since 1.0.0
 */
public class JaxRsOpenApiAnnotationProcessor extends AbstractProcessor {
    private static final Logger LOG = Logger.getLogger(JaxRsOpenApiAnnotationProcessor.class.getName());

    private Set<String> applicationElementNames = new HashSet<>();
    private Set<String> resourceElementNames = new HashSet<>();

    private Filer filer;

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton("*");
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        filer = processingEnv.getFiler();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<? extends Element> elements;

        try {
            Class.forName("javax.ws.rs.core.Application");
        } catch (ClassNotFoundException e) {
            LOG.info("javax.ws.rs.core.Application not found, skipping OpenAPI annotation processing");
            return false;
        }

        elements = roundEnv.getElementsAnnotatedWith(Path.class);
        elements.forEach(e -> getElementPackage(resourceElementNames, e));

        elements = roundEnv.getElementsAnnotatedWith(OpenAPIDefinition.class);
        elements.forEach(e -> getElementName(applicationElementNames, e));

        OpenApiConfiguration oac = null;

        ApiSpecURLs urls = new ApiSpecURLs();

        if (elements.size() != 0) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

            for (Element element : elements) {
                OpenAPI openAPI = new OpenAPI();

                io.swagger.oas.models.info.Info info = new io.swagger.oas.models.info.Info();

                OpenAPIDefinition definitionAnnotation = element.getAnnotation(OpenAPIDefinition.class);

                if (definitionAnnotation != null) {

                    info.setTitle(definitionAnnotation.info().title());
                    info.setVersion(definitionAnnotation.info().version());

                    Contact contact = null;
                    if (!definitionAnnotation.info().contact().email().equals("")) {
                        contact = new Contact();
                        contact.setEmail(definitionAnnotation.info().contact().email());
                    }
                    if (!definitionAnnotation.info().contact().name().equals("")) {
                        if (contact == null) contact = new Contact();
                        contact.setName(definitionAnnotation.info().contact().name());
                    }
                    if (!definitionAnnotation.info().contact().url().equals("")) {
                        if (contact == null) contact = new Contact();
                        contact.setUrl(definitionAnnotation.info().contact().url());
                    }
                    info.setContact(contact);

                    if (!definitionAnnotation.info().description().equals("")) {
                        info.setDescription(definitionAnnotation.info().description());
                    }

                    License license = null;

                    if (!definitionAnnotation.info().license().name().equals("")) {
                        license = new License();
                        license.setName(definitionAnnotation.info().license().name());
                    }
                    if (!definitionAnnotation.info().license().url().equals("")) {
                        if (license == null) license = new License();
                        license.setUrl(definitionAnnotation.info().license().url());
                    }
                    info.setLicense(license);

                    if (definitionAnnotation.info().termsOfService().equals("")) {
                        info.setTermsOfService(definitionAnnotation.info().termsOfService());
                    }

                    openAPI.setInfo(info);

                    ApplicationPath applicationPathAnnotation = element.getAnnotation(ApplicationPath.class);
                    if (definitionAnnotation.servers().length == 0) {
                        Server server = new Server();
                        if (applicationPathAnnotation != null) {
                            server.setUrl("http://localhost:8080/" + applicationPathAnnotation.value());
                        } else {
                            server.setUrl("http://localhost:8080/");
                        }
                        openAPI.addServersItem(server);
                    } else {
                        for (io.swagger.oas.annotations.servers.Server s : definitionAnnotation.servers()) {
                            Server server = new Server();
                            server.setUrl(s.url());
                            server.setDescription(s.description());
                            openAPI.addServersItem(server);
                        }
                    }

                    oac = new OpenApiConfiguration();

                    oac.setPrettyPrint(true);
                    if (applicationElementNames.size() == 1) {
                        oac.setResourcePackages(resourceElementNames);
                    }

                    oac.setOpenAPI(openAPI);

                    try {

                        String jsonOAC = mapper.writeValueAsString(oac);

                        String path = new URL(openAPI.getServers().get(0).getUrl()).getPath();

                        path = StringUtils.strip(path, "/");

                        URL serverUrl = new URL(openAPI.getServers().get(0).getUrl());

                        ApiSpecURL apiSpecURL;
                        if (path.equals("")) {
                            AnnotationProcessorUtil.writeFile(jsonOAC, "api-specs/openapi-configuration.json", filer);

                            apiSpecURL = new ApiSpecURL(oac.getOpenAPI().getInfo().getTitle() + " - " + oac.getOpenAPI()
                                    .getInfo().getVersion(), serverUrl.getProtocol() + "://" + serverUrl.getHost() + ":" + serverUrl
                                    .getPort() + "/api-specs/openapi-configuration.json");
                        } else {
                            AnnotationProcessorUtil.writeFile(jsonOAC, "api-specs/" + path + "/openapi-configuration.json", filer);

                            apiSpecURL = new ApiSpecURL(oac.getOpenAPI().getInfo().getTitle() + " - " + oac.getOpenAPI()
                                    .getInfo().getVersion(), serverUrl.getProtocol() + "://" + serverUrl.getHost() + ":" +
                                    serverUrl.getPort() + "/api-specs/" + path + "/openapi-configuration.json");

                        }

                        urls.addApiSpecUrl(apiSpecURL);
                    } catch (IOException e) {
                        LOG.warning(e.getMessage());
                    }
                }
            }

            try {
                AnnotationProcessorUtil.writeFileSet(applicationElementNames, "META-INF/services/javax.ws.rs.core.Application", filer);
            } catch (IOException e) {
                LOG.warning(e.getMessage());
            }

            if (urls.getUrls().size() != 0) {
                try {
                    String urlsJson = mapper.writeValueAsString(urls);

                    LOG.warning("URLS: " + urlsJson);

                    java.nio.file.Path path = Paths.get("/classes/swagger-ui/api-specs/ui/index.html");
                    String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);

                    LOG.warning(content);

                    content.replace("url: \"http://petstore.swagger.io/v2/swagger.json\"", urlsJson);

                    Files.write(path, content.getBytes(StandardCharsets.UTF_8));

                } catch (IOException e) {
                    LOG.warning(e.getMessage());
                }
            }

        }

        return false;
    }

    private void getElementPackage(Set<String> jaxRsElementNames, Element e) {

        ElementKind elementKind = e.getKind();

        if (elementKind.equals(ElementKind.CLASS)) {
            jaxRsElementNames.add(e.toString().substring(0, e.toString().lastIndexOf(".")));
        }
    }

    private void getElementName(Set<String> jaxRsElementNames, Element e) {

        ElementKind elementKind = e.getKind();

        if (elementKind.equals(ElementKind.CLASS)) {
            if (((TypeElement) e).getSuperclass().toString().equals(Application.class.getTypeName())) {
                jaxRsElementNames.add(e.toString());
            }
        }
    }
}
