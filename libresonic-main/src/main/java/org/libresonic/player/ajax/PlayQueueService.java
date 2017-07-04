/*
 This file is part of Libresonic.

 Libresonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Libresonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Libresonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2016 (C) Libresonic Authors
 Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
 */
package org.libresonic.player.ajax;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.directwebremoting.WebContextFactory;
import org.libresonic.player.dao.MediaFileDao;
import org.libresonic.player.dao.PlayQueueDao;
import org.libresonic.player.domain.*;
import org.libresonic.player.service.*;
import org.libresonic.player.service.PlaylistService;
import org.libresonic.player.util.StringUtil;
import org.springframework.web.servlet.support.RequestContextUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.*;

/**
 * Provides AJAX-enabled services for manipulating the play queue of a player.
 * This class is used by the DWR framework (http://getahead.ltd.uk/dwr/).
 *
 * @author Sindre Mehus
 */
@SuppressWarnings("UnusedDeclaration")
public class PlayQueueService {

    private PlayerService playerService;
    private JukeboxService jukeboxService;
    private JukeboxJavaService jukeboxJavaService;
    private TranscodingService transcodingService;
    private SettingsService settingsService;
    private MediaFileService mediaFileService;
    private LastFmService lastFmService;
    private SecurityService securityService;
    private SearchService searchService;
    private RatingService ratingService;
    private PodcastService podcastService;
    private org.libresonic.player.service.PlaylistService playlistService;
    private MediaFileDao mediaFileDao;
    private PlayQueueDao playQueueDao;
    private JWTSecurityService jwtSecurityService;

    /**
     * Returns the play queue for the player of the current user.
     *
     * @return The play queue.
     */
    public PlayQueueInfo getPlayQueue() throws Exception {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();
        Player player = getCurrentPlayer(request, response);
        return convert(request, player, false);
    }

    public PlayQueueInfo start() throws Exception {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();
        return doStart(request, response);
    }

    public PlayQueueInfo doStart(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Player player = getCurrentPlayer(request, response);
        player.getPlayQueue().setStatus(PlayQueue.Status.PLAYING);
        return convert(request, player, true);
    }

    public PlayQueueInfo stop() throws Exception {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();
        return doStop(request, response);
    }

    public PlayQueueInfo doStop(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Player player = getCurrentPlayer(request, response);
        player.getPlayQueue().setStatus(PlayQueue.Status.STOPPED);
        return convert(request, player, true);
    }

    public PlayQueueInfo toggleStartStop() throws Exception {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();
        return doToggleStartStop(request, response);
    }

    public PlayQueueInfo doToggleStartStop(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Player player = getCurrentPlayer(request, response);
        if (player.getPlayQueue().getStatus() == PlayQueue.Status.STOPPED) {
            player.getPlayQueue().setStatus(PlayQueue.Status.PLAYING);
        } else if (player.getPlayQueue().getStatus() == PlayQueue.Status.PLAYING) {
            player.getPlayQueue().setStatus(PlayQueue.Status.STOPPED);
        }
        return convert(request, player, true);
    }

    public PlayQueueInfo skip(int index) throws Exception {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();
        return doSkip(request, response, index, 0);
    }

    public PlayQueueInfo doSkip(HttpServletRequest request, HttpServletResponse response, int index, int offset) throws Exception {
        Player player = getCurrentPlayer(request, response);
        player.getPlayQueue().setIndex(index);
        boolean serverSidePlaylist = !player.isExternalWithPlaylist();
        return convert(request, player, serverSidePlaylist, offset);
    }

    public PlayQueueInfo reloadSearchCriteria() throws Exception {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();
        String username = securityService.getCurrentUsername(request);
        Player player = getCurrentPlayer(request, response);
        PlayQueue playQueue = player.getPlayQueue();
        if (playQueue.getRandomSearchCriteria() != null) {
            playQueue.addFiles(true, mediaFileService.getRandomSongs(playQueue.getRandomSearchCriteria(), username));
        }
        return convert(request, player, false);
    }

    public void savePlayQueue(int currentSongIndex, long positionMillis) {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();

        Player player = getCurrentPlayer(request, response);
        String username = securityService.getCurrentUsername(request);
        PlayQueue playQueue = player.getPlayQueue();
        List<Integer> ids = MediaFile.toIdList(playQueue.getFiles());

        Integer currentId = currentSongIndex == -1 ? null : playQueue.getFile(currentSongIndex).getId();
        SavedPlayQueue savedPlayQueue = new SavedPlayQueue(null, username, ids, currentId, positionMillis, new Date(), "Libresonic");
        playQueueDao.savePlayQueue(savedPlayQueue);
    }

    public PlayQueueInfo loadPlayQueue() throws Exception {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();
        Player player = getCurrentPlayer(request, response);
        String username = securityService.getCurrentUsername(request);
        SavedPlayQueue savedPlayQueue = playQueueDao.getPlayQueue(username);

        if (savedPlayQueue == null) {
            return convert(request, player, false);
        }

        PlayQueue playQueue = player.getPlayQueue();
        playQueue.clear();
        for (Integer mediaFileId : savedPlayQueue.getMediaFileIds()) {
            MediaFile mediaFile = mediaFileService.getMediaFile(mediaFileId);
            if (mediaFile != null) {
                playQueue.addFiles(true, mediaFile);
            }
        }
        PlayQueueInfo result = convert(request, player, false);

        Integer currentId = savedPlayQueue.getCurrentMediaFileId();
        int currentIndex = -1;
        long positionMillis = savedPlayQueue.getPositionMillis() == null ? 0L : savedPlayQueue.getPositionMillis();
        if (currentId != null) {
            MediaFile current = mediaFileService.getMediaFile(currentId);
            currentIndex = playQueue.getFiles().indexOf(current);
            if (currentIndex != -1) {
                result.setStartPlayerAt(currentIndex);
                result.setStartPlayerAtPosition(positionMillis);
            }
        }

        boolean serverSidePlaylist = !player.isExternalWithPlaylist();
        if (serverSidePlaylist && currentIndex != -1) {
            doSkip(request, response, currentIndex, (int) (positionMillis / 1000L));
        }

        return result;
    }

    public PlayQueueInfo play(int id) throws Exception {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();

        Player player = getCurrentPlayer(request, response);
        MediaFile file = mediaFileService.getMediaFile(id);

        if (file.isFile()) {
            String username = securityService.getCurrentUsername(request);
            boolean queueFollowingSongs = settingsService.getUserSettings(username).isQueueFollowingSongs();
            List<MediaFile> songs;
            if (queueFollowingSongs) {
                MediaFile dir = mediaFileService.getParentOf(file);
                songs = mediaFileService.getChildrenOf(dir, true, false, true);
                if (!songs.isEmpty()) {
                    int index = songs.indexOf(file);
                    songs = songs.subList(index, songs.size());
                }
            } else {
                songs = Arrays.asList(file);
            }
            return doPlay(request, player, songs).setStartPlayerAt(0);
        } else {
            List<MediaFile> songs = mediaFileService.getDescendantsOf(file, true);
            return doPlay(request, player, songs).setStartPlayerAt(0);
        }
    }

    /**
     * @param index Start playing at this index, or play whole playlist if {@code null}.
     */
    public PlayQueueInfo playPlaylist(int id, Integer index) throws Exception {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();

        String username = securityService.getCurrentUsername(request);
        boolean queueFollowingSongs = settingsService.getUserSettings(username).isQueueFollowingSongs();

        List<MediaFile> files = playlistService.getFilesInPlaylist(id, true);
        if (!files.isEmpty() && index != null) {
            if (queueFollowingSongs) {
                files = files.subList(index, files.size());
            } else {
                files = Arrays.asList(files.get(index));
            }
        }

        // Remove non-present files
        Iterator<MediaFile> iterator = files.iterator();
        while (iterator.hasNext()) {
            MediaFile file = iterator.next();
            if (!file.isPresent()) {
                iterator.remove();
            }
        }
        Player player = getCurrentPlayer(request, response);
        return doPlay(request, player, files).setStartPlayerAt(0);
    }

    /**
     * @param index Start playing at this index, or play all top songs if {@code null}.
     */
    public PlayQueueInfo playTopSong(int id, Integer index) throws Exception {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();

        String username = securityService.getCurrentUsername(request);
        boolean queueFollowingSongs = settingsService.getUserSettings(username).isQueueFollowingSongs();

        List<MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username);
        List<MediaFile> files = lastFmService.getTopSongs(mediaFileService.getMediaFile(id), 50, musicFolders);
        if (!files.isEmpty() && index != null) {
            if (queueFollowingSongs) {
                files = files.subList(index, files.size());
            } else {
                files = Arrays.asList(files.get(index));
            }
        }

        Player player = getCurrentPlayer(request, response);
        return doPlay(request, player, files).setStartPlayerAt(0);
    }

    public PlayQueueInfo playPodcastChannel(int id) throws Exception {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();

        List<PodcastEpisode> episodes = podcastService.getEpisodes(id);
        List<MediaFile> files = new ArrayList<MediaFile>();
        for (PodcastEpisode episode : episodes) {
            if (episode.getStatus() == PodcastStatus.COMPLETED) {
                MediaFile mediaFile = mediaFileService.getMediaFile(episode.getMediaFileId());
                if (mediaFile != null && mediaFile.isPresent()) {
                    files.add(mediaFile);
                }
            }
        }
        Player player = getCurrentPlayer(request, response);
        return doPlay(request, player, files).setStartPlayerAt(0);
    }

    public PlayQueueInfo playPodcastEpisode(int id) throws Exception {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();

        PodcastEpisode episode = podcastService.getEpisode(id, false);
        List<PodcastEpisode> allEpisodes = podcastService.getEpisodes(episode.getChannelId());
        List<MediaFile> files = new ArrayList<MediaFile>();

        String username = securityService.getCurrentUsername(request);
        boolean queueFollowingSongs = settingsService.getUserSettings(username).isQueueFollowingSongs();

        for (PodcastEpisode ep : allEpisodes) {
            if (ep.getStatus() == PodcastStatus.COMPLETED) {
                MediaFile mediaFile = mediaFileService.getMediaFile(ep.getMediaFileId());
                if (mediaFile != null && mediaFile.isPresent() &&
                    (ep.getId().equals(episode.getId()) || queueFollowingSongs && !files.isEmpty())) {
                    files.add(mediaFile);
                }
            }
        }
        Player player = getCurrentPlayer(request, response);
        return doPlay(request, player, files).setStartPlayerAt(0);
    }

    public PlayQueueInfo playNewestPodcastEpisode(Integer index) throws Exception {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();

        List<PodcastEpisode> episodes = podcastService.getNewestEpisodes(10);
        List<MediaFile> files = Lists.transform(episodes, new Function<PodcastEpisode, MediaFile>() {
            @Override
            public MediaFile apply(PodcastEpisode episode) {
                return mediaFileService.getMediaFile(episode.getMediaFileId());
            }
        });

        String username = securityService.getCurrentUsername(request);
        boolean queueFollowingSongs = settingsService.getUserSettings(username).isQueueFollowingSongs();

        if (!files.isEmpty() && index != null) {
            if (queueFollowingSongs) {
                files = files.subList(index, files.size());
            } else {
                files = Arrays.asList(files.get(index));
            }
        }

        Player player = getCurrentPlayer(request, response);
        return doPlay(request, player, files).setStartPlayerAt(0);
    }

    public PlayQueueInfo playStarred() throws Exception {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();

        String username = securityService.getCurrentUsername(request);
        List<MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username);
        List<MediaFile> files = mediaFileDao.getStarredFiles(0, Integer.MAX_VALUE, username, musicFolders);
        Player player = getCurrentPlayer(request, response);
        return doPlay(request, player, files).setStartPlayerAt(0);
    }

    public PlayQueueInfo playShuffle(String albumListType, int offset, int count, String genre, String decade) throws Exception {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();
        String username = securityService.getCurrentUsername(request);
        UserSettings userSettings = settingsService.getUserSettings(securityService.getCurrentUsername(request));

        MusicFolder selectedMusicFolder = settingsService.getSelectedMusicFolder(username);
        List<MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username,
                                                                                selectedMusicFolder == null ? null : selectedMusicFolder.getId());
        List<MediaFile> albums;
        if ("highest".equals(albumListType)) {
            albums = ratingService.getHighestRatedAlbums(offset, count, musicFolders);
        } else if ("frequent".equals(albumListType)) {
            albums = mediaFileService.getMostFrequentlyPlayedAlbums(offset, count, musicFolders);
        } else if ("recent".equals(albumListType)) {
            albums = mediaFileService.getMostRecentlyPlayedAlbums(offset, count, musicFolders);
        } else if ("newest".equals(albumListType)) {
            albums = mediaFileService.getNewestAlbums(offset, count, musicFolders);
        } else if ("starred".equals(albumListType)) {
            albums = mediaFileService.getStarredAlbums(offset, count, username, musicFolders);
        } else if ("random".equals(albumListType)) {
            albums = searchService.getRandomAlbums(count, musicFolders);
        } else if ("alphabetical".equals(albumListType)) {
            albums = mediaFileService.getAlphabeticalAlbums(offset, count, true, musicFolders);
        } else if ("decade".equals(albumListType)) {
            int fromYear = Integer.parseInt(decade);
            int toYear = fromYear + 9;
            albums = mediaFileService.getAlbumsByYear(offset, count, fromYear, toYear, musicFolders);
        } else if ("genre".equals(albumListType)) {
            albums = mediaFileService.getAlbumsByGenre(offset, count, genre, musicFolders);
        } else {
            albums = Collections.emptyList();
        }

        List<MediaFile> songs = new ArrayList<MediaFile>();
        for (MediaFile album : albums) {
            songs.addAll(mediaFileService.getChildrenOf(album, true, false, false));
        }
        Collections.shuffle(songs);
        songs = songs.subList(0, Math.min(40, songs.size()));

        Player player = getCurrentPlayer(request, response);
        return doPlay(request, player, songs).setStartPlayerAt(0);
    }

    private PlayQueueInfo doPlay(HttpServletRequest request, Player player, List<MediaFile> files) throws Exception {
        if (player.isWeb()) {
            mediaFileService.removeVideoFiles(files);
        }
        player.getPlayQueue().addFiles(false, files);
        player.getPlayQueue().setRandomSearchCriteria(null);
        return convert(request, player, true);
    }

    public PlayQueueInfo playRandom(int id, int count) throws Exception {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();

        MediaFile file = mediaFileService.getMediaFile(id);
        List<MediaFile> randomFiles = mediaFileService.getRandomSongsForParent(file, count);
        Player player = getCurrentPlayer(request, response);
        player.getPlayQueue().addFiles(false, randomFiles);
        player.getPlayQueue().setRandomSearchCriteria(null);
        return convert(request, player, true).setStartPlayerAt(0);
    }

    public PlayQueueInfo playSimilar(int id, int count) throws Exception {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();
        MediaFile artist = mediaFileService.getMediaFile(id);
        String username = securityService.getCurrentUsername(request);
        List<MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username);
        List<MediaFile> similarSongs = lastFmService.getSimilarSongs(artist, count, musicFolders);
        Player player = getCurrentPlayer(request, response);
        player.getPlayQueue().addFiles(false, similarSongs);
        return convert(request, player, true).setStartPlayerAt(0);
    }

    public PlayQueueInfo add(int id) throws Exception {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();
        return doAdd(request, response, new int[]{id}, null);
    }

    public PlayQueueInfo addAt(int id, int index) throws Exception {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();
        return doAdd(request, response, new int[]{id}, index);
    }

    public PlayQueueInfo doAdd(HttpServletRequest request, HttpServletResponse response, int[] ids, Integer index) throws Exception {
        Player player = getCurrentPlayer(request, response);
        List<MediaFile> files = new ArrayList<MediaFile>(ids.length);
        for (int id : ids) {
            MediaFile ancestor = mediaFileService.getMediaFile(id);
            files.addAll(mediaFileService.getDescendantsOf(ancestor, true));
        }
        if (player.isWeb()) {
            mediaFileService.removeVideoFiles(files);
        }
        if (index != null) {
            player.getPlayQueue().addFilesAt(files, index);
        } else {
            player.getPlayQueue().addFiles(true, files);
        }
        player.getPlayQueue().setRandomSearchCriteria(null);
        return convert(request, player, false);
    }
    
    public PlayQueueInfo doSet(HttpServletRequest request, HttpServletResponse response, int[] ids) throws Exception {
        Player player = getCurrentPlayer(request, response);
        PlayQueue playQueue = player.getPlayQueue();
        MediaFile currentFile = playQueue.getCurrentFile();
        PlayQueue.Status status = playQueue.getStatus();

        playQueue.clear();
        PlayQueueInfo result = doAdd(request, response, ids, null);

        int index = currentFile == null ? -1 : playQueue.getFiles().indexOf(currentFile);
        playQueue.setIndex(index);
        playQueue.setStatus(status);
        return result;
    }

    public PlayQueueInfo clear() throws Exception {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();
        return doClear(request, response);
    }

    public PlayQueueInfo doClear(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Player player = getCurrentPlayer(request, response);
        player.getPlayQueue().clear();
        boolean serverSidePlaylist = !player.isExternalWithPlaylist();
        return convert(request, player, serverSidePlaylist);
    }

    public PlayQueueInfo shuffle() throws Exception {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();
        return doShuffle(request, response);
    }

    public PlayQueueInfo doShuffle(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Player player = getCurrentPlayer(request, response);
        player.getPlayQueue().shuffle();
        return convert(request, player, false);
    }

    public PlayQueueInfo remove(int index) throws Exception {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();
        return doRemove(request, response, index);
    }

    public PlayQueueInfo toggleStar(int index) throws Exception {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();
        Player player = getCurrentPlayer(request, response);

        MediaFile file = player.getPlayQueue().getFile(index);
        String username = securityService.getCurrentUsername(request);
        boolean starred = mediaFileDao.getMediaFileStarredDate(file.getId(), username) != null;
        if (starred) {
            mediaFileDao.unstarMediaFile(file.getId(), username);
        } else {
            mediaFileDao.starMediaFile(file.getId(), username);
        }
        return convert(request, player, false);
    }

    public PlayQueueInfo doRemove(HttpServletRequest request, HttpServletResponse response, int index) throws Exception {
        Player player = getCurrentPlayer(request, response);
        player.getPlayQueue().removeFileAt(index);
        return convert(request, player, false);
    }

    public PlayQueueInfo removeMany(int[] indexes) throws Exception {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();
        Player player = getCurrentPlayer(request, response);
        for (int i = indexes.length - 1; i >= 0; i--) {
            player.getPlayQueue().removeFileAt(indexes[i]);
        }
        return convert(request, player, false);
    }

    public PlayQueueInfo rearrange(int[] indexes) throws Exception {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();
        Player player = getCurrentPlayer(request, response);
        player.getPlayQueue().rearrange(indexes);
        return convert(request, player, false);
    }

    public PlayQueueInfo up(int index) throws Exception {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();
        Player player = getCurrentPlayer(request, response);
        player.getPlayQueue().moveUp(index);
        return convert(request, player, false);
    }

    public PlayQueueInfo down(int index) throws Exception {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();
        Player player = getCurrentPlayer(request, response);
        player.getPlayQueue().moveDown(index);
        return convert(request, player, false);
    }

    public PlayQueueInfo toggleRepeat() throws Exception {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();
        Player player = getCurrentPlayer(request, response);
        PlayQueue playQueue = player.getPlayQueue();
        if (playQueue.isRadioEnabled()) {
            playQueue.setRandomSearchCriteria(null);
            playQueue.setRepeatEnabled(false);
        } else {
            playQueue.setRepeatEnabled(!player.getPlayQueue().isRepeatEnabled());
        }
        return convert(request, player, false);
    }

    public PlayQueueInfo undo() throws Exception {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();
        Player player = getCurrentPlayer(request, response);
        player.getPlayQueue().undo();
        boolean serverSidePlaylist = !player.isExternalWithPlaylist();
        return convert(request, player, serverSidePlaylist);
    }

    public PlayQueueInfo sortByTrack() throws Exception {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();
        Player player = getCurrentPlayer(request, response);
        player.getPlayQueue().sort(PlayQueue.SortOrder.TRACK);
        return convert(request, player, false);
    }

    public PlayQueueInfo sortByArtist() throws Exception {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();
        Player player = getCurrentPlayer(request, response);
        player.getPlayQueue().sort(PlayQueue.SortOrder.ARTIST);
        return convert(request, player, false);
    }

    public PlayQueueInfo sortByAlbum() throws Exception {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();
        Player player = getCurrentPlayer(request, response);
        player.getPlayQueue().sort(PlayQueue.SortOrder.ALBUM);
        return convert(request, player, false);
    }

    public void setGain(float gain) {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();
        Player player = getCurrentPlayer(request, response);
        if (player != null) {
            if (player.getTechnology().equals(PlayerTechnology.JAVA_JUKEBOX)) {
                jukeboxJavaService.setGain(player,gain);
            } else if (player.getTechnology().equals(PlayerTechnology.JUKEBOX)) {
                jukeboxService.setGain(gain);
            }
        }
    }

    public void setJavaJukeboxPosition(int positionInSeconds) {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();
        Player player = getCurrentPlayer(request, response);
        jukeboxJavaService.setPosition(player,positionInSeconds);
    }

    private PlayQueueInfo convert(HttpServletRequest request, Player player, boolean serverSidePlaylist) throws Exception {
        return convert(request, player, serverSidePlaylist, 0);
    }

    private void updateJukebox(Player player, int offset) throws Exception {
        if (player.getTechnology() == PlayerTechnology.JUKEBOX) {
            jukeboxService.updateJukebox(player, offset);
        } else if (player.getTechnology() == PlayerTechnology.JAVA_JUKEBOX) {
            jukeboxJavaService.updateJukebox(player, offset);
        }
    }

    private PlayQueueInfo convert(HttpServletRequest request, Player player, boolean serverSidePlaylist, int offset) throws Exception {
        String url = NetworkService.getBaseUrl(request);

        if (serverSidePlaylist && player.isJukebox()) {
            updateJukebox(player, offset);
        }
        boolean isCurrentPlayer = player.getIpAddress() != null && player.getIpAddress().equals(request.getRemoteAddr());

        boolean m3uSupported = player.isExternal() || player.isExternalWithPlaylist();
        serverSidePlaylist = player.isAutoControlEnabled() && m3uSupported && isCurrentPlayer && serverSidePlaylist;
        Locale locale = RequestContextUtils.getLocale(request);

        List<PlayQueueInfo.Entry> entries = new ArrayList<PlayQueueInfo.Entry>();
        PlayQueue playQueue = player.getPlayQueue();

        for (MediaFile file : playQueue.getFiles()) {

            String albumUrl = url + "/main.view?id=" + file.getId();
            String streamUrl = url + "/stream?player=" + player.getId() + "&id=" + file.getId();
            String coverArtUrl = url + "/coverArt.view?id=" + file.getId();

            String remoteStreamUrl = jwtSecurityService.addJWTToken(url + "/ext/stream?player=" + player.getId() + "&id=" + file.getId());
            String remoteCoverArtUrl = jwtSecurityService.addJWTToken(url + "/ext/coverArt.view?id=" + file.getId());

            String format = formatFormat(player, file);
            String username = securityService.getCurrentUsername(request);
            boolean starred = mediaFileService.getMediaFileStarredDate(file.getId(), username) != null;
            entries.add(new PlayQueueInfo.Entry(file.getId(), file.getTrackNumber(), file.getTitle(), file.getArtist(),
                    file.getAlbumName(), file.getGenre(), file.getYear(), formatBitRate(file),
                    file.getDurationSeconds(), file.getDurationString(), format, formatContentType(format),
                    formatFileSize(file.getFileSize(), locale), starred, albumUrl, streamUrl, remoteStreamUrl,
                    coverArtUrl, remoteCoverArtUrl));
        }
        boolean isStopEnabled = playQueue.getStatus() == PlayQueue.Status.PLAYING && !player.isExternalWithPlaylist();

        float gain = 0.0f;
        if (player.getTechnology() == PlayerTechnology.JUKEBOX) {
            gain = jukeboxService.getGain();
        } else if (player.getTechnology() == PlayerTechnology.JAVA_JUKEBOX) {
            gain = jukeboxJavaService.getGain(player);
        }

        return new PlayQueueInfo(entries, isStopEnabled, playQueue.isRepeatEnabled(), playQueue.isRadioEnabled(), serverSidePlaylist, gain);
    }

    private String formatFileSize(Long fileSize, Locale locale) {
        if (fileSize == null) {
            return null;
        }
        return StringUtil.formatBytes(fileSize, locale);
    }

    private String formatFormat(Player player, MediaFile file) {
        return file.getFormat();
    }

    private String formatContentType(String format) {
        return StringUtil.getMimeType(format);
    }

    private String formatBitRate(MediaFile mediaFile) {
        if (mediaFile.getBitRate() == null) {
            return null;
        }
        if (mediaFile.isVariableBitRate()) {
            return mediaFile.getBitRate() + " Kbps vbr";
        }
        return mediaFile.getBitRate() + " Kbps";
    }

    private Player getCurrentPlayer(HttpServletRequest request, HttpServletResponse response) {
        return playerService.getPlayer(request, response);
    }

    public void setPlayerService(PlayerService playerService) {
        this.playerService = playerService;
    }

    public void setMediaFileService(MediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
    }

    public void setLastFmService(LastFmService lastFmService) {
        this.lastFmService = lastFmService;
    }

    public void setJukeboxService(JukeboxService jukeboxService) {
        this.jukeboxService = jukeboxService;
    }

    public void setJukeboxJavaService(JukeboxJavaService jukeboxJavaService) {
        this.jukeboxJavaService = jukeboxJavaService;
    }

    public void setTranscodingService(TranscodingService transcodingService) {
        this.transcodingService = transcodingService;
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

    public void setRatingService(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    public void setPodcastService(PodcastService podcastService) {
        this.podcastService = podcastService;
    }

    public void setMediaFileDao(MediaFileDao mediaFileDao) {
        this.mediaFileDao = mediaFileDao;
    }

    public void setPlayQueueDao(PlayQueueDao playQueueDao) {
        this.playQueueDao = playQueueDao;
    }

    public void setPlaylistService(PlaylistService playlistService) {
        this.playlistService = playlistService;
    }

    public void setJwtSecurityService(JWTSecurityService jwtSecurityService) {
        this.jwtSecurityService = jwtSecurityService;
    }
}
