package org.airsonic.test.cucumber.steps.api;

import cucumber.api.java8.En;
import org.airsonic.test.cucumber.server.AirsonicServer;
import org.airsonic.test.domain.SavedHttpResponse;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StreamStepDef implements En {

    private CloseableHttpClient client;
    private List<SavedHttpResponse> responses = new ArrayList<>();
    private String streamName;

    public StreamStepDef(AirsonicServer server) {
        this.client = HttpClientBuilder.create().build();
        Given("Media file (.*) is added", (String streamName) -> {
            this.streamName = streamName;
            server.uploadToDefaultMusicFolder(
                    Paths.get(this.getClass().getResource("/blobs/stream/" + streamName).toURI()),
                    "");
        });

        Then("The response bytes are equal", () -> {
            for (int i = 0; i < responses.size(); i++) {
                checkBody(responses.get(i), i);
            }
        });
        Then("^Print debug output$", () -> {
            for (int i = 0; i < responses.size(); i++) {
                System.out.printf("Response %d%n", i + 1);
                printDebugInfo(responses.get(i), 2);
            }
        });
        Then("^The length headers are correct$", () -> {
            responses.forEach(this::checkLengths);
        });
        When("^A stream is consumed$", () -> {
            while(shouldDoRequest()) {
                responses.add(consumeResponse(doRequest(server)));
            }
        });

    }

    private void checkBody(SavedHttpResponse savedHttpResponse, int iter) throws URISyntaxException, IOException {
        String expectedBodyResource = String.format("/blobs/stream/"+streamName+"/responses/%d.dat", iter+1);
        byte[] expected = IOUtils.toByteArray(
                this.getClass()
                    .getResource(expectedBodyResource)
                    .toURI());
        // TODO if debug...
//        FileUtils.writeByteArrayToFile(
//                new File(String.format("/tmp/bytearray-%d", iter+1)),
//                savedHttpResponse.getBody());
//            HexDump.dump(expected, 0, System.out, 0);

        Assert.assertArrayEquals(expected, savedHttpResponse.getBody());

    }

    private void printDebugInfo(SavedHttpResponse savedHttpResponse, int indentLevel) {
        String indent = StringUtils.repeat(' ', indentLevel);
        System.out.println(indent + "Headers:");
        for (Header header : savedHttpResponse.getHeaders()) {
            System.out.print(indent + header.getName());
            System.out.print(": ");
            for (HeaderElement element : header.getElements()) {
                System.out.print(element);
                System.out.print(", ");
            }
            System.out.println();
        }
    }

    private void checkLengths(SavedHttpResponse response) {
        Header header = response.getHeader("Content-Length");
        Assert.assertEquals(response.getBody().length, Integer.parseInt(header.getValue()));
    }

    private boolean shouldDoRequest() {
        return responses.isEmpty() || isUnconsumedContent(responses.get(responses.size() - 1));
    }

    private Pattern CONTENT_RANGE_PATTERN = Pattern.compile("^bytes (\\d+)-(\\d+)/(\\d+)$");
    private boolean isUnconsumedContent(SavedHttpResponse savedHttpResponse) {
        Header header = savedHttpResponse.getHeader("Content-Range");
        Matcher matcher = CONTENT_RANGE_PATTERN.matcher(header.getValue());
        if(!matcher.matches()) {
            throw new RuntimeException("Unexpected Content-Range format");
        }
        int start = Integer.parseInt(matcher.group(1));
        int end = Integer.parseInt(matcher.group(2));
        int total = Integer.parseInt(matcher.group(3));
        return (total - 1) > end;
    }

    private CloseableHttpResponse doRequest(AirsonicServer server) throws IOException {
        RequestBuilder builder = RequestBuilder.create("GET").setUri(server.getBaseUri() + "/rest/stream");
        builder.addParameter("id", "2"); // TODO abstract this out
        builder.addHeader("Range", "bytes=0-");
        builder.addHeader("Accept", "audio/webm,audio/ogg,audio/wav,audio/*;");
        server.addRestParameters(builder);
        return client.execute(builder.build());
    }

    SavedHttpResponse consumeResponse(CloseableHttpResponse response) throws IOException {
        byte[] body = EntityUtils.toByteArray(response.getEntity());
        List<Header> headers = Arrays.asList(response.getAllHeaders());
        response.close();
        return new SavedHttpResponse(headers, body);
    }

}
