/*
 This file is part of Airsonic.

 Airsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Airsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2016 (C) Airsonic Authors
 Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
 */
package org.airsonic.player.controller;

import org.airsonic.player.ajax.LyricsInfo;
import org.airsonic.player.ajax.LyricsService;
import org.airsonic.player.ajax.PlayQueueService;
import org.airsonic.player.command.UserSettingsCommand;
import org.airsonic.player.dao.AlbumDao;
import org.airsonic.player.dao.ArtistDao;
import org.airsonic.player.dao.MediaFileDao;
import org.airsonic.player.dao.PlayQueueDao;
import org.airsonic.player.domain.*;
import org.airsonic.player.domain.Bookmark;
import org.airsonic.player.domain.PlayQueue;
import org.airsonic.player.i18n.LocaleResolver;
import org.airsonic.player.service.*;
import org.airsonic.player.util.Pair;
import org.airsonic.player.util.StringUtil;
import org.airsonic.player.util.Util;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.subsonic.restapi.*;
import org.subsonic.restapi.PodcastStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.airsonic.player.security.RESTRequestParameterProcessingFilter.decrypt;
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
@RequestMapping(value = "/rest", method = {RequestMethod.GET, RequestMethod.POST})
public class SubsonicRESTController {

    private static final Logger LOG = LoggerFactory.getLogger(SubsonicRESTController.class);

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
    private BookmarkService bookmarkService;
    @Autowired
    private PlayQueueDao playQueueDao;
    @Autowired
    private MediaScannerService mediaScannerService;
    @Autowired
    private LocaleResolver localeResolver;

    private final Map<BookmarkKey, org.airsonic.player.domain.Bookmark> bookmarkCache = new ConcurrentHashMap<BookmarkKey, org.airsonic.player.domain.Bookmark>();
    private final JAXBWriter jaxbWriter = new JAXBWriter();

    private static final String NOT_YET_IMPLEMENTED = "Not yet implemented";
    private static final String NO_LONGER_SUPPORTED = "No longer supported";

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public void handleMissingRequestParam(HttpServletRequest request,
                                          HttpServletResponse response,
                                          MissingServletRequestParameterException exception) throws Exception {
        error(request, response, ErrorCode.MISSING_PARAMETER, "Required param ("+exception.getParameterName()+") is missing");
    }

    @RequestMapping(value = "/ping")
    public void ping(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Response res = createResponse();
        jaxbWriter.writeResponse(request, response, res);
    }


    /**
     * CAUTION : this method is required by mobile applications and must not be removed.
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(value = "/getLicense")
    public void getLicense(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        License license = new License();


        license.setEmail("airsonic@github.com");
        license.setValid(true);
        Date neverExpireDate = new Date(Long.MAX_VALUE);
        license.setLicenseExpires(jaxbWriter.convertDate(neverExpireDate));
        license.setTrialExpires(jaxbWriter.convertDate(neverExpireDate));

        Response res = createResponse();
        res.setLicense(license);
        jaxbWriter.writeResponse(request, response, res);
    }


    @RequestMapping(value = "/getMusicFolders")
    public void getMusicFolders(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);

        MusicFolders musicFolders = new MusicFolders();
        String username = securityService.getCurrentUsername(request);
        for (org.airsonic.player.domain.MusicFolder musicFolder : settingsService.getMusicFoldersForUser(username)) {
            org.subsonic.restapi.MusicFolder mf = new org.subsonic.restapi.MusicFolder();
            mf.setId(musicFolder.getId());
            mf.setName(musicFolder.getName());
            musicFolders.getMusicFolder().add(mf);
        }
        Response res = createResponse();
        res.setMusicFolders(musicFolders);
        jaxbWriter.writeResponse(request, response, res);
    }

    @RequestMapping(value = "/getIndexes")
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

        List<org.airsonic.player.domain.MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username);
        Integer musicFolderId = getIntParameter(request, "musicFolderId");
        if (musicFolderId != null) {
            for (org.airsonic.player.domain.MusicFolder musicFolder : musicFolders) {
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
                        org.subsonic.restapi.Artist a = new org.subsonic.restapi.Artist();
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

    @RequestMapping(value = "/getGenres")
    public void getGenres(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        org.subsonic.restapi.Genres genres = new org.subsonic.restapi.Genres();

        for (org.airsonic.player.domain.Genre genre : mediaFileDao.getGenres(false)) {
            org.subsonic.restapi.Genre g = new org.subsonic.restapi.Genre();
            genres.getGenre().add(g);
            g.setContent(genre.getName());
            g.setAlbumCount(genre.getAlbumCount());
            g.setSongCount(genre.getSongCount());
        }
        Response res = createResponse();
        res.setGenres(genres);
        jaxbWriter.writeResponse(request, response, res);
    }

    @RequestMapping(value = "/getSongsByGenre")
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
        List<org.airsonic.player.domain.MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username, musicFolderId);

        for (MediaFile mediaFile : mediaFileDao.getSongsByGenre(genre, offset, count, musicFolders)) {
            songs.getSong().add(createJaxbChild(player, mediaFile, username));
        }
        Response res = createResponse();
        res.setSongsByGenre(songs);
        jaxbWriter.writeResponse(request, response, res);
    }

    @RequestMapping(value = "/getArtists")
    public void getArtists(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        String username = securityService.getCurrentUsername(request);

        ArtistsID3 result = new ArtistsID3();
        result.setIgnoredArticles(settingsService.getIgnoredArticles());
        List<org.airsonic.player.domain.MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username);

        List<org.airsonic.player.domain.Artist> artists = artistDao.getAlphabetialArtists(0, Integer.MAX_VALUE, musicFolders);
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

    @RequestMapping(value = "/getSimilarSongs")
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
        List<org.airsonic.player.domain.MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username);
        List<MediaFile> similarSongs = lastFmService.getSimilarSongs(mediaFile, count, musicFolders);
        Player player = playerService.getPlayer(request, response);
        for (MediaFile similarSong : similarSongs) {
            result.getSong().add(createJaxbChild(player, similarSong, username));
        }

        Response res = createResponse();
        res.setSimilarSongs(result);
        jaxbWriter.writeResponse(request, response, res);
    }

    @RequestMapping(value = "/getSimilarSongs2")
    public void getSimilarSongs2(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        String username = securityService.getCurrentUsername(request);

        int id = getRequiredIntParameter(request, "id");
        int count = getIntParameter(request, "count", 50);

        SimilarSongs2 result = new SimilarSongs2();

        org.airsonic.player.domain.Artist artist = artistDao.getArtist(id);
        if (artist == null) {
            error(request, response, ErrorCode.NOT_FOUND, "Artist not found.");
            return;
        }

        List<org.airsonic.player.domain.MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username);
        List<MediaFile> similarSongs = lastFmService.getSimilarSongs(artist, count, musicFolders);
        Player player = playerService.getPlayer(request, response);
        for (MediaFile similarSong : similarSongs) {
            result.getSong().add(createJaxbChild(player, similarSong, username));
        }

        Response res = createResponse();
        res.setSimilarSongs2(result);
        jaxbWriter.writeResponse(request, response, res);
    }

    @RequestMapping(value = "/getTopSongs")
    public void getTopSongs(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        String username = securityService.getCurrentUsername(request);

        String artist = getRequiredStringParameter(request, "artist");
        int count = getIntParameter(request, "count", 50);

        TopSongs result = new TopSongs();

        List<org.airsonic.player.domain.MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username);
        List<MediaFile> topSongs = lastFmService.getTopSongs(artist, count, musicFolders);
        Player player = playerService.getPlayer(request, response);
        for (MediaFile topSong : topSongs) {
            result.getSong().add(createJaxbChild(player, topSong, username));
        }

        Response res = createResponse();
        res.setTopSongs(result);
        jaxbWriter.writeResponse(request, response, res);
    }

    @RequestMapping(value = "/getArtistInfo")
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
        List<org.airsonic.player.domain.MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username);
        List<MediaFile> similarArtists = lastFmService.getSimilarArtists(mediaFile, count, includeNotPresent, musicFolders);
        for (MediaFile similarArtist : similarArtists) {
            result.getSimilarArtist().add(createJaxbArtist(similarArtist, username));
        }
        ArtistBio artistBio = lastFmService.getArtistBio(mediaFile, localeResolver.resolveLocale(request));
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

    @RequestMapping(value = "/getArtistInfo2")
    public void getArtistInfo2(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        String username = securityService.getCurrentUsername(request);

        int id = getRequiredIntParameter(request, "id");
        int count = getIntParameter(request, "count", 20);
        boolean includeNotPresent = ServletRequestUtils.getBooleanParameter(request, "includeNotPresent", false);

        ArtistInfo2 result = new ArtistInfo2();

        org.airsonic.player.domain.Artist artist = artistDao.getArtist(id);
        if (artist == null) {
            error(request, response, ErrorCode.NOT_FOUND, "Artist not found.");
            return;
        }

        List<org.airsonic.player.domain.MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username);
        List<org.airsonic.player.domain.Artist> similarArtists = lastFmService.getSimilarArtists(artist, count, includeNotPresent, musicFolders);
        for (org.airsonic.player.domain.Artist similarArtist : similarArtists) {
            result.getSimilarArtist().add(createJaxbArtist(new ArtistID3(), similarArtist, username));
        }
        ArtistBio artistBio = lastFmService.getArtistBio(artist, localeResolver.resolveLocale(request));
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

    private <T extends ArtistID3> T createJaxbArtist(T jaxbArtist, org.airsonic.player.domain.Artist artist, String username) {
        jaxbArtist.setId(String.valueOf(artist.getId()));
        jaxbArtist.setName(artist.getName());
        jaxbArtist.setStarred(jaxbWriter.convertDate(mediaFileDao.getMediaFileStarredDate(artist.getId(), username)));
        jaxbArtist.setAlbumCount(artist.getAlbumCount());
        if (artist.getCoverArtPath() != null) {
            jaxbArtist.setCoverArt(CoverArtController.ARTIST_COVERART_PREFIX + artist.getId());
        }
        return jaxbArtist;
    }

    private org.subsonic.restapi.Artist createJaxbArtist(MediaFile artist, String username) {
        org.subsonic.restapi.Artist result = new org.subsonic.restapi.Artist();
        result.setId(String.valueOf(artist.getId()));
        result.setName(artist.getArtist());
        Date starred = mediaFileDao.getMediaFileStarredDate(artist.getId(), username);
        result.setStarred(jaxbWriter.convertDate(starred));
        return result;
    }

    @RequestMapping(value = "/getArtist")
    public void getArtist(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);

        String username = securityService.getCurrentUsername(request);
        int id = getRequiredIntParameter(request, "id");
        org.airsonic.player.domain.Artist artist = artistDao.getArtist(id);
        if (artist == null) {
            error(request, response, ErrorCode.NOT_FOUND, "Artist not found.");
            return;
        }

        List<org.airsonic.player.domain.MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username);
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
            org.airsonic.player.domain.Artist artist = artistDao.getArtist(album.getArtist());
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

    private <T extends org.subsonic.restapi.Playlist> T createJaxbPlaylist(T jaxbPlaylist, org.airsonic.player.domain.Playlist playlist) {
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

    @RequestMapping(value = "/getAlbum")
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

    @RequestMapping(value = "/getSong")
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

    @RequestMapping(value = "/getMusicDirectory")
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
        directory.setPlayCount((long) dir.getPlayCount());

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

    @RequestMapping(value = "/search")
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
        List<org.airsonic.player.domain.MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username);

        org.airsonic.player.domain.SearchResult result = searchService.search(criteria, musicFolders, SearchService.IndexType.SONG);
        org.subsonic.restapi.SearchResult searchResult = new org.subsonic.restapi.SearchResult();
        searchResult.setOffset(result.getOffset());
        searchResult.setTotalHits(result.getTotalHits());

        for (MediaFile mediaFile : result.getMediaFiles()) {
            searchResult.getMatch().add(createJaxbChild(player, mediaFile, username));
        }
        Response res = createResponse();
        res.setSearchResult(searchResult);
        jaxbWriter.writeResponse(request, response, res);
    }

    @RequestMapping(value = "/search2")
    public void search2(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        Player player = playerService.getPlayer(request, response);
        String username = securityService.getCurrentUsername(request);
        Integer musicFolderId = getIntParameter(request, "musicFolderId");
        List<org.airsonic.player.domain.MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username, musicFolderId);

        SearchResult2 searchResult = new SearchResult2();

        String query = request.getParameter("query");
        SearchCriteria criteria = new SearchCriteria();
        criteria.setQuery(StringUtils.trimToEmpty(query));
        criteria.setCount(getIntParameter(request, "artistCount", 20));
        criteria.setOffset(getIntParameter(request, "artistOffset", 0));
        org.airsonic.player.domain.SearchResult artists = searchService.search(criteria, musicFolders, SearchService.IndexType.ARTIST);
        for (MediaFile mediaFile : artists.getMediaFiles()) {
            searchResult.getArtist().add(createJaxbArtist(mediaFile, username));
        }

        criteria.setCount(getIntParameter(request, "albumCount", 20));
        criteria.setOffset(getIntParameter(request, "albumOffset", 0));
        org.airsonic.player.domain.SearchResult albums = searchService.search(criteria, musicFolders, SearchService.IndexType.ALBUM);
        for (MediaFile mediaFile : albums.getMediaFiles()) {
            searchResult.getAlbum().add(createJaxbChild(player, mediaFile, username));
        }

        criteria.setCount(getIntParameter(request, "songCount", 20));
        criteria.setOffset(getIntParameter(request, "songOffset", 0));
        org.airsonic.player.domain.SearchResult songs = searchService.search(criteria, musicFolders, SearchService.IndexType.SONG);
        for (MediaFile mediaFile : songs.getMediaFiles()) {
            searchResult.getSong().add(createJaxbChild(player, mediaFile, username));
        }

        Response res = createResponse();
        res.setSearchResult2(searchResult);
        jaxbWriter.writeResponse(request, response, res);
    }

    @RequestMapping(value = "/search3")
    public void search3(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        Player player = playerService.getPlayer(request, response);
        String username = securityService.getCurrentUsername(request);
        Integer musicFolderId = getIntParameter(request, "musicFolderId");
        List<org.airsonic.player.domain.MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username, musicFolderId);

        SearchResult3 searchResult = new SearchResult3();

        String query = request.getParameter("query");
        SearchCriteria criteria = new SearchCriteria();
        criteria.setQuery(StringUtils.trimToEmpty(query));
        criteria.setCount(getIntParameter(request, "artistCount", 20));
        criteria.setOffset(getIntParameter(request, "artistOffset", 0));
        org.airsonic.player.domain.SearchResult result = searchService.search(criteria, musicFolders, SearchService.IndexType.ARTIST_ID3);
        for (org.airsonic.player.domain.Artist artist : result.getArtists()) {
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

    @RequestMapping(value = "/getPlaylists")
    public void getPlaylists(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);

        org.airsonic.player.domain.User user = securityService.getCurrentUser(request);
        String authenticatedUsername = user.getUsername();
        String requestedUsername = request.getParameter("username");

        if (requestedUsername == null) {
            requestedUsername = authenticatedUsername;
        } else if (!user.isAdminRole()) {
            error(request, response, ErrorCode.NOT_AUTHORIZED, authenticatedUsername + " is not authorized to get playlists for " + requestedUsername);
            return;
        }

        Playlists result = new Playlists();

        for (org.airsonic.player.domain.Playlist playlist : playlistService.getReadablePlaylistsForUser(requestedUsername)) {
            result.getPlaylist().add(createJaxbPlaylist(new org.subsonic.restapi.Playlist(), playlist));
        }

        Response res = createResponse();
        res.setPlaylists(result);
        jaxbWriter.writeResponse(request, response, res);
    }

    @RequestMapping(value = "/getPlaylist")
    public void getPlaylist(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        Player player = playerService.getPlayer(request, response);
        String username = securityService.getCurrentUsername(request);

        int id = getRequiredIntParameter(request, "id");

        org.airsonic.player.domain.Playlist playlist = playlistService.getPlaylist(id);
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

    @RequestMapping(value = "/jukeboxControl")
    public void jukeboxControl(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request, true);

        org.airsonic.player.domain.User user = securityService.getCurrentUser(request);
        if (!user.isJukeboxRole()) {
            error(request, response, ErrorCode.NOT_AUTHORIZED, user.getUsername() + " is not authorized to use jukebox.");
            return;
        }

        Player player = playerService.getPlayer(request, response);

        boolean returnPlaylist = false;
        String action = getRequiredStringParameter(request, "action");

        switch (action) {
            case "start":
                player.getPlayQueue().setStatus(PlayQueue.Status.PLAYING);
                jukeboxService.start(player);
                break;
            case "stop":
                player.getPlayQueue().setStatus(PlayQueue.Status.STOPPED);
                jukeboxService.stop(player);
                break;
            case "skip":
                int index = getRequiredIntParameter(request, "index");
                int offset = getIntParameter(request, "offset", 0);
                player.getPlayQueue().setIndex(index);
                jukeboxService.skip(player,index,offset);
                break;
            case "add":
                int[] ids = getIntParameters(request, "id");
                playQueueService.addMediaFilesToPlayQueue(player.getPlayQueue(),ids,null,true);
                break;
            case "set":
                ids = getIntParameters(request, "id");
                playQueueService.resetPlayQueue(player.getPlayQueue(),ids,true);
                break;
            case "clear":
                player.getPlayQueue().clear();
                break;
            case "remove":
                index = getRequiredIntParameter(request, "index");
                player.getPlayQueue().removeFileAt(index);
                break;
            case "shuffle":
                player.getPlayQueue().shuffle();
                break;
            case "setGain":
                float gain = getRequiredFloatParameter(request, "gain");
                jukeboxService.setGain(player,gain);
                break;
            case "get":
                returnPlaylist = true;
                break;
            case "status":
                // No action necessary.
                break;
            default:
                throw new Exception("Unknown jukebox action: '" + action + "'.");
        }

        String username = securityService.getCurrentUsername(request);
        PlayQueue playQueue = player.getPlayQueue();

        // this variable is only needed for the JukeboxLegacySubsonicService. To be removed.
        boolean controlsJukebox = jukeboxService.canControl(player);

        int currentIndex = controlsJukebox && !playQueue.isEmpty() ? playQueue.getIndex() : -1;
        boolean playing = controlsJukebox && !playQueue.isEmpty() && playQueue.getStatus() == PlayQueue.Status.PLAYING;
        float gain;
        int position;
        gain = jukeboxService.getGain(player);
        position = controlsJukebox && !playQueue.isEmpty() ? jukeboxService.getPosition(player) : 0;

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

    @RequestMapping(value = "/createPlaylist")
    public void createPlaylist(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request, true);
        String username = securityService.getCurrentUsername(request);

        Integer playlistId = getIntParameter(request, "playlistId");
        String name = request.getParameter("name");
        if (playlistId == null && name == null) {
            error(request, response, ErrorCode.MISSING_PARAMETER, "Playlist ID or name must be specified.");
            return;
        }

        org.airsonic.player.domain.Playlist playlist;
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
            playlist = new org.airsonic.player.domain.Playlist();
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

    @RequestMapping(value = "/updatePlaylist")
    public void updatePlaylist(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request, true);
        String username = securityService.getCurrentUsername(request);

        int id = getRequiredIntParameter(request, "playlistId");
        org.airsonic.player.domain.Playlist playlist = playlistService.getPlaylist(id);
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

    @RequestMapping(value = "/deletePlaylist")
    public void deletePlaylist(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request, true);
        String username = securityService.getCurrentUsername(request);

        int id = getRequiredIntParameter(request, "id");
        org.airsonic.player.domain.Playlist playlist = playlistService.getPlaylist(id);
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

    @RequestMapping(value = "/getAlbumList")
    public void getAlbumList(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        Player player = playerService.getPlayer(request, response);
        String username = securityService.getCurrentUsername(request);

        int size = getIntParameter(request, "size", 10);
        int offset = getIntParameter(request, "offset", 0);
        Integer musicFolderId = getIntParameter(request, "musicFolderId");

        List<org.airsonic.player.domain.MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username, musicFolderId);

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

    @RequestMapping(value = "/getAlbumList2")
    public void getAlbumList2(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);

        int size = getIntParameter(request, "size", 10);
        int offset = getIntParameter(request, "offset", 0);
        size = Math.max(0, Math.min(size, 500));
        String type = getRequiredStringParameter(request, "type");
        String username = securityService.getCurrentUsername(request);
        Integer musicFolderId = getIntParameter(request, "musicFolderId");
        List<org.airsonic.player.domain.MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username, musicFolderId);

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

    @RequestMapping(value = "/getRandomSongs")
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
        List<org.airsonic.player.domain.MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username, musicFolderId);
        RandomSearchCriteria criteria = new RandomSearchCriteria(size, genre, fromYear, toYear, musicFolders);

        Songs result = new Songs();
        for (MediaFile mediaFile : searchService.getRandomSongs(criteria)) {
            result.getSong().add(createJaxbChild(player, mediaFile, username));
        }
        Response res = createResponse();
        res.setRandomSongs(result);
        jaxbWriter.writeResponse(request, response, res);
    }

    @RequestMapping(value = "/getVideos")
    public void getVideos(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        Player player = playerService.getPlayer(request, response);
        String username = securityService.getCurrentUsername(request);

        int size = getIntParameter(request, "size", Integer.MAX_VALUE);
        int offset = getIntParameter(request, "offset", 0);
        List<org.airsonic.player.domain.MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username);

        Videos result = new Videos();
        for (MediaFile mediaFile : mediaFileDao.getVideos(size, offset, musicFolders)) {
            result.getVideo().add(createJaxbChild(player, mediaFile, username));
        }
        Response res = createResponse();
        res.setVideos(result);
        jaxbWriter.writeResponse(request, response, res);
    }

    @RequestMapping(value = "/getNowPlaying")
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
                entry.setPlayerId(player.getId());
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
            child.setPath(getRelativePath(mediaFile, settingsService));

            org.airsonic.player.domain.Bookmark bookmark = bookmarkCache.get(new BookmarkKey(username, mediaFile.getId()));
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
                org.airsonic.player.domain.Artist artist = artistDao.getArtist(mediaFile.getArtist());
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

    public static String getRelativePath(MediaFile musicFile, SettingsService settingsService) {

        String filePath = musicFile.getPath();

        // Convert slashes.
        filePath = filePath.replace('\\', '/');

        String filePathLower = filePath.toLowerCase();

        List<org.airsonic.player.domain.MusicFolder> musicFolders = settingsService.getAllMusicFolders(false, true);
        for (org.airsonic.player.domain.MusicFolder musicFolder : musicFolders) {
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

    @RequestMapping(value = "/download")
    public void download(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        org.airsonic.player.domain.User user = securityService.getCurrentUser(request);
        if (!user.isDownloadRole()) {
            error(request, response, ErrorCode.NOT_AUTHORIZED, user.getUsername() + " is not authorized to download files.");
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

    @RequestMapping(value = "/stream")
    public void stream(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        org.airsonic.player.domain.User user = securityService.getCurrentUser(request);
        if (!user.isStreamRole()) {
            error(request, response, ErrorCode.NOT_AUTHORIZED, user.getUsername() + " is not authorized to play files.");
            return;
        }

        streamController.handleRequest(request, response);
    }

    @RequestMapping(value = "/hls")
    public void hls(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        org.airsonic.player.domain.User user = securityService.getCurrentUser(request);
        if (!user.isStreamRole()) {
            error(request, response, ErrorCode.NOT_AUTHORIZED, user.getUsername() + " is not authorized to play files.");
            return;
        }
        int id = getRequiredIntParameter(request, "id");
        MediaFile video = mediaFileDao.getMediaFile(id);
        if (video == null || video.isDirectory()) {
            error(request, response, ErrorCode.NOT_FOUND, "Video not found.");
            return;
        }
        if (!securityService.isFolderAccessAllowed(video, user.getUsername())) {
            error(request, response, ErrorCode.NOT_AUTHORIZED, "Access denied");
            return;
        }
        hlsController.handleRequest(request, response);
    }

    @RequestMapping(value = "/scrobble")
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

    @RequestMapping(value = "/star")
    public void star(HttpServletRequest request, HttpServletResponse response) throws Exception {
        starOrUnstar(request, response, true);
    }

    @RequestMapping(value = "/unstar")
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
            org.airsonic.player.domain.Artist artist = artistDao.getArtist(artistId);
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

    @RequestMapping(value = "/getStarred")
    public void getStarred(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        Player player = playerService.getPlayer(request, response);
        String username = securityService.getCurrentUsername(request);
        Integer musicFolderId = getIntParameter(request, "musicFolderId");
        List<org.airsonic.player.domain.MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username, musicFolderId);

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

    @RequestMapping(value = "/getStarred2")
    public void getStarred2(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        Player player = playerService.getPlayer(request, response);
        String username = securityService.getCurrentUsername(request);
        Integer musicFolderId = getIntParameter(request, "musicFolderId");
        List<org.airsonic.player.domain.MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username, musicFolderId);

        Starred2 result = new Starred2();
        for (org.airsonic.player.domain.Artist artist : artistDao.getStarredArtists(0, Integer.MAX_VALUE, username, musicFolders)) {
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

    @RequestMapping(value = "/getPodcasts")
    public void getPodcasts(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        Player player = playerService.getPlayer(request, response);
        String username = securityService.getCurrentUsername(request);
        boolean includeEpisodes = getBooleanParameter(request, "includeEpisodes", true);
        Integer channelId = getIntParameter(request, "id");

        Podcasts result = new Podcasts();

        for (org.airsonic.player.domain.PodcastChannel channel : podcastService.getAllChannels()) {
            if (channelId == null || channelId.equals(channel.getId())) {

                org.subsonic.restapi.PodcastChannel c = new org.subsonic.restapi.PodcastChannel();
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
                    List<org.airsonic.player.domain.PodcastEpisode> episodes = podcastService.getEpisodes(channel.getId());
                    for (org.airsonic.player.domain.PodcastEpisode episode : episodes) {
                        c.getEpisode().add(createJaxbPodcastEpisode(player, username, episode));
                    }
                }
            }
        }
        Response res = createResponse();
        res.setPodcasts(result);
        jaxbWriter.writeResponse(request, response, res);
    }

    @RequestMapping(value = "/getNewestPodcasts")
    public void getNewestPodcasts(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        Player player = playerService.getPlayer(request, response);
        String username = securityService.getCurrentUsername(request);

        int count = getIntParameter(request, "count", 20);
        NewestPodcasts result = new NewestPodcasts();

        for (org.airsonic.player.domain.PodcastEpisode episode : podcastService.getNewestEpisodes(count)) {
            result.getEpisode().add(createJaxbPodcastEpisode(player, username, episode));
        }

        Response res = createResponse();
        res.setNewestPodcasts(result);
        jaxbWriter.writeResponse(request, response, res);
    }

    private org.subsonic.restapi.PodcastEpisode createJaxbPodcastEpisode(Player player, String username, org.airsonic.player.domain.PodcastEpisode episode) {
        org.subsonic.restapi.PodcastEpisode e = new org.subsonic.restapi.PodcastEpisode();

        String path = episode.getPath();
        if (path != null) {
            MediaFile mediaFile = mediaFileService.getMediaFile(path);
            e = createJaxbChild(new org.subsonic.restapi.PodcastEpisode(), player, mediaFile, username);
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

    @RequestMapping(value = "/refreshPodcasts")
    public void refreshPodcasts(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        org.airsonic.player.domain.User user = securityService.getCurrentUser(request);
        if (!user.isPodcastRole()) {
            error(request, response, ErrorCode.NOT_AUTHORIZED, user.getUsername() + " is not authorized to administrate podcasts.");
            return;
        }
        podcastService.refreshAllChannels(true);
        writeEmptyResponse(request, response);
    }

    @RequestMapping(value = "/createPodcastChannel")
    public void createPodcastChannel(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        org.airsonic.player.domain.User user = securityService.getCurrentUser(request);
        if (!user.isPodcastRole()) {
            error(request, response, ErrorCode.NOT_AUTHORIZED, user.getUsername() + " is not authorized to administrate podcasts.");
            return;
        }

        String url = getRequiredStringParameter(request, "url");
        podcastService.createChannel(url);
        writeEmptyResponse(request, response);
    }

    @RequestMapping(value = "/deletePodcastChannel")
    public void deletePodcastChannel(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        org.airsonic.player.domain.User user = securityService.getCurrentUser(request);
        if (!user.isPodcastRole()) {
            error(request, response, ErrorCode.NOT_AUTHORIZED, user.getUsername() + " is not authorized to administrate podcasts.");
            return;
        }

        int id = getRequiredIntParameter(request, "id");
        podcastService.deleteChannel(id);
        writeEmptyResponse(request, response);
    }

    @RequestMapping(value = "/deletePodcastEpisode")
    public void deletePodcastEpisode(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        org.airsonic.player.domain.User user = securityService.getCurrentUser(request);
        if (!user.isPodcastRole()) {
            error(request, response, ErrorCode.NOT_AUTHORIZED, user.getUsername() + " is not authorized to administrate podcasts.");
            return;
        }

        int id = getRequiredIntParameter(request, "id");
        podcastService.deleteEpisode(id, true);
        writeEmptyResponse(request, response);
    }

    @RequestMapping(value = "/downloadPodcastEpisode")
    public void downloadPodcastEpisode(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        org.airsonic.player.domain.User user = securityService.getCurrentUser(request);
        if (!user.isPodcastRole()) {
            error(request, response, ErrorCode.NOT_AUTHORIZED, user.getUsername() + " is not authorized to administrate podcasts.");
            return;
        }

        int id = getRequiredIntParameter(request, "id");
        org.airsonic.player.domain.PodcastEpisode episode = podcastService.getEpisode(id, true);
        if (episode == null) {
            error(request, response, ErrorCode.NOT_FOUND, "Podcast episode " + id + " not found.");
            return;
        }

        podcastService.downloadEpisode(episode);
        writeEmptyResponse(request, response);
    }

    @RequestMapping(value = "/getInternetRadioStations")
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

    @RequestMapping(value = "/getBookmarks")
    public void getBookmarks(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        Player player = playerService.getPlayer(request, response);
        String username = securityService.getCurrentUsername(request);

        Bookmarks result = new Bookmarks();
        for (Bookmark bookmark : bookmarkService.getBookmarks(username)) {
            org.subsonic.restapi.Bookmark b = new org.subsonic.restapi.Bookmark();
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

    @RequestMapping(value = "/createBookmark")
    public void createBookmark(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        String username = securityService.getCurrentUsername(request);
        int mediaFileId = getRequiredIntParameter(request, "id");
        long position = getRequiredLongParameter(request, "position");
        String comment = request.getParameter("comment");
        Date now = new Date();

        Bookmark bookmark = new Bookmark(0, mediaFileId, position, username, comment, now, now);
        bookmarkService.createOrUpdateBookmark(bookmark);
        writeEmptyResponse(request, response);
    }

    @RequestMapping(value = "/deleteBookmark")
    public void deleteBookmark(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);

        String username = securityService.getCurrentUsername(request);
        int mediaFileId = getRequiredIntParameter(request, "id");
        bookmarkService.deleteBookmark(username, mediaFileId);

        writeEmptyResponse(request, response);
    }

    @RequestMapping(value = "/getPlayQueue")
    public void getPlayQueue(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        String username = securityService.getCurrentUsername(request);
        Player player = playerService.getPlayer(request, response);

        SavedPlayQueue playQueue = playQueueDao.getPlayQueue(username);
        if (playQueue == null) {
            writeEmptyResponse(request, response);
            return;
        }

        org.subsonic.restapi.PlayQueue restPlayQueue = new org.subsonic.restapi.PlayQueue();
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

    @RequestMapping(value = "/savePlayQueue")
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

    @RequestMapping(value = "/getShares")
    public void getShares(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        Player player = playerService.getPlayer(request, response);
        String username = securityService.getCurrentUsername(request);
        org.airsonic.player.domain.User user = securityService.getCurrentUser(request);
        List<org.airsonic.player.domain.MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username);

        Shares result = new Shares();
        for (org.airsonic.player.domain.Share share : shareService.getSharesForUser(user)) {
            org.subsonic.restapi.Share s = createJaxbShare(request, share);
            result.getShare().add(s);

            for (MediaFile mediaFile : shareService.getSharedFiles(share.getId(), musicFolders)) {
                s.getEntry().add(createJaxbChild(player, mediaFile, username));
            }
        }
        Response res = createResponse();
        res.setShares(result);
        jaxbWriter.writeResponse(request, response, res);
    }

    @RequestMapping(value = "/createShare")
    public void createShare(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        Player player = playerService.getPlayer(request, response);
        String username = securityService.getCurrentUsername(request);

        org.airsonic.player.domain.User user = securityService.getCurrentUser(request);
        if (!user.isShareRole()) {
            error(request, response, ErrorCode.NOT_AUTHORIZED, user.getUsername() + " is not authorized to share media.");
            return;
        }

        List<MediaFile> files = new ArrayList<MediaFile>();
        for (int id : getRequiredIntParameters(request, "id")) {
            files.add(mediaFileService.getMediaFile(id));
        }

        org.airsonic.player.domain.Share share = shareService.createShare(request, files);
        share.setDescription(request.getParameter("description"));
        long expires = getLongParameter(request, "expires", 0L);
        if (expires != 0) {
            share.setExpires(new Date(expires));
        }
        shareService.updateShare(share);

        Shares result = new Shares();
        org.subsonic.restapi.Share s = createJaxbShare(request, share);
        result.getShare().add(s);

        List<org.airsonic.player.domain.MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username);

        for (MediaFile mediaFile : shareService.getSharedFiles(share.getId(), musicFolders)) {
            s.getEntry().add(createJaxbChild(player, mediaFile, username));
        }

        Response res = createResponse();
        res.setShares(result);
        jaxbWriter.writeResponse(request, response, res);
    }

    @RequestMapping(value = "/deleteShare")
    public void deleteShare(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        org.airsonic.player.domain.User user = securityService.getCurrentUser(request);
        int id = getRequiredIntParameter(request, "id");

        org.airsonic.player.domain.Share share = shareService.getShareById(id);
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

    @RequestMapping(value = "/updateShare")
    public void updateShare(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        org.airsonic.player.domain.User user = securityService.getCurrentUser(request);
        int id = getRequiredIntParameter(request, "id");

        org.airsonic.player.domain.Share share = shareService.getShareById(id);
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

    private org.subsonic.restapi.Share createJaxbShare(HttpServletRequest request, org.airsonic.player.domain.Share share) {
        org.subsonic.restapi.Share result = new org.subsonic.restapi.Share();
        result.setId(String.valueOf(share.getId()));
        result.setUrl(shareService.getShareUrl(request, share));
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

    @RequestMapping(value = "/getCoverArt")
    public void getCoverArt(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        coverArtController.handleRequest(request, response);
    }

    @RequestMapping(value = "/getAvatar")
    public void getAvatar(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        avatarController.handleRequest(request, response);
    }

    @RequestMapping(value = "/changePassword")
    public void changePassword(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);

        String username = getRequiredStringParameter(request, "username");
        String password = decrypt(getRequiredStringParameter(request, "password"));

        org.airsonic.player.domain.User authUser = securityService.getCurrentUser(request);

        boolean allowed = authUser.isAdminRole()
                || username.equals(authUser.getUsername()) && authUser.isSettingsRole();

        if (!allowed) {
            error(request, response, ErrorCode.NOT_AUTHORIZED, authUser.getUsername() + " is not authorized to change password for " + username);
            return;
        }

        org.airsonic.player.domain.User user = securityService.getUserByName(username);
        user.setPassword(password);
        securityService.updateUser(user);

        writeEmptyResponse(request, response);
    }

    @RequestMapping(value = "/getUser")
    public void getUser(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);

        String username = getRequiredStringParameter(request, "username");

        org.airsonic.player.domain.User currentUser = securityService.getCurrentUser(request);
        if (!username.equals(currentUser.getUsername()) && !currentUser.isAdminRole()) {
            error(request, response, ErrorCode.NOT_AUTHORIZED, currentUser.getUsername() + " is not authorized to get details for other users.");
            return;
        }

        org.airsonic.player.domain.User requestedUser = securityService.getUserByName(username);
        if (requestedUser == null) {
            error(request, response, ErrorCode.NOT_FOUND, "No such user: " + username);
            return;
        }

        Response res = createResponse();
        res.setUser(createJaxbUser(requestedUser));
        jaxbWriter.writeResponse(request, response, res);
    }

    @RequestMapping(value = "/getUsers")
    public void getUsers(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);

        org.airsonic.player.domain.User currentUser = securityService.getCurrentUser(request);
        if (!currentUser.isAdminRole()) {
            error(request, response, ErrorCode.NOT_AUTHORIZED, currentUser.getUsername() + " is not authorized to get details for other users.");
            return;
        }

        Users result = new Users();
        for (org.airsonic.player.domain.User user : securityService.getAllUsers()) {
            result.getUser().add(createJaxbUser(user));
        }

        Response res = createResponse();
        res.setUsers(result);
        jaxbWriter.writeResponse(request, response, res);
    }

    private org.subsonic.restapi.User createJaxbUser(org.airsonic.player.domain.User user) {
        UserSettings userSettings = settingsService.getUserSettings(user.getUsername());

        org.subsonic.restapi.User result = new org.subsonic.restapi.User();
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

        List<org.airsonic.player.domain.MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(user.getUsername());
        for (org.airsonic.player.domain.MusicFolder musicFolder : musicFolders) {
            result.getFolder().add(musicFolder.getId());
        }
        return result;
    }

    @RequestMapping(value = "/createUser")
    public void createUser(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        org.airsonic.player.domain.User user = securityService.getCurrentUser(request);
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
            folderIds = Util.toIntArray(org.airsonic.player.domain.MusicFolder.toIdList(settingsService.getAllMusicFolders()));
        }
        command.setAllowedMusicFolderIds(folderIds);

        userSettingsController.createUser(command);
        writeEmptyResponse(request, response);
    }

    @RequestMapping(value = "/updateUser")
    public void updateUser(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        org.airsonic.player.domain.User user = securityService.getCurrentUser(request);
        if (!user.isAdminRole()) {
            error(request, response, ErrorCode.NOT_AUTHORIZED, user.getUsername() + " is not authorized to update users.");
            return;
        }

        String username = getRequiredStringParameter(request, "username");
        org.airsonic.player.domain.User u = securityService.getUserByName(username);
        UserSettings s = settingsService.getUserSettings(username);

        if (u == null) {
            error(request, response, ErrorCode.NOT_FOUND, "No such user: " + username);
            return;
        } else if (org.airsonic.player.domain.User.USERNAME_ADMIN.equals(username)) {
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
            folderIds = Util.toIntArray(org.airsonic.player.domain.MusicFolder.toIdList(settingsService.getMusicFoldersForUser(username)));
        }
        command.setAllowedMusicFolderIds(folderIds);

        userSettingsController.updateUser(command);
        writeEmptyResponse(request, response);
    }

    private boolean hasParameter(HttpServletRequest request, String name) {
        return request.getParameter(name) != null;
    }

    @RequestMapping(value = "/deleteUser")
    public void deleteUser(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        org.airsonic.player.domain.User user = securityService.getCurrentUser(request);
        if (!user.isAdminRole()) {
            error(request, response, ErrorCode.NOT_AUTHORIZED, user.getUsername() + " is not authorized to delete users.");
            return;
        }

        String username = getRequiredStringParameter(request, "username");
        if (org.airsonic.player.domain.User.USERNAME_ADMIN.equals(username)) {
            error(request, response, ErrorCode.NOT_AUTHORIZED, "Not allowed to delete admin user");
            return;
        }

        securityService.deleteUser(username);

        writeEmptyResponse(request, response);
    }

    @RequestMapping(value = "/getChatMessages")
    public ResponseEntity<String> getChatMessages(HttpServletRequest request, HttpServletResponse response) {
        return ResponseEntity.status(HttpStatus.SC_GONE).body(NO_LONGER_SUPPORTED);
    }

    @RequestMapping(value = "/addChatMessage")
    public ResponseEntity<String> addChatMessage(HttpServletRequest request, HttpServletResponse response) {
        return ResponseEntity.status(HttpStatus.SC_GONE).body(NO_LONGER_SUPPORTED);
    }

    @RequestMapping(value = "/getLyrics")
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

    @RequestMapping(value = "/setRating")
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

    @RequestMapping(path = "/getAlbumInfo")
    public void getAlbumInfo(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);

        int id = ServletRequestUtils.getRequiredIntParameter(request, "id");

        MediaFile mediaFile = this.mediaFileService.getMediaFile(id);
        if (mediaFile == null) {
            error(request, response, SubsonicRESTController.ErrorCode.NOT_FOUND, "Media file not found.");
            return;
        }
        AlbumNotes albumNotes = this.lastFmService.getAlbumNotes(mediaFile);

        AlbumInfo result = getAlbumInfoInternal(albumNotes);
        Response res = createResponse();
        res.setAlbumInfo(result);
        this.jaxbWriter.writeResponse(request, response, res);
    }

    @RequestMapping(path = "/getAlbumInfo2")
    public void getAlbumInfo2(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);

        int id = ServletRequestUtils.getRequiredIntParameter(request, "id");

        Album album = this.albumDao.getAlbum(id);
        if (album == null) {
            error(request, response, SubsonicRESTController.ErrorCode.NOT_FOUND, "Album not found.");
            return;
        }
        AlbumNotes albumNotes = this.lastFmService.getAlbumNotes(album);

        AlbumInfo result = getAlbumInfoInternal(albumNotes);
        Response res = createResponse();
        res.setAlbumInfo(result);
        this.jaxbWriter.writeResponse(request, response, res);
    }

    private AlbumInfo getAlbumInfoInternal(AlbumNotes albumNotes) {
        AlbumInfo result = new AlbumInfo();
        if (albumNotes != null)
        {
            result.setNotes(albumNotes.getNotes());
            result.setMusicBrainzId(albumNotes.getMusicBrainzId());
            result.setLastFmUrl(albumNotes.getLastFmUrl());
            result.setSmallImageUrl(albumNotes.getSmallImageUrl());
            result.setMediumImageUrl(albumNotes.getMediumImageUrl());
            result.setLargeImageUrl(albumNotes.getLargeImageUrl());
        }
        return result;
    }

    @RequestMapping(value = "/getVideoInfo")
    public ResponseEntity<String> getVideoInfo() throws Exception {
        return ResponseEntity.status(HttpStatus.SC_NOT_IMPLEMENTED).body(NOT_YET_IMPLEMENTED);
    }

    @RequestMapping(value = "/getCaptions")
    public ResponseEntity<String> getCaptions() {
        return ResponseEntity.status(HttpStatus.SC_NOT_IMPLEMENTED).body(NOT_YET_IMPLEMENTED);
    }

    @RequestMapping(value = "/startScan")
    public void startScan(HttpServletRequest request, HttpServletResponse response) {
        request = wrapRequest(request);
        mediaScannerService.scanLibrary();
        getScanStatus(request, response);
    }

    @RequestMapping(value = "/getScanStatus")
    public void getScanStatus(HttpServletRequest request, HttpServletResponse response) {
        request = wrapRequest(request);
        ScanStatus scanStatus = new ScanStatus();
        scanStatus.setScanning(this.mediaScannerService.isScanning());
        scanStatus.setCount((long) this.mediaScannerService.getScanCount());

        Response res = createResponse();
        res.setScanStatus(scanStatus);
        this.jaxbWriter.writeResponse(request, response, res);
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
        return !players.isEmpty() ? String.valueOf(players.get(0).getId()) : null;
    }

    public enum ErrorCode {

        GENERIC(0, "A generic error."),
        MISSING_PARAMETER(10, "Required parameter is missing."),
        PROTOCOL_MISMATCH_CLIENT_TOO_OLD(20, "Incompatible Airsonic REST protocol version. Client must upgrade."),
        PROTOCOL_MISMATCH_SERVER_TOO_OLD(30, "Incompatible Airsonic REST protocol version. Server must upgrade."),
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

        static BookmarkKey forBookmark(org.airsonic.player.domain.Bookmark b) {
            return new BookmarkKey(b.getUsername(), b.getMediaFileId());
        }
    }
}
