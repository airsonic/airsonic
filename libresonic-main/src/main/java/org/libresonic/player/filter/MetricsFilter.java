package org.libresonic.player.filter;

import org.libresonic.player.monitor.MetricsManager;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;

import java.io.IOException;

/**
 * Created by remi on 12/01/17.
 */
public class MetricsFilter implements Filter {

    @Autowired
    private MetricsManager metricsManager;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest)request;

        String timerName = httpServletRequest.getRequestURI();
        try (MetricsManager.Timer t = metricsManager.condition(timerName.contains("main.view")).timer(this,timerName)) {
            chain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {
    }
}
