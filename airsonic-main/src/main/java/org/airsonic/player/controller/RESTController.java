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
package org.airsonic.player.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.airsonic.player.ajax.LyricsInfo;
import org.airsonic.player.ajax.LyricsService;
import org.airsonic.player.ajax.PlayQueueService;
import org.airsonic.player.command.UserSettingsCommand;
import org.airsonic.player.dao.AlbumDao;
import org.airsonic.player.dao.ArtistDao;
import org.airsonic.player.dao.MediaFileDao;
import org.airsonic.player.dao.PlayQueueDao;
import org.airsonic.player.domain.*;
import org.airsonic.player.domain.Artist;
import org.airsonic.player.domain.Bookmark;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.PlayQueue;
import org.airsonic.player.domain.Playlist;
import org.airsonic.player.domain.PodcastChannel;
import org.airsonic.player.domain.PodcastEpisode;
import org.airsonic.player.domain.Share;
import org.airsonic.player.domain.User;
import org.airsonic.player.service.*;
import org.airsonic.player.util.StringUtil;
import org.airsonic.player.util.Util;
import org.airsonic.restapi.domain.*;
import org.airsonic.restapi.domain.Genres;
import org.airsonic.restapi.domain.PodcastStatus;
import org.airsonic.restapi.domain.SearchResult;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.*;

import static org.airsonic.player.security.RESTRequestParameterProcessingFilter.decrypt;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.web.bind.ServletRequestUtils.*;

@SuppressWarnings("Duplicates")
@Controller
@RequestMapping(value = "/api", consumes = {"application/json"}, produces = {"application/json"})
public class RESTController {

    private static final Logger LOG = LoggerFactory.getLogger(RESTController.class);

    @Autowired
    private SettingsService settingsService;
    @Autowired
    private SecurityService securityService;
    @Autowired
    private PlayerService playerService;
    @Autowired
    private MediaFileService mediaFileService;
    @Autowired
    private LastFmService lastFmService;
    @Autowired
    private MusicIndexService musicIndexService;
    @Autowired
    private DownloadController downloadController;
    @Autowired
    private CoverArtController coverArtController;
    @Autowired
    private AvatarController avatarController;
    @Autowired
    private UserSettingsController userSettingsController;
    @Autowired
    private LeftController leftController;
    @Autowired
    private StatusService statusService;
    @Autowired
    private StreamController streamController;
    @Autowired
    private HLSController hlsController;
    @Autowired
    private ShareService shareService;
    @Autowired
    private PlaylistService playlistService;
    @Autowired
    private LyricsService lyricsService;
    @Autowired
    private PlayQueueService playQueueService;
    @Autowired
    private JukeboxService jukeboxService;
    @Autowired
    private AudioScrobblerService audioScrobblerService;
    @Autowired
    private PodcastService podcastService;
    @Autowired
    private RatingService ratingService;
    @Autowired
    private SearchService searchService;
    @Autowired
    private MediaFileDao mediaFileDao;
    @Autowired
    private ArtistDao artistDao;
    @Autowired
    private AlbumDao albumDao;
    @Autowired
    private PlayQueueDao playQueueDao;
    @Autowired
    private MediaScannerService mediaScannerService;
    @Autowired
    private BookmarkService bookmarkService;

    @ApiResponses({
            @ApiResponse(code = 204, message = "Successful ping")
    })
    @RequestMapping(value = "/ping", method = RequestMethod.GET)
    public ResponseEntity<Void> ping() {
        return ResponseEntity.noContent().build();
    }

    @ApiResponses({
            @ApiResponse(code = 200, message = "Returns all configured top-level music folders")
    })
    @RequestMapping(value = "/getMusicFolders", method = RequestMethod.GET)
    public ResponseEntity<MusicFolders> getMusicFolders(HttpServletRequest request) throws Exception {
        MusicFolders musicFolders = new MusicFolders();
        String username = securityService.getCurrentUsername(request);
        for (MusicFolder musicFolder : settingsService.getMusicFoldersForUser(username)) {
            org.airsonic.restapi.domain.MusicFolder mf = new org.airsonic.restapi.domain.MusicFolder();
            mf.setId(musicFolder.getId());
            mf.setName(musicFolder.getName());
            musicFolders.getMusicFolder().add(mf);
        }
        return ResponseEntity.ok(musicFolders);
    }

    @ApiOperation(value = "Get Indexes", notes = "Returns indexed data")
    @ApiResponses({
            @ApiResponse(code = 304, message = "Not Modified"),
            @ApiResponse(code = 200, message = "OK")
    })
    @RequestMapping(value = "/getIndexes", method = RequestMethod.GET)
    public ResponseEntity<Indexes> getIndexes(
            @RequestParam(name = "ifModifiedSince", required = false, defaultValue = "0") Long ifModifiedSince,
            @RequestParam(name = "musicFolderId", required = false) Integer musicFolderId,
            HttpServletRequest request) throws Exception {
        String username = securityService.getCurrentUser(request).getUsername();

        long lastModified = leftController.getLastModified(request);

        if (lastModified <= ifModifiedSince) {
            return ResponseEntity.status(NOT_MODIFIED).build();
        }

        Indexes indexes = new Indexes();
        indexes.setLastModified(lastModified);
        indexes.setIgnoredArticles(settingsService.getIgnoredArticles());

        List<MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username);
        if (musicFolderId != null) {
            for (MusicFolder musicFolder : musicFolders) {
                if (musicFolderId.equals(musicFolder.getId())) {
                    musicFolders = Collections.singletonList(musicFolder);
                    break;
                }
            }
        }

        for (MediaFile shortcut : musicIndexService.getShortcuts(musicFolders)) {
            indexes.getShortcut().add(createArtist(shortcut, username));
        }

        MusicFolderContent musicFolderContent = musicIndexService.getMusicFolderContent(musicFolders, false);

        for (Map.Entry<MusicIndex, List<MusicIndex.SortableArtistWithMediaFiles>> entry : musicFolderContent.getIndexedArtists().entrySet()) {
            Index index = new Index();
            indexes.getIndex().add(index);
            index.setName(entry.getKey().getIndex());

            for (MusicIndex.SortableArtistWithMediaFiles artist : entry.getValue()) {
                for (MediaFile mediaFile : artist.getMediaFiles()) {
                    if (mediaFile.isDirectory()) {
                        Date starredDate = mediaFileDao.getMediaFileStarredDate(mediaFile.getId(), username);
                        org.airsonic.restapi.domain.Artist a = new org.airsonic.restapi.domain.Artist();
                        index.getArtist().add(a);
                        a.setId(String.valueOf(mediaFile.getId()));
                        a.setName(artist.getName());
                        a.setStarred(starredDate);

                        if (mediaFile.isAlbum()) {
                            a.setAverageRating(ratingService.getAverageRating(mediaFile));
                            a.setUserRating(ratingService.getRatingForUser(username, mediaFile));
                        }
                    }
                }
            }
        }

        for (MediaFile singleSong : musicFolderContent.getSingleSongs()) {
            indexes.getChild().add(createChild(singleSong, username));
        }

        return ResponseEntity.ok(indexes);
    }

    @RequestMapping(value = "/getGenres", method = RequestMethod.GET)
    public ResponseEntity<Genres> getGenres() throws Exception {
        org.airsonic.restapi.domain.Genres genres = new org.airsonic.restapi.domain.Genres();

        for (org.airsonic.player.domain.Genre genre : mediaFileDao.getGenres(false)) {
            org.airsonic.restapi.domain.Genre g = new org.airsonic.restapi.domain.Genre();
            genres.getGenre().add(g);
            g.setContent(genre.getName());
            g.setAlbumCount(genre.getAlbumCount());
            g.setSongCount(genre.getSongCount());
        }
        return ResponseEntity.ok(genres);
    }

    @RequestMapping(value = "/getSongsByGenre", method = RequestMethod.GET)
    public ResponseEntity<Songs> getSongsByGenre(
            @RequestParam("genre") String genre,
            @RequestParam(name = "offset", required = false, defaultValue = "0") Integer offset,
            @RequestParam(name = "count", required = false, defaultValue = "10") Integer count,
            @RequestParam(name = "musicFolderId", required = false) Integer musicFolderId,
            HttpServletRequest request) throws Exception {

        String username = securityService.getCurrentUsername(request);

        Songs songs = new Songs();

        count = Math.max(0, Math.min(count, 500));
        List<MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username, musicFolderId);

        for (MediaFile mediaFile : mediaFileDao.getSongsByGenre(genre, offset, count, musicFolders)) {
            songs.getSong().add(createChild(mediaFile, username));
        }
        return ResponseEntity.ok(songs);
    }

    @RequestMapping(value = "/getArtists", method = RequestMethod.GET)
    public ResponseEntity<ArtistsID3> getArtists(HttpServletRequest request) throws Exception {
        String username = securityService.getCurrentUsername(request);

        ArtistsID3 result = new ArtistsID3();
        result.setIgnoredArticles(settingsService.getIgnoredArticles());
        List<MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username);

        List<Artist> artists = artistDao.getAlphabetialArtists(0, Integer.MAX_VALUE, musicFolders);
        SortedMap<MusicIndex, List<MusicIndex.SortableArtistWithArtist>> indexedArtists = musicIndexService.getIndexedArtists(
                artists);
        for (Map.Entry<MusicIndex, List<MusicIndex.SortableArtistWithArtist>> entry : indexedArtists.entrySet()) {
            IndexID3 index = new IndexID3();
            result.getIndex().add(index);
            index.setName(entry.getKey().getIndex());
            for (MusicIndex.SortableArtistWithArtist sortableArtist : entry.getValue()) {
                index.getArtist().add(createArtist(new ArtistID3(), sortableArtist.getArtist(), username));
            }
        }

        return ResponseEntity.ok(result);
    }

    @RequestMapping(value = "/getSimilarSongsByArtist", method = RequestMethod.GET)
    public ResponseEntity<SimilarSongs> getSimilarSongsByArtist(HttpServletRequest request) throws Exception {
        String username = securityService.getCurrentUsername(request);

        int id = getRequiredIntParameter(request, "id");
        int count = getIntParameter(request, "count", 50);

        SimilarSongs result = new SimilarSongs();

        Artist artist = artistDao.getArtist(id);
        if (artist == null) {
            return ResponseEntity.notFound().build();
        }

        List<MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username);
        List<MediaFile> similarSongs = lastFmService.getSimilarSongs(artist, count, musicFolders);
        for (MediaFile similarSong : similarSongs) {
            result.getSong().add(createChild(similarSong, username));
        }

        return ResponseEntity.ok(result);
    }

    @RequestMapping(value = "/getTopSongs", method = RequestMethod.GET)
    public ResponseEntity<TopSongs> getTopSongs(HttpServletRequest request) throws Exception {
        String username = securityService.getCurrentUsername(request);

        String artist = getRequiredStringParameter(request, "artist");
        int count = getIntParameter(request, "count", 50);

        TopSongs result = new TopSongs();

        List<MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username);
        List<MediaFile> topSongs = lastFmService.getTopSongs(artist, count, musicFolders);
        for (MediaFile topSong : topSongs) {
            result.getSong().add(createChild(topSong, username));
        }

        return ResponseEntity.ok(result);
    }

    @RequestMapping(value = "/getArtistInfo", method = RequestMethod.GET)
    public ResponseEntity<ArtistInfo> getArtistInfo(HttpServletRequest request) throws Exception {
        String username = securityService.getCurrentUsername(request);

        int id = getRequiredIntParameter(request, "id");
        int count = getIntParameter(request, "count", 20);
        boolean includeNotPresent = ServletRequestUtils.getBooleanParameter(request, "includeNotPresent", false);

        ArtistInfo result = new ArtistInfo();

        Artist artist = artistDao.getArtist(id);
        if (artist == null) {
            return ResponseEntity.notFound().build();
        }

        List<MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username);
        List<Artist> similarArtists = lastFmService.getSimilarArtists(artist, count, includeNotPresent, musicFolders);
        for (Artist similarArtist : similarArtists) {
            result.getSimilarArtist().add(createArtist(new ArtistID3(), similarArtist, username));
        }
        ArtistBio artistBio = lastFmService.getArtistBio(artist);
        if (artistBio != null) {
            result.setBiography(artistBio.getBiography());
            result.setMusicBrainzId(artistBio.getMusicBrainzId());
            result.setLastFmUrl(artistBio.getLastFmUrl());
            result.setSmallImageUrl(artistBio.getSmallImageUrl());
            result.setMediumImageUrl(artistBio.getMediumImageUrl());
            result.setLargeImageUrl(artistBio.getLargeImageUrl());
        }

        return ResponseEntity.ok(result);
    }

    private <T extends ArtistID3> T createArtist(T jaxbArtist, Artist artist, String username) {
        jaxbArtist.setId(String.valueOf(artist.getId()));
        jaxbArtist.setName(artist.getName());
        jaxbArtist.setStarred(mediaFileDao.getMediaFileStarredDate(artist.getId(), username));
        jaxbArtist.setAlbumCount(artist.getAlbumCount());
        if (artist.getCoverArtPath() != null) {
            jaxbArtist.setCoverArt(CoverArtController.ARTIST_COVERART_PREFIX + artist.getId());
        }
        return jaxbArtist;
    }

    private org.airsonic.restapi.domain.Artist createArtist(MediaFile artist, String username) {
        org.airsonic.restapi.domain.Artist result = new org.airsonic.restapi.domain.Artist();
        result.setId(String.valueOf(artist.getId()));
        result.setName(artist.getArtist());
        Date starred = mediaFileDao.getMediaFileStarredDate(artist.getId(), username);
        result.setStarred(starred);
        return result;
    }

    @RequestMapping(value = "/getArtist", method = RequestMethod.GET)
    public ResponseEntity<ArtistWithAlbumsID3> getArtist(HttpServletRequest request) throws Exception {
        String username = securityService.getCurrentUsername(request);
        int id = getRequiredIntParameter(request, "id");
        Artist artist = artistDao.getArtist(id);
        if (artist == null) {
            return ResponseEntity.notFound().build();
        }

        List<MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username);
        ArtistWithAlbumsID3 result = createArtist(new ArtistWithAlbumsID3(), artist, username);
        for (Album album : albumDao.getAlbumsForArtist(artist.getName(), musicFolders)) {
            result.getAlbum().add(createAlbum(new AlbumID3(), album, username));
        }

        return ResponseEntity.ok(result);
    }

    private <T extends AlbumID3> T createAlbum(T jaxbAlbum, Album album, String username) {
        jaxbAlbum.setId(String.valueOf(album.getId()));
        jaxbAlbum.setName(album.getName());
        if (album.getArtist() != null) {
            jaxbAlbum.setArtist(album.getArtist());
            Artist artist = artistDao.getArtist(album.getArtist());
            if (artist != null) {
                jaxbAlbum.setArtistId(String.valueOf(artist.getId()));
            }
        }
        if (album.getCoverArtPath() != null) {
            jaxbAlbum.setCoverArt(CoverArtController.ALBUM_COVERART_PREFIX + album.getId());
        }
        jaxbAlbum.setSongCount(album.getSongCount());
        jaxbAlbum.setDuration(album.getDurationSeconds());
        jaxbAlbum.setCreated(album.getCreated());
        jaxbAlbum.setStarred(albumDao.getAlbumStarredDate(album.getId(), username));
        jaxbAlbum.setYear(album.getYear());
        jaxbAlbum.setGenre(album.getGenre());
        return jaxbAlbum;
    }

    private <T extends org.airsonic.restapi.domain.Playlist> T createPlaylist(T jaxbPlaylist, Playlist playlist) {
        jaxbPlaylist.setId(String.valueOf(playlist.getId()));
        jaxbPlaylist.setName(playlist.getName());
        jaxbPlaylist.setComment(playlist.getComment());
        jaxbPlaylist.setOwner(playlist.getUsername());
        jaxbPlaylist.setPublic(playlist.isShared());
        jaxbPlaylist.setSongCount(playlist.getFileCount());
        jaxbPlaylist.setDuration(playlist.getDurationSeconds());
        jaxbPlaylist.setCreated(playlist.getCreated());
        jaxbPlaylist.setChanged(playlist.getChanged());
        jaxbPlaylist.setCoverArt(CoverArtController.PLAYLIST_COVERART_PREFIX + playlist.getId());

        for (String username : playlistService.getPlaylistUsers(playlist.getId())) {
            jaxbPlaylist.getAllowedUser().add(username);
        }
        return jaxbPlaylist;
    }

    @RequestMapping(value = "/getAlbum", method = RequestMethod.GET)
    public ResponseEntity<AlbumWithSongsID3> getAlbum(HttpServletRequest request) throws Exception {
        String username = securityService.getCurrentUsername(request);

        int id = getRequiredIntParameter(request, "id");
        Album album = albumDao.getAlbum(id);
        if (album == null) {
            return ResponseEntity.notFound().build();
        }

        AlbumWithSongsID3 result = createAlbum(new AlbumWithSongsID3(), album, username);
        for (MediaFile mediaFile : mediaFileDao.getSongsForAlbum(album.getArtist(), album.getName())) {
            result.getSong().add(createChild(mediaFile, username));
        }

        return ResponseEntity.ok(result);
    }

    @RequestMapping(value = "/getSong", method = RequestMethod.GET)
    public ResponseEntity<Child> getSong(HttpServletRequest request) throws Exception {
        String username = securityService.getCurrentUsername(request);

        int id = getRequiredIntParameter(request, "id");
        MediaFile song = mediaFileDao.getMediaFile(id);
        if (song == null || song.isDirectory()) {
            return ResponseEntity.notFound().build();
        }
        if (!securityService.isFolderAccessAllowed(song, username)) {
            return ResponseEntity.status(UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(createChild(song, username));
    }

    @RequestMapping(value = "/getMusicDirectory", method = RequestMethod.GET)
    public ResponseEntity<Directory> getMusicDirectory(HttpServletRequest request) throws Exception {
        String username = securityService.getCurrentUsername(request);

        int id = getRequiredIntParameter(request, "id");
        MediaFile dir = mediaFileService.getMediaFile(id);
        if (dir == null) {
            return ResponseEntity.notFound().build();
        }
        if (!securityService.isFolderAccessAllowed(dir, username)) {
            return ResponseEntity.status(UNAUTHORIZED).build();
        }

        MediaFile parent = mediaFileService.getParentOf(dir);
        Directory directory = new Directory();
        directory.setId(String.valueOf(id));
        try {
            if (!mediaFileService.isRoot(parent)) {
                directory.setParent(String.valueOf(parent.getId()));
            }
        } catch (SecurityException x) {
            // Ignored.
        }
        directory.setName(dir.getName());
        directory.setStarred(mediaFileDao.getMediaFileStarredDate(id, username));
        directory.setPlayCount((long) dir.getPlayCount());

        if (dir.isAlbum()) {
            directory.setAverageRating(ratingService.getAverageRating(dir));
            directory.setUserRating(ratingService.getRatingForUser(username, dir));
        }

        for (MediaFile child : mediaFileService.getChildrenOf(dir, true, true, true)) {
            directory.getChild().add(createChild(child, username));
        }

        return ResponseEntity.ok(directory);
    }

    @RequestMapping(value = "/search", method = RequestMethod.GET)
    public ResponseEntity<SearchResult> search(HttpServletRequest request) throws Exception {
        String username = securityService.getCurrentUsername(request);
        Integer musicFolderId = getIntParameter(request, "musicFolderId");
        List<MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username, musicFolderId);

        SearchResult searchResult = new SearchResult();

        String query = request.getParameter("query");
        SearchCriteria criteria = new SearchCriteria();
        criteria.setQuery(StringUtils.trimToEmpty(query));
        criteria.setCount(getIntParameter(request, "artistCount", 20));
        criteria.setOffset(getIntParameter(request, "artistOffset", 0));
        org.airsonic.player.domain.SearchResult result = searchService.search(criteria, musicFolders, SearchService.IndexType.ARTIST_ID3);
        for (Artist artist : result.getArtists()) {
            searchResult.getArtist().add(createArtist(new ArtistID3(), artist, username));
        }

        criteria.setCount(getIntParameter(request, "albumCount", 20));
        criteria.setOffset(getIntParameter(request, "albumOffset", 0));
        result = searchService.search(criteria, musicFolders, SearchService.IndexType.ALBUM_ID3);
        for (Album album : result.getAlbums()) {
            searchResult.getAlbum().add(createAlbum(new AlbumID3(), album, username));
        }

        criteria.setCount(getIntParameter(request, "songCount", 20));
        criteria.setOffset(getIntParameter(request, "songOffset", 0));
        result = searchService.search(criteria, musicFolders, SearchService.IndexType.SONG);
        for (MediaFile song : result.getMediaFiles()) {
            searchResult.getSong().add(createChild(song, username));
        }

        return ResponseEntity.ok(searchResult);
    }

    @RequestMapping(value = "/getPlaylists", method = RequestMethod.GET)
    public ResponseEntity<Playlists> getPlaylists(HttpServletRequest request) throws Exception {
        User user = securityService.getCurrentUser(request);
        String authenticatedUsername = user.getUsername();
        String requestedUsername = request.getParameter("username");

        if (requestedUsername == null) {
            requestedUsername = authenticatedUsername;
        } else if (!user.isAdminRole()) {
            return ResponseEntity.status(UNAUTHORIZED).build();
        }

        Playlists result = new Playlists();

        for (Playlist playlist : playlistService.getReadablePlaylistsForUser(requestedUsername)) {
            result.getPlaylist().add(createPlaylist(new org.airsonic.restapi.domain.Playlist(), playlist));
        }

        return ResponseEntity.ok(result);
    }

    @RequestMapping(value = "/getPlaylist", method = RequestMethod.GET)
    public ResponseEntity<PlaylistWithSongs> getPlaylist(HttpServletRequest request) throws Exception {
        String username = securityService.getCurrentUsername(request);

        int id = getRequiredIntParameter(request, "id");

        Playlist playlist = playlistService.getPlaylist(id);
        if (playlist == null) {
            return ResponseEntity.notFound().build();
        }
        if (!playlistService.isReadAllowed(playlist, username)) {
            return ResponseEntity.status(UNAUTHORIZED).build();
        }
        PlaylistWithSongs result = createPlaylist(new PlaylistWithSongs(), playlist);
        for (MediaFile mediaFile : playlistService.getFilesInPlaylist(id)) {
            if (securityService.isFolderAccessAllowed(mediaFile, username)) {
                result.getEntry().add(createChild(mediaFile, username));
            }
        }

        return ResponseEntity.ok(result);
    }

    @RequestMapping(value = "/jukeboxControl", method = RequestMethod.POST)
    public ResponseEntity<JukeboxStatus> jukeboxControl(HttpServletRequest request, HttpServletResponse response) throws Exception {

        User user = securityService.getCurrentUser(request);
        if (!user.isJukeboxRole()) {
            return ResponseEntity.status(UNAUTHORIZED).build();
        }

        boolean returnPlaylist = false;
        String action = getRequiredStringParameter(request, "action");
        if ("start".equals(action)) {
            playQueueService.doStart(request, response);
        } else if ("stop".equals(action)) {
            playQueueService.doStop(request, response);
        } else if ("skip".equals(action)) {
            int index = getRequiredIntParameter(request, "index");
            int offset = getIntParameter(request, "offset", 0);
            playQueueService.doSkip(request, response, index, offset);
        } else if ("add".equals(action)) {
            int[] ids = getIntParameters(request, "id");
            playQueueService.doAdd(request, response, ids, null);
        } else if ("set".equals(action)) {
            int[] ids = getIntParameters(request, "id");
            playQueueService.doSet(request, response, ids);
        } else if ("clear".equals(action)) {
            playQueueService.doClear(request, response);
        } else if ("remove".equals(action)) {
            int index = getRequiredIntParameter(request, "index");
            playQueueService.doRemove(request, response, index);
        } else if ("shuffle".equals(action)) {
            playQueueService.doShuffle(request, response);
        } else if ("setGain".equals(action)) {
            float gain = getRequiredFloatParameter(request, "gain");
            jukeboxService.setGain(gain);
        } else if ("get".equals(action)) {
            returnPlaylist = true;
        } else if ("status".equals(action)) {
            // No action necessary.
        } else {
            LOG.warn("Unknown jukebox action: '" + action + "'.");
            return ResponseEntity.status(BAD_REQUEST).build();
        }

        Player player = getPlayer(request, response);
        String username = securityService.getCurrentUsername(request);
        Player jukeboxPlayer = jukeboxService.getPlayer();
        boolean controlsJukebox = jukeboxPlayer != null && jukeboxPlayer.getId().equals(player.getId());
        PlayQueue playQueue = player.getPlayQueue();

        int currentIndex = controlsJukebox && !playQueue.isEmpty() ? playQueue.getIndex() : -1;
        boolean playing = controlsJukebox && !playQueue.isEmpty() && playQueue.getStatus() == PlayQueue.Status.PLAYING;
        float gain = jukeboxService.getGain();
        int position = controlsJukebox && !playQueue.isEmpty() ? jukeboxService.getPosition() : 0;

        JukeboxStatus result;
        if (returnPlaylist) {
            result = new JukeboxPlaylist();
            result.setCurrentIndex(currentIndex);
            result.setPlaying(playing);
            result.setGain(gain);
            result.setPosition(position);
            for (MediaFile mediaFile : playQueue.getFiles()) {
                ((JukeboxPlaylist) result).getEntry().add(createChild(mediaFile, username));
            }
        } else {
            result = new JukeboxStatus();
            result.setCurrentIndex(currentIndex);
            result.setPlaying(playing);
            result.setGain(gain);
            result.setPosition(position);
        }

        return ResponseEntity.ok(result);
    }

    @RequestMapping(value = "/createPlaylist", method = RequestMethod.POST)
    public ResponseEntity<Void> createPlaylist(HttpServletRequest request) throws Exception {
        String username = securityService.getCurrentUsername(request);

        Integer playlistId = getIntParameter(request, "playlistId");
        String name = request.getParameter("name");
        if (playlistId == null && name == null) {
            LOG.warn("Playlist ID or name must be specified.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        Playlist playlist;
        if (playlistId != null) {
            playlist = playlistService.getPlaylist(playlistId);
            if (playlist == null) {
                return ResponseEntity.notFound().build();
            }
            if (!playlistService.isWriteAllowed(playlist, username)) {
                return ResponseEntity.status(UNAUTHORIZED).build();
            }
        } else {
            playlist = new Playlist();
            playlist.setName(name);
            playlist.setCreated(new Date());
            playlist.setChanged(new Date());
            playlist.setShared(false);
            playlist.setUsername(username);
            playlistService.createPlaylist(playlist);
        }

        List<MediaFile> songs = new ArrayList<MediaFile>();
        for (int id : getIntParameters(request, "songId")) {
            MediaFile song = mediaFileService.getMediaFile(id);
            if (song != null) {
                songs.add(song);
            }
        }
        playlistService.setFilesInPlaylist(playlist.getId(), songs);

        return ResponseEntity.noContent().build();
    }

    @RequestMapping(value = "/updatePlaylist", method = RequestMethod.POST)
    public ResponseEntity<Void> updatePlaylist(HttpServletRequest request) throws Exception {
        String username = securityService.getCurrentUsername(request);

        int id = getRequiredIntParameter(request, "playlistId");
        Playlist playlist = playlistService.getPlaylist(id);
        if (playlist == null) {
            return ResponseEntity.notFound().build();
        }
        if (!playlistService.isWriteAllowed(playlist, username)) {
            return ResponseEntity.status(UNAUTHORIZED).build();
        }

        String name = request.getParameter("name");
        if (name != null) {
            playlist.setName(name);
        }
        String comment = request.getParameter("comment");
        if (comment != null) {
            playlist.setComment(comment);
        }
        Boolean shared = getBooleanParameter(request, "public");
        if (shared != null) {
            playlist.setShared(shared);
        }
        playlistService.updatePlaylist(playlist);

        // TODO: Add later
//            for (String usernameToAdd : ServletRequestUtils.getStringParameters(request, "usernameToAdd")) {
//                if (securityService.getUserByName(usernameToAdd) != null) {
//                    playlistService.addPlaylistUser(id, usernameToAdd);
//                }
//            }
//            for (String usernameToRemove : ServletRequestUtils.getStringParameters(request, "usernameToRemove")) {
//                if (securityService.getUserByName(usernameToRemove) != null) {
//                    playlistService.deletePlaylistUser(id, usernameToRemove);
//                }
//            }
        List<MediaFile> songs = playlistService.getFilesInPlaylist(id);
        boolean songsChanged = false;

        SortedSet<Integer> tmp = new TreeSet<Integer>();
        for (int songIndexToRemove : getIntParameters(request, "songIndexToRemove")) {
            tmp.add(songIndexToRemove);
        }
        List<Integer> songIndexesToRemove = new ArrayList<Integer>(tmp);
        Collections.reverse(songIndexesToRemove);
        for (Integer songIndexToRemove : songIndexesToRemove) {
            songs.remove(songIndexToRemove.intValue());
            songsChanged = true;
        }
        for (int songToAdd : getIntParameters(request, "songIdToAdd")) {
            MediaFile song = mediaFileService.getMediaFile(songToAdd);
            if (song != null) {
                songs.add(song);
                songsChanged = true;
            }
        }
        if (songsChanged) {
            playlistService.setFilesInPlaylist(id, songs);
        }

        return ResponseEntity.noContent().build();
    }

    @RequestMapping(value = "/deletePlaylist", method = RequestMethod.DELETE)
    public ResponseEntity<Void> deletePlaylist(HttpServletRequest request) throws Exception {
        String username = securityService.getCurrentUsername(request);

        int id = getRequiredIntParameter(request, "id");
        Playlist playlist = playlistService.getPlaylist(id);
        if (playlist == null) {
            return ResponseEntity.notFound().build();
        }
        if (!playlistService.isWriteAllowed(playlist, username)) {
            return ResponseEntity.status(UNAUTHORIZED).build();
        }
        playlistService.deletePlaylist(id);

        return ResponseEntity.status(NO_CONTENT).build();
    }

    @RequestMapping(value = "/getAlbumList", method = RequestMethod.GET)
    public ResponseEntity<AlbumList> getAlbumList(HttpServletRequest request) throws Exception {
        int size = getIntParameter(request, "size", 10);
        int offset = getIntParameter(request, "offset", 0);
        size = Math.max(0, Math.min(size, 500));
        String type = getRequiredStringParameter(request, "type");
        String username = securityService.getCurrentUsername(request);
        Integer musicFolderId = getIntParameter(request, "musicFolderId");
        List<MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username, musicFolderId);

        List<Album> albums;
        if ("frequent".equals(type)) {
            albums = albumDao.getMostFrequentlyPlayedAlbums(offset, size, musicFolders);
        } else if ("recent".equals(type)) {
            albums = albumDao.getMostRecentlyPlayedAlbums(offset, size, musicFolders);
        } else if ("newest".equals(type)) {
            albums = albumDao.getNewestAlbums(offset, size, musicFolders);
        } else if ("alphabeticalByArtist".equals(type)) {
            albums = albumDao.getAlphabetialAlbums(offset, size, true, musicFolders);
        } else if ("alphabeticalByName".equals(type)) {
            albums = albumDao.getAlphabetialAlbums(offset, size, false, musicFolders);
        } else if ("byGenre".equals(type)) {
            albums = albumDao.getAlbumsByGenre(offset,
                    size,
                    getRequiredStringParameter(request, "genre"),
                    musicFolders);
        } else if ("byYear".equals(type)) {
            albums = albumDao.getAlbumsByYear(offset, size, getRequiredIntParameter(request, "fromYear"),
                    getRequiredIntParameter(request, "toYear"), musicFolders);
        } else if ("starred".equals(type)) {
            albums = albumDao.getStarredAlbums(offset,
                    size,
                    securityService.getCurrentUser(request).getUsername(),
                    musicFolders);
        } else if ("random".equals(type)) {
            albums = searchService.getRandomAlbumsId3(size, musicFolders);
        } else {
            LOG.warn("Invalid list type: " + type);
            return ResponseEntity.badRequest().build();
        }
        AlbumList result = new AlbumList();
        for (Album album : albums) {
            result.getAlbum().add(createAlbum(new AlbumID3(), album, username));
        }
        return ResponseEntity.ok(result);
    }

    @RequestMapping(value = "/getRandomSongs", method = RequestMethod.GET)
    public ResponseEntity<Songs> getRandomSongs(HttpServletRequest request) throws Exception {
        String username = securityService.getCurrentUsername(request);

        int size = getIntParameter(request, "size", 10);
        size = Math.max(0, Math.min(size, 500));
        String genre = getStringParameter(request, "genre");
        Integer fromYear = getIntParameter(request, "fromYear");
        Integer toYear = getIntParameter(request, "toYear");
        Integer musicFolderId = getIntParameter(request, "musicFolderId");
        List<MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username, musicFolderId);
        RandomSearchCriteria criteria = new RandomSearchCriteria(size, genre, fromYear, toYear, musicFolders);

        Songs result = new Songs();
        for (MediaFile mediaFile : searchService.getRandomSongs(criteria)) {
            result.getSong().add(createChild(mediaFile, username));
        }
        return ResponseEntity.ok(result);
    }

    @RequestMapping(value = "/getVideos", method = RequestMethod.GET)
    public ResponseEntity<Videos> getVideos(HttpServletRequest request) throws Exception {
        String username = securityService.getCurrentUsername(request);

        int size = getIntParameter(request, "size", Integer.MAX_VALUE);
        int offset = getIntParameter(request, "offset", 0);
        List<MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username);

        Videos result = new Videos();
        for (MediaFile mediaFile : mediaFileDao.getVideos(size, offset, musicFolders)) {
            result.getVideo().add(createChild(mediaFile, username));
        }
        return ResponseEntity.ok(result);
    }

    @RequestMapping(value = "/getNowPlaying", method = RequestMethod.GET)
    public ResponseEntity<NowPlaying> getNowPlaying() {
        NowPlaying result = new NowPlaying();

        for (PlayStatus status : statusService.getPlayStatuses()) {

            Player player = status.getPlayer();
            MediaFile mediaFile = status.getMediaFile();
            String username = player.getUsername();
            if (username == null) {
                continue;
            }

            UserSettings userSettings = settingsService.getUserSettings(username);
            if (!userSettings.isNowPlayingAllowed()) {
                continue;
            }

            long minutesAgo = status.getMinutesAgo();
            if (minutesAgo < 60) {
                NowPlayingEntry entry = new NowPlayingEntry();
                entry.setUsername(username);
                entry.setPlayerId(Integer.parseInt(player.getId()));
                entry.setPlayerName(player.getName());
                entry.setMinutesAgo((int) minutesAgo);
                result.getEntry().add(createChild(entry, mediaFile, username));
            }
        }

        return ResponseEntity.ok(result);
    }

    private Child createChild(MediaFile mediaFile, String username) {
        return createChild(new Child(), mediaFile, username);
    }

    private <T extends Child> T createChild(T child, MediaFile mediaFile, String username) {
        MediaFile parent = mediaFileService.getParentOf(mediaFile);
        child.setId(String.valueOf(mediaFile.getId()));
        try {
            if (!mediaFileService.isRoot(parent)) {
                child.setParent(String.valueOf(parent.getId()));
            }
        } catch (SecurityException x) {
            // Ignored.
        }
        child.setTitle(mediaFile.getName());
        child.setAlbum(mediaFile.getAlbumName());
        child.setArtist(mediaFile.getArtist());
        child.setIsDir(mediaFile.isDirectory());
        child.setCoverArt(findCoverArt(mediaFile, parent));
        child.setYear(mediaFile.getYear());
        child.setGenre(mediaFile.getGenre());
        child.setCreated(mediaFile.getCreated());
        child.setStarred(mediaFileDao.getMediaFileStarredDate(mediaFile.getId(), username));
        child.setUserRating(ratingService.getRatingForUser(username, mediaFile));
        child.setAverageRating(ratingService.getAverageRating(mediaFile));
        child.setPlayCount((long) mediaFile.getPlayCount());

        if (mediaFile.isFile()) {
            child.setDuration(mediaFile.getDurationSeconds());
            child.setBitRate(mediaFile.getBitRate());
            child.setTrack(mediaFile.getTrackNumber());
            child.setDiscNumber(mediaFile.getDiscNumber());
            child.setSize(mediaFile.getFileSize());
            String suffix = mediaFile.getFormat();
            child.setSuffix(suffix);
            child.setContentType(StringUtil.getMimeType(suffix));
            child.setIsVideo(mediaFile.isVideo());
            child.setPath(getRelativePath(mediaFile));

            Bookmark bookmark = bookmarkService.getBookmarkForUserAndMediaFile(username, mediaFile);
            if (bookmark != null) {
                child.setBookmarkPosition(bookmark.getPositionMillis());
            }

            if (mediaFile.getAlbumArtist() != null && mediaFile.getAlbumName() != null) {
                Album album = albumDao.getAlbum(mediaFile.getAlbumArtist(), mediaFile.getAlbumName());
                if (album != null) {
                    child.setAlbumId(String.valueOf(album.getId()));
                }
            }
            if (mediaFile.getArtist() != null) {
                Artist artist = artistDao.getArtist(mediaFile.getArtist());
                if (artist != null) {
                    child.setArtistId(String.valueOf(artist.getId()));
                }
            }
            switch (mediaFile.getMediaType()) {
                case MUSIC:
                    child.setType(MediaType.MUSIC);
                    break;
                case PODCAST:
                    child.setType(MediaType.PODCAST);
                    break;
                case AUDIOBOOK:
                    child.setType(MediaType.AUDIOBOOK);
                    break;
                case VIDEO:
                    child.setType(MediaType.VIDEO);
                    child.setOriginalWidth(mediaFile.getWidth());
                    child.setOriginalHeight(mediaFile.getHeight());
                    break;
                default:
                    break;
            }
        }
        return child;
    }

    private String findCoverArt(MediaFile mediaFile, MediaFile parent) {
        MediaFile dir = mediaFile.isDirectory() ? mediaFile : parent;
        if (dir != null && dir.getCoverArtPath() != null) {
            return String.valueOf(dir.getId());
        }
        return null;
    }

    private String getRelativePath(MediaFile musicFile) {

        String filePath = musicFile.getPath();

        // Convert slashes.
        filePath = filePath.replace('\\', '/');

        String filePathLower = filePath.toLowerCase();

        List<MusicFolder> musicFolders = settingsService.getAllMusicFolders(false, true);
        for (MusicFolder musicFolder : musicFolders) {
            String folderPath = musicFolder.getPath().getPath();
            folderPath = folderPath.replace('\\', '/');
            String folderPathLower = folderPath.toLowerCase();
            if (!folderPathLower.endsWith("/")) {
                folderPathLower += "/";
            }

            if (filePathLower.startsWith(folderPathLower)) {
                String relativePath = filePath.substring(folderPath.length());
                return relativePath.startsWith("/") ? relativePath.substring(1) : relativePath;
            }
        }

        return null;
    }

    @RequestMapping(value = "/download", method = RequestMethod.GET)
    public void download(HttpServletRequest request, HttpServletResponse response) throws Exception {
        User user = securityService.getCurrentUser(request);
        if (!user.isDownloadRole()) {
            LOG.warn(user.getUsername() + " is not authorized to download files.");
            response.sendError(UNAUTHORIZED.value());
            return;
        }

        long ifModifiedSince = request.getDateHeader("If-Modified-Since");
        long lastModified = downloadController.getLastModified(request);

        if (ifModifiedSince != -1 && lastModified != -1 && lastModified <= ifModifiedSince) {
            response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }

        if (lastModified != -1) {
            response.setDateHeader("Last-Modified", lastModified);
        }

        downloadController.handleRequest(request, response);
    }

    @RequestMapping(value = "/stream", method = {RequestMethod.GET, RequestMethod.POST})
    public void stream(HttpServletRequest request, HttpServletResponse response) throws Exception {
        User user = securityService.getCurrentUser(request);
        if (!user.isStreamRole()) {
            LOG.warn(user.getUsername() + " is not authorized to play files.");
            response.sendError(UNAUTHORIZED.value());
            return;
        }

        streamController.handleRequest(request, response);
    }

    @RequestMapping(value = "/hls", method = {RequestMethod.GET, RequestMethod.POST})
    public void hls(HttpServletRequest request, HttpServletResponse response) throws Exception {
        User user = securityService.getCurrentUser(request);
        if (!user.isStreamRole()) {
            LOG.warn(user.getUsername() + " is not authorized to play files.");
            response.sendError(UNAUTHORIZED.value());
            return;
        }
        int id = getRequiredIntParameter(request, "id");
        MediaFile video = mediaFileDao.getMediaFile(id);
        if (video == null || video.isDirectory()) {
            LOG.warn("Video not found.");
            response.sendError(HttpStatus.NOT_FOUND.value());
            return;
        }
        if (!securityService.isFolderAccessAllowed(video, user.getUsername())) {
            LOG.warn("Access denied");
            response.sendError(UNAUTHORIZED.value());
            return;
        }
        hlsController.handleRequest(request, response);
    }

    @RequestMapping(value = "/scrobble", method = RequestMethod.POST)
    public ResponseEntity<Void> scrobble(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Player player = getPlayer(request, response);

        boolean submission = getBooleanParameter(request, "submission", true);
        int[] ids = getRequiredIntParameters(request, "id");
        long[] times = getLongParameters(request, "time");
        if (times.length > 0 && times.length != ids.length) {
            LOG.warn("Wrong number of timestamps: " + times.length);
            return ResponseEntity.status(BAD_REQUEST).build();
        }

        for (int i = 0; i < ids.length; i++) {
            int id = ids[i];
            MediaFile file = mediaFileService.getMediaFile(id);
            if (file == null) {
                LOG.warn("File to scrobble not found: " + id);
                continue;
            }
            Date time = times.length == 0 ? null : new Date(times[i]);

            statusService.addRemotePlay(new PlayStatus(file, player, time == null ? new Date() : time));
            mediaFileService.incrementPlayCount(file);
            if (settingsService.getUserSettings(player.getUsername()).isLastFmEnabled()) {
                audioScrobblerService.register(file, player.getUsername(), submission, time);
            }
        }
        return ResponseEntity.status(NO_CONTENT).build();
    }

    @RequestMapping(value = "/star", method = RequestMethod.POST)
    public ResponseEntity<Void> star(HttpServletRequest request) throws Exception {
        return starOrUnstar(request, true);
    }

    @RequestMapping(value = "/unstar", method = RequestMethod.POST)
    public ResponseEntity<Void> unstar(HttpServletRequest request) throws Exception {
        return starOrUnstar(request, false);
    }

    private ResponseEntity<Void> starOrUnstar(HttpServletRequest request, boolean star) throws Exception {

        String username = securityService.getCurrentUser(request).getUsername();
        for (int id : getIntParameters(request, "id")) {
            MediaFile mediaFile = mediaFileDao.getMediaFile(id);
            if (mediaFile == null) {
                LOG.warn("Media file not found: " + id);
                return ResponseEntity.notFound().build();
            }
            if (star) {
                mediaFileDao.starMediaFile(id, username);
            } else {
                mediaFileDao.unstarMediaFile(id, username);
            }
        }
        for (int albumId : getIntParameters(request, "albumId")) {
            Album album = albumDao.getAlbum(albumId);
            if (album == null) {
                LOG.warn("Album not found: " + albumId);
                return ResponseEntity.notFound().build();
            }
            if (star) {
                albumDao.starAlbum(albumId, username);
            } else {
                albumDao.unstarAlbum(albumId, username);
            }
        }
        for (int artistId : getIntParameters(request, "artistId")) {
            Artist artist = artistDao.getArtist(artistId);
            if (artist == null) {
                LOG.warn("Artist not found: " + artistId);
                return ResponseEntity.notFound().build();
            }
            if (star) {
                artistDao.starArtist(artistId, username);
            } else {
                artistDao.unstarArtist(artistId, username);
            }
        }
        return ResponseEntity.status(NO_CONTENT).build();
    }

    @RequestMapping(value = "/getStarred", method = RequestMethod.GET)
    public ResponseEntity<Starred> getStarred(HttpServletRequest request) throws Exception {
        String username = securityService.getCurrentUsername(request);
        Integer musicFolderId = getIntParameter(request, "musicFolderId");
        List<MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username, musicFolderId);

        Starred result = new Starred();
        for (Artist artist : artistDao.getStarredArtists(0, Integer.MAX_VALUE, username, musicFolders)) {
            result.getArtist().add(createArtist(new ArtistID3(), artist, username));
        }
        for (Album album : albumDao.getStarredAlbums(0, Integer.MAX_VALUE, username, musicFolders)) {
            result.getAlbum().add(createAlbum(new AlbumID3(), album, username));
        }
        for (MediaFile song : mediaFileDao.getStarredFiles(0, Integer.MAX_VALUE, username, musicFolders)) {
            result.getSong().add(createChild(song, username));
        }
        return ResponseEntity.ok(result);
    }

    @RequestMapping(value = "/getPodcasts", method = RequestMethod.GET)
    public ResponseEntity<Podcasts> getPodcasts(HttpServletRequest request) throws Exception {
        String username = securityService.getCurrentUsername(request);
        boolean includeEpisodes = getBooleanParameter(request, "includeEpisodes", true);
        Integer channelId = getIntParameter(request, "id");

        Podcasts result = new Podcasts();

        for (PodcastChannel channel : podcastService.getAllChannels()) {
            if (channelId == null || channelId.equals(channel.getId())) {

                org.airsonic.restapi.domain.PodcastChannel c = new org.airsonic.restapi.domain.PodcastChannel();
                result.getChannel().add(c);

                c.setId(String.valueOf(channel.getId()));
                c.setUrl(channel.getUrl());
                c.setStatus(PodcastStatus.valueOf(channel.getStatus().name()));
                c.setTitle(channel.getTitle());
                c.setDescription(channel.getDescription());
                c.setCoverArt(CoverArtController.PODCAST_COVERART_PREFIX + channel.getId());
                c.setOriginalImageUrl(channel.getImageUrl());
                c.setErrorMessage(channel.getErrorMessage());

                if (includeEpisodes) {
                    List<PodcastEpisode> episodes = podcastService.getEpisodes(channel.getId());
                    for (PodcastEpisode episode : episodes) {
                        c.getEpisode().add(createPodcastEpisode(username, episode));
                    }
                }
            }
        }
        return ResponseEntity.ok(result);
    }

    @RequestMapping(value = "/getNewestPodcasts", method = RequestMethod.GET)
    public ResponseEntity<NewestPodcasts> getNewestPodcasts(HttpServletRequest request) throws Exception {
        String username = securityService.getCurrentUsername(request);

        int count = getIntParameter(request, "count", 20);
        NewestPodcasts result = new NewestPodcasts();

        for (PodcastEpisode episode : podcastService.getNewestEpisodes(count)) {
            result.getEpisode().add(createPodcastEpisode(username, episode));
        }

        return ResponseEntity.ok(result);
    }

    private org.airsonic.restapi.domain.PodcastEpisode createPodcastEpisode(String username, PodcastEpisode episode) {
        org.airsonic.restapi.domain.PodcastEpisode e = new org.airsonic.restapi.domain.PodcastEpisode();

        String path = episode.getPath();
        if (path != null) {
            MediaFile mediaFile = mediaFileService.getMediaFile(path);
            e = createChild(new org.airsonic.restapi.domain.PodcastEpisode(), mediaFile, username);
            e.setStreamId(String.valueOf(mediaFile.getId()));
        }

        e.setId(String.valueOf(episode.getId()));  // Overwrites the previous "id" attribute.
        e.setChannelId(String.valueOf(episode.getChannelId()));
        e.setStatus(PodcastStatus.valueOf(episode.getStatus().name()));
        e.setTitle(episode.getTitle());
        e.setDescription(episode.getDescription());
        e.setPublishDate(episode.getPublishDate());
        return e;
    }

    @RequestMapping(value = "/refreshPodcasts", method = RequestMethod.POST)
    public ResponseEntity<Void> refreshPodcasts(HttpServletRequest request) throws Exception {
        User user = securityService.getCurrentUser(request);
        if (!user.isPodcastRole()) {
            LOG.warn(user.getUsername() + " is not authorized to administrate podcasts.");
            return ResponseEntity.status(UNAUTHORIZED).build();
        }
        podcastService.refreshAllChannels(true);
        return ResponseEntity.status(NO_CONTENT).build();
    }

    @RequestMapping(value = "/createPodcastChannel", method = RequestMethod.POST)
    public ResponseEntity<Void> createPodcastChannel(HttpServletRequest request) throws Exception {
        User user = securityService.getCurrentUser(request);
        if (!user.isPodcastRole()) {
            LOG.warn(user.getUsername() + " is not authorized to administrate podcasts.");
            return ResponseEntity.status(UNAUTHORIZED).build();
        }

        String url = getRequiredStringParameter(request, "url");
        podcastService.createChannel(url);
        return ResponseEntity.status(NO_CONTENT).build();
    }

    @RequestMapping(value = "/deletePodcastChannel", method = RequestMethod.DELETE)
    public ResponseEntity<Void> deletePodcastChannel(HttpServletRequest request) throws Exception {
        User user = securityService.getCurrentUser(request);
        if (!user.isPodcastRole()) {
            LOG.warn(user.getUsername() + " is not authorized to administrate podcasts.");
            return ResponseEntity.status(UNAUTHORIZED).build();
        }

        int id = getRequiredIntParameter(request, "id");
        podcastService.deleteChannel(id);
        return ResponseEntity.status(NO_CONTENT).build();
    }

    @RequestMapping(value = "/deletePodcastEpisode", method = RequestMethod.DELETE)
    public ResponseEntity<Void> deletePodcastEpisode(HttpServletRequest request) throws Exception {
        User user = securityService.getCurrentUser(request);
        if (!user.isPodcastRole()) {
            LOG.warn(user.getUsername() + " is not authorized to administrate podcasts.");
            return ResponseEntity.status(UNAUTHORIZED).build();
        }

        int id = getRequiredIntParameter(request, "id");
        podcastService.deleteEpisode(id, true);
        return ResponseEntity.status(NO_CONTENT).build();
    }

    @RequestMapping(value = "/downloadPodcastEpisode", method = RequestMethod.POST)
    public ResponseEntity<Void> downloadPodcastEpisode(HttpServletRequest request) throws Exception {
        User user = securityService.getCurrentUser(request);
        if (!user.isPodcastRole()) {
            LOG.warn(user.getUsername() + " is not authorized to administrate podcasts.");
            return ResponseEntity.status(UNAUTHORIZED).build();
        }

        int id = getRequiredIntParameter(request, "id");
        PodcastEpisode episode = podcastService.getEpisode(id, true);
        if (episode == null) {
            LOG.warn("Podcast episode " + id + " not found.");
            return ResponseEntity.notFound().build();
        }

        podcastService.downloadEpisode(episode);
        return ResponseEntity.accepted().build();
    }

    @RequestMapping(value = "/getInternetRadioStations", method = RequestMethod.GET)
    public ResponseEntity<InternetRadioStations> getInternetRadioStations() throws Exception {

        InternetRadioStations result = new InternetRadioStations();
        for (InternetRadio radio : settingsService.getAllInternetRadios()) {
            InternetRadioStation i = new InternetRadioStation();
            i.setId(String.valueOf(radio.getId()));
            i.setName(radio.getName());
            i.setStreamUrl(radio.getStreamUrl());
            i.setHomePageUrl(radio.getHomepageUrl());
            result.getInternetRadioStation().add(i);
        }
        return ResponseEntity.ok(result);
    }

    @RequestMapping(value = "/getBookmarks", method = RequestMethod.GET)
    public ResponseEntity<Bookmarks> getBookmarks(HttpServletRequest request) throws Exception {
        String username = securityService.getCurrentUsername(request);

        Bookmarks result = new Bookmarks();
        for (Bookmark bookmark : bookmarkService.getBookmarks(username)) {
            org.airsonic.restapi.domain.Bookmark b = new org.airsonic.restapi.domain.Bookmark();
            result.getBookmark().add(b);
            b.setPosition(bookmark.getPositionMillis());
            b.setUsername(bookmark.getUsername());
            b.setComment(bookmark.getComment());
            b.setCreated(bookmark.getCreated());
            b.setChanged(bookmark.getChanged());

            MediaFile mediaFile = mediaFileService.getMediaFile(bookmark.getMediaFileId());
            b.setEntry(createChild(mediaFile, username));
        }

        return ResponseEntity.ok(result);
    }

    @RequestMapping(value = "/createBookmark", method = RequestMethod.POST)
    public ResponseEntity<Void> createBookmark(HttpServletRequest request) throws Exception {
        String username = securityService.getCurrentUsername(request);
        int mediaFileId = getRequiredIntParameter(request, "id");
        long position = getRequiredLongParameter(request, "position");
        String comment = request.getParameter("comment");
        Date now = new Date();

        Bookmark bookmark = new Bookmark(0, mediaFileId, position, username, comment, now, now);
        bookmarkService.createOrUpdateBookmark(bookmark);
        return ResponseEntity.noContent().build();
    }

    @RequestMapping(value = "/deleteBookmark", method = RequestMethod.DELETE)
    public ResponseEntity<Void> deleteBookmark(HttpServletRequest request) throws Exception {

        String username = securityService.getCurrentUsername(request);
        int mediaFileId = getRequiredIntParameter(request, "id");
        bookmarkService.deleteBookmark(username, mediaFileId);

        return ResponseEntity.noContent().build();
    }

    @RequestMapping(value = "/getPlayQueue", method = RequestMethod.GET)
    public ResponseEntity<org.airsonic.restapi.domain.PlayQueue> getPlayQueue(HttpServletRequest request) throws Exception {
        String username = securityService.getCurrentUsername(request);

        SavedPlayQueue playQueue = playQueueDao.getPlayQueue(username);
        if (playQueue == null) {
            return ResponseEntity.noContent().build();
        }

        org.airsonic.restapi.domain.PlayQueue restPlayQueue = new org.airsonic.restapi.domain.PlayQueue();
        restPlayQueue.setUsername(playQueue.getUsername());
        restPlayQueue.setCurrent(playQueue.getCurrentMediaFileId());
        restPlayQueue.setPosition(playQueue.getPositionMillis());
        restPlayQueue.setChanged(playQueue.getChanged());
        restPlayQueue.setChangedBy(playQueue.getChangedBy());

        for (Integer mediaFileId : playQueue.getMediaFileIds()) {
            MediaFile mediaFile = mediaFileService.getMediaFile(mediaFileId);
            if (mediaFile != null) {
                restPlayQueue.getEntry().add(createChild(mediaFile, username));
            }
        }

        return ResponseEntity.ok(restPlayQueue);
    }

    @RequestMapping(value = "/savePlayQueue", method = RequestMethod.POST)
    public ResponseEntity<Void> savePlayQueue(HttpServletRequest request) throws Exception {
        String username = securityService.getCurrentUsername(request);
        List<Integer> mediaFileIds = Util.toIntegerList(getIntParameters(request, "id"));
        Integer current = getIntParameter(request, "current");
        Long position = getLongParameter(request, "position");
        Date changed = new Date();
        String changedBy = getRequiredStringParameter(request, "c");

        if (!mediaFileIds.contains(current)) {
            LOG.warn("Current track is not included in play queue");
            return ResponseEntity.status(INTERNAL_SERVER_ERROR).build();
        }

        SavedPlayQueue playQueue = new SavedPlayQueue(null,
                username,
                mediaFileIds,
                current,
                position,
                changed,
                changedBy);
        playQueueDao.savePlayQueue(playQueue);
        return ResponseEntity.noContent().build();
    }

    @RequestMapping(value = "/getShares", method = RequestMethod.GET)
    public ResponseEntity<Shares> getShares(HttpServletRequest request) throws Exception {
        String username = securityService.getCurrentUsername(request);
        User user = securityService.getCurrentUser(request);
        List<MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username);

        Shares result = new Shares();
        for (Share share : shareService.getSharesForUser(user)) {
            org.airsonic.restapi.domain.Share s = createShare(request, share);
            result.getShare().add(s);

            for (MediaFile mediaFile : shareService.getSharedFiles(share.getId(), musicFolders)) {
                s.getEntry().add(createChild(mediaFile, username));
            }
        }
        return ResponseEntity.ok(result);
    }

    @RequestMapping(value = "/createShare", method = RequestMethod.POST)
    public ResponseEntity<Shares> createShare(HttpServletRequest request) throws Exception {
        String username = securityService.getCurrentUsername(request);

        User user = securityService.getCurrentUser(request);
        if (!user.isShareRole()) {
            LOG.warn(user.getUsername() + " is not authorized to share media.");
            return ResponseEntity.status(UNAUTHORIZED).build();
        }

        List<MediaFile> files = new ArrayList<MediaFile>();
        for (int id : getRequiredIntParameters(request, "id")) {
            files.add(mediaFileService.getMediaFile(id));
        }

        Share share = shareService.createShare(request, files);
        share.setDescription(request.getParameter("description"));
        long expires = getLongParameter(request, "expires", 0L);
        if (expires != 0) {
            share.setExpires(new Date(expires));
        }
        shareService.updateShare(share);

        Shares result = new Shares();
        org.airsonic.restapi.domain.Share s = createShare(request, share);
        result.getShare().add(s);

        List<MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username);

        for (MediaFile mediaFile : shareService.getSharedFiles(share.getId(), musicFolders)) {
            s.getEntry().add(createChild(mediaFile, username));
        }

        return ResponseEntity.ok(result);
    }

    private Player getPlayer(HttpServletRequest request, HttpServletResponse response) {
        return playerService.getPlayer(request, response);
    }

    @RequestMapping(value = "/deleteShare", method = RequestMethod.DELETE)
    public ResponseEntity<Void> deleteShare(HttpServletRequest request) throws Exception {
        User user = securityService.getCurrentUser(request);
        int id = getRequiredIntParameter(request, "id");

        Share share = shareService.getShareById(id);
        if (share == null) {
            LOG.warn("Shared media not found.");
            return ResponseEntity.notFound().build();
        }
        if (!user.isAdminRole() && !share.getUsername().equals(user.getUsername())) {
            LOG.warn("Not authorized to delete shared media.");
            return ResponseEntity.status(UNAUTHORIZED).build();
        }

        shareService.deleteShare(id);
        return ResponseEntity.noContent().build();
    }

    @RequestMapping(value = "/updateShare", method = RequestMethod.POST)
    public ResponseEntity<Void> updateShare(HttpServletRequest request) throws Exception {
        User user = securityService.getCurrentUser(request);
        int id = getRequiredIntParameter(request, "id");

        Share share = shareService.getShareById(id);
        if (share == null) {
            LOG.warn("Shared media not found.");
            return ResponseEntity.notFound().build();
        }
        if (!user.isAdminRole() && !share.getUsername().equals(user.getUsername())) {
            LOG.warn("Not authorized to modify shared media.");
            return ResponseEntity.status(UNAUTHORIZED).build();
        }

        share.setDescription(request.getParameter("description"));
        String expiresString = request.getParameter("expires");
        if (expiresString != null) {
            long expires = Long.parseLong(expiresString);
            share.setExpires(expires == 0L ? null : new Date(expires));
        }
        shareService.updateShare(share);
        return ResponseEntity.noContent().build();
    }

    private org.airsonic.restapi.domain.Share createShare(HttpServletRequest request, Share share) {
        org.airsonic.restapi.domain.Share result = new org.airsonic.restapi.domain.Share();
        result.setId(String.valueOf(share.getId()));
        result.setUrl(shareService.getShareUrl(request, share));
        result.setUsername(share.getUsername());
        result.setCreated(share.getCreated());
        result.setVisitCount(share.getVisitCount());
        result.setDescription(share.getDescription());
        result.setExpires(share.getExpires());
        result.setLastVisited(share.getLastVisited());
        return result;
    }

    @RequestMapping(value = "/getCoverArt", method = RequestMethod.GET)
    public void getCoverArt(HttpServletRequest request, HttpServletResponse response) throws Exception {
        coverArtController.handleRequest(request, response);
    }

    @RequestMapping(value = "/getAvatar", method = RequestMethod.GET)
    public void getAvatar(HttpServletRequest request, HttpServletResponse response) throws Exception {
        avatarController.handleRequest(request, response);
    }

    @RequestMapping(value = "/changePassword", method = RequestMethod.POST)
    public ResponseEntity<Void> changePassword(HttpServletRequest request) throws Exception {

        String username = getRequiredStringParameter(request, "username");
        String password = decrypt(getRequiredStringParameter(request, "password"));

        User authUser = securityService.getCurrentUser(request);

        boolean allowed = authUser.isAdminRole()
                || username.equals(authUser.getUsername()) && authUser.isSettingsRole();

        if (!allowed) {
            LOG.warn(authUser.getUsername() + " is not authorized to change password for " + username);
            return ResponseEntity.status(UNAUTHORIZED).build();
        }

        User user = securityService.getUserByName(username);
        user.setPassword(password);
        securityService.updateUser(user);

        return ResponseEntity.noContent().build();
    }

    @RequestMapping(value = "/getUser", method = RequestMethod.GET)
    public ResponseEntity<User> getUser(HttpServletRequest request) throws Exception {

        String username = getRequiredStringParameter(request, "username");

        User currentUser = securityService.getCurrentUser(request);
        if (!username.equals(currentUser.getUsername()) && !currentUser.isAdminRole()) {
            LOG.warn(currentUser.getUsername() + " is not authorized to get details for other users.");
            return ResponseEntity.status(UNAUTHORIZED).build();
        }

        User requestedUser = securityService.getUserByName(username);
        if (requestedUser == null) {
            LOG.warn("No such user: " + username);
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(requestedUser);
    }

    @RequestMapping(value = "/getUsers", method = RequestMethod.GET)
    public ResponseEntity<Users> getUsers(HttpServletRequest request) throws Exception {

        User currentUser = securityService.getCurrentUser(request);
        if (!currentUser.isAdminRole()) {
            LOG.warn(currentUser.getUsername() + " is not authorized to get details for other users.");
            return ResponseEntity.status(UNAUTHORIZED).build();
        }

        Users result = new Users();
        for (User user : securityService.getAllUsers()) {
            result.getUser().add(createUser(user));
        }

        return ResponseEntity.ok(result);
    }

    private org.airsonic.restapi.domain.User createUser(User user) {
        UserSettings userSettings = settingsService.getUserSettings(user.getUsername());

        org.airsonic.restapi.domain.User result = new org.airsonic.restapi.domain.User();
        result.setUsername(user.getUsername());
        result.setEmail(user.getEmail());
        result.setScrobblingEnabled(userSettings.isLastFmEnabled());
        result.setAdminRole(user.isAdminRole());
        result.setSettingsRole(user.isSettingsRole());
        result.setDownloadRole(user.isDownloadRole());
        result.setUploadRole(user.isUploadRole());
        result.setPlaylistRole(true);  // Since 1.8.0
        result.setCoverArtRole(user.isCoverArtRole());
        result.setCommentRole(user.isCommentRole());
        result.setPodcastRole(user.isPodcastRole());
        result.setStreamRole(user.isStreamRole());
        result.setJukeboxRole(user.isJukeboxRole());
        result.setShareRole(user.isShareRole());
        // currently this role isn't supported by airsonic
        result.setVideoConversionRole(false);
        // Useless
        result.setAvatarLastChanged(null);

        TranscodeScheme transcodeScheme = userSettings.getTranscodeScheme();
        if (transcodeScheme != null && transcodeScheme != TranscodeScheme.OFF) {
            result.setMaxBitRate(transcodeScheme.getMaxBitRate());
        }

        List<MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(user.getUsername());
        for (MusicFolder musicFolder : musicFolders) {
            result.getFolder().add(musicFolder.getId());
        }
        return result;
    }

    @RequestMapping(value = "/createUser", method = RequestMethod.POST)
    public ResponseEntity<Void> createUser(HttpServletRequest request) throws Exception {
        User user = securityService.getCurrentUser(request);
        if (!user.isAdminRole()) {
            LOG.warn(user.getUsername() + " is not authorized to create new users.");
            return ResponseEntity.status(UNAUTHORIZED).build();
        }

        UserSettingsCommand command = new UserSettingsCommand();
        command.setUsername(getRequiredStringParameter(request, "username"));
        command.setPassword(decrypt(getRequiredStringParameter(request, "password")));
        command.setEmail(getRequiredStringParameter(request, "email"));
        command.setLdapAuthenticated(getBooleanParameter(request, "ldapAuthenticated", false));
        command.setAdminRole(getBooleanParameter(request, "adminRole", false));
        command.setCommentRole(getBooleanParameter(request, "commentRole", false));
        command.setCoverArtRole(getBooleanParameter(request, "coverArtRole", false));
        command.setDownloadRole(getBooleanParameter(request, "downloadRole", false));
        command.setStreamRole(getBooleanParameter(request, "streamRole", true));
        command.setUploadRole(getBooleanParameter(request, "uploadRole", false));
        command.setJukeboxRole(getBooleanParameter(request, "jukeboxRole", false));
        command.setPodcastRole(getBooleanParameter(request, "podcastRole", false));
        command.setSettingsRole(getBooleanParameter(request, "settingsRole", true));
        command.setShareRole(getBooleanParameter(request, "shareRole", false));
        command.setTranscodeSchemeName(TranscodeScheme.OFF.name());

        int[] folderIds = ServletRequestUtils.getIntParameters(request, "musicFolderId");
        if (folderIds.length == 0) {
            folderIds = Util.toIntArray(MusicFolder.toIdList(settingsService.getAllMusicFolders()));
        }
        command.setAllowedMusicFolderIds(folderIds);

        userSettingsController.createUser(command);
        return ResponseEntity.noContent().build();
    }

    @RequestMapping(value = "/updateUser", method = RequestMethod.POST)
    public ResponseEntity<Void> updateUser(HttpServletRequest request) throws Exception {
        User user = securityService.getCurrentUser(request);
        if (!user.isAdminRole()) {
            LOG.warn(user.getUsername() + " is not authorized to update users.");
            return ResponseEntity.status(UNAUTHORIZED).build();
        }

        String username = getRequiredStringParameter(request, "username");
        User u = securityService.getUserByName(username);
        UserSettings s = settingsService.getUserSettings(username);

        if (u == null) {
            LOG.warn("No such user: " + username);
            return ResponseEntity.notFound().build();
        } else if (User.USERNAME_ADMIN.equals(username)) {
            LOG.warn("Not allowed to change admin user");
            return ResponseEntity.status(UNAUTHORIZED).build();
        }

        UserSettingsCommand command = new UserSettingsCommand();
        command.setUsername(username);
        command.setEmail(getStringParameter(request, "email", u.getEmail()));
        command.setLdapAuthenticated(getBooleanParameter(request, "ldapAuthenticated", u.isLdapAuthenticated()));
        command.setAdminRole(getBooleanParameter(request, "adminRole", u.isAdminRole()));
        command.setCommentRole(getBooleanParameter(request, "commentRole", u.isCommentRole()));
        command.setCoverArtRole(getBooleanParameter(request, "coverArtRole", u.isCoverArtRole()));
        command.setDownloadRole(getBooleanParameter(request, "downloadRole", u.isDownloadRole()));
        command.setStreamRole(getBooleanParameter(request, "streamRole", u.isDownloadRole()));
        command.setUploadRole(getBooleanParameter(request, "uploadRole", u.isUploadRole()));
        command.setJukeboxRole(getBooleanParameter(request, "jukeboxRole", u.isJukeboxRole()));
        command.setPodcastRole(getBooleanParameter(request, "podcastRole", u.isPodcastRole()));
        command.setSettingsRole(getBooleanParameter(request, "settingsRole", u.isSettingsRole()));
        command.setShareRole(getBooleanParameter(request, "shareRole", u.isShareRole()));

        int maxBitRate = getIntParameter(request, "maxBitRate", s.getTranscodeScheme().getMaxBitRate());
        command.setTranscodeSchemeName(TranscodeScheme.fromMaxBitRate(maxBitRate).name());

        if (hasParameter(request, "password")) {
            command.setPassword(decrypt(getRequiredStringParameter(request, "password")));
            command.setPasswordChange(true);
        }

        int[] folderIds = ServletRequestUtils.getIntParameters(request, "musicFolderId");
        if (folderIds.length == 0) {
            folderIds = Util.toIntArray(MusicFolder.toIdList(settingsService.getMusicFoldersForUser(username)));
        }
        command.setAllowedMusicFolderIds(folderIds);

        userSettingsController.updateUser(command);
        return ResponseEntity.noContent().build();
    }

    private boolean hasParameter(HttpServletRequest request, String name) {
        return request.getParameter(name) != null;
    }

    @RequestMapping(value = "/deleteUser", method = RequestMethod.DELETE)
    public ResponseEntity<Void> deleteUser(HttpServletRequest request) throws Exception {
        User user = securityService.getCurrentUser(request);
        if (!user.isAdminRole()) {
            LOG.warn(user.getUsername() + " is not authorized to delete users.");
            return ResponseEntity.status(UNAUTHORIZED).build();
        }

        String username = getRequiredStringParameter(request, "username");
        if (User.USERNAME_ADMIN.equals(username)) {
            LOG.warn("Not allowed to delete admin user");
            return ResponseEntity.status(UNAUTHORIZED).build();
        }

        securityService.deleteUser(username);

        return ResponseEntity.noContent().build();
    }

    @RequestMapping(value = "/getLyrics", method = RequestMethod.GET)
    public ResponseEntity<Lyrics> getLyrics(HttpServletRequest request) throws Exception {
        String artist = request.getParameter("artist");
        String title = request.getParameter("title");
        LyricsInfo lyrics = lyricsService.getLyrics(artist, title);

        Lyrics result = new Lyrics();
        result.setArtist(lyrics.getArtist());
        result.setTitle(lyrics.getTitle());
        result.setContent(lyrics.getLyrics());

        return ResponseEntity.ok(result);
    }

    @RequestMapping(value = "/setRating", method = RequestMethod.POST)
    public ResponseEntity<Void> setRating(HttpServletRequest request) throws Exception {
        Integer rating = getRequiredIntParameter(request, "rating");
        if (rating == 0) {
            rating = null;
        }

        int id = getRequiredIntParameter(request, "id");
        MediaFile mediaFile = mediaFileService.getMediaFile(id);
        if (mediaFile == null) {
            LOG.warn("File not found: " + id);
            return ResponseEntity.notFound().build();
        }

        String username = securityService.getCurrentUsername(request);
        ratingService.setRatingForUser(username, mediaFile, rating);

        return ResponseEntity.noContent().build();
    }

    @RequestMapping(path = "/getAlbumInfo", method = RequestMethod.GET)
    public ResponseEntity<AlbumInfo> getAlbumInfo(HttpServletRequest request) throws Exception {

        int id = ServletRequestUtils.getRequiredIntParameter(request, "id");

        MediaFile mediaFile = this.mediaFileService.getMediaFile(id);
        if (mediaFile == null) {
            LOG.warn("Media file not found.");
            return ResponseEntity.notFound().build();
        }
        AlbumNotes albumNotes = this.lastFmService.getAlbumNotes(mediaFile);

        AlbumInfo result = getAlbumInfoInternal(albumNotes);
        return ResponseEntity.ok(result);
    }

    @RequestMapping(path = "/getAlbumInfo2", method = RequestMethod.GET)
    public ResponseEntity<AlbumInfo> getAlbumInfo2(HttpServletRequest request) throws Exception {

        int id = ServletRequestUtils.getRequiredIntParameter(request, "id");

        Album album = this.albumDao.getAlbum(id);
        if (album == null) {
            LOG.warn("Album not found.");
            return ResponseEntity.notFound().build();
        }
        AlbumNotes albumNotes = this.lastFmService.getAlbumNotes(album);

        AlbumInfo result = getAlbumInfoInternal(albumNotes);
        return ResponseEntity.ok(result);
    }

    private AlbumInfo getAlbumInfoInternal(AlbumNotes albumNotes) {
        AlbumInfo result = new AlbumInfo();
        if (albumNotes != null) {
            result.setNotes(albumNotes.getNotes());
            result.setMusicBrainzId(albumNotes.getMusicBrainzId());
            result.setLastFmUrl(albumNotes.getLastFmUrl());
            result.setSmallImageUrl(albumNotes.getSmallImageUrl());
            result.setMediumImageUrl(albumNotes.getMediumImageUrl());
            result.setLargeImageUrl(albumNotes.getLargeImageUrl());
        }
        return result;
    }

    @RequestMapping(value = "/startScan", method = RequestMethod.POST)
    public ResponseEntity<Void> startScan() {
        mediaScannerService.scanLibrary();
        getScanStatus();
        return ResponseEntity.status(ACCEPTED).build();
    }

    @RequestMapping(value = "/getScanStatus", method = RequestMethod.GET)
    public ResponseEntity<ScanStatus> getScanStatus() {
        ScanStatus scanStatus = new ScanStatus();
        scanStatus.setScanning(this.mediaScannerService.isScanning());
        scanStatus.setCount((long) this.mediaScannerService.getScanCount());

        return ResponseEntity.ok(scanStatus);
    }
}
