package org.airsonic.player.service;

import org.airsonic.player.domain.Transcoding;
import org.airsonic.player.util.HomeRule;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.junit4.SpringRunner;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TranscodingServiceIntTest {

    @Autowired
    private TranscodingService transcodingService;
    @SpyBean
    private PlayerService playerService;

    @ClassRule
    public static final HomeRule classRule = new HomeRule(); // sets airsonic.home to a temporary dir

    @Test
    public void createTranscodingTest() {
        // Given
        Transcoding transcoding = new Transcoding(null,
                "test-transcoding",
                "mp3",
                "wav",
                "step1",
                "step2",
                "step3",
                true);

        transcodingService.createTranscoding(transcoding);
        verify(playerService).getAllPlayers();
    }
}
