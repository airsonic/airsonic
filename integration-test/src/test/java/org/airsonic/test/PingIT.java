package org.airsonic.test;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;
import org.xmlunit.builder.Input;

import static org.junit.Assert.assertThat;
import static org.xmlunit.matchers.CompareMatcher.isIdenticalTo;

public class PingIT {
    @Test
    public void pingMissingAuthTest() {
        ResponseEntity<String> response = Scanner.rest.getForEntity(
                UriComponentsBuilder.fromHttpUrl(Scanner.SERVER + "/rest/ping").toUriString(),
                String.class);

        assertThat(response.getStatusCode(), Matchers.equalTo(HttpStatus.OK));
        assertThat(response.getBody(),
                isIdenticalTo(Input.fromStream(getClass().getResourceAsStream("/blobs/ping/missing-auth.xml")))
                        .ignoreWhitespace());
    }
}
