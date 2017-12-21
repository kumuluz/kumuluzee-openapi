package com.kumuluz.ee.openapi.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * ApiSpecsURLs class.
 *
 * @author Zvone Gazvoda
 * @since 1.0.0
 */
public class ApiSpecURLs {

    private List<ApiSpecURL> urls;


    public List<ApiSpecURL> getUrls() {
        if (urls == null) {
            urls = new ArrayList<>();
        }
        return urls;
    }

    public void setUrls(List<ApiSpecURL> urls) {
        this.urls = urls;
    }

    public void addApiSpecUrl(ApiSpecURL url) {
        this.getUrls().add(url);
    }
}
