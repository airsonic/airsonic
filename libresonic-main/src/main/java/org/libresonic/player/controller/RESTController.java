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
package org.libresonic.player.controller;

import org.apache.commons.lang.StringUtils;
import org.libresonic.player.Logger;
import org.libresonic.player.ajax.ChatService;
import org.libresonic.player.ajax.LyricsInfo;
import org.libresonic.player.ajax.LyricsService;
import org.libresonic.player.ajax.PlayQueueService;
import org.libresonic.player.command.UserSettingsCommand;
import org.libresonic.player.dao.*;
import org.libresonic.player.domain.*;
import org.libresonic.player.domain.Artist;
import org.libresonic.player.domain.Bookmark;
import org.libresonic.player.domain.Genre;
import org.libresonic.player.domain.MusicFolder;
import org.libresonic.player.domain.PlayQueue;
import org.libresonic.player.domain.Playlist;
import org.libresonic.player.domain.PodcastChannel;
import org.libresonic.player.domain.PodcastEpisode;
import org.libresonic.player.domain.SearchResult;
import org.libresonic.player.domain.Share;
import org.libresonic.player.domain.User;
import org.libresonic.player.service.*;
import org.libresonic.player.util.Pair;
import org.libresonic.player.util.StringUtil;
import org.libresonic.player.util.Util;
import org.libresonic.restapi.*;
import org.libresonic.restapi.Genres;
import org.libresonic.restapi.PodcastStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.libresonic.player.security.RESTRequestParameterProcessingFilter.decrypt;
import static org.springframework.web.bind.ServletRequestUtils.*;

/**
 * Multi-controller used for the REST API.
 * <p/>
 * For documentation, please refer to api.jsp.
 * <p/>
 * Note: Exceptions thrown from the methods are intercepted by RESTFilter.
 *
 * @author Sindre Mehus
 */
@Controller
public class RESTController {

    private static final Logger LOG = Logger.getLogger(RESTController.class);

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
    private TranscodingService transcodingService;
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
    private ChatService chatService;
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
    private BookmarkDao bookmarkDao;
    @Autowired
    private PlayQueueDao playQueueDao;

    private final Map<BookmarkKey, Bookmark> bookmarkCache = new ConcurrentHashMap<BookmarkKey, Bookmark>();
    private final JAXBWriter jaxbWriter = new JAXBWriter();

    @PostConstruct
    public void init() {
        refreshBookmarkCache();
    }

    private void refreshBookmarkCache() {
        bookmarkCache.clear();
        for (Bookmark bookmark : bookmarkDao.getBookmarks()) {
            bookmarkCache.put(BookmarkKey.forBookmark(bookmark), bookmark);
        }
    }

    @RequestMapping(value = "/rest/ping", method = RequestMethod.GET)
    public void ping(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Response res = createResponse();
        jaxbWriter.writeResponse(request, response, res);
    }

    @RequestMapping(value = "/rest/getMusicFolders", method = RequestMethod.GET)
    public void getMusicFolders(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);

        MusicFolders musicFolders = new MusicFolders();
        String username = securityService.getCurrentUsername(request);
        for (MusicFolder musicFolder : settingsService.getMusicFoldersForUser(username)) {
            org.libresonic.restapi.MusicFolder mf = new org.libresonic.restapi.MusicFolder();
            mf.setId(musicFolder.getId());
            mf.setName(musicFolder.getName());
            musicFolders.getMusicFolder().add(mf);
        }
        Response res = createResponse();
        res.setMusicFolders(musicFolders);
        jaxbWriter.writeResponse(request, response, res);
    }

    @RequestMapping(value = "/rest/getIndexes", method = RequestMethod.GET)
    public void getIndexes(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        Response res = createResponse();
        String username = securityService.getCurrentUser(request).getUsername();

        long ifModifiedSince = getLongParameter(request, "ifModifiedSince", 0L);
        long lastModified = leftController.getLastModified(request);

        if (lastModified <= ifModifiedSince) {
            jaxbWriter.writeResponse(request, response, res);
            return;
        }

        Indexes indexes = new Indexes();
        indexes.setLastModified(lastModified);
        indexes.setIgnoredArticles(settingsService.getIgnoredArticles());

        List<MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username);
        Integer musicFolderId = getIntParameter(request, "musicFolderId");
        if (musicFolderId != null) {
            for (MusicFolder musicFolder : musicFolders) {
                if (musicFolderId.equals(musicFolder.getId())) {
                    musicFolders = Collections.singletonList(musicFolder);
                    break;
                }
            }
        }

        for (MediaFile shortcut : musicIndexService.getShortcuts(musicFolders)) {
            indexes.getShortcut().add(createJaxbArtist(shortcut, username));
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
                        org.libresonic.restapi.Artist a = new org.libresonic.restapi.Artist();
                        index.getArtist().add(a);
                        a.setId(String.valueOf(mediaFile.getId()));
                        a.setName(artist.getName());
                        a.setStarred(jaxbWriter.convertDate(starredDate));

                        if (mediaFile.isAlbum()) {
                            a.setAverageRating(ratingService.getAverageRating(mediaFile));
                            a.setUserRating(ratingService.getRatingForUser(username, mediaFile));
                        }
                    }
                }
            }
        }

        // Add children
        Player player = playerService.getPlayer(request, response);

        for (MediaFile singleSong : musicFolderContent.getSingleSongs()) {
            indexes.getChild().add(createJaxbChild(player, singleSong, username));
        }

        res.setIndexes(indexes);
        jaxbWriter.writeResponse(request, response, res);
    }

    @RequestMapping(value = "/rest/getGenres", method = RequestMethod.GET)
    public void getGenres(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        Genres genres = new Genres();

        for (Genre genre : mediaFileDao.getGenres(false)) {
            org.libresonic.restapi.Genre g = new org.libresonic.restapi.Genre();
            genres.getGenre().add(g);
            g.setContent(genre.getName());
            g.setAlbumCount(genre.getAlbumCount());
            g.setSongCount(genre.getSongCount());
        }
        Response res = createResponse();
        res.setGenres(genres);
        jaxbWriter.writeResponse(request, response, res);
    }

    @RequestMapping(value = "/rest/getSongsByGenre", method = RequestMethod.GET)
    public void getSongsByGenre(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        Player player = playerService.getPlayer(request, response);
        String username = securityService.getCurrentUsername(request);

        Songs songs = new Songs();

        String genre = getRequiredStringParameter(request, "genre");
        int offset = getIntParameter(request, "offset", 0);
        int count = getIntParameter(request, "count", 10);
        count = Math.max(0, Math.min(count, 500));
        Integer musicFolderId = getIntParameter(request, "musicFolderId");
        List<MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username, musicFolderId);

        for (MediaFile mediaFile : mediaFileDao.getSongsByGenre(genre, offset, count, musicFolders)) {
            songs.getSong().add(createJaxbChild(player, mediaFile, username));
        }
        Response res = createResponse();
        res.setSongsByGenre(songs);
        jaxbWriter.writeResponse(request, response, res);
    }

    @RequestMapping(value = "/rest/getArtists", method = RequestMethod.GET)
    public void getArtists(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        String username = securityService.getCurrentUsername(request);

        ArtistsID3 result = new ArtistsID3();
        result.setIgnoredArticles(settingsService.getIgnoredArticles());
        List<MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username);

        List<Artist> artists = artistDao.getAlphabetialArtists(0, Integer.MAX_VALUE, musicFolders);
        SortedMap<MusicIndex, List<MusicIndex.SortableArtistWithArtist>> indexedArtists = musicIndexService.getIndexedArtists(artists);
        for (Map.Entry<MusicIndex, List<MusicIndex.SortableArtistWithArtist>> entry : indexedArtists.entrySet()) {
            IndexID3 index = new IndexID3();
            result.getIndex().add(index);
            index.setName(entry.getKey().getIndex());
            for (MusicIndex.SortableArtistWithArtist sortableArtist : entry.getValue()) {
                index.getArtist().add(createJaxbArtist(new ArtistID3(), sortableArtist.getArtist(), username));
            }
        }

        Response res = createResponse();
        res.setArtists(result);
        jaxbWriter.writeResponse(request, response, res);
    }

    @RequestMapping(value = "/rest/getSimilarSongs", method = RequestMethod.GET)
    public void getSimilarSongs(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        String username = securityService.getCurrentUsername(request);

        int id = getRequiredIntParameter(request, "id");
        int count = getIntParameter(request, "count", 50);

        SimilarSongs result = new SimilarSongs();

        MediaFile mediaFile = mediaFileService.getMediaFile(id);
        if (mediaFile == null) {
            error(request, response, ErrorCode.NOT_FOUND, "Media file not found.");
            return;
        }
        List<MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username);
        List<MediaFile> similarSongs = lastFmService.getSimilarSongs(mediaFile, count, musicFolders);
        Player player = playerService.getPlayer(request, response);
        for (MediaFile similarSong : similarSongs) {
            result.getSong().add(createJaxbChild(player, similarSong, username));
        }

        Response res = createResponse();
        res.setSimilarSongs(result);
        jaxbWriter.writeResponse(request, response, res);
    }

    @RequestMapping(value = "/rest/getSimilarSongs2", method = RequestMethod.GET)
    public void getSimilarSongs2(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        String username = securityService.getCurrentUsername(request);

        int id = getRequiredIntParameter(request, "id");
        int count = getIntParameter(request, "count", 50);

        SimilarSongs2 result = new SimilarSongs2();

        Artist artist = artistDao.getArtist(id);
        if (artist == null) {
            error(request, response, ErrorCode.NOT_FOUND, "Artist not found.");
            return;
        }

        List<MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username);
        List<MediaFile> similarSongs = lastFmService.getSimilarSongs(artist, count, musicFolders);
        Player player = playerService.getPlayer(request, response);
        for (MediaFile similarSong : similarSongs) {
            result.getSong().add(createJaxbChild(player, similarSong, username));
        }

        Response res = createResponse();
        res.setSimilarSongs2(result);
        jaxbWriter.writeResponse(request, response, res);
    }

    @RequestMapping(value = "/rest/getTopSongs", method = RequestMethod.GET)
    public void getTopSongs(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        String username = securityService.getCurrentUsername(request);

        String artist = getRequiredStringParameter(request, "artist");
        int count = getIntParameter(request, "count", 50);

        TopSongs result = new TopSongs();

        List<MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username);
        List<MediaFile> topSongs = lastFmService.getTopSongs(artist, count, musicFolders);
        Player player = playerService.getPlayer(request, response);
        for (MediaFile topSong : topSongs) {
            result.getSong().add(createJaxbChild(player, topSong, username));
        }

        Response res = createResponse();
        res.setTopSongs(result);
        jaxbWriter.writeResponse(request, response, res);
    }

    @RequestMapping(value = "/rest/getArtistInfo", method = RequestMethod.GET)
    public void getArtistInfo(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        String username = securityService.getCurrentUsername(request);

        int id = getRequiredIntParameter(request, "id");
        int count = getIntParameter(request, "count", 20);
        boolean includeNotPresent = ServletRequestUtils.getBooleanParameter(request, "includeNotPresent", false);

        ArtistInfo result = new ArtistInfo();

        MediaFile mediaFile = mediaFileService.getMediaFile(id);
        if (mediaFile == null) {
            error(request, response, ErrorCode.NOT_FOUND, "Media file not found.");
            return;
        }
        List<MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username);
        List<MediaFile> similarArtists = lastFmService.getSimilarArtists(mediaFile, count, includeNotPresent, musicFolders);
        for (MediaFile similarArtist : similarArtists) {
            result.getSimilarArtist().add(createJaxbArtist(similarArtist, username));
        }
        ArtistBio artistBio = lastFmService.getArtistBio(mediaFile);
        if (artistBio != null) {
            result.setBiography(artistBio.getBiography());
            result.setMusicBrainzId(artistBio.getMusicBrainzId());
            result.setLastFmUrl(artistBio.getLastFmUrl());
            result.setSmallImageUrl(artistBio.getSmallImageUrl());
            result.setMediumImageUrl(artistBio.getMediumImageUrl());
            result.setLargeImageUrl(artistBio.getLargeImageUrl());
        }

        Response res = createResponse();
        res.setArtistInfo(result);
        jaxbWriter.writeResponse(request, response, res);
    }

    @RequestMapping(value = "/rest/getArtistInfo2", method = RequestMethod.GET)
    public void getArtistInfo2(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        String username = securityService.getCurrentUsername(request);

        int id = getRequiredIntParameter(request, "id");
        int count = getIntParameter(request, "count", 20);
        boolean includeNotPresent = ServletRequestUtils.getBooleanParameter(request, "includeNotPresent", false);

        ArtistInfo2 result = new ArtistInfo2();

        Artist artist = artistDao.getArtist(id);
        if (artist == null) {
            error(request, response, ErrorCode.NOT_FOUND, "Artist not found.");
            return;
        }

        List<MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username);
        List<Artist> similarArtists = lastFmService.getSimilarArtists(artist, count, includeNotPresent, musicFolders);
        for (Artist similarArtist : similarArtists) {
            result.getSimilarArtist().add(createJaxbArtist(new ArtistID3(), similarArtist, username));
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

        Response res = createResponse();
        res.setArtistInfo2(result);
        jaxbWriter.writeResponse(request, response, res);
    }

    private <T extends ArtistID3> T createJaxbArtist(T jaxbArtist, Artist artist, String username) {
        jaxbArtist.setId(String.valueOf(artist.getId()));
        jaxbArtist.setName(artist.getName());
        jaxbArtist.setStarred(jaxbWriter.convertDate(mediaFileDao.getMediaFileStarredDate(artist.getId(), username)));
        jaxbArtist.setAlbumCount(artist.getAlbumCount());
        if (artist.getCoverArtPath() != null) {
            jaxbArtist.setCoverArt(CoverArtController.ARTIST_COVERART_PREFIX + artist.getId());
        }
        return jaxbArtist;
    }

    private org.libresonic.restapi.Artist createJaxbArtist(MediaFile artist, String username) {
        org.libresonic.restapi.Artist result = new org.libresonic.restapi.Artist();
        result.setId(String.valueOf(artist.getId()));
        result.setName(artist.getArtist());
        Date starred = mediaFileDao.getMediaFileStarredDate(artist.getId(), username);
        result.setStarred(jaxbWriter.convertDate(starred));
        return result;
    }

    @RequestMapping(value = "/rest/getArtist", method = RequestMethod.GET)
    public void getArtist(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);

        String username = securityService.getCurrentUsername(request);
        int id = getRequiredIntParameter(request, "id");
        Artist artist = artistDao.getArtist(id);
        if (artist == null) {
            error(request, response, ErrorCode.NOT_FOUND, "Artist not found.");
            return;
        }

        List<MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username);
        ArtistWithAlbumsID3 result = createJaxbArtist(new ArtistWithAlbumsID3(), artist, username);
        for (Album album : albumDao.getAlbumsForArtist(artist.getName(), musicFolders)) {
            result.getAlbum().add(createJaxbAlbum(new AlbumID3(), album, username));
        }

        Response res = createResponse();
        res.setArtist(result);
        jaxbWriter.writeResponse(request, response, res);
    }

    private <T extends AlbumID3> T createJaxbAlbum(T jaxbAlbum, Album album, String username) {
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
        jaxbAlbum.setCreated(jaxbWriter.convertDate(album.getCreated()));
        jaxbAlbum.setStarred(jaxbWriter.convertDate(albumDao.getAlbumStarredDate(album.getId(), username)));
        jaxbAlbum.setYear(album.getYear());
        jaxbAlbum.setGenre(album.getGenre());
        return jaxbAlbum;
    }

    private <T extends org.libresonic.restapi.Playlist> T createJaxbPlaylist(T jaxbPlaylist, Playlist playlist) {
        jaxbPlaylist.setId(String.valueOf(playlist.getId()));
        jaxbPlaylist.setName(playlist.getName());
        jaxbPlaylist.setComment(playlist.getComment());
        jaxbPlaylist.setOwner(playlist.getUsername());
        jaxbPlaylist.setPublic(playlist.isShared());
        jaxbPlaylist.setSongCount(playlist.getFileCount());
        jaxbPlaylist.setDuration(playlist.getDurationSeconds());
        jaxbPlaylist.setCreated(jaxbWriter.convertDate(playlist.getCreated()));
        jaxbPlaylist.setChanged(jaxbWriter.convertDate(playlist.getChanged()));
        jaxbPlaylist.setCoverArt(CoverArtController.PLAYLIST_COVERART_PREFIX + playlist.getId());

        for (String username : playlistService.getPlaylistUsers(playlist.getId())) {
            jaxbPlaylist.getAllowedUser().add(username);
        }
        return jaxbPlaylist;
    }

    @RequestMapping(value = "/rest/getAlbum", method = RequestMethod.GET)
    public void getAlbum(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        Player player = playerService.getPlayer(request, response);
        String username = securityService.getCurrentUsername(request);

        int id = getRequiredIntParameter(request, "id");
        Album album = albumDao.getAlbum(id);
        if (album == null) {
            error(request, response, ErrorCode.NOT_FOUND, "Album not found.");
            return;
        }

        AlbumWithSongsID3 result = createJaxbAlbum(new AlbumWithSongsID3(), album, username);
        for (MediaFile mediaFile : mediaFileDao.getSongsForAlbum(album.getArtist(), album.getName())) {
            result.getSong().add(createJaxbChild(player, mediaFile, username));
        }

        Response res = createResponse();
        res.setAlbum(result);
        jaxbWriter.writeResponse(request, response, res);
    }

    @RequestMapping(value = "/rest/getSong", method = RequestMethod.GET)
    public void getSong(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        Player player = playerService.getPlayer(request, response);
        String username = securityService.getCurrentUsername(request);

        int id = getRequiredIntParameter(request, "id");
        MediaFile song = mediaFileDao.getMediaFile(id);
        if (song == null || song.isDirectory()) {
            error(request, response, ErrorCode.NOT_FOUND, "Song not found.");
            return;
        }
        if (!securityService.isFolderAccessAllowed(song, username)) {
            error(request, response, ErrorCode.NOT_AUTHORIZED, "Access denied");
            return;
        }

        Response res = createResponse();
        res.setSong(createJaxbChild(player, song, username));
        jaxbWriter.writeResponse(request, response, res);
    }

    @RequestMapping(value = "/rest/getMusicDirectory", method = RequestMethod.GET)
    public void getMusicDirectory(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        Player player = playerService.getPlayer(request, response);
        String username = securityService.getCurrentUsername(request);

        int id = getRequiredIntParameter(request, "id");
        MediaFile dir = mediaFileService.getMediaFile(id);
        if (dir == null) {
            error(request, response, ErrorCode.NOT_FOUND, "Directory not found");
            return;
        }
        if (!securityService.isFolderAccessAllowed(dir, username)) {
            error(request, response, ErrorCode.NOT_AUTHORIZED, "Access denied");
            return;
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
        directory.setStarred(jaxbWriter.convertDate(mediaFileDao.getMediaFileStarredDate(id, username)));

        if (dir.isAlbum()) {
            directory.setAverageRating(ratingService.getAverageRating(dir));
            directory.setUserRating(ratingService.getRatingForUser(username, dir));
        }

        for (MediaFile child : mediaFileService.getChildrenOf(dir, true, true, true)) {
            directory.getChild().add(createJaxbChild(player, child, username));
        }

        Response res = createResponse();
        res.setDirectory(directory);
        jaxbWriter.writeResponse(request, response, res);
    }

    @RequestMapping(value = "/rest/search", method = RequestMethod.GET)
    public void search(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        Player player = playerService.getPlayer(request, response);
        String username = securityService.getCurrentUsername(request);

        String any = request.getParameter("any");
        String artist = request.getParameter("artist");
        String album = request.getParameter("album");
        String title = request.getParameter("title");

        StringBuilder query = new StringBuilder();
        if (any != null) {
            query.append(any).append(" ");
        }
        if (artist != null) {
            query.append(artist).append(" ");
        }
        if (album != null) {
            query.append(album).append(" ");
        }
        if (title != null) {
            query.append(title);
        }

        SearchCriteria criteria = new SearchCriteria();
        criteria.setQuery(query.toString().trim());
        criteria.setCount(getIntParameter(request, "count", 20));
        criteria.setOffset(getIntParameter(request, "offset", 0));
        List<MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username);

        SearchResult result = searchService.search(criteria, musicFolders, SearchService.IndexType.SONG);
        org.libresonic.restapi.SearchResult searchResult = new org.libresonic.restapi.SearchResult();
        searchResult.setOffset(result.getOffset());
        searchResult.setTotalHits(result.getTotalHits());

        for (MediaFile mediaFile : result.getMediaFiles()) {
            searchResult.getMatch().add(createJaxbChild(player, mediaFile, username));
        }
        Response res = createResponse();
        res.setSearchResult(searchResult);
        jaxbWriter.writeResponse(request, response, res);
    }

    @RequestMapping(value = "/rest/search2", method = RequestMethod.GET)
    public void search2(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        Player player = playerService.getPlayer(request, response);
        String username = securityService.getCurrentUsername(request);
        Integer musicFolderId = getIntParameter(request, "musicFolderId");
        List<MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username, musicFolderId);

        SearchResult2 searchResult = new SearchResult2();

        String query = request.getParameter("query");
        SearchCriteria criteria = new SearchCriteria();
        criteria.setQuery(StringUtils.trimToEmpty(query));
        criteria.setCount(getIntParameter(request, "artistCount", 20));
        criteria.setOffset(getIntParameter(request, "artistOffset", 0));
        SearchResult artists = searchService.search(criteria, musicFolders, SearchService.IndexType.ARTIST);
        for (MediaFile mediaFile : artists.getMediaFiles()) {
            searchResult.getArtist().add(createJaxbArtist(mediaFile, username));
        }

        criteria.setCount(getIntParameter(request, "albumCount", 20));
        criteria.setOffset(getIntParameter(request, "albumOffset", 0));
        SearchResult albums = searchService.search(criteria, musicFolders, SearchService.IndexType.ALBUM);
        for (MediaFile mediaFile : albums.getMediaFiles()) {
            searchResult.getAlbum().add(createJaxbChild(player, mediaFile, username));
        }

        criteria.setCount(getIntParameter(request, "songCount", 20));
        criteria.setOffset(getIntParameter(request, "songOffset", 0));
        SearchResult songs = searchService.search(criteria, musicFolders, SearchService.IndexType.SONG);
        for (MediaFile mediaFile : songs.getMediaFiles()) {
            searchResult.getSong().add(createJaxbChild(player, mediaFile, username));
        }

        Response res = createResponse();
        res.setSearchResult2(searchResult);
        jaxbWriter.writeResponse(request, response, res);
    }

    @RequestMapping(value = "/rest/search3", method = RequestMethod.GET)
    public void search3(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        Player player = playerService.getPlayer(request, response);
        String username = securityService.getCurrentUsername(request);
        Integer musicFolderId = getIntParameter(request, "musicFolderId");
        List<MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username, musicFolderId);

        SearchResult3 searchResult = new SearchResult3();

        String query = request.getParameter("query");
        SearchCriteria criteria = new SearchCriteria();
        criteria.setQuery(StringUtils.trimToEmpty(query));
        criteria.setCount(getIntParameter(request, "artistCount", 20));
        criteria.setOffset(getIntParameter(request, "artistOffset", 0));
        SearchResult result = searchService.search(criteria, musicFolders, SearchService.IndexType.ARTIST_ID3);
        for (Artist artist : result.getArtists()) {
            searchResult.getArtist().add(createJaxbArtist(new ArtistID3(), artist, username));
        }

        criteria.setCount(getIntParameter(request, "albumCount", 20));
        criteria.setOffset(getIntParameter(request, "albumOffset", 0));
        result = searchService.search(criteria, musicFolders, SearchService.IndexType.ALBUM_ID3);
        for (Album album : result.getAlbums()) {
            searchResult.getAlbum().add(createJaxbAlbum(new AlbumID3(), album, username));
        }

        criteria.setCount(getIntParameter(request, "songCount", 20));
        criteria.setOffset(getIntParameter(request, "songOffset", 0));
        result = searchService.search(criteria, musicFolders, SearchService.IndexType.SONG);
        for (MediaFile song : result.getMediaFiles()) {
            searchResult.getSong().add(createJaxbChild(player, song, username));
        }

        Response res = createResponse();
        res.setSearchResult3(searchResult);
        jaxbWriter.writeResponse(request, response, res);
    }

    @RequestMapping(value = "/rest/getPlaylists", method = RequestMethod.GET)
    public void getPlaylists(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);

        User user = securityService.getCurrentUser(request);
        String authenticatedUsername = user.getUsername();
        String requestedUsername = request.getParameter("username");

        if (requestedUsername == null) {
            requestedUsername = authenticatedUsername;
        } else if (!user.isAdminRole()) {
            error(request, response, ErrorCode.NOT_AUTHORIZED, authenticatedUsername + " is not authorized to get playlists for " + requestedUsername);
            return;
        }

        Playlists result = new Playlists();

        for (Playlist playlist : playlistService.getReadablePlaylistsForUser(requestedUsername)) {
            result.getPlaylist().add(createJaxbPlaylist(new org.libresonic.restapi.Playlist(), playlist));
        }

        Response res = createResponse();
        res.setPlaylists(result);
        jaxbWriter.writeResponse(request, response, res);
    }

    @RequestMapping(value = "/rest/getPlaylist", method = RequestMethod.GET)
    public void getPlaylist(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        Player player = playerService.getPlayer(request, response);
        String username = securityService.getCurrentUsername(request);

        int id = getRequiredIntParameter(request, "id");

        Playlist playlist = playlistService.getPlaylist(id);
        if (playlist == null) {
            error(request, response, ErrorCode.NOT_FOUND, "Playlist not found: " + id);
            return;
        }
        if (!playlistService.isReadAllowed(playlist, username)) {
            error(request, response, ErrorCode.NOT_AUTHORIZED, "Permission denied for playlist " + id);
            return;
        }
        PlaylistWithSongs result = createJaxbPlaylist(new PlaylistWithSongs(), playlist);
        for (MediaFile mediaFile : playlistService.getFilesInPlaylist(id)) {
            if (securityService.isFolderAccessAllowed(mediaFile, username)) {
                result.getEntry().add(createJaxbChild(player, mediaFile, username));
            }
        }

        Response res = createResponse();
        res.setPlaylist(result);
        jaxbWriter.writeResponse(request, response, res);
    }

    @RequestMapping(value = "/rest/jukeboxControl", method = RequestMethod.GET)
    public void jukeboxControl(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request, true);

        User user = securityService.getCurrentUser(request);
        if (!user.isJukeboxRole()) {
            error(request, response, ErrorCode.NOT_AUTHORIZED, user.getUsername() + " is not authorized to use jukebox.");
            return;
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
            throw new Exception("Unknown jukebox action: '" + action + "'.");
        }

        Player player = playerService.getPlayer(request, response);
        String username = securityService.getCurrentUsername(request);
        Player jukeboxPlayer = jukeboxService.getPlayer();
        boolean controlsJukebox = jukeboxPlayer != null && jukeboxPlayer.getId().equals(player.getId());
        PlayQueue playQueue = player.getPlayQueue();


        int currentIndex = controlsJukebox && !playQueue.isEmpty() ? playQueue.getIndex() : -1;
        boolean playing = controlsJukebox && !playQueue.isEmpty() && playQueue.getStatus() == PlayQueue.Status.PLAYING;
        float gain = jukeboxService.getGain();
        int position = controlsJukebox && !playQueue.isEmpty() ? jukeboxService.getPosition() : 0;

        Response res = createResponse();
        if (returnPlaylist) {
            JukeboxPlaylist result = new JukeboxPlaylist();
            res.setJukeboxPlaylist(result);
            result.setCurrentIndex(currentIndex);
            result.setPlaying(playing);
            result.setGain(gain);
            result.setPosition(position);
            for (MediaFile mediaFile : playQueue.getFiles()) {
                result.getEntry().add(createJaxbChild(player, mediaFile, username));
            }
        } else {
            JukeboxStatus result = new JukeboxStatus();
            res.setJukeboxStatus(result);
            result.setCurrentIndex(currentIndex);
            result.setPlaying(playing);
            result.setGain(gain);
            result.setPosition(position);
        }

        jaxbWriter.writeResponse(request, response, res);
    }

    @RequestMapping(value = "/rest/createPlaylist", method = RequestMethod.GET)
    public void createPlaylist(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request, true);
        String username = securityService.getCurrentUsername(request);

        Integer playlistId = getIntParameter(request, "playlistId");
        String name = request.getParameter("name");
        if (playlistId == null && name == null) {
            error(request, response, ErrorCode.MISSING_PARAMETER, "Playlist ID or name must be specified.");
            return;
        }

        Playlist playlist;
        if (playlistId != null) {
            playlist = playlistService.getPlaylist(playlistId);
            if (playlist == null) {
                error(request, response, ErrorCode.NOT_FOUND, "Playlist not found: " + playlistId);
                return;
            }
            if (!playlistService.isWriteAllowed(playlist, username)) {
                error(request, response, ErrorCode.NOT_AUTHORIZED, "Permission denied for playlist " + playlistId);
                return;
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

        writeEmptyResponse(request, response);
    }

    @RequestMapping(value = "/rest/updatePlaylist", method = RequestMethod.GET)
    public void updatePlaylist(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request, true);
        String username = securityService.getCurrentUsername(request);

        int id = getRequiredIntParameter(request, "playlistId");
        Playlist playlist = playlistService.getPlaylist(id);
        if (playlist == null) {
            error(request, response, ErrorCode.NOT_FOUND, "Playlist not found: " + id);
            return;
        }
        if (!playlistService.isWriteAllowed(playlist, username)) {
            error(request, response, ErrorCode.NOT_AUTHORIZED, "Permission denied for playlist " + id);
            return;
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

        writeEmptyResponse(request, response);
    }

    @RequestMapping(value = "/rest/deletePlaylist", method = RequestMethod.GET)
    public void deletePlaylist(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request, true);
        String username = securityService.getCurrentUsername(request);

        int id = getRequiredIntParameter(request, "id");
        Playlist playlist = playlistService.getPlaylist(id);
        if (playlist == null) {
            error(request, response, ErrorCode.NOT_FOUND, "Playlist not found: " + id);
            return;
        }
        if (!playlistService.isWriteAllowed(playlist, username)) {
            error(request, response, ErrorCode.NOT_AUTHORIZED, "Permission denied for playlist " + id);
            return;
        }
        playlistService.deletePlaylist(id);

        writeEmptyResponse(request, response);
    }

    @RequestMapping(value = "/rest/getAlbumList", method = RequestMethod.GET)
    public void getAlbumList(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        Player player = playerService.getPlayer(request, response);
        String username = securityService.getCurrentUsername(request);

        int size = getIntParameter(request, "size", 10);
        int offset = getIntParameter(request, "offset", 0);
        Integer musicFolderId = getIntParameter(request, "musicFolderId");

        List<MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username, musicFolderId);

        size = Math.max(0, Math.min(size, 500));
        String type = getRequiredStringParameter(request, "type");

        List<MediaFile> albums;
        if ("highest".equals(type)) {
            albums = ratingService.getHighestRatedAlbums(offset, size, musicFolders);
        } else if ("frequent".equals(type)) {
            albums = mediaFileService.getMostFrequentlyPlayedAlbums(offset, size, musicFolders);
        } else if ("recent".equals(type)) {
            albums = mediaFileService.getMostRecentlyPlayedAlbums(offset, size, musicFolders);
        } else if ("newest".equals(type)) {
            albums = mediaFileService.getNewestAlbums(offset, size, musicFolders);
        } else if ("starred".equals(type)) {
            albums = mediaFileService.getStarredAlbums(offset, size, username, musicFolders);
        } else if ("alphabeticalByArtist".equals(type)) {
            albums = mediaFileService.getAlphabeticalAlbums(offset, size, true, musicFolders);
        } else if ("alphabeticalByName".equals(type)) {
            albums = mediaFileService.getAlphabeticalAlbums(offset, size, false, musicFolders);
        } else if ("byGenre".equals(type)) {
            albums = mediaFileService.getAlbumsByGenre(offset, size, getRequiredStringParameter(request, "genre"), musicFolders);
        } else if ("byYear".equals(type)) {
            albums = mediaFileService.getAlbumsByYear(offset, size, getRequiredIntParameter(request, "fromYear"),
                    getRequiredIntParameter(request, "toYear"), musicFolders);
        } else if ("random".equals(type)) {
            albums = searchService.getRandomAlbums(size, musicFolders);
        } else {
            throw new Exception("Invalid list type: " + type);
        }

        AlbumList result = new AlbumList();
        for (MediaFile album : albums) {
            result.getAlbum().add(createJaxbChild(player, album, username));
        }

        Response res = createResponse();
        res.setAlbumList(result);
        jaxbWriter.writeResponse(request, response, res);
    }

    @RequestMapping(value = "/rest/getAlbumList2", method = RequestMethod.GET)
    public void getAlbumList2(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);

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
            albums = albumDao.getAlbumsByGenre(offset, size, getRequiredStringParameter(request, "genre"), musicFolders);
        } else if ("byYear".equals(type)) {
            albums = albumDao.getAlbumsByYear(offset, size, getRequiredIntParameter(request, "fromYear"),
                                              getRequiredIntParameter(request, "toYear"), musicFolders);
        } else if ("starred".equals(type)) {
            albums = albumDao.getStarredAlbums(offset, size, securityService.getCurrentUser(request).getUsername(), musicFolders);
        } else if ("random".equals(type)) {
            albums = searchService.getRandomAlbumsId3(size, musicFolders);
        } else {
            throw new Exception("Invalid list type: " + type);
        }
        AlbumList2 result = new AlbumList2();
        for (Album album : albums) {
            result.getAlbum().add(createJaxbAlbum(new AlbumID3(), album, username));
        }
        Response res = createResponse();
        res.setAlbumList2(result);
        jaxbWriter.writeResponse(request, response, res);
    }

    @RequestMapping(value = "/rest/getRandomSongs", method = RequestMethod.GET)
    public void getRandomSongs(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        Player player = playerService.getPlayer(request, response);
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
            result.getSong().add(createJaxbChild(player, mediaFile, username));
        }
        Response res = createResponse();
        res.setRandomSongs(result);
        jaxbWriter.writeResponse(request, response, res);
    }

    @RequestMapping(value = "/rest/getVideos", method = RequestMethod.GET)
    public void getVideos(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        Player player = playerService.getPlayer(request, response);
        String username = securityService.getCurrentUsername(request);

        int size = getIntParameter(request, "size", Integer.MAX_VALUE);
        int offset = getIntParameter(request, "offset", 0);
        List<MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username);

        Videos result = new Videos();
        for (MediaFile mediaFile : mediaFileDao.getVideos(size, offset, musicFolders)) {
            result.getVideo().add(createJaxbChild(player, mediaFile, username));
        }
        Response res = createResponse();
        res.setVideos(result);
        jaxbWriter.writeResponse(request, response, res);
    }

    @RequestMapping(value = "/rest/getNowPlaying", method = RequestMethod.GET)
    public void getNowPlaying(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
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
                result.getEntry().add(createJaxbChild(entry, player, mediaFile, username));
            }
        }

        Response res = createResponse();
        res.setNowPlaying(result);
        jaxbWriter.writeResponse(request, response, res);
    }

    private Child createJaxbChild(Player player, MediaFile mediaFile, String username) {
        return createJaxbChild(new Child(), player, mediaFile, username);
    }

    private <T extends Child> T createJaxbChild(T child, Player player, MediaFile mediaFile, String username) {
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
        child.setCreated(jaxbWriter.convertDate(mediaFile.getCreated()));
        child.setStarred(jaxbWriter.convertDate(mediaFileDao.getMediaFileStarredDate(mediaFile.getId(), username)));
        child.setUserRating(ratingService.getRatingForUser(username, mediaFile));
        child.setAverageRating(ratingService.getAverageRating(mediaFile));

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

            Bookmark bookmark = bookmarkCache.get(new BookmarkKey(username, mediaFile.getId()));
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

            if (transcodingService.isTranscodingRequired(mediaFile, player)) {
                String transcodedSuffix = transcodingService.getSuffix(player, mediaFile, null);
                child.setTranscodedSuffix(transcodedSuffix);
                child.setTranscodedContentType(StringUtil.getMimeType(transcodedSuffix));
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

    @RequestMapping(value = "/rest/download", method = RequestMethod.GET)
    public ModelAndView download(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        User user = securityService.getCurrentUser(request);
        if (!user.isDownloadRole()) {
            error(request, response, ErrorCode.NOT_AUTHORIZED, user.getUsername() + " is not authorized to download files.");
            return null;
        }

        long ifModifiedSince = request.getDateHeader("If-Modified-Since");
        long lastModified = downloadController.getLastModified(request);

        if (ifModifiedSince != -1 && lastModified != -1 && lastModified <= ifModifiedSince) {
            response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
            return null;
        }

        if (lastModified != -1) {
            response.setDateHeader("Last-Modified", lastModified);
        }

        return downloadController.handleRequest(request, response);
    }

    @RequestMapping(value = "/rest/stream", method = RequestMethod.GET)
    public ModelAndView stream(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        User user = securityService.getCurrentUser(request);
        if (!user.isStreamRole()) {
            error(request, response, ErrorCode.NOT_AUTHORIZED, user.getUsername() + " is not authorized to play files.");
            return null;
        }

        streamController.handleRequest(request, response);
        return null;
    }

    @RequestMapping(value = "/rest/hls", method = RequestMethod.GET)
    public ModelAndView hls(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        User user = securityService.getCurrentUser(request);
        if (!user.isStreamRole()) {
            error(request, response, ErrorCode.NOT_AUTHORIZED, user.getUsername() + " is not authorized to play files.");
            return null;
        }
        int id = getRequiredIntParameter(request, "id");
        MediaFile video = mediaFileDao.getMediaFile(id);
        if (video == null || video.isDirectory()) {
            error(request, response, ErrorCode.NOT_FOUND, "Video not found.");
            return null;
        }
        if (!securityService.isFolderAccessAllowed(video, user.getUsername())) {
            error(request, response, ErrorCode.NOT_AUTHORIZED, "Access denied");
            return null;
        }
        hlsController.handleRequest(request, response);
        return null;
    }

    @RequestMapping(value = "/rest/scrobble", method = RequestMethod.GET)
    public void scrobble(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);

        Player player = playerService.getPlayer(request, response);

        boolean submission = getBooleanParameter(request, "submission", true);
        int[] ids = getRequiredIntParameters(request, "id");
        long[] times = getLongParameters(request, "time");
        if (times.length > 0 && times.length != ids.length) {
            error(request, response, ErrorCode.GENERIC, "Wrong number of timestamps: " + times.length);
            return;
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

        writeEmptyResponse(request, response);
    }

    @RequestMapping(value = "/rest/star", method = RequestMethod.GET)
    public void star(HttpServletRequest request, HttpServletResponse response) throws Exception {
        starOrUnstar(request, response, true);
    }

    @RequestMapping(value = "/rest/unstar", method = RequestMethod.GET)
    public void unstar(HttpServletRequest request, HttpServletResponse response) throws Exception {
        starOrUnstar(request, response, false);
    }

    private void starOrUnstar(HttpServletRequest request, HttpServletResponse response, boolean star) throws Exception {
        request = wrapRequest(request);

        String username = securityService.getCurrentUser(request).getUsername();
        for (int id : getIntParameters(request, "id")) {
            MediaFile mediaFile = mediaFileDao.getMediaFile(id);
            if (mediaFile == null) {
                error(request, response, ErrorCode.NOT_FOUND, "Media file not found: " + id);
                return;
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
                error(request, response, ErrorCode.NOT_FOUND, "Album not found: " + albumId);
                return;
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
                error(request, response, ErrorCode.NOT_FOUND, "Artist not found: " + artistId);
                return;
            }
            if (star) {
                artistDao.starArtist(artistId, username);
            } else {
                artistDao.unstarArtist(artistId, username);
            }
        }

        writeEmptyResponse(request, response);
    }

    @RequestMapping(value = "/rest/getStarred", method = RequestMethod.GET)
    public void getStarred(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        Player player = playerService.getPlayer(request, response);
        String username = securityService.getCurrentUsername(request);
        Integer musicFolderId = getIntParameter(request, "musicFolderId");
        List<MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username, musicFolderId);

        Starred result = new Starred();
        for (MediaFile artist : mediaFileDao.getStarredDirectories(0, Integer.MAX_VALUE, username, musicFolders)) {
            result.getArtist().add(createJaxbArtist(artist, username));
        }
        for (MediaFile album : mediaFileDao.getStarredAlbums(0, Integer.MAX_VALUE, username, musicFolders)) {
            result.getAlbum().add(createJaxbChild(player, album, username));
        }
        for (MediaFile song : mediaFileDao.getStarredFiles(0, Integer.MAX_VALUE, username, musicFolders)) {
            result.getSong().add(createJaxbChild(player, song, username));
        }
        Response res = createResponse();
        res.setStarred(result);
        jaxbWriter.writeResponse(request, response, res);
    }

    @RequestMapping(value = "/rest/getStarred2", method = RequestMethod.GET)
    public void getStarred2(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        Player player = playerService.getPlayer(request, response);
        String username = securityService.getCurrentUsername(request);
        Integer musicFolderId = getIntParameter(request, "musicFolderId");
        List<MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username, musicFolderId);

        Starred2 result = new Starred2();
        for (Artist artist : artistDao.getStarredArtists(0, Integer.MAX_VALUE, username, musicFolders)) {
            result.getArtist().add(createJaxbArtist(new ArtistID3(), artist, username));
        }
        for (Album album : albumDao.getStarredAlbums(0, Integer.MAX_VALUE, username, musicFolders)) {
            result.getAlbum().add(createJaxbAlbum(new AlbumID3(), album, username));
        }
        for (MediaFile song : mediaFileDao.getStarredFiles(0, Integer.MAX_VALUE, username, musicFolders)) {
            result.getSong().add(createJaxbChild(player, song, username));
        }
        Response res = createResponse();
        res.setStarred2(result);
        jaxbWriter.writeResponse(request, response, res);
    }

    @RequestMapping(value = "/rest/getPodcasts", method = RequestMethod.GET)
    public void getPodcasts(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        Player player = playerService.getPlayer(request, response);
        String username = securityService.getCurrentUsername(request);
        boolean includeEpisodes = getBooleanParameter(request, "includeEpisodes", true);
        Integer channelId = getIntParameter(request, "id");

        Podcasts result = new Podcasts();

        for (PodcastChannel channel : podcastService.getAllChannels()) {
            if (channelId == null || channelId.equals(channel.getId())) {

                org.libresonic.restapi.PodcastChannel c = new org.libresonic.restapi.PodcastChannel();
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
                        c.getEpisode().add(createJaxbPodcastEpisode(player, username, episode));
                    }
                }
            }
        }
        Response res = createResponse();
        res.setPodcasts(result);
        jaxbWriter.writeResponse(request, response, res);
    }

    @RequestMapping(value = "/rest/getNewestPodcasts", method = RequestMethod.GET)
    public void getNewestPodcasts(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        Player player = playerService.getPlayer(request, response);
        String username = securityService.getCurrentUsername(request);

        int count = getIntParameter(request, "count", 20);
        NewestPodcasts result = new NewestPodcasts();

        for (PodcastEpisode episode : podcastService.getNewestEpisodes(count)) {
            result.getEpisode().add(createJaxbPodcastEpisode(player, username, episode));
        }

        Response res = createResponse();
        res.setNewestPodcasts(result);
        jaxbWriter.writeResponse(request, response, res);
    }

    private org.libresonic.restapi.PodcastEpisode createJaxbPodcastEpisode(Player player, String username, PodcastEpisode episode) {
        org.libresonic.restapi.PodcastEpisode e = new org.libresonic.restapi.PodcastEpisode();

        String path = episode.getPath();
        if (path != null) {
            MediaFile mediaFile = mediaFileService.getMediaFile(path);
            e = createJaxbChild(new org.libresonic.restapi.PodcastEpisode(), player, mediaFile, username);
            e.setStreamId(String.valueOf(mediaFile.getId()));
        }

        e.setId(String.valueOf(episode.getId()));  // Overwrites the previous "id" attribute.
        e.setChannelId(String.valueOf(episode.getChannelId()));
        e.setStatus(PodcastStatus.valueOf(episode.getStatus().name()));
        e.setTitle(episode.getTitle());
        e.setDescription(episode.getDescription());
        e.setPublishDate(jaxbWriter.convertDate(episode.getPublishDate()));
        return e;
    }

    @RequestMapping(value = "/rest/refreshPodcasts", method = RequestMethod.GET)
    public void refreshPodcasts(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        User user = securityService.getCurrentUser(request);
        if (!user.isPodcastRole()) {
            error(request, response, ErrorCode.NOT_AUTHORIZED, user.getUsername() + " is not authorized to administrate podcasts.");
            return;
        }
        podcastService.refreshAllChannels(true);
        writeEmptyResponse(request, response);
    }

    @RequestMapping(value = "/rest/createPodcastChannel", method = RequestMethod.GET)
    public void createPodcastChannel(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        User user = securityService.getCurrentUser(request);
        if (!user.isPodcastRole()) {
            error(request, response, ErrorCode.NOT_AUTHORIZED, user.getUsername() + " is not authorized to administrate podcasts.");
            return;
        }

        String url = getRequiredStringParameter(request, "url");
        podcastService.createChannel(url);
        writeEmptyResponse(request, response);
    }

    @RequestMapping(value = "/rest/deletePodcastChannel", method = RequestMethod.GET)
    public void deletePodcastChannel(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        User user = securityService.getCurrentUser(request);
        if (!user.isPodcastRole()) {
            error(request, response, ErrorCode.NOT_AUTHORIZED, user.getUsername() + " is not authorized to administrate podcasts.");
            return;
        }

        int id = getRequiredIntParameter(request, "id");
        podcastService.deleteChannel(id);
        writeEmptyResponse(request, response);
    }

    @RequestMapping(value = "/rest/deletePodcastEpisode", method = RequestMethod.GET)
    public void deletePodcastEpisode(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        User user = securityService.getCurrentUser(request);
        if (!user.isPodcastRole()) {
            error(request, response, ErrorCode.NOT_AUTHORIZED, user.getUsername() + " is not authorized to administrate podcasts.");
            return;
        }

        int id = getRequiredIntParameter(request, "id");
        podcastService.deleteEpisode(id, true);
        writeEmptyResponse(request, response);
    }

    @RequestMapping(value = "/rest/downloadPodcastEpisode", method = RequestMethod.GET)
    public void downloadPodcastEpisode(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        User user = securityService.getCurrentUser(request);
        if (!user.isPodcastRole()) {
            error(request, response, ErrorCode.NOT_AUTHORIZED, user.getUsername() + " is not authorized to administrate podcasts.");
            return;
        }

        int id = getRequiredIntParameter(request, "id");
        PodcastEpisode episode = podcastService.getEpisode(id, true);
        if (episode == null) {
            error(request, response, ErrorCode.NOT_FOUND, "Podcast episode " + id + " not found.");
            return;
        }

        podcastService.downloadEpisode(episode);
        writeEmptyResponse(request, response);
    }

    @RequestMapping(value = "/rest/getInternetRadioStations", method = RequestMethod.GET)
    public void getInternetRadioStations(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);

        InternetRadioStations result = new InternetRadioStations();
        for (InternetRadio radio : settingsService.getAllInternetRadios()) {
            InternetRadioStation i = new InternetRadioStation();
            i.setId(String.valueOf(radio.getId()));
            i.setName(radio.getName());
            i.setStreamUrl(radio.getStreamUrl());
            i.setHomePageUrl(radio.getHomepageUrl());
            result.getInternetRadioStation().add(i);
        }
        Response res = createResponse();
        res.setInternetRadioStations(result);
        jaxbWriter.writeResponse(request, response, res);
    }

    @RequestMapping(value = "/rest/getBookmarks", method = RequestMethod.GET)
    public void getBookmarks(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        Player player = playerService.getPlayer(request, response);
        String username = securityService.getCurrentUsername(request);

        Bookmarks result = new Bookmarks();
        for (Bookmark bookmark : bookmarkDao.getBookmarks(username)) {
            org.libresonic.restapi.Bookmark b = new org.libresonic.restapi.Bookmark();
            result.getBookmark().add(b);
            b.setPosition(bookmark.getPositionMillis());
            b.setUsername(bookmark.getUsername());
            b.setComment(bookmark.getComment());
            b.setCreated(jaxbWriter.convertDate(bookmark.getCreated()));
            b.setChanged(jaxbWriter.convertDate(bookmark.getChanged()));

            MediaFile mediaFile = mediaFileService.getMediaFile(bookmark.getMediaFileId());
            b.setEntry(createJaxbChild(player, mediaFile, username));
        }

        Response res = createResponse();
        res.setBookmarks(result);
        jaxbWriter.writeResponse(request, response, res);
    }

    @RequestMapping(value = "/rest/createBookmark", method = RequestMethod.GET)
    public void createBookmark(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        String username = securityService.getCurrentUsername(request);
        int mediaFileId = getRequiredIntParameter(request, "id");
        long position = getRequiredLongParameter(request, "position");
        String comment = request.getParameter("comment");
        Date now = new Date();

        Bookmark bookmark = new Bookmark(0, mediaFileId, position, username, comment, now, now);
        bookmarkDao.createOrUpdateBookmark(bookmark);
        refreshBookmarkCache();
        writeEmptyResponse(request, response);
    }

    @RequestMapping(value = "/rest/deleteBookmark", method = RequestMethod.GET)
    public void deleteBookmark(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);

        String username = securityService.getCurrentUsername(request);
        int mediaFileId = getRequiredIntParameter(request, "id");
        bookmarkDao.deleteBookmark(username, mediaFileId);
        refreshBookmarkCache();

        writeEmptyResponse(request, response);
    }

    @RequestMapping(value = "/rest/getPlayQueue", method = RequestMethod.GET)
    public void getPlayQueue(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        String username = securityService.getCurrentUsername(request);
        Player player = playerService.getPlayer(request, response);

        SavedPlayQueue playQueue = playQueueDao.getPlayQueue(username);
        if (playQueue == null) {
            writeEmptyResponse(request, response);
            return;
        }

        org.libresonic.restapi.PlayQueue restPlayQueue = new org.libresonic.restapi.PlayQueue();
        restPlayQueue.setUsername(playQueue.getUsername());
        restPlayQueue.setCurrent(playQueue.getCurrentMediaFileId());
        restPlayQueue.setPosition(playQueue.getPositionMillis());
        restPlayQueue.setChanged(jaxbWriter.convertDate(playQueue.getChanged()));
        restPlayQueue.setChangedBy(playQueue.getChangedBy());

        for (Integer mediaFileId : playQueue.getMediaFileIds()) {
            MediaFile mediaFile = mediaFileService.getMediaFile(mediaFileId);
            if (mediaFile != null) {
                restPlayQueue.getEntry().add(createJaxbChild(player, mediaFile, username));
            }
        }

        Response res = createResponse();
        res.setPlayQueue(restPlayQueue);
        jaxbWriter.writeResponse(request, response, res);
    }

    @RequestMapping(value = "/rest/savePlayQueue", method = RequestMethod.GET)
    public void savePlayQueue(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        String username = securityService.getCurrentUsername(request);
        List<Integer> mediaFileIds = Util.toIntegerList(getIntParameters(request, "id"));
        Integer current = getIntParameter(request, "current");
        Long position = getLongParameter(request, "position");
        Date changed = new Date();
        String changedBy = getRequiredStringParameter(request, "c");

        if (!mediaFileIds.contains(current)) {
            error(request, response, ErrorCode.GENERIC, "Current track is not included in play queue");
            return;
        }

        SavedPlayQueue playQueue = new SavedPlayQueue(null, username, mediaFileIds, current, position, changed, changedBy);
        playQueueDao.savePlayQueue(playQueue);
        writeEmptyResponse(request, response);
    }

    @RequestMapping(value = "/rest/getShares", method = RequestMethod.GET)
    public void getShares(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        Player player = playerService.getPlayer(request, response);
        String username = securityService.getCurrentUsername(request);
        User user = securityService.getCurrentUser(request);
        List<MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username);

        Shares result = new Shares();
        for (Share share : shareService.getSharesForUser(user)) {
            org.libresonic.restapi.Share s = createJaxbShare(share);
            result.getShare().add(s);

            for (MediaFile mediaFile : shareService.getSharedFiles(share.getId(), musicFolders)) {
                s.getEntry().add(createJaxbChild(player, mediaFile, username));
            }
        }
        Response res = createResponse();
        res.setShares(result);
        jaxbWriter.writeResponse(request, response, res);
    }

    @RequestMapping(value = "/rest/createShare", method = RequestMethod.GET)
    public void createShare(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        Player player = playerService.getPlayer(request, response);
        String username = securityService.getCurrentUsername(request);

        User user = securityService.getCurrentUser(request);
        if (!user.isShareRole()) {
            error(request, response, ErrorCode.NOT_AUTHORIZED, user.getUsername() + " is not authorized to share media.");
            return;
        }

        if (!settingsService.isUrlRedirectionEnabled()) {
            error(request, response, ErrorCode.GENERIC, "Sharing is only supported for *.libresonic.org domain names.");
            return;
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
        org.libresonic.restapi.Share s = createJaxbShare(share);
        result.getShare().add(s);

        List<MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username);

        for (MediaFile mediaFile : shareService.getSharedFiles(share.getId(), musicFolders)) {
            s.getEntry().add(createJaxbChild(player, mediaFile, username));
        }

        Response res = createResponse();
        res.setShares(result);
        jaxbWriter.writeResponse(request, response, res);
    }

    @RequestMapping(value = "/rest/deleteShare", method = RequestMethod.GET)
    public void deleteShare(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        User user = securityService.getCurrentUser(request);
        int id = getRequiredIntParameter(request, "id");

        Share share = shareService.getShareById(id);
        if (share == null) {
            error(request, response, ErrorCode.NOT_FOUND, "Shared media not found.");
            return;
        }
        if (!user.isAdminRole() && !share.getUsername().equals(user.getUsername())) {
            error(request, response, ErrorCode.NOT_AUTHORIZED, "Not authorized to delete shared media.");
            return;
        }

        shareService.deleteShare(id);
        writeEmptyResponse(request, response);
    }

    @RequestMapping(value = "/rest/updateShare", method = RequestMethod.GET)
    public void updateShare(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        User user = securityService.getCurrentUser(request);
        int id = getRequiredIntParameter(request, "id");

        Share share = shareService.getShareById(id);
        if (share == null) {
            error(request, response, ErrorCode.NOT_FOUND, "Shared media not found.");
            return;
        }
        if (!user.isAdminRole() && !share.getUsername().equals(user.getUsername())) {
            error(request, response, ErrorCode.NOT_AUTHORIZED, "Not authorized to modify shared media.");
            return;
        }

        share.setDescription(request.getParameter("description"));
        String expiresString = request.getParameter("expires");
        if (expiresString != null) {
            long expires = Long.parseLong(expiresString);
            share.setExpires(expires == 0L ? null : new Date(expires));
        }
        shareService.updateShare(share);
        writeEmptyResponse(request, response);
    }

    private org.libresonic.restapi.Share createJaxbShare(Share share) {
        org.libresonic.restapi.Share result = new org.libresonic.restapi.Share();
        result.setId(String.valueOf(share.getId()));
        result.setUrl(shareService.getShareUrl(share));
        result.setUsername(share.getUsername());
        result.setCreated(jaxbWriter.convertDate(share.getCreated()));
        result.setVisitCount(share.getVisitCount());
        result.setDescription(share.getDescription());
        result.setExpires(jaxbWriter.convertDate(share.getExpires()));
        result.setLastVisited(jaxbWriter.convertDate(share.getLastVisited()));
        return result;
    }

    @SuppressWarnings("UnusedParameters")
    public ModelAndView videoPlayer(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);

        Map<String, Object> map = new HashMap<String, Object>();
        int id = getRequiredIntParameter(request, "id");
        MediaFile file = mediaFileService.getMediaFile(id);

        int timeOffset = getIntParameter(request, "timeOffset", 0);
        timeOffset = Math.max(0, timeOffset);
        Integer duration = file.getDurationSeconds();
        if (duration != null) {
            map.put("skipOffsets", VideoPlayerController.createSkipOffsets(duration));
            timeOffset = Math.min(duration, timeOffset);
            duration -= timeOffset;
        }

        map.put("id", request.getParameter("id"));
        map.put("u", request.getParameter("u"));
        map.put("p", request.getParameter("p"));
        map.put("c", request.getParameter("c"));
        map.put("v", request.getParameter("v"));
        map.put("video", file);
        map.put("maxBitRate", getIntParameter(request, "maxBitRate", VideoPlayerController.DEFAULT_BIT_RATE));
        map.put("duration", duration);
        map.put("timeOffset", timeOffset);
        map.put("bitRates", VideoPlayerController.BIT_RATES);
        map.put("autoplay", getBooleanParameter(request, "autoplay", true));

        ModelAndView result = new ModelAndView("rest/videoPlayer");
        result.addObject("model", map);
        return result;
    }

    @RequestMapping(value = "/rest/getCoverArt", method = RequestMethod.GET)
    public ModelAndView getCoverArt(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        return coverArtController.handleRequest(request, response);
    }

    @RequestMapping(value = "/rest/getAvatar", method = RequestMethod.GET)
    public ModelAndView getAvatar(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        return avatarController.handleRequest(request, response);
    }

    @RequestMapping(value = "/rest/changePassword", method = RequestMethod.GET)
    public void changePassword(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);

        String username = getRequiredStringParameter(request, "username");
        String password = decrypt(getRequiredStringParameter(request, "password"));

        User authUser = securityService.getCurrentUser(request);

        boolean allowed = authUser.isAdminRole()
                || username.equals(authUser.getUsername()) && authUser.isSettingsRole();

        if (!allowed) {
            error(request, response, ErrorCode.NOT_AUTHORIZED, authUser.getUsername() + " is not authorized to change password for " + username);
            return;
        }

        User user = securityService.getUserByName(username);
        user.setPassword(password);
        securityService.updateUser(user);

        writeEmptyResponse(request, response);
    }

    @RequestMapping(value = "/rest/getUser", method = RequestMethod.GET)
    public void getUser(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);

        String username = getRequiredStringParameter(request, "username");

        User currentUser = securityService.getCurrentUser(request);
        if (!username.equals(currentUser.getUsername()) && !currentUser.isAdminRole()) {
            error(request, response, ErrorCode.NOT_AUTHORIZED, currentUser.getUsername() + " is not authorized to get details for other users.");
            return;
        }

        User requestedUser = securityService.getUserByName(username);
        if (requestedUser == null) {
            error(request, response, ErrorCode.NOT_FOUND, "No such user: " + username);
            return;
        }

        Response res = createResponse();
        res.setUser(createJaxbUser(requestedUser));
        jaxbWriter.writeResponse(request, response, res);
    }

    @RequestMapping(value = "/rest/getUsers", method = RequestMethod.GET)
    public void getUsers(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);

        User currentUser = securityService.getCurrentUser(request);
        if (!currentUser.isAdminRole()) {
            error(request, response, ErrorCode.NOT_AUTHORIZED, currentUser.getUsername() + " is not authorized to get details for other users.");
            return;
        }

        Users result = new Users();
        for (User user : securityService.getAllUsers()) {
            result.getUser().add(createJaxbUser(user));
        }

        Response res = createResponse();
        res.setUsers(result);
        jaxbWriter.writeResponse(request, response, res);
    }

    private org.libresonic.restapi.User createJaxbUser(User user) {
        UserSettings userSettings = settingsService.getUserSettings(user.getUsername());

        org.libresonic.restapi.User result = new org.libresonic.restapi.User();
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

    @RequestMapping(value = "/rest/createUser", method = RequestMethod.GET)
    public void createUser(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        User user = securityService.getCurrentUser(request);
        if (!user.isAdminRole()) {
            error(request, response, ErrorCode.NOT_AUTHORIZED, user.getUsername() + " is not authorized to create new users.");
            return;
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
        writeEmptyResponse(request, response);
    }

    @RequestMapping(value = "/rest/updateUser", method = RequestMethod.GET)
    public void updateUser(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        User user = securityService.getCurrentUser(request);
        if (!user.isAdminRole()) {
            error(request, response, ErrorCode.NOT_AUTHORIZED, user.getUsername() + " is not authorized to update users.");
            return;
        }

        String username = getRequiredStringParameter(request, "username");
        User u = securityService.getUserByName(username);
        UserSettings s = settingsService.getUserSettings(username);

        if (u == null) {
            error(request, response, ErrorCode.NOT_FOUND, "No such user: " + username);
            return;
        } else if (User.USERNAME_ADMIN.equals(username)) {
            error(request, response, ErrorCode.NOT_AUTHORIZED, "Not allowed to change admin user");
            return;
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
        writeEmptyResponse(request, response);
    }

    private boolean hasParameter(HttpServletRequest request, String name) {
        return request.getParameter(name) != null;
    }

    @RequestMapping(value = "/rest/deleteUser", method = RequestMethod.GET)
    public void deleteUser(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        User user = securityService.getCurrentUser(request);
        if (!user.isAdminRole()) {
            error(request, response, ErrorCode.NOT_AUTHORIZED, user.getUsername() + " is not authorized to delete users.");
            return;
        }

        String username = getRequiredStringParameter(request, "username");
        if (User.USERNAME_ADMIN.equals(username)) {
            error(request, response, ErrorCode.NOT_AUTHORIZED, "Not allowed to delete admin user");
            return;
        }

        securityService.deleteUser(username);

        writeEmptyResponse(request, response);
    }

    @RequestMapping(value = "/rest/getChatMessages", method = RequestMethod.GET)
    public void getChatMessages(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        long since = getLongParameter(request, "since", 0L);

        ChatMessages result = new ChatMessages();
        for (ChatService.Message message : chatService.getMessages(0L).getMessages()) {
            long time = message.getDate().getTime();
            if (time > since) {
                ChatMessage c = new ChatMessage();
                result.getChatMessage().add(c);
                c.setUsername(message.getUsername());
                c.setTime(time);
                c.setMessage(message.getContent());
            }
        }
        Response res = createResponse();
        res.setChatMessages(result);
        jaxbWriter.writeResponse(request, response, res);
    }

    @RequestMapping(value = "/rest/addChatMessage", method = RequestMethod.GET)
    public void addChatMessage(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        chatService.doAddMessage(getRequiredStringParameter(request, "message"), request);
        writeEmptyResponse(request, response);
    }

    @RequestMapping(value = "/rest/getLyrics", method = RequestMethod.GET)
    public void getLyrics(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        String artist = request.getParameter("artist");
        String title = request.getParameter("title");
        LyricsInfo lyrics = lyricsService.getLyrics(artist, title);

        Lyrics result = new Lyrics();
        result.setArtist(lyrics.getArtist());
        result.setTitle(lyrics.getTitle());
        result.setContent(lyrics.getLyrics());

        Response res = createResponse();
        res.setLyrics(result);
        jaxbWriter.writeResponse(request, response, res);
    }

    @RequestMapping(value = "/rest/setRating", method = RequestMethod.GET)
    public void setRating(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        Integer rating = getRequiredIntParameter(request, "rating");
        if (rating == 0) {
            rating = null;
        }

        int id = getRequiredIntParameter(request, "id");
        MediaFile mediaFile = mediaFileService.getMediaFile(id);
        if (mediaFile == null) {
            error(request, response, ErrorCode.NOT_FOUND, "File not found: " + id);
            return;
        }

        String username = securityService.getCurrentUsername(request);
        ratingService.setRatingForUser(username, mediaFile, rating);

        writeEmptyResponse(request, response);
    }

    private HttpServletRequest wrapRequest(HttpServletRequest request) {
        return wrapRequest(request, false);
    }

    private HttpServletRequest wrapRequest(final HttpServletRequest request, boolean jukebox) {
        final String playerId = createPlayerIfNecessary(request, jukebox);
        return new HttpServletRequestWrapper(request) {
            @Override
            public String getParameter(String name) {
                // Returns the correct player to be used in PlayerService.getPlayer()
                if ("player".equals(name)) {
                    return playerId;
                }

                // Support old style ID parameters.
                if ("id".equals(name)) {
                    return mapId(request.getParameter("id"));
                }

                return super.getParameter(name);
            }
        };
    }

    private String mapId(String id) {
        if (id == null || id.startsWith(CoverArtController.ALBUM_COVERART_PREFIX) ||
                id.startsWith(CoverArtController.ARTIST_COVERART_PREFIX) || StringUtils.isNumeric(id)) {
            return id;
        }

        try {
            String path = StringUtil.utf8HexDecode(id);
            MediaFile mediaFile = mediaFileService.getMediaFile(path);
            return String.valueOf(mediaFile.getId());
        } catch (Exception x) {
            return id;
        }
    }

    private Response createResponse() {
        return jaxbWriter.createResponse(true);
    }

    private void writeEmptyResponse(HttpServletRequest request, HttpServletResponse response) throws Exception {
        jaxbWriter.writeResponse(request, response, createResponse());
    }

    public void error(HttpServletRequest request, HttpServletResponse response, ErrorCode code, String message) throws Exception {
        jaxbWriter.writeErrorResponse(request, response, code, message);
    }

    private String createPlayerIfNecessary(HttpServletRequest request, boolean jukebox) {
        String username = request.getRemoteUser();
        String clientId = request.getParameter("c");
        if (jukebox) {
            clientId += "-jukebox";
        }

        List<Player> players = playerService.getPlayersForUserAndClientId(username, clientId);

        // If not found, create it.
        if (players.isEmpty()) {
            Player player = new Player();
            player.setIpAddress(request.getRemoteAddr());
            player.setUsername(username);
            player.setClientId(clientId);
            player.setName(clientId);
            player.setTechnology(jukebox ? PlayerTechnology.JUKEBOX : PlayerTechnology.EXTERNAL_WITH_PLAYLIST);
            playerService.createPlayer(player);
            players = playerService.getPlayersForUserAndClientId(username, clientId);
        }

        // Return the player ID.
        return !players.isEmpty() ? players.get(0).getId() : null;
    }


    public enum ErrorCode {

        GENERIC(0, "A generic error."),
        MISSING_PARAMETER(10, "Required parameter is missing."),
        PROTOCOL_MISMATCH_CLIENT_TOO_OLD(20, "Incompatible Libresonic REST protocol version. Client must upgrade."),
        PROTOCOL_MISMATCH_SERVER_TOO_OLD(30, "Incompatible Libresonic REST protocol version. Server must upgrade."),
        NOT_AUTHENTICATED(40, "Wrong username or password."),
        NOT_AUTHORIZED(50, "User is not authorized for the given operation."),
        NOT_FOUND(70, "Requested data was not found.");

        private final int code;
        private final String message;

        ErrorCode(int code, String message) {
            this.code = code;
            this.message = message;
        }

        public int getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }
    }

    private static class BookmarkKey extends Pair<String, Integer> {
        private BookmarkKey(String username, int mediaFileId) {
            super(username, mediaFileId);
        }

        static BookmarkKey forBookmark(Bookmark b) {
            return new BookmarkKey(b.getUsername(), b.getMediaFileId());
        }
    }
}
