package com.kumuluz.ee.openapi.utils;

/**
 * ApiSpecsURLs class.
 *
 * @author Zvone Gazvoda
 * @since 1.0.0
 */
public class ApiSpecURL {

    public ApiSpecURL(String name, String url) {
        this.name = name;
        this.url = url;
    }

    private String name;
    private String url;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
