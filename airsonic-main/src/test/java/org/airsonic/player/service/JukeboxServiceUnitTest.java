package org.airsonic.player.service;

import org.airsonic.player.domain.Player;
import org.airsonic.player.domain.PlayerTechnology;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

@RunWith(value = MockitoJUnitRunner.class)
public class JukeboxServiceUnitTest {

    private JukeboxService jukeboxService;
    @Mock
    private JukeboxLegacySubsonicService jukeboxLegacySubsonicService;
    @Mock
    private JukeboxJavaService jukeboxJavaService;
    private Player jukeboxPlayer;
    private Player legacyJukeboxPlayer;
    private Player nonJukeboxPlayer;

    @Before
    public void setUp() {
        jukeboxService = new JukeboxService(jukeboxLegacySubsonicService, jukeboxJavaService);
        jukeboxPlayer = generateJukeboxPlayer();
        legacyJukeboxPlayer = generateLegacyJukeboxPlayer();
        nonJukeboxPlayer = generateNonJukeboxPlayer();
    }

    private Player generateNonJukeboxPlayer() {
        Player player = new Player();
        player.setId(0);
        player.setTechnology(PlayerTechnology.WEB);
        return player;
    }

    private Player generateLegacyJukeboxPlayer() {
        Player player = new Player();
        player.setId(1);
        player.setTechnology(PlayerTechnology.JUKEBOX);
        return player;
    }

    private Player generateJukeboxPlayer() {
        Player player = new Player();
        player.setId(2);
        player.setTechnology(PlayerTechnology.JAVA_JUKEBOX);
        return player;
    }

    @Test
    public void setPositionWithJukeboxPlayer() {
        // When
        jukeboxService.setPosition(jukeboxPlayer, 0);
        // Then
        verify(jukeboxJavaService).setPosition(jukeboxPlayer, 0);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void setPositionWithLegacyJukeboxPlayer() {
        // When
        jukeboxService.setPosition(legacyJukeboxPlayer, 0);
    }

    @Test
    public void getGainWithJukeboxPlayer() {
        // When
        jukeboxService.getGain(jukeboxPlayer);
        // Then
        verify(jukeboxJavaService).getGain(jukeboxPlayer);
    }

    @Test
    public void getGainWithLegacyJukeboxPlayer() {
        // When
        jukeboxService.getGain(legacyJukeboxPlayer);
        // Then
        verify(jukeboxLegacySubsonicService).getGain();
    }

    @Test
    public void getGainWithNonJukeboxPlayer() {
        // When
        float gain = jukeboxService.getGain(nonJukeboxPlayer);
        // Then
        assertThat(gain).isEqualTo(0);
    }

    @Test
    public void updateJukebox() throws Exception {
        // When
        jukeboxService.updateJukebox(legacyJukeboxPlayer, 0);
        // Then
        verify(jukeboxLegacySubsonicService).updateJukebox(legacyJukeboxPlayer, 0);
    }

    @Test
    public void getPositionWithJukeboxPlayer() {
        // When
        jukeboxService.getPosition(jukeboxPlayer);
        // Then
        verify(jukeboxJavaService).getPosition(jukeboxPlayer);
    }

    @Test
    public void getPositionWithLegacyJukeboxPlayer() {
        // When
        jukeboxService.getPosition(legacyJukeboxPlayer);
        // Then
        verify(jukeboxLegacySubsonicService).getPosition();
    }

    @Test
    public void getPasitionWithNonJukeboxPlayer() {
        // When
        int position = jukeboxService.getPosition(nonJukeboxPlayer);
        // Then
        assertThat(position).isEqualTo(0);
    }

    @Test
    public void setGainWithJukeboxPlayer() {
        // When
        jukeboxService.setGain(jukeboxPlayer, 0.5f);
        // Then
        verify(jukeboxJavaService).setGain(jukeboxPlayer, 0.5f);
    }

    @Test
    public void setGaintWithLegacyJukeboxPlayer() {
        // When
        jukeboxService.setGain(legacyJukeboxPlayer, 0.5f);
        // Then
        verify(jukeboxLegacySubsonicService).setGain(0.5f);
    }

    @Test
    public void startWithJukeboxPlayer() throws Exception {
        // When
        jukeboxService.start(jukeboxPlayer);
        // Then
        verify(jukeboxJavaService).start(jukeboxPlayer);
    }

    @Test
    public void startWithLegacyJukeboxPlayer() throws Exception {
        // When
        jukeboxService.start(legacyJukeboxPlayer);

        // Then
        verify(jukeboxLegacySubsonicService).updateJukebox(legacyJukeboxPlayer, 0);
    }

    @Test
    public void playWithJukeboxPlayer() throws Exception {
        // When
        jukeboxService.play(jukeboxPlayer);
        // Then
        verify(jukeboxJavaService).play(jukeboxPlayer);
    }

    @Test
    public void playWithLegacyJukeboxPlayer() throws Exception {
        // When
        jukeboxService.play(legacyJukeboxPlayer);
        // Then
        verify(jukeboxLegacySubsonicService).updateJukebox(legacyJukeboxPlayer, 0);
    }

    @Test
    public void stopWithJukeboxPlayer() throws Exception {
        // When
        jukeboxService.stop(jukeboxPlayer);
        // Then
        verify(jukeboxJavaService).stop(jukeboxPlayer);
    }

    @Test
    public void stopWithLegacyJukeboxPlayer() throws Exception {
        // When
        jukeboxService.stop(legacyJukeboxPlayer);
        // Then
        verify(jukeboxLegacySubsonicService).updateJukebox(legacyJukeboxPlayer, 0);
    }


    @Test
    public void skipWithJukeboxPlayer() throws Exception {
        // When
        jukeboxService.skip(jukeboxPlayer, 0, 1);
        // Then
        verify(jukeboxJavaService).skip(jukeboxPlayer, 0, 1);
    }

    @Test
    public void skipWithLegacyJukeboxPlayer() throws Exception {
        // When
        jukeboxService.skip(legacyJukeboxPlayer, 0, 1);
        // Then
        verify(jukeboxLegacySubsonicService).updateJukebox(legacyJukeboxPlayer, 1);
    }

    @Test
    public void canControlWithJukeboxPlayer() {
        // When
        boolean canControl = jukeboxService.canControl(jukeboxPlayer);
        // Then
        assertThat(canControl).isEqualTo(true);
    }

    @Test
    public void canControlWithLegacyJukeboxPlayer() {
        // When
        when(jukeboxLegacySubsonicService.getPlayer()).thenReturn(legacyJukeboxPlayer);
        boolean canControl = jukeboxService.canControl(legacyJukeboxPlayer);
        // Then
        assertThat(canControl).isEqualTo(true);
    }

    @Test
    public void canControlWithLegacyJukeboxPlayerWrongPlayer() {
        // When
        when(jukeboxLegacySubsonicService.getPlayer()).thenReturn(nonJukeboxPlayer);
        boolean canControl = jukeboxService.canControl(legacyJukeboxPlayer);
        // Then
        assertThat(canControl).isEqualTo(false);
    }
}