package com.kumuluz.ee.openapi.processor;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.kumuluz.ee.openapi.models.OpenApiConfiguration;
import com.kumuluz.ee.openapi.utils.AnnotationProcessorUtil;
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
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Created by zvoneg on 21/09/2017.
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

        elements = roundEnv.getElementsAnnotatedWith(ApplicationPath.class);

        OpenApiConfiguration oac = null;

        if (elements.size() != 0) {
            for (Element element : elements) {
                OpenAPI openAPI = new OpenAPI();

                io.swagger.oas.models.info.Info info = new io.swagger.oas.models.info.Info();

                OpenAPIDefinition definitionAnnotation = element.getAnnotation(OpenAPIDefinition.class);

                if (definitionAnnotation != null) {

                    info.setTitle(definitionAnnotation.info().title());
                    info.setVersion(definitionAnnotation.info().version());

                    Contact contact = new Contact();
                    contact.setEmail(definitionAnnotation.info().contact().email());
                    contact.setName(definitionAnnotation.info().contact().name());
                    contact.setUrl(definitionAnnotation.info().contact().url());
                    info.setContact(contact);

                    info.setDescription(definitionAnnotation.info().description());

                    License license = new License();
                    license.setName(definitionAnnotation.info().license().name());
                    license.setUrl(definitionAnnotation.info().license().url());

                    info.setLicense(license);

                    info.setTermsOfService(definitionAnnotation.info().termsOfService());

                    openAPI.setInfo(info);

                    ApplicationPath applicationPathAnnotation = element.getAnnotation(ApplicationPath.class);
                    if (definitionAnnotation.servers().length == 0) {
                        if (applicationPathAnnotation != null && !applicationPathAnnotation.value().equals("")) {
                            Server server = new Server();
                            server.setUrl("http://localhost:8080/" + applicationPathAnnotation.value());
                            openAPI.addServersItem(server);
                        }
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
                    if (elements.size() == 1) {
                        oac.setResourcePackages(resourceElementNames);
                    }
                    oac.setOpenAPI(openAPI);

                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        mapper.enable(SerializationFeature.INDENT_OUTPUT);
                        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
                        String jsonOAC = mapper.writeValueAsString(oac);

                        String path = applicationPathAnnotation.value();

                        path = StringUtils.strip(path, "/");

                        AnnotationProcessorUtil.writeFile(jsonOAC, "api-specs/" + path + "/openapi-configuration.json", filer);
                    } catch (IOException e) {
                        LOG.warning(e.getMessage());
                    }
                }
            }

            elements.forEach(e -> getElementName(applicationElementNames, e));
            try {
                AnnotationProcessorUtil.writeFileSet(applicationElementNames, "META-INF/services/javax.ws.rs.core.Application", filer);
            } catch (IOException e) {
                LOG.warning(e.getMessage());
            }
        }
        return false;
    }

    private void getElementPackage(Set<String> jaxRsElementNames, Element e) {

        ElementKind elementKind = e.getKind();

        if (elementKind.equals(ElementKind.CLASS)) {
            resourceElementNames.add(e.toString().substring(0, e.toString().lastIndexOf(".")));
        }
    }

    private void getElementName(Set<String> jaxRsElementNames, Element e) {

        ElementKind elementKind = e.getKind();

        if (elementKind.equals(ElementKind.CLASS)) {
            applicationElementNames.add(e.toString());
        }
    }
}
