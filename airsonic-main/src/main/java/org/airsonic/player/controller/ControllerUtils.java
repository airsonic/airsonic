package org.airsonic.player.controller;

import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;

/**
 * This class has been created to refactor code previously present
 * in the MultiController.
 */
public class ControllerUtils {

    public static String extractMatched(final HttpServletRequest request){

        String path = (String) request.getAttribute(
                HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String bestMatchPattern = (String ) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);

        AntPathMatcher apm = new AntPathMatcher();

        return apm.extractPathWithinPattern(bestMatchPattern, path);

    }
}
