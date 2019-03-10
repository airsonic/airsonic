package org.airsonic.player.spring;

import org.airsonic.player.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LoggingExceptionResolver implements HandlerExceptionResolver, Ordered {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingExceptionResolver.class);

    @Override
    public ModelAndView resolveException(
            HttpServletRequest request, HttpServletResponse response, Object o, Exception e
    ) {
        if (e instanceof org.apache.catalina.connector.ClientAbortException) {
            LOG.info("{}: Client unexpectedly closed connection while loading {} ({})", request.getRemoteAddr(), Util.getURLForRequest(request), e.getCause().toString());
        } else {
            LOG.error("{}: An exception occurred while loading {}", request.getRemoteAddr(), Util.getURLForRequest(request), e);
        }
        return null;
    }

    @Override
    public int getOrder() {
        return Integer.MIN_VALUE;
    }
}
