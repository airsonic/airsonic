package org.airsonic.player.service.jukebox;

import com.github.biconou.AudioPlayer.JavaPlayer;
import com.github.biconou.AudioPlayer.api.Player;
import org.springframework.stereotype.Component;

@Component
public class JavaPlayerFactory {

    public Player createJavaPlayer() {
        return new JavaPlayer();
    }

    public Player createJavaPlayer(String mixerName) {
        return new JavaPlayer(mixerName);
    }
}
