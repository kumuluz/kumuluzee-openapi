package com.kumuluz.ee.openapi.ui.filters;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * SwaggerUIFilter class.
 *
 * @author Zvone Gazvoda
 * @since 1.0.0
 */
public class SwaggerUIFilter implements Filter {

    private String specUrl;
    private String uiPath;
    private String oauth2RedirectUrl;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.specUrl = filterConfig.getInitParameter("specUrl");
        this.uiPath = filterConfig.getInitParameter("uiPath");
        this.oauth2RedirectUrl = filterConfig.getInitParameter("oauth2RedirectUrl");
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;

        String path = httpServletRequest.getContextPath()+httpServletRequest.getServletPath();

        // check if request is for UI
        if (path.contains(uiPath)) {
            // match static files and urls with existing parameter url=... set
            Pattern staticFiles = Pattern.compile("(\\.css|\\.js|\\.html|url=)");
            String requestQueryString = httpServletRequest.getRequestURI();
            if (httpServletRequest.getQueryString() != null) {
                requestQueryString += httpServletRequest.getQueryString();
            }
            if (!staticFiles.matcher(requestQueryString).find()) {
                // not a static file, redirect to appropriate url
                httpServletResponse.sendRedirect(uiPath +
                        "/?url=" + specUrl +
                        "&oauth2RedirectUrl=" + oauth2RedirectUrl);
            } else {
                // static file, leave as is
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