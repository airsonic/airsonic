package org.airsonic.player.api.jukebox;

import com.github.biconou.AudioPlayer.JavaPlayer;
import org.airsonic.player.domain.Player;
import org.airsonic.player.domain.PlayerTechnology;
import org.airsonic.player.service.jukebox.JavaPlayerFactory;
import org.junit.Before;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AirsonicRestApiJukeboxIntTest extends AbstractAirsonicRestApiJukeboxIntTest {

    @MockBean
    protected JavaPlayerFactory javaPlayerFactory;

    @Before
    @Override
    public void setup() throws Exception {
        super.setup();
        JavaPlayer mockJavaPlayer = mock(JavaPlayer.class);
        when(mockJavaPlayer.getPlayingInfos()).thenReturn(() -> 0);
        when(mockJavaPlayer.getGain()).thenReturn(0.75f);
        when(javaPlayerFactory.createJavaPlayer()).thenReturn(mockJavaPlayer);
    }

    @Override
    protected void createTestPlayer() {
        Player jukeboxPlayer = new Player();
        jukeboxPlayer.setName(JUKEBOX_PLAYER_NAME);
        jukeboxPlayer.setUsername("admin");
        jukeboxPlayer.setClientId(CLIENT_NAME + "-jukebox");
        jukeboxPlayer.setTechnology(PlayerTechnology.JAVA_JUKEBOX);
        playerService.createPlayer(jukeboxPlayer);
    }

}