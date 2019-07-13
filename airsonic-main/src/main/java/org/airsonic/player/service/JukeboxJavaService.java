package org.airsonic.player.service;

import com.github.biconou.AudioPlayer.api.PlayList;
import com.github.biconou.AudioPlayer.api.PlayerListener;
import org.airsonic.player.domain.*;
import org.airsonic.player.service.jukebox.JavaPlayerFactory;
import org.airsonic.player.util.FileUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.*;


/**
 * @author Rémi Cocula
 */
@Service
public class JukeboxJavaService {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(JukeboxJavaService.class);

    private static final float DEFAULT_GAIN = 0.75f;

    private AudioScrobblerService audioScrobblerService;
    private StatusService statusService;
    private SecurityService securityService;
    private MediaFileService mediaFileService;
    private JavaPlayerFactory javaPlayerFactory;


    private TransferStatus status;
    private Map<Integer, com.github.biconou.AudioPlayer.api.Player> activeAudioPlayers = new HashMap<>();
    private Map<String, List<com.github.biconou.AudioPlayer.api.Player>> activeAudioPlayersPerMixer = new HashMap<>();
    private final static String DEFAULT_MIXER_ENTRY_KEY = "_default";


    public JukeboxJavaService(AudioScrobblerService audioScrobblerService,
                              StatusService statusService,
                              SecurityService securityService,
                              MediaFileService mediaFileService,
                              JavaPlayerFactory javaPlayerFactory) {
        this.audioScrobblerService = audioScrobblerService;
        this.statusService = statusService;
        this.securityService = securityService;
        this.mediaFileService = mediaFileService;
        this.javaPlayerFactory = javaPlayerFactory;
    }

    /**
     * Finds the corresponding active audio player for a given airsonic player.
     * If no player exists we create one.
     * The JukeboxJavaService references all active audio players in a map indexed by airsonic player id.
     *
     * @param airsonicPlayer a given airsonic player.
     * @return the corresponding active audio player.
     */
    private com.github.biconou.AudioPlayer.api.Player retrieveAudioPlayerForAirsonicPlayer(Player airsonicPlayer) {
        com.github.biconou.AudioPlayer.api.Player foundPlayer = activeAudioPlayers.get(airsonicPlayer.getId());
        if (foundPlayer == null) {
            synchronized (activeAudioPlayers) {
                com.github.biconou.AudioPlayer.api.Player newPlayer = initAudioPlayer(airsonicPlayer);
                activeAudioPlayers.put(airsonicPlayer.getId(), newPlayer);
                String mixer = airsonicPlayer.getJavaJukeboxMixer();
                if (StringUtils.isBlank(mixer)) {
                    mixer = DEFAULT_MIXER_ENTRY_KEY;
                }
                List<com.github.biconou.AudioPlayer.api.Player> playersForMixer = activeAudioPlayersPerMixer.get(mixer);
                if (playersForMixer == null) {
                    playersForMixer = new ArrayList<>();
                    activeAudioPlayersPerMixer.put(mixer, playersForMixer);
                }
                playersForMixer.add(newPlayer);
                foundPlayer = newPlayer;
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
            audioPlayer = javaPlayerFactory.createJavaPlayer(airsonicPlayer.getJavaJukeboxMixer());
        } else {
            log.info("use default mixer");
            audioPlayer = javaPlayerFactory.createJavaPlayer();
        }
        if (audioPlayer != null) {
            audioPlayer.setGain(DEFAULT_GAIN);
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
        return audioPlayer.getGain();
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

    public void start(Player airsonicPlayer) {
        play(airsonicPlayer);
    }

    public void stop(Player airsonicPlayer) {
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
}
