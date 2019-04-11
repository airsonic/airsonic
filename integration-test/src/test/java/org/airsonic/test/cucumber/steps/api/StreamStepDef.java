package org.airsonic.test.cucumber.steps.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.subsonic.restapi.Child;
import org.subsonic.restapi.MusicFolder;
import org.subsonic.restapi.Response;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StreamStepDef implements En {

    private CloseableHttpClient client;
    private List<SavedHttpResponse> responses = new ArrayList<>();
    private String streamName;
    private String mediaFileId;
    private ObjectMapper mapper = new ObjectMapper();

    public StreamStepDef(AirsonicServer server) {
        mapper.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
        this.client = HttpClientBuilder.create().build();
        Given("Media file (.*) is added", (String streamName) -> {
            this.streamName = streamName;
            server.uploadToDefaultMusicFolder(
                    Paths.get(this.getClass().getResource("/blobs/stream/" + streamName + "/input").toURI()),
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
        Then("^The length headers are absent$", () -> {
            responses.forEach(this::noLengths);
        });
        When("^A stream is consumed$", () -> {
            while(shouldDoRequest()) {
                responses.add(consumeResponse(doRequest(server)));
            }
        });
        Then("^Save response bodies to files$", () -> {
            for (int i = 0; i < responses.size(); i++) {
                saveBody(responses.get(i), i);
            }
        });
        And("^The media file id is found$", () -> {
            mediaFileId = getMediaFilesInMusicFolder(server).get(0).getId();
        });

    }

    private List<Child> getMediaFilesInMusicFolder(AirsonicServer server) throws IOException {
        RequestBuilder builder = RequestBuilder.create("GET").setUri(server.getBaseUri() + "/rest/getMusicFolders");
        builder.addParameter("f", "json");
        server.addRestParameters(builder);
        CloseableHttpResponse response = client.execute(builder.build());

        String responseAsString = EntityUtils.toString(response.getEntity());
        JsonNode jsonNode = mapper.readTree(responseAsString).get("subsonic-response");
        Response subsonicResponse = mapper.treeToValue(jsonNode, Response.class);
        List<MusicFolder> musicFolder = subsonicResponse.getMusicFolders().getMusicFolder();
        MusicFolder music = musicFolder
                .stream()
                .filter(folder -> Objects.equals(folder.getName(), "Music"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No top level folder named Music"));

        return getMediaFiles(server, music.getId());
    }

    private List<Child> getMediaFiles(AirsonicServer server, int folderId) throws IOException {
        RequestBuilder builder = RequestBuilder.create("GET").setUri(server.getBaseUri() + "/rest/getIndexes");
        builder.addParameter("f", "json");
        builder.addParameter("musicFolderId", String.valueOf(folderId));
        server.addRestParameters(builder);
        CloseableHttpResponse response = client.execute(builder.build());

        String responseAsString = EntityUtils.toString(response.getEntity());
        JsonNode jsonNode = mapper.readTree(responseAsString).get("subsonic-response");
        Response subsonicResponse = mapper.treeToValue(jsonNode, Response.class);
        return subsonicResponse.getIndexes().getChild();
    }

    private void saveBody(SavedHttpResponse savedHttpResponse, int iter) throws IOException {
        FileUtils.writeByteArrayToFile(
                new File(String.format("/tmp/bytearray-%d", iter+1)),
                savedHttpResponse.getBody());
        // TODO if debug...
//            HexDump.dump(expected, 0, System.out, 0);
    }

    private void checkBody(SavedHttpResponse savedHttpResponse, int iter) throws URISyntaxException, IOException {
        String expectedBodyResource = String.format("/blobs/stream/"+streamName+"/responses/%d.dat", iter+1);
        byte[] expected = IOUtils.toByteArray(
                this.getClass()
                    .getResourceAsStream(expectedBodyResource));

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

    private void noLengths(SavedHttpResponse response) {
        Header header = response.getHeader("Content-Length");
        Assert.assertNull(header);
    }

    private boolean shouldDoRequest() {
        return responses.isEmpty() || isUnconsumedContent(responses.get(responses.size() - 1));
    }

    private Pattern CONTENT_RANGE_PATTERN = Pattern.compile("^bytes (\\d+)-(\\d+)/(\\d+)$");
    private boolean isUnconsumedContent(SavedHttpResponse savedHttpResponse) {
        ContentRange contentRange = getContentRange(savedHttpResponse);
        if(contentRange == null) {
            return false;
        }
        return (contentRange.getTotal() - 1) > contentRange.getEnd();
    }

    private ContentRange getContentRange(SavedHttpResponse savedHttpResponse) {
        Header header = savedHttpResponse.getHeader("Content-Range");
        if(header == null) {
            return null;
        }
        Matcher matcher = CONTENT_RANGE_PATTERN.matcher(header.getValue());
        if(!matcher.matches()) {
            throw new RuntimeException("Unexpected Content-Range format");
        }
        int start = Integer.parseInt(matcher.group(1));
        int end = Integer.parseInt(matcher.group(2));
        int total = Integer.parseInt(matcher.group(3));
        return new ContentRange(start, end, total);
    }

    private String calculateRange() {
        Integer start = null;
        Integer end = null;
        if(responses.isEmpty()) {
            start = 0;
        } else {
            SavedHttpResponse lastResponse = responses.get(responses.size() - 1);
            ContentRange contentRange = getContentRange(lastResponse);
            start = contentRange.getEnd();
        }
        return start + "-" + (end == null ? "" : end);
    }

    private CloseableHttpResponse doRequest(AirsonicServer server) throws IOException {
        RequestBuilder builder = RequestBuilder.create("GET").setUri(server.getBaseUri() + "/rest/stream");
        builder.addParameter("id", mediaFileId);

        String range = calculateRange();
        System.out.println("In request "+ (responses.size() + 1) +" asking for range " + range);
        builder.addHeader("Range", "bytes=" + range);
        builder.addHeader("Accept", "audio/webm,audio/ogg,audio/wav,audio/*;");
        server.addRestParameters(builder);
        return client.execute(builder.build());
    }

    private SavedHttpResponse consumeResponse(CloseableHttpResponse response) throws IOException {
        byte[] body = EntityUtils.toByteArray(response.getEntity());
        List<Header> headers = Arrays.asList(response.getAllHeaders());
        response.close();
        return new SavedHttpResponse(headers, body);
    }

    private class ContentRange {
        private final int start;
        private final int end;
        private final int total;

        public ContentRange(int start, int end, int total) {
            this.start = start;
            this.end = end;
            this.total = total;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }

        public int getTotal() {
            return total;
        }
    }
}
