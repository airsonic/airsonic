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
 *
 *
 * @author Rémi Cocula
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
    private Map<String,MediaFile> currentPlayingFileMap = new Hashtable();
    private Map<String, com.github.biconou.AudioPlayer.api.Player> activeAudioPlayers = new Hashtable<>();
    private Map<String, List<com.github.biconou.AudioPlayer.api.Player>> activeAudioPlayersPerMixer = new Hashtable<>();
    private final static String DEFAULT_MIXER_ENTRY_KEY = "_default";

    /**
     * Finds the corresponding active audio player for a given libresonic player.
     * The JukeboxJavaService references all active audio players in a map indexed by libresonic player id.
     *
     * @param libresonicPlayer a given libresonic player.
     * @return the corresponding active audio player of null if none exists.
     */
    private com.github.biconou.AudioPlayer.api.Player retrieveAudioPlayerForLibresonicPlayer(Player libresonicPlayer) {
        com.github.biconou.AudioPlayer.api.Player foundPlayer = activeAudioPlayers.get(libresonicPlayer.getId());
        if (foundPlayer == null) {
            synchronized (activeAudioPlayers) {
                foundPlayer = initAudioPlayer(libresonicPlayer);
                if (foundPlayer == null) {
                    throw new RuntimeException("Did not initialized a player");
                } else {
                    activeAudioPlayers.put(libresonicPlayer.getId(), foundPlayer);
                    String mixer = libresonicPlayer.getJavaJukeboxMixer();
                    if (StringUtils.isBlank(mixer)) {
                        mixer = DEFAULT_MIXER_ENTRY_KEY;
                    }
                    List<com.github.biconou.AudioPlayer.api.Player> playersForMixer = activeAudioPlayersPerMixer.get(mixer);
                    if (playersForMixer == null) {
                        playersForMixer = new ArrayList<>();
                        activeAudioPlayersPerMixer.put(mixer,playersForMixer);
                    }
                    playersForMixer.add(foundPlayer);
                }
            }
        }
        return foundPlayer;
    }

    private MediaFile getCurrentPlayingFileForPlayer(Player libresonicPlayer) {
        return currentPlayingFileMap.get(libresonicPlayer.getId());
    }

    private void setCurrentPlayingFileForPlayer(Player libresonicPlayer, MediaFile mediaFile) {
        currentPlayingFileMap.put(libresonicPlayer.getId(),mediaFile);
    }

    public void updateJukebox(Player libresonicPlayer, int offset) throws Exception {

        log.debug("begin updateJukebox : player = id:{};name:{}",libresonicPlayer.getId(),libresonicPlayer.getName());

        com.github.biconou.AudioPlayer.api.Player audioPlayer = retrieveAudioPlayerForLibresonicPlayer(libresonicPlayer);

        // Control user authorizations
        User user = securityService.getUserByName(libresonicPlayer.getUsername());
        if (!user.isJukeboxRole()) {
            log.warn("{} is not authorized for jukebox playback.",user.getUsername());
            return;
        }

        log.debug("PlayQueue.Status is {}",libresonicPlayer.getPlayQueue().getStatus());
        if (libresonicPlayer.getPlayQueue().getStatus() == PlayQueue.Status.PLAYING) {
            MediaFile currentFileInPlayQueue;
            currentFileInPlayQueue = libresonicPlayer.getPlayQueue().getCurrentFile();
            log.debug("Current file in play queue is {}",currentFileInPlayQueue.getName());

            boolean sameFile = currentFileInPlayQueue != null && currentFileInPlayQueue.equals(getCurrentPlayingFileForPlayer(libresonicPlayer));
            boolean paused = audioPlayer.isPaused();
            boolean stopped = audioPlayer.getState().equals(com.github.biconou.AudioPlayer.api.Player.State.STOPPED);
            boolean closed = audioPlayer.getState().equals(com.github.biconou.AudioPlayer.api.Player.State.CLOSED);

            if (sameFile && (paused || stopped || closed)) {
                log.debug("Same file -> try to resume playing");
                audioPlayer.play();
            } else {
                if (sameFile) {
                    log.debug("Same file and offset={} -> try to move to this position",offset);
                    audioPlayer.setPos(offset);
                } else {
                    log.debug("Different file to play -> start a new play list");
                    if (currentFileInPlayQueue != null) {
                        audioPlayer.setPlayList(new PlayList() {

                            @Override
                            public File getNextAudioFile() throws IOException {
                                libresonicPlayer.getPlayQueue().next();
                                return getCurrentAudioFile();
                            }

                            @Override
                            public File getCurrentAudioFile() {
                                MediaFile current = libresonicPlayer.getPlayQueue().getCurrentFile();
                                if (current != null) {
                                    return libresonicPlayer.getPlayQueue().getCurrentFile().getFile();
                                } else {
                                    return null;
                                }
                            }

                            @Override
                            public int getSize() {
                                return libresonicPlayer.getPlayQueue().size();
                            }

                            @Override
                            public int getIndex() {
                                return libresonicPlayer.getPlayQueue().getIndex();
                            }
                        });
                        // Close any other player using the same mixer.
                        String mixer = libresonicPlayer.getJavaJukeboxMixer();
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
            }
        } else {
            try {
                log.debug("try to pause player");
                audioPlayer.pause();
            } catch (Exception e) {
                log.error("Error trying to pause",e);
                throw e;
            }
        }
    }

    private com.github.biconou.AudioPlayer.api.Player initAudioPlayer(final Player libresonicPlayer) {

        if (!libresonicPlayer.getTechnology().equals(PlayerTechnology.JAVA_JUKEBOX)) {
            throw new RuntimeException("The player "+libresonicPlayer.getName()+" is not a java jukebox player");
        }

        log.info("begin initAudioPlayer");

        com.github.biconou.AudioPlayer.api.Player audioPlayer;

        if (StringUtils.isNotBlank(libresonicPlayer.getJavaJukeboxMixer())) {
            log.info("use mixer : {}",libresonicPlayer.getJavaJukeboxMixer());
            audioPlayer = new JavaPlayer(libresonicPlayer.getJavaJukeboxMixer());
        } else {
            log.info("use default mixer");
            audioPlayer = new JavaPlayer();
        }
        if (audioPlayer != null) {
            audioPlayer.registerListener(new PlayerListener() {
                @Override
                public void onBegin(int index, File currentFile) {
                    MediaFile currentPlayingFile = libresonicPlayer.getPlayQueue().getCurrentFile();
                    setCurrentPlayingFileForPlayer(libresonicPlayer,currentPlayingFile);
                    onSongStart(libresonicPlayer, currentPlayingFile);
                }

                @Override
                public void onEnd(int index, File file) {
                    onSongEnd(libresonicPlayer, getCurrentPlayingFileForPlayer(libresonicPlayer));
                }

                @Override
                public void onFinished() {
                    libresonicPlayer.getPlayQueue().setStatus(PlayQueue.Status.STOPPED);
                }

                @Override
                public void onStop() {
                    libresonicPlayer.getPlayQueue().setStatus(PlayQueue.Status.STOPPED);
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


    public synchronized int getPosition(final Player libresonicPlayer) {

        if (!libresonicPlayer.getTechnology().equals(PlayerTechnology.JAVA_JUKEBOX)) {
            throw new RuntimeException("The player "+libresonicPlayer.getName()+" is not a java jukebox player");
        }
        com.github.biconou.AudioPlayer.api.Player audioPlayer = retrieveAudioPlayerForLibresonicPlayer(libresonicPlayer);
        if (audioPlayer == null) {
            return 0;
        } else {
            return audioPlayer.getPlayingInfos().currentAudioPositionInSeconds();
        }
    }

    public void setPosition(final Player libresonicPlayer,int positionInSeconds) {
        if (!libresonicPlayer.getTechnology().equals(PlayerTechnology.JAVA_JUKEBOX)) {
            throw new RuntimeException("The player "+libresonicPlayer.getName()+" is not a java jukebox player");
        }
        com.github.biconou.AudioPlayer.api.Player audioPlayer = retrieveAudioPlayerForLibresonicPlayer(libresonicPlayer);
        if (audioPlayer != null) {
            audioPlayer.setPos(positionInSeconds);
        } else {
            throw new RuntimeException("The player "+libresonicPlayer.getName()+" has no real audio player");
        }
    }

    public float getGain(final Player libresonicPlayer) {
        if (!libresonicPlayer.getTechnology().equals(PlayerTechnology.JAVA_JUKEBOX)) {
            throw new RuntimeException("The player "+libresonicPlayer.getName()+" is not a java jukebox player");
        }
        com.github.biconou.AudioPlayer.api.Player audioPlayer = retrieveAudioPlayerForLibresonicPlayer(libresonicPlayer);
        if (audioPlayer != null) {
            return audioPlayer.getGain();
        }
        return 0.5f;
    }

    public void setGain(final Player libresonicPlayer, final float gain) {
        if (!libresonicPlayer.getTechnology().equals(PlayerTechnology.JAVA_JUKEBOX)) {
            throw new RuntimeException("The player "+libresonicPlayer.getName()+" is not a java jukebox player");
        }
        com.github.biconou.AudioPlayer.api.Player audioPlayer = retrieveAudioPlayerForLibresonicPlayer(libresonicPlayer);
        log.debug("setGain : gain={}",gain);
        if (audioPlayer != null) {
            audioPlayer.setGain(gain);
        } else {
            throw new RuntimeException("The player "+libresonicPlayer.getName()+" has no real audio player");
        }
    }


    private void onSongStart(Player player,MediaFile file) {
        log.info("[onSongStart] {} starting jukebox for \"{}\"",player.getUsername(),FileUtil.getShortPath(file.getFile()));
        if (status != null) {
            statusService.removeStreamStatus(status);
            status = null;
        }
        status = statusService.createStreamStatus(player);
        status.setFile(file.getFile());
        status.addBytesTransfered(file.getFileSize());
        mediaFileService.incrementPlayCount(file);
        scrobble(player,file, false);
    }

    private void onSongEnd(Player player,MediaFile file) {
        log.info("[onSongEnd] {} stopping jukebox for \"{}\"",player.getUsername(),FileUtil.getShortPath(file.getFile()));
        if (status != null) {
            statusService.removeStreamStatus(status);
            status = null;
        }
        scrobble(player,file, true);
    }

    private void scrobble(Player player,MediaFile file, boolean submission) {
        if (player.getClientId() == null) {  // Don't scrobble REST players.
            audioScrobblerService.register(file, player.getUsername(), submission, null);
        }
    }
}
