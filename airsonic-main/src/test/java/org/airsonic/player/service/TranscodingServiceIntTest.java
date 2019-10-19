package org.airsonic.player.service;

import org.airsonic.player.domain.Transcoding;
import org.airsonic.player.util.HomeRule;
import org.airsonic.player.util.MigrationConstantsRule;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles({ "legacy" })
public class TranscodingServiceIntTest {

    @Autowired
    private TranscodingService transcodingService;
    @SpyBean
    private PlayerService playerService;

    // sets airsonic.home to a temporary dir
    @ClassRule
    public static TestRule rules = RuleChain.outerRule(new HomeRule()).around(new MigrationConstantsRule());

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
