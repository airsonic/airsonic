package org.airsonic.player.api.jukebox;

import org.airsonic.player.domain.Player;
import org.airsonic.player.domain.PlayerTechnology;
import org.airsonic.player.service.TranscodingService;
import org.airsonic.player.service.jukebox.AudioPlayer;
import org.airsonic.player.service.jukebox.AudioPlayerFactory;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.security.test.context.support.WithMockUser;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class AirsonicRestApiJukeboxLegacyIntTest extends AirsonicRestApiJukeboxIntTest {

    @SpyBean
    private TranscodingService transcodingService;
    @MockBean
    protected AudioPlayerFactory audioPlayerFactory;

    private AudioPlayer mockAudioPlayer;

    @Before
    @Override
    public void setup() throws Exception {
        super.setup();
        mockAudioPlayer = mock(AudioPlayer.class);
        when(audioPlayerFactory.createAudioPlayer(any(), any())).thenReturn(mockAudioPlayer);
        doReturn(null).when(transcodingService).getTranscodedInputStream(any());
    }

    @Override
    protected final void createTestPlayer() {
        Player jukeBoxPlayer = new Player();
        jukeBoxPlayer.setName(JUKEBOX_PLAYER_NAME);
        jukeBoxPlayer.setUsername("admin");
        jukeBoxPlayer.setClientId(CLIENT_NAME + "-jukebox");
        jukeBoxPlayer.setTechnology(PlayerTechnology.JUKEBOX);
        playerService.createPlayer(jukeBoxPlayer);
    }

    @Test
    @WithMockUser(username = "admin")
    @Override
    public void jukeboxStartActionTest() throws Exception {
        super.jukeboxStartActionTest();
        verify(mockAudioPlayer).play();
    }

    @Test
    @WithMockUser(username = "admin")
    @Override
    public void jukeboxStopActionTest() throws Exception {
        super.jukeboxStopActionTest();
        verify(mockAudioPlayer).play();
        verify(mockAudioPlayer).pause();
    }

}
