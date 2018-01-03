package com.kumuluz.ee.openapi.ui.filters;

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
    }

    @Override
    public void destroy() {

    }
}