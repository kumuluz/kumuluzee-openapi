package com.kumuluz.ee.openapi.models;

import io.swagger.oas.models.OpenAPI;

import java.util.Set;

/**
 * OpenApiConfiguration class.
 *
 * @author Zvone Gazvoda
 * @since 1.0.0
 */
public class OpenApiConfiguration {

    private Set<String> resourcePackages;
    private boolean prettyPrint;
    private OpenAPI openAPI;

    public Set<String> getResourcePackages() {
        return resourcePackages;
    }

    public void setResourcePackages(Set<String> resourcePackages) {
        this.resourcePackages = resourcePackages;
    }

    public OpenAPI getOpenAPI() {
        return openAPI;
    }

    public void setOpenAPI(OpenAPI openAPI) {
        this.openAPI = openAPI;
    }

    public boolean isPrettyPrint() {
        return prettyPrint;
    }

    public void setPrettyPrint(boolean prettyPrint) {
        this.prettyPrint = prettyPrint;
    }
}
