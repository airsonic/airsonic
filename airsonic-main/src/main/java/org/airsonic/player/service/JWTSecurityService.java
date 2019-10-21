package org.airsonic.player.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.airsonic.player.domain.SonosLink;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Date;

@Service("jwtSecurityService")
public class JWTSecurityService {
    private static final Logger LOG = LoggerFactory.getLogger(JWTSecurityService.class);

    public static final String JWT_PARAM_NAME = "jwt";
    public static final String CLAIM_PATH = "path";

    public static final String CLAIM_USERNAME = "username";
    public static final String CLAIM_HOUSEHOLDID = "householdid";
    public static final String CLAIM_LINKCODE = "linkcode";

    // TODO make this configurable
    public static final int DEFAULT_DAYS_VALID_FOR = 7;
    private static SecureRandom secureRandom = new SecureRandom();

    private final SettingsService settingsService;

    @Autowired
    public JWTSecurityService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public static String generateKey() {
        BigInteger randomInt = new BigInteger(130, secureRandom);
        return randomInt.toString(32);
    }

    public static Algorithm getAlgorithm(String jwtKey) {
        return Algorithm.HMAC256(jwtKey);
    }

    private static String createToken(String jwtKey, String path, Date expireDate) {
        UriComponents components = UriComponentsBuilder.fromUriString(path).build();
        String query = components.getQuery();
        String claim = components.getPath() + (!StringUtils.isBlank(query) ? "?" + components.getQuery() : "");
        LOG.debug("Creating token with claim " + claim);
        return JWT.create()
                .withClaim(CLAIM_PATH, claim)
                .withExpiresAt(expireDate)
                .sign(getAlgorithm(jwtKey));
    }

    public String addJWTToken(String uri) {
        return addJWTToken(UriComponentsBuilder.fromUriString(uri)).build().toString();
    }

    public UriComponentsBuilder addJWTToken(UriComponentsBuilder builder) {
        return addJWTToken(builder, DateUtils.addDays(new Date(), DEFAULT_DAYS_VALID_FOR));
    }

    public UriComponentsBuilder addJWTToken(UriComponentsBuilder builder, Date expires) {
        String token = JWTSecurityService.createToken(
                settingsService.getJWTKey(),
                builder.toUriString(),
                expires);
        builder.queryParam(JWTSecurityService.JWT_PARAM_NAME, token);
        return builder;
    }

    public static DecodedJWT verify(String jwtKey, String token) {
        Algorithm algorithm = JWTSecurityService.getAlgorithm(jwtKey);
        JWTVerifier verifier = JWT.require(algorithm).build();
        return verifier.verify(token);
    }

    public DecodedJWT verify(String credentials) {
        return verify(settingsService.getJWTKey(), credentials);
    }


    /**
     * Create an unexpiring token
     */
    public String createSonosToken(String username, String householdId, String linkCode){
        return JWT.create()
                .withClaim(CLAIM_USERNAME, username)
                .withClaim(CLAIM_HOUSEHOLDID, householdId)
                .withClaim(CLAIM_LINKCODE, linkCode)
                .sign(getAlgorithm(settingsService.getJWTKey()));
    }


    public SonosLink verifySonosLink(String sonosLinkToken) {
        DecodedJWT jwt = verify(sonosLinkToken);

        SonosLink sonosLink = new SonosLink(jwt.getClaim(CLAIM_USERNAME).asString(),
                jwt.getClaim(CLAIM_HOUSEHOLDID).asString(), jwt.getClaim(CLAIM_LINKCODE).asString());

        return sonosLink;
    }
}
