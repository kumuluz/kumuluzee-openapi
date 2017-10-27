# KumuluzEE OpenAPI
[![Build Status](https://img.shields.io/travis/kumuluz/kumuluzee-openapi/master.svg?style=flat)](https://travis-ci.org/kumuluz/kumuluzee-openapi)

> KumuluzEE OpenAPI extension provides powerful tools to incorporate the OpenAPI Specification to your microservice.

KumuluzEE OpenAPI extension allows you to document microservice APIs using OpenAPI v3 compliant annotations. Extension will automatically hook-up servlet that will 
serve your API specifications on endpoint ```/api-specs/<jax-rs application-base-path>/openapi.[json|yaml]```. Furthermore, extensions allows you to integrate Swagger-UI into your
microservice that will visualize APIs documentation and allow you to interact with your APIs resources.
 
More details: [OpenAPI v3 Specification](https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.0.md).

## Usage

You can enable KumuluzEE OpenAPI support by adding the following dependency:
```xml
<dependency>
    <groupId>com.kumuluz.ee.openapi</groupId>
    <artifactId>kumuluzee-openapi</artifactId>
    <version>${kumuluzee-openapi.version}</version>
</dependency>
```

### OpenAPI configuration

When kumuluzee-openapi dependency is included in the project, you can start documenting your REST API using [Swagger-Core Annotations](https://github.com/swagger-api/swagger-core/wiki/Annotations-2.X).

#### Documenting application class:
```java
@SecurityScheme(name = "openid-connect", type = SecuritySchemeType.OPENIDCONNECT, 
                openIdConnectUrl = "http://auth-server-url/.well-known/openid-configuration")
@OpenAPIDefinition(info = @Info(title = "Rest API", version = "v1", description = "JavaSI API for managing conference.", 
                    security = @SecurityRequirement(name = "openid-connect"), servers = @Server(url ="http://localhost:8080/v1")))
@ApplicationPath("v1")
public class JavaSiApplication extends Application {...}
```

#### Documenting resource class and operations:
```java
@Path("sessions")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SessionsResource {

    @Operation(description = "Returns list of sessions.", summary = "Sessions list", tags = "sessions", responses = {
            @ApiResponse(responseCode = "200",
                    description = "List of sessions",
                    content = @Content(
                            schema = @Schema(implementation
                            = Session.class)),
                    headers = {@Header(name = "X-Total-Count",
                            schema = @Schema(type = "int"))}
                    )})
    @SecurityRequirement(name = "openid-connect")
    @GET
    public Response getSessions() {...} 
    ...
}
```

### Accessing API specification

Build and run project using:

```bash
mvn clean package
java -jar target/${project.build.finalName}.jar
```

After startup API specification will be available at:

**http://<-hostname-:<-port->/api-specs/<-application-base-path->/openapi.[json,yaml]**

Example:

http://localhost:8080/api-specs/v1/openapi.json


### Adding Swagger-UI

To serve API specification in visual form and to allow API consumers to interact with API resources you can add Swagger-UI by setting 
 **includeSwaggerUI** to ***true*** in **kumuluzee-maven-plugin** configuration.

```xml
<configuration>
    <specificationConfig>
        <includeSwaggerUI>true</includeSwaggerUI>
    </specificationConfig>
</configuration>
```

After startup Swagger-UI is available at: http://localhost:8080/api-specs/ui (for all APIs).

By default Swagger-UI will not be added to application.


### Support for mutliple JAX-RS Application classes in single microservice

If your microservice contains multiple JAX-RS Applications, e.g. two versions of API, you have to privde some additional configuration for OpenAPI.

First, resources that belong to specific JAX-RS Application must be defined in ```getClasses()``` method

```java
    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();

        classes.add(SessionsResource.class);
        classes.add(SpeakersResource.class);

        return classes;
    }
```

and second, you have to provide list of packages to openapi extension for each JAX-RS Application by providing **apiSpecifications** in configuration of **kumuluzee-maven-plugin**:

```xml
<configuration>
    <specificationConfig>
        <includeSwaggerUI>true</includeSwaggerUI>
        
        <apiSpecifications>
            <apiSpecification>
                <applicationPath>/v1</applicationPath>
                <resourcePackages>
                    com.kumuluz.ee.samples.v1.resources
                </resourcePackages>
            </apiSpecification>
            <apiSpecification>
                <applicationPath>/v2</applicationPath>
                <resourcePackages>
                    com.kumuluz.ee.samples.v2.resources
                </resourcePackages>
            </apiSpecification>
        </apiSpecifications>
        
    </specificationConfig>
</configuration>
```

Multiple JAX-RS applications in singe JAR only work without CDI.


## Changelog

Recent changes can be viewed on Github on the [Releases Page](https://github.com/kumuluz/kumuluzee-openapi/releases)


## Contribute

See the [contributing docs](https://github.com/kumuluz/kumuluzee-openapi/blob/master/CONTRIBUTING.md)

When submitting an issue, please follow the 
[guidelines](https://github.com/kumuluz/kumuluzee-openapi/blob/master/CONTRIBUTING.md#bugs).

When submitting a bugfix, write a test that exposes the bug and fails before applying your fix. Submit the test 
alongside the fix.

When submitting a new feature, add tests that cover the feature.

## License

MIT
