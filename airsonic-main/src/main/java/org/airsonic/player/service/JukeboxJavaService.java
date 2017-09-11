package org.airsonic.player.service;

import com.github.biconou.AudioPlayer.JavaPlayer;
import com.github.biconou.AudioPlayer.api.*;
import org.airsonic.player.domain.*;
import org.airsonic.player.domain.Player;
import org.airsonic.player.util.FileUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;


/**
 * @author R??mi Cocula
 */
@Service
public class JukeboxJavaService {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(JukeboxJavaService.class);

    @Autowired
    private AudioScrobblerService audioScrobblerService;
    @Autowired
    private StatusService statusService;
    @Autowired
    private SettingsService settingsService;
    @Autowired
    private SecurityService securityService;
    @Autowired
    private MediaFileService mediaFileService;


    private TransferStatus status;
    private Map<String, com.github.biconou.AudioPlayer.api.Player> activeAudioPlayers = new Hashtable<>();
    private Map<String, List<com.github.biconou.AudioPlayer.api.Player>> activeAudioPlayersPerMixer = new Hashtable<>();
    private final static String DEFAULT_MIXER_ENTRY_KEY = "_default";

    /**
     * Finds the corresponding active audio player for a given airsonic player.
     * The JukeboxJavaService references all active audio players in a map indexed by airsonic player id.
     *
     * @param airsonicPlayer a given airsonic player.
     * @return the corresponding active audio player of null if none exists.
     */
    private com.github.biconou.AudioPlayer.api.Player retrieveAudioPlayerForAirsonicPlayer(Player airsonicPlayer) {
        com.github.biconou.AudioPlayer.api.Player foundPlayer = activeAudioPlayers.get(airsonicPlayer.getId());
        if (foundPlayer == null) {
            synchronized (activeAudioPlayers) {
                foundPlayer = initAudioPlayer(airsonicPlayer);
                if (foundPlayer == null) {
                    throw new RuntimeException("Did not initialized a player");
                } else {
                    activeAudioPlayers.put(airsonicPlayer.getId(), foundPlayer);
                    String mixer = airsonicPlayer.getJavaJukeboxMixer();
                    if (StringUtils.isBlank(mixer)) {
                        mixer = DEFAULT_MIXER_ENTRY_KEY;
                    }
                    List<com.github.biconou.AudioPlayer.api.Player> playersForMixer = activeAudioPlayersPerMixer.get(mixer);
                    if (playersForMixer == null) {
                        playersForMixer = new ArrayList<>();
                        activeAudioPlayersPerMixer.put(mixer, playersForMixer);
                    }
                    playersForMixer.add(foundPlayer);
                }
            }
        }
        return foundPlayer;
    }


    private com.github.biconou.AudioPlayer.api.Player initAudioPlayer(final Player airsonicPlayer) {

        if (!airsonicPlayer.getTechnology().equals(PlayerTechnology.JAVA_JUKEBOX)) {
            throw new RuntimeException("The player " + airsonicPlayer.getName() + " is not a java jukebox player");
        }

        log.info("begin initAudioPlayer");

        com.github.biconou.AudioPlayer.api.Player audioPlayer;

        if (StringUtils.isNotBlank(airsonicPlayer.getJavaJukeboxMixer())) {
            log.info("use mixer : {}", airsonicPlayer.getJavaJukeboxMixer());
            audioPlayer = new JavaPlayer(airsonicPlayer.getJavaJukeboxMixer());
        } else {
            log.info("use default mixer");
            audioPlayer = new JavaPlayer();
        }
        if (audioPlayer != null) {
            audioPlayer.registerListener(new PlayerListener() {
                @Override
                public void onBegin(int index, File currentFile) {
                    onSongStart(airsonicPlayer);
                }

                @Override
                public void onEnd(int index, File file) {
                    onSongEnd(airsonicPlayer);
                }

                @Override
                public void onFinished() {
                    airsonicPlayer.getPlayQueue().setStatus(PlayQueue.Status.STOPPED);
                }

                @Override
                public void onStop() {
                    airsonicPlayer.getPlayQueue().setStatus(PlayQueue.Status.STOPPED);
                }

                @Override
                public void onPause() {
                    // Nothing to do here
                }

            });
            log.info("New audio player {} has been initialized.", audioPlayer.toString());
        } else {
            throw new RuntimeException("AudioPlayer has not been initialized properly");
        }
        return audioPlayer;
    }


    public int getPosition(final Player airsonicPlayer) {

        if (!airsonicPlayer.getTechnology().equals(PlayerTechnology.JAVA_JUKEBOX)) {
            throw new RuntimeException("The player " + airsonicPlayer.getName() + " is not a java jukebox player");
        }
        com.github.biconou.AudioPlayer.api.Player audioPlayer = retrieveAudioPlayerForAirsonicPlayer(airsonicPlayer);
        if (audioPlayer == null) {
            return 0;
        } else {
            return audioPlayer.getPlayingInfos().currentAudioPositionInSeconds();
        }
    }

    public void setPosition(final Player airsonicPlayer, int positionInSeconds) {
        if (!airsonicPlayer.getTechnology().equals(PlayerTechnology.JAVA_JUKEBOX)) {
            throw new RuntimeException("The player " + airsonicPlayer.getName() + " is not a java jukebox player");
        }
        com.github.biconou.AudioPlayer.api.Player audioPlayer = retrieveAudioPlayerForAirsonicPlayer(airsonicPlayer);
        if (audioPlayer != null) {
            audioPlayer.setPos(positionInSeconds);
        } else {
            throw new RuntimeException("The player " + airsonicPlayer.getName() + " has no real audio player");
        }
    }

    public float getGain(final Player airsonicPlayer) {
        if (!airsonicPlayer.getTechnology().equals(PlayerTechnology.JAVA_JUKEBOX)) {
            throw new RuntimeException("The player " + airsonicPlayer.getName() + " is not a java jukebox player");
        }
        com.github.biconou.AudioPlayer.api.Player audioPlayer = retrieveAudioPlayerForAirsonicPlayer(airsonicPlayer);
        if (audioPlayer != null) {
            return audioPlayer.getGain();
        }
        return 0.5f;
    }

    public void setGain(final Player airsonicPlayer, final float gain) {
        if (!airsonicPlayer.getTechnology().equals(PlayerTechnology.JAVA_JUKEBOX)) {
            throw new RuntimeException("The player " + airsonicPlayer.getName() + " is not a java jukebox player");
        }
        com.github.biconou.AudioPlayer.api.Player audioPlayer = retrieveAudioPlayerForAirsonicPlayer(airsonicPlayer);
        log.debug("setGain : gain={}", gain);
        if (audioPlayer != null) {
            audioPlayer.setGain(gain);
        } else {
            throw new RuntimeException("The player " + airsonicPlayer.getName() + " has no real audio player");
        }
    }


    private void onSongStart(Player player) {
        MediaFile file = player.getPlayQueue().getCurrentFile();
        log.info("[onSongStart] {} starting jukebox for \"{}\"", player.getUsername(), FileUtil.getShortPath(file.getFile()));
        if (status != null) {
            statusService.removeStreamStatus(status);
            status = null;
        }
        status = statusService.createStreamStatus(player);
        status.setFile(file.getFile());
        status.addBytesTransfered(file.getFileSize());
        mediaFileService.incrementPlayCount(file);
        scrobble(player, file, false);
    }

    private void onSongEnd(Player player) {
        MediaFile file = player.getPlayQueue().getCurrentFile();
        log.info("[onSongEnd] {} stopping jukebox for \"{}\"", player.getUsername(), FileUtil.getShortPath(file.getFile()));
        if (status != null) {
            statusService.removeStreamStatus(status);
            status = null;
        }
        scrobble(player, file, true);
    }

    private void scrobble(Player player, MediaFile file, boolean submission) {
        if (player.getClientId() == null) {  // Don't scrobble REST players.
            audioScrobblerService.register(file, player.getUsername(), submission, null);
        }
    }

    /**
     * Plays the playqueue of a jukebox player starting at the beginning.
     *
     * @param airsonicPlayer
     */
    public void play(Player airsonicPlayer) {
        log.debug("begin play jukebox : player = id:{};name:{}", airsonicPlayer.getId(), airsonicPlayer.getName());

        com.github.biconou.AudioPlayer.api.Player audioPlayer = retrieveAudioPlayerForAirsonicPlayer(airsonicPlayer);

        // Control user authorizations
        User user = securityService.getUserByName(airsonicPlayer.getUsername());
        if (!user.isJukeboxRole()) {
            log.warn("{} is not authorized for jukebox playback.", user.getUsername());
            return;
        }

        log.debug("Different file to play -> start a new play list");
        if (airsonicPlayer.getPlayQueue().getCurrentFile() != null) {
            audioPlayer.setPlayList(new PlayList() {

                @Override
                public File getNextAudioFile() throws IOException {
                    airsonicPlayer.getPlayQueue().next();
                    return getCurrentAudioFile();
                }

                @Override
                public File getCurrentAudioFile() {
                    MediaFile current = airsonicPlayer.getPlayQueue().getCurrentFile();
                    if (current != null) {
                        return airsonicPlayer.getPlayQueue().getCurrentFile().getFile();
                    } else {
                        return null;
                    }
                }

                @Override
                public int getSize() {
                    return airsonicPlayer.getPlayQueue().size();
                }

                @Override
                public int getIndex() {
                    return airsonicPlayer.getPlayQueue().getIndex();
                }
            });
            // Close any other player using the same mixer.
            String mixer = airsonicPlayer.getJavaJukeboxMixer();
            if (StringUtils.isBlank(mixer)) {
                mixer = DEFAULT_MIXER_ENTRY_KEY;
            }
            List<com.github.biconou.AudioPlayer.api.Player> playersForSameMixer = activeAudioPlayersPerMixer.get(mixer);
            playersForSameMixer.forEach(player -> {
                if (player != audioPlayer) {
                    player.close();
                }
            });
            audioPlayer.play();
        }
    }

    public void start(Player airsonicPlayer) throws Exception {
        log.debug("begin start jukebox : player = id:{};name:{}", airsonicPlayer.getId(), airsonicPlayer.getName());

        com.github.biconou.AudioPlayer.api.Player audioPlayer = retrieveAudioPlayerForAirsonicPlayer(airsonicPlayer);

        // Control user authorizations
        User user = securityService.getUserByName(airsonicPlayer.getUsername());
        if (!user.isJukeboxRole()) {
            log.warn("{} is not authorized for jukebox playback.", user.getUsername());
            return;
        }

        log.debug("PlayQueue.Status is {}", airsonicPlayer.getPlayQueue().getStatus());
        audioPlayer.play();
    }

    public void stop(Player airsonicPlayer) throws Exception {
        log.debug("begin stop jukebox : player = id:{};name:{}", airsonicPlayer.getId(), airsonicPlayer.getName());

        com.github.biconou.AudioPlayer.api.Player audioPlayer = retrieveAudioPlayerForAirsonicPlayer(airsonicPlayer);

        // Control user authorizations
        User user = securityService.getUserByName(airsonicPlayer.getUsername());
        if (!user.isJukeboxRole()) {
            log.warn("{} is not authorized for jukebox playback.", user.getUsername());
            return;
        }

        log.debug("PlayQueue.Status is {}", airsonicPlayer.getPlayQueue().getStatus());
        audioPlayer.pause();
    }

    /**
     * @param airsonicPlayer
     * @param index
     * @throws Exception
     */
    public void skip(Player airsonicPlayer, int index, int offset) throws Exception {
        log.debug("begin skip jukebox : player = id:{};name:{}", airsonicPlayer.getId(), airsonicPlayer.getName());

        com.github.biconou.AudioPlayer.api.Player audioPlayer = retrieveAudioPlayerForAirsonicPlayer(airsonicPlayer);

        // Control user authorizations
        User user = securityService.getUserByName(airsonicPlayer.getUsername());
        if (!user.isJukeboxRole()) {
            log.warn("{} is not authorized for jukebox playback.", user.getUsername());
            return;
        }

        if (index == 0 && offset == 0) {
            play(airsonicPlayer);
        } else {
            if (offset == 0) {
                audioPlayer.stop();
                audioPlayer.play();
            } else {
                audioPlayer.setPos(offset);
            }
        }
    }


    public void setAudioScrobblerService(AudioScrobblerService audioScrobblerService) {
        this.audioScrobblerService = audioScrobblerService;
    }

    public void setStatusService(StatusService statusService) {
        this.statusService = statusService;
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    public void setMediaFileService(MediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
    }

}
