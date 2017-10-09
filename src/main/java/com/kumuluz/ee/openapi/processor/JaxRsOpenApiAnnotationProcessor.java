package com.kumuluz.ee.openapi.processor;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.kumuluz.ee.openapi.models.OpenApiConfiguration;
import com.kumuluz.ee.openapi.utils.AnnotationProcessorUtil;
import io.swagger.oas.annotations.info.Info;
import io.swagger.oas.models.OpenAPI;
import io.swagger.oas.models.info.Contact;
import io.swagger.oas.models.info.License;
import io.swagger.oas.models.servers.Server;

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
            LOG.info("javax.ws.rs.core.Application not found, skipping JAX-RS CORS annotation processing");
            return false;
        }

        elements = roundEnv.getElementsAnnotatedWith(Path.class);
        elements.forEach(e -> getElementName(applicationElementNames, e));

        OpenApiConfiguration oac = new OpenApiConfiguration();
        oac.setPrettyPrint(true);
        oac.setResourcePackages(applicationElementNames);

        elements = roundEnv.getElementsAnnotatedWith(Info.class);

        OpenAPI openAPI = new OpenAPI();
        if (elements.size() == 1) {

            Element[] elems = elements.toArray(new Element[elements.size()]);
            io.swagger.oas.models.info.Info info = new io.swagger.oas.models.info.Info();

            Info infoAnnotation = elems[0].getAnnotation(Info.class);

            info.setTitle(infoAnnotation.title());
            info.setVersion(infoAnnotation.version());

            Contact contact = new Contact();
            contact.setEmail(infoAnnotation.contact().email());
            contact.setName(infoAnnotation.contact().name());
            contact.setUrl(infoAnnotation.contact().url());
            info.setContact(contact);

            info.setDescription(infoAnnotation.description());

            License license = new License();
            license.setName(infoAnnotation.license().name());
            license.setUrl(infoAnnotation.license().url());

            info.setLicense(license);

            info.setTermsOfService(infoAnnotation.termsOfService());

            openAPI.setInfo(info);

            Server server = new Server();

            ApplicationPath applicationPathAnnotation = elems[0].getAnnotation(ApplicationPath.class);
            if (applicationPathAnnotation != null && !applicationPathAnnotation.value().equals("")) {
                server.setUrl("http://localhost:8080/" + applicationPathAnnotation.value());
            }

            openAPI.addServersItem(server);

        }

        oac.setOpenAPI(openAPI);

        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            String jsonOAC = mapper.writeValueAsString(oac);

            AnnotationProcessorUtil.writeFile(jsonOAC, "openapi-configuration.json", filer);
        } catch (IOException e) {
            LOG.warning(e.getMessage());
        }

        return true;
    }

    private void getElementName(Set<String> jaxRsElementNames, Element e) {

        ElementKind elementKind = e.getKind();

        if (elementKind.equals(ElementKind.CLASS)) {
            jaxRsElementNames.add(e.toString().substring(0, e.toString().lastIndexOf(".")));
        }
    }
}
