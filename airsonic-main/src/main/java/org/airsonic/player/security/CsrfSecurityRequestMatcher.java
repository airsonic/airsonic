package org.airsonic.player.security;

import org.springframework.security.web.util.matcher.RegexRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;

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
    private Pattern allowedMethods = Pattern.compile("^(GET|HEAD|TRACE|OPTIONS)$");
    private Collection<RegexRequestMatcher> whiteListedMatchers;

    public CsrfSecurityRequestMatcher() {
        Collection<RegexRequestMatcher> whiteListedMatchers = new ArrayList<>();
        whiteListedMatchers.add(new RegexRequestMatcher("/dwr/.*\\.dwr", "POST"));
        whiteListedMatchers.add(new RegexRequestMatcher("/rest/.*\\.view(\\?.*)?", "POST"));
        whiteListedMatchers.add(new RegexRequestMatcher("/search(?:\\.view)?", "POST"));
        this.whiteListedMatchers = whiteListedMatchers;
    }

    @Override
    public boolean matches(HttpServletRequest request) {

        boolean skipCSRF =
                allowedMethods.matcher(request.getMethod()).matches() ||
                whiteListedMatchers.stream().anyMatch(matcher -> matcher.matches(request));

        return !skipCSRF;
    }
}