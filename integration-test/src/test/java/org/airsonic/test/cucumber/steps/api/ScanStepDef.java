package org.airsonic.test.cucumber.steps.api;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
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

    public ScanStepDef(AirsonicServer server) {
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
        final JsonReader jsonreader = Json.createReader(responseAsString);
        boolean ret = jsonreader.getJsonObject("subsonic-response").getJsonObject("ScanStatus").getBoolean("scanning");
        jsonreader.close();
        return ret;
    }

}
