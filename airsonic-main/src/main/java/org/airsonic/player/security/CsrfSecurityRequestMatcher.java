package org.airsonic.player.security;

import org.springframework.security.web.util.matcher.RegexRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

import java.util.Arrays;
import java.util.List;

/**
 * See
 *
 * http://blogs.sourceallies.com/2014/04/customizing-csrf-protection-in-spring-security/
 * https://docs.spring.io/spring-security/site/docs/current/reference/html/appendix-namespace.html#nsa-csrf
 *
 *
 */
@Component
public class CsrfSecurityRequestMatcher implements RequestMatcher {
    static private List<String> allowedMethods = Arrays.asList("GET", "HEAD", "TRACE", "OPTIONS");
    private List<RegexRequestMatcher> whiteListedMatchers;

    public CsrfSecurityRequestMatcher() {
        this.whiteListedMatchers = Arrays.asList(
            new RegexRequestMatcher("/dwr/.*\\.dwr", "POST"),
            new RegexRequestMatcher("/rest/.*\\.view(\\?.*)?", "POST"),
            new RegexRequestMatcher("/search(?:\\.view)?", "POST")
        );
    }

    @Override
    public boolean matches(HttpServletRequest request) {
        boolean skipCSRF = allowedMethods.contains(request.getMethod()) ||
            whiteListedMatchers.stream().anyMatch(matcher -> matcher.matches(request));
        return !skipCSRF;
    }
}
