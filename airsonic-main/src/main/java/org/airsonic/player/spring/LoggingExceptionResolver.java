package org.airsonic.player.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LoggingExceptionResolver implements HandlerExceptionResolver, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(LoggingExceptionResolver.class);

    @Override
    public ModelAndView resolveException(
            HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, Exception e
    ) {
        logger.error("Exception occurred", e);
        return null;
    }

    @Override
    public int getOrder() {
        return Integer.MIN_VALUE;
    }
}
