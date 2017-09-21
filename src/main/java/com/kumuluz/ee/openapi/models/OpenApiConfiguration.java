package com.kumuluz.ee.openapi.models;

import java.util.Set;

import io.swagger.oas.models.OpenAPI;

/**
 * Created by zvoneg on 21/09/2017.
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
