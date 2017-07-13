package org.airsonic.player.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class JWTSecurityServiceTest {

    private final String key = "someKey";
    private final JWTSecurityService service = new JWTSecurityService(settingsWithKey(key));
    private final String uriString;
    private final Algorithm algorithm = JWTSecurityService.getAlgorithm(key);
    private final JWTVerifier verifier = JWT.require(algorithm).build();
    private final String expectedClaimString;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { "http://localhost:8080/airsonic/stream?id=4", "/airsonic/stream?id=4" },
                { "/airsonic/stream?id=4", "/airsonic/stream?id=4" },
        });
    }

    public JWTSecurityServiceTest(String uriString, String expectedClaimString) {
        this.uriString = uriString;
        this.expectedClaimString = expectedClaimString;
    }


    @Test
    public void addJWTToken() throws Exception {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(uriString);
        String actualUri = service.addJWTToken(builder).build().toUriString();
        String jwtToken = UriComponentsBuilder.fromUriString(actualUri).build().getQueryParams().getFirst(
                JWTSecurityService.JWT_PARAM_NAME);
        DecodedJWT verify = verifier.verify(jwtToken);
        Claim claim = verify.getClaim(JWTSecurityService.CLAIM_PATH);
        assertEquals(expectedClaimString, claim.asString());
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