package org.libresonic.player.filter;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Created by remi on 12/01/17.
 */
public class MetricsFilter implements Filter {

    private final MetricRegistry metrics = new MetricRegistry();
    JmxReporter reporter;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        reporter = JmxReporter.forRegistry(metrics)
                .convertRatesTo(TimeUnit.SECONDS.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
        reporter.start();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        HttpServletRequest httpServletRequest = (HttpServletRequest)request;
        Timer requestTimer = metrics.timer(MetricRegistry.name(MetricsFilter.class, httpServletRequest.getRequestURI()));

        Timer.Context requestTimerContext =  requestTimer.time();

        chain.doFilter(request, response);
        requestTimerContext.stop();
    }

    @Override
    public void destroy() {
        reporter.stop();
    }
}
