package org.airsonic.player.service;

import org.airsonic.player.domain.*;
import org.airsonic.player.service.jukebox.JavaPlayerFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class JukeboxJavaServiceUnitTest {

    private static final String USER_NAME = "admin";

    private JukeboxJavaService service;
    @Mock
    private Player airsonicPlayer;
    @Mock
    private AudioScrobblerService audioScrobblerService;
    @Mock
    private StatusService statusService;
    @Mock
    private SecurityService securityService;
    @Mock
    private MediaFileService mediaFileService;
    @Mock
    private JavaPlayerFactory javaPlayerFactory;
    @Mock
    private com.github.biconou.AudioPlayer.api.Player player;
    @Mock
    private User user;
    @Mock
    private PlayQueue playQueue;
    @Mock
    private MediaFile mediaFile;


    @Before
    public void setup() {
        service = new JukeboxJavaService(audioScrobblerService, statusService, securityService, mediaFileService, javaPlayerFactory);
        when(airsonicPlayer.getTechnology()).thenReturn(PlayerTechnology.JAVA_JUKEBOX);
        when(airsonicPlayer.getUsername()).thenReturn(USER_NAME);
        when(javaPlayerFactory.createJavaPlayer()).thenReturn(player);
        when(securityService.getUserByName(USER_NAME)).thenReturn(user);
        when(user.isJukeboxRole()).thenReturn(true);
        when(airsonicPlayer.getPlayQueue()).thenReturn(playQueue);
        when(playQueue.getCurrentFile()).thenReturn(mediaFile);
    }

    @Test
    public void play() {
        // When
        service.play(airsonicPlayer);
        // Then
        verify(javaPlayerFactory).createJavaPlayer();
        verify(player).play();
    }

    @Test
    public void playForNonDefaultMixer() {
        // Given
        when(airsonicPlayer.getJavaJukeboxMixer()).thenReturn("mixer");
        when(javaPlayerFactory.createJavaPlayer("mixer")).thenReturn(player);
        // When
        service.play(airsonicPlayer);
        // Then
        verify(javaPlayerFactory).createJavaPlayer("mixer");
        verify(player).play();
    }

    @Test
    public void playAndStop() {
        // When
        service.play(airsonicPlayer);
        // Then
        verify(javaPlayerFactory).createJavaPlayer();
        verify(player).play();
        // When
        service.stop(airsonicPlayer);
        // Then
        verifyNoMoreInteractions(javaPlayerFactory);
        verify(player).pause();

    }

    @Test
    public void playWithNonJukeboxUser() {
        // Given
        when(user.isJukeboxRole()).thenReturn(false);
        // When
        service.play(airsonicPlayer);
        // Then
        verify(javaPlayerFactory).createJavaPlayer();
        verify(player, never()).play();
    }

    @Test(expected = RuntimeException.class)
    public void playWithNonJukeboxPlayer() {
        // Given
        when(airsonicPlayer.getTechnology()).thenReturn(PlayerTechnology.WEB);
        // When
        service.play(airsonicPlayer);
    }

    @Test
    public void playWithNoPlayQueueEmpty() {
        // Given
        when(playQueue.getCurrentFile()).thenReturn(null);
        // When
        service.play(airsonicPlayer);
        // Then
        verify(javaPlayerFactory).createJavaPlayer();
        verify(player, never()).play();
    }

    @Test(expected = RuntimeException.class)
    public void playerInitProblem() {
        // Given
        when(javaPlayerFactory.createJavaPlayer()).thenReturn(null);
        // When
        service.play(airsonicPlayer);
    }
}