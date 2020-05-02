package org.airsonic.test;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.nio.file.Paths;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class StreamIT {
    @Test
    public void testStreamFlacAsMp3() throws Exception {
        testFileStreaming("dead");
    }

    @Test
    public void testStreamM4aAsMp3() throws Exception {
        testFileStreaming("dance");
    }

    @Test
    public void testStreamMp3() throws Exception {
        testFileStreaming("piano");
    }

    private void testFileStreaming(String file) throws Exception {
        Scanner.uploadToDefaultMusicFolder(
                Paths.get(this.getClass().getResource("/blobs/stream/" + file + "/input").toURI()),
                "");
        Scanner.doScan();
        String mediaFileId = Scanner.getMediaFilesInMusicFolder().parallelStream()
                .filter(x -> StringUtils.containsIgnoreCase(x.getTitle(), file))
                .findAny()
                .map(x -> x.getId())
                .orElseThrow(() -> new RuntimeException("no media file id matched"));

        byte[] fromServer = Scanner.getMediaFileData(mediaFileId);
        String expectedBodyResource = String.format("/blobs/stream/" + file + "/responses/1.dat");
        byte[] expected = IOUtils.toByteArray(StreamIT.class.getResourceAsStream(expectedBodyResource));
        if (!StringUtils.isBlank(System.getProperty("IntegrationTestsOutputDirectory", ""))) {
            Path fromServerPath = Paths.get(System.getProperty("IntegrationTestsOutputDirectory", ""), file + ".dat");
            FileUtils.writeByteArrayToFile(fromServerPath.toFile(), fromServer);
        }
        assertThat(fromServer).containsExactly(expected);
    }
}
