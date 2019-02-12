package org.airsonic.test.cucumber.steps.api;

import cucumber.api.java8.En;
import org.airsonic.test.cucumber.server.AirsonicServer;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.xmlunit.builder.Input;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.xmlunit.matchers.CompareMatcher.isIdenticalTo;

public class PingStepDef implements En {

    private CloseableHttpResponse response;
    private CloseableHttpClient client;

    public PingStepDef(AirsonicServer server) {
        this.client = HttpClientBuilder.create().build();
        When("^A ping request is sent$", () -> {
            HttpGet httpGet = new HttpGet(server.getBaseUri() + "/rest/ping");
            this.response = client.execute(httpGet);
        });
        Then("^A required parameter response is received$", () -> {
            if(response == null) {
                throw new IllegalStateException();
            }
            try {
                StatusLine statusLine = response.getStatusLine();
                assertEquals(statusLine.getStatusCode(), 200);
                HttpEntity entity = response.getEntity();
                String actual = EntityUtils.toString(entity);
                assertThat(
                        actual,
                        isIdenticalTo(
                                Input.fromStream(
                                        getClass().getResourceAsStream("/blobs/ping/missing-auth.xml")))
                                .ignoreWhitespace());
            } finally {
                response.close();
            }
        });
    }

}
