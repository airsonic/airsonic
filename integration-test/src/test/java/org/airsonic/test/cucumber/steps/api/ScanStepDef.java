package org.airsonic.test.cucumber.steps.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import cucumber.api.java8.En;
import org.airsonic.test.cucumber.server.AirsonicServer;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.subsonic.restapi.Response;

import java.io.IOException;

public class ScanStepDef implements En {

    private final AirsonicServer server;
    private CloseableHttpResponse response;
    private CloseableHttpClient client;
    private ObjectMapper mapper = new ObjectMapper();

    public ScanStepDef(AirsonicServer server) {
        mapper.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
        this.client = HttpClientBuilder.create().build();
        this.server = server;

        Given("a scan is done", () -> {
            Assert.assertFalse(isScanning());

            RequestBuilder builder = RequestBuilder.create("GET").setUri(server.getBaseUri() + "/rest/startScan");
            server.addRestParameters(builder);
            response = client.execute(builder.build());
            System.out.println(EntityUtils.toString(response.getEntity()));
            Long waitTime = 30000L;
            Long sleepTime = 1000L;
            while(waitTime > 0 && isScanning()) {
                waitTime -= sleepTime;
                Thread.sleep(sleepTime);
            }

            Assert.assertFalse(isScanning());
        });

    }

    private boolean isScanning() throws IOException {
        RequestBuilder builder = RequestBuilder.create("GET").setUri(server.getBaseUri() + "/rest/getScanStatus");
        builder.addParameter("f", "json");
        server.addRestParameters(builder);
        response = client.execute(builder.build());

        String responseAsString = EntityUtils.toString(response.getEntity());
        JsonNode jsonNode = mapper.readTree(responseAsString).get("subsonic-response");
        Response response = mapper.treeToValue(jsonNode, Response.class);
        return response.getScanStatus().isScanning();
    }

}
