package org.airsonic.player.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

@RunWith(Parameterized.class)
public class JWTSecurityServiceTest {

    private final String key = "someKey";
    private final SecurityService securityService = mock(SecurityService.class);
    private final JWTSecurityService service = new JWTSecurityService(settingsWithKey(key));
    private final Algorithm algorithm = JWTSecurityService.getAlgorithm(key);
    private final JWTVerifier verifier = JWT.require(algorithm).build();

    private final String uriString;
    private final String expectedClaimString;
    private final String username;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { "http://localhost:8080/airsonic/stream?id=4", "/airsonic/stream?id=4", "bob" },
                { "/airsonic/stream?id=4", "/airsonic/stream?id=4", "frank" },
        });
    }

    public JWTSecurityServiceTest(String uriString, String expectedClaimString, String username) {
        this.uriString = uriString;
        this.expectedClaimString = expectedClaimString;
        this.username = username;
    }

    @Test
    public void addJWTToken() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new User(username, "password", Collections.emptyList()), "password"));

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(uriString);
        String actualUri = service.addJWTToken(builder).build().toUriString();
        String jwtToken = UriComponentsBuilder.fromUriString(actualUri).build().getQueryParams().getFirst(
                JWTSecurityService.JWT_PARAM_NAME);
        DecodedJWT verify = verifier.verify(jwtToken);

        Claim claimPath = verify.getClaim(JWTSecurityService.CLAIM_PATH);
        Claim claimUsername = verify.getClaim(JWTSecurityService.CLAIM_USERNAME);

        assertEquals(expectedClaimString, claimPath.asString());
        assertEquals(username, claimUsername.asString());
    }

    private SettingsService settingsWithKey(String jwtKey) {
        return new SettingsService() {
            @Override
            public String getJWTKey() {
                return jwtKey;
            }
        };
    }

}