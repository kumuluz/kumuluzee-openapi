package com.kumuluz.ee.openapi.filters;

import com.kumuluz.ee.configuration.utils.ConfigurationUtil;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class SwaggerUIFilter implements Filter {

    private FilterConfig filterConfig;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.filterConfig = filterConfig;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        ConfigurationUtil configurationUtil = ConfigurationUtil.getInstance();
        if (configurationUtil.getBoolean("kumuluzee.openapi.enabled").orElse(true)) {

            HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
            HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;

            String path = httpServletRequest.getServletPath();

            if (path.contains("/ui")) {
                if ((httpServletRequest.getQueryString() == null || !httpServletRequest.getQueryString().contains
                        ("url"))) {
                    String url = filterConfig.getInitParameter("url");
                    if (httpServletRequest.getPathInfo() != null) {
                        httpServletResponse.sendRedirect("/api-specs/ui" + httpServletRequest.getPathInfo() + "?url=" + url);
                    } else {
                        httpServletResponse.sendRedirect("/api-specs/ui/?url=" + url);
                    }
                } else {
                    filterChain.doFilter(httpServletRequest, httpServletResponse);
                }

            } else {

                filterChain.doFilter(servletRequest, servletResponse);
            }

        } else {
            ((HttpServletResponse) servletResponse).sendError(HttpServletResponse.SC_NOT_FOUND);
        }

    }

    @Override
    public void destroy() {

    }
}