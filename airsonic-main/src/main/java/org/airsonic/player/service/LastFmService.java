/*
 * This file is part of Airsonic.
 *
 *  Airsonic is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Airsonic is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Copyright 2014 (C) Sindre Mehus
 */

package org.airsonic.player.service;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import de.umass.lastfm.*;
import de.umass.lastfm.Album;
import de.umass.lastfm.Artist;
import org.airsonic.player.dao.ArtistDao;
import org.airsonic.player.dao.MediaFileDao;
import org.airsonic.player.domain.*;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides services from the Last.fm REST API.
 *
 * @author Sindre Mehus
 * @version $Id$
 */
@Service
public class LastFmService {

    private static final String LAST_FM_KEY = "ece4499898a9440896dfdce5dab26bbf";
    private static final long CACHE_TIME_TO_LIVE_MILLIS = 6 * 30 * 24 * 3600 * 1000L; // 6 months
    private static final Logger LOG = LoggerFactory.getLogger(LastFmService.class);

    @Autowired
    private MediaFileDao mediaFileDao;
    @Autowired
    private MediaFileService mediaFileService;
    @Autowired
    private ArtistDao artistDao;

    @PostConstruct
    public void init() {
        Caller caller = Caller.getInstance();
        caller.setUserAgent("Airsonic");

        File cacheDir = new File(SettingsService.getAirsonicHome(), "lastfmcache");
        caller.setCache(new LastFmCache(cacheDir, CACHE_TIME_TO_LIVE_MILLIS));
    }

    /**
     * Returns similar artists, using last.fm REST API.
     *
     * @param mediaFile         The media file (song, album or artist).
     * @param count             Max number of similar artists to return.
     * @param includeNotPresent Whether to include artists that are not present in the media library.
     * @param musicFolders      Only return artists present in these folders.
     * @return Similar artists, ordered by presence then similarity.
     */
    public List<MediaFile> getSimilarArtists(MediaFile mediaFile, int count, boolean includeNotPresent, List<MusicFolder> musicFolders) {
        List<MediaFile> result = new ArrayList<MediaFile>();
        if (mediaFile == null) {
            return result;
        }

        String artistName = getArtistName(mediaFile);
        try {
            Collection<Artist> similarArtists = Artist.getSimilar(getCanonicalArtistName(artistName), LAST_FM_KEY);

            // First select artists that are present.
            for (Artist lastFmArtist : similarArtists) {
                MediaFile similarArtist = mediaFileDao.getArtistByName(lastFmArtist.getName(), musicFolders);
                if (similarArtist != null) {
                    result.add(similarArtist);
                    if (result.size() == count) {
                        return result;
                    }
                }
            }

            // Then fill up with non-present artists
            if (includeNotPresent) {
                for (Artist lastFmArtist : similarArtists) {
                    MediaFile similarArtist = mediaFileDao.getArtistByName(lastFmArtist.getName(), musicFolders);
                    if (similarArtist == null) {
                        MediaFile notPresentArtist = new MediaFile();
                        notPresentArtist.setId(-1);
                        notPresentArtist.setArtist(lastFmArtist.getName());
                        result.add(notPresentArtist);
                        if (result.size() == count) {
                            return result;
                        }
                    }
                }
            }

        } catch (Throwable x) {
            LOG.warn("Failed to find similar artists for " + artistName, x);
        }
        return result;
    }

    /**
     * Returns similar artists, using last.fm REST API.
     *
     * @param artist            The artist.
     * @param count             Max number of similar artists to return.
     * @param includeNotPresent Whether to include artists that are not present in the media library.
     * @param musicFolders      Only return songs from artists in these folders.
     * @return Similar artists, ordered by presence then similarity.
     */
    public List<org.airsonic.player.domain.Artist> getSimilarArtists(org.airsonic.player.domain.Artist artist,
                                                                     int count, boolean includeNotPresent, List<MusicFolder> musicFolders) {
        List<org.airsonic.player.domain.Artist> result = new ArrayList<org.airsonic.player.domain.Artist>();

        try {

            // First select artists that are present.
            Collection<Artist> similarArtists = Artist.getSimilar(getCanonicalArtistName(artist.getName()), LAST_FM_KEY);
            for (Artist lastFmArtist : similarArtists) {
                org.airsonic.player.domain.Artist similarArtist = artistDao.getArtist(lastFmArtist.getName(), musicFolders);
                if (similarArtist != null) {
                    result.add(similarArtist);
                    if (result.size() == count) {
                        return result;
                    }
                }
            }

            // Then fill up with non-present artists
            if (includeNotPresent) {
                for (Artist lastFmArtist : similarArtists) {
                    org.airsonic.player.domain.Artist similarArtist = artistDao.getArtist(lastFmArtist.getName());
                    if (similarArtist == null) {
                        org.airsonic.player.domain.Artist notPresentArtist = new org.airsonic.player.domain.Artist();
                        notPresentArtist.setId(-1);
                        notPresentArtist.setName(lastFmArtist.getName());
                        result.add(notPresentArtist);
                        if (result.size() == count) {
                            return result;
                        }
                    }
                }
            }

        } catch (Throwable x) {
            LOG.warn("Failed to find similar artists for " + artist.getName(), x);
        }
        return result;
    }

    /**
     * Returns songs from similar artists, using last.fm REST API. Typically used for artist radio features.
     *
     * @param artist       The artist.
     * @param count        Max number of songs to return.
     * @param musicFolders Only return songs from artists in these folders.
     * @return Songs from similar artists;
     */
    public List<MediaFile> getSimilarSongs(org.airsonic.player.domain.Artist artist, int count,
                                           List<MusicFolder> musicFolders) throws IOException {
        List<MediaFile> similarSongs = new ArrayList<MediaFile>();

        similarSongs.addAll(mediaFileDao.getSongsByArtist(artist.getName(), 0, 1000));
        for (org.airsonic.player.domain.Artist similarArtist : getSimilarArtists(artist, 100, false, musicFolders)) {
            similarSongs.addAll(mediaFileDao.getSongsByArtist(similarArtist.getName(), 0, 1000));
        }
        Collections.shuffle(similarSongs);
        return similarSongs.subList(0, Math.min(count, similarSongs.size()));
    }

    /**
     * Returns songs from similar artists, using last.fm REST API. Typically used for artist radio features.
     *
     * @param mediaFile    The media file (song, album or artist).
     * @param count        Max number of songs to return.
     * @param musicFolders Only return songs from artists present in these folders.
     * @return Songs from similar artists;
     */
    public List<MediaFile> getSimilarSongs(MediaFile mediaFile, int count, List<MusicFolder> musicFolders) {
        List<MediaFile> similarSongs = new ArrayList<MediaFile>();

        String artistName = getArtistName(mediaFile);
        MediaFile artist = mediaFileDao.getArtistByName(artistName, musicFolders);
        if (artist != null) {
            similarSongs.addAll(mediaFileService.getRandomSongsForParent(artist, count));
        }

        for (MediaFile similarArtist : getSimilarArtists(mediaFile, 100, false, musicFolders)) {
            similarSongs.addAll(mediaFileService.getRandomSongsForParent(similarArtist, count));
        }
        Collections.shuffle(similarSongs);
        return similarSongs.subList(0, Math.min(count, similarSongs.size()));
    }

    /**
     * Returns artist bio and images.
     *
     * @param mediaFile The media file (song, album or artist).
     * @return Artist bio.
     */
    public ArtistBio getArtistBio(MediaFile mediaFile, Locale locale) {
        return getArtistBio(getCanonicalArtistName(getArtistName(mediaFile)), locale);
    }

    /**
     * Returns artist bio and images.
     *
     * @param artist The artist.
     * @return Artist bio.
     */
    public ArtistBio getArtistBio(org.airsonic.player.domain.Artist artist, Locale locale) {
        return getArtistBio(getCanonicalArtistName(artist.getName()), locale);
    }

    /**
     * Returns top songs for the given artist, using last.fm REST API.
     *
     * @param artist       The artist.
     * @param count        Max number of songs to return.
     * @param musicFolders Only return songs present in these folders.
     * @return Top songs for artist.
     */
    public List<MediaFile> getTopSongs(MediaFile artist, int count, List<MusicFolder> musicFolders) {
        return getTopSongs(artist.getName(), count, musicFolders);
    }

    /**
     * Returns top songs for the given artist, using last.fm REST API.
     *
     * @param artistName   The artist name.
     * @param count        Max number of songs to return.
     * @param musicFolders Only return songs present in these folders.
     * @return Top songs for artist.
     */
    public List<MediaFile> getTopSongs(String artistName, int count, List<MusicFolder> musicFolders) {
        try {
            if (StringUtils.isBlank(artistName) || count <= 0) {
                return Collections.emptyList();
            }

            List<MediaFile> result = new ArrayList<MediaFile>();
            for (Track topTrack : Artist.getTopTracks(artistName, LAST_FM_KEY)) {
                MediaFile song = mediaFileDao.getSongByArtistAndTitle(artistName, topTrack.getName(), musicFolders);
                if (song != null) {
                    result.add(song);
                    if (result.size() == count) {
                        return result;
                    }
                }
            }
            return result;
        } catch (Throwable x) {
            LOG.warn("Failed to find top songs for " + artistName, x);
            return Collections.emptyList();
        }
    }

    /**
     * Returns album notes and images.
     *
     * @param mediaFile The media file (song or album).
    * @return Album notes.
    */
    public AlbumNotes getAlbumNotes(MediaFile mediaFile) {
        return getAlbumNotes(getCanonicalArtistName(getArtistName(mediaFile)), mediaFile.getAlbumName());
    }

    /**
     * Returns album notes and images.
     *
     * @param album The album.
     * @return Album notes.
     */
    public AlbumNotes getAlbumNotes(org.airsonic.player.domain.Album album) {
        return getAlbumNotes(getCanonicalArtistName(album.getArtist()), album.getName());
    }

    /**
     * Returns album notes and images.
     *
     * @param artist The artist name.
     * @param album The album name.
     * @return Album notes.
     */
    private AlbumNotes getAlbumNotes(String artist, String album) {
        if (artist == null || album == null) {
            return null;
        }
        try {
            Album info = Album.getInfo(artist, album, LAST_FM_KEY);
            if (info == null) {
                return null;
            }
            return new AlbumNotes(processWikiText(info.getWikiSummary()),
                                 info.getMbid(),
                                 info.getUrl(),
                                 info.getImageURL(ImageSize.MEDIUM),
                                 info.getImageURL(ImageSize.LARGE),
                                 info.getImageURL(ImageSize.MEGA));
        } catch (Throwable x) {
            LOG.warn("Failed to find album notes for " + artist + " - " + album, x);
            return null;
        }
    }

    public List<LastFmCoverArt> searchCoverArt(String artist, String album) {
        if (artist == null && album == null) {
            return Collections.emptyList();
        }
        try {
            StringBuilder query = new StringBuilder();
            if (artist != null) {
                query.append(artist).append(" ");
            }
            if (album != null) {
                query.append(album);
            }

            Collection<Album> matches = Album.search(query.toString(), LAST_FM_KEY);
            return FluentIterable.from(matches)
                                 .transform(new Function<Album, LastFmCoverArt>() {
                                     @Override
                                     public LastFmCoverArt apply(Album album) {
                                         return convert(album);
                                     }
                                 })
                                 .filter(Predicates.notNull())
                                 .toList();
        } catch (Throwable x) {
            LOG.warn("Failed to search for cover art for " + artist + " - " + album, x);
            return Collections.emptyList();
        }
    }

    private LastFmCoverArt convert(Album album) {
        String imageUrl = null;
        for (ImageSize imageSize : Lists.reverse(Arrays.asList(ImageSize.values()))) {
            imageUrl = StringUtils.trimToNull(album.getImageURL(imageSize));
            if (imageUrl != null) {
                break;
            }
        }

        return imageUrl == null ? null : new LastFmCoverArt(imageUrl, album.getArtist(), album.getName());
    }


    private ArtistBio getArtistBio(String artistName, Locale locale) {
        try {
            if (artistName == null) {
                return null;
            }

            Artist info = Artist.getInfo(artistName, locale, null /* username */, LAST_FM_KEY);
            if (info == null) {
                return null;
            }
            return new ArtistBio(processWikiText(info.getWikiSummary()),
                                 info.getMbid(),
                                 info.getUrl(),
                                 info.getImageURL(ImageSize.MEDIUM),
                                 info.getImageURL(ImageSize.LARGE),
                                 info.getImageURL(ImageSize.MEGA));
        } catch (Throwable x) {
            LOG.warn("Failed to find artist bio for " + artistName, x);
            return null;
        }
    }

    private String getCanonicalArtistName(String artistName) {
        try {
            if (artistName == null) {
                return null;
            }

            Artist info = Artist.getInfo(artistName, LAST_FM_KEY);
            if (info == null) {
                return null;
            }

            String biography = processWikiText(info.getWikiSummary());
            String redirectedArtistName = getRedirectedArtist(biography);
            return redirectedArtistName != null ? redirectedArtistName : artistName;
        } catch (Throwable x) {
            LOG.warn("Failed to find artist bio for " + artistName, x);
            return null;
        }
    }

    private String getRedirectedArtist(String biography) {
        /*
         This is mistagged for <a target='_blank' href="http://www.last.fm/music/The+Boomtown+Rats" class="bbcode_artist">The Boomtown Rats</a>;
         it would help Last.fm if you could correct your tags.
         <a target='_blank' href="http://www.last.fm/music/+noredirect/Boomtown+Rats">Boomtown Rats on Last.fm</a>.

        -- or --

         Fix your tags to <a target='_blank' href="http://www.last.fm/music/The+Chemical+Brothers" class="bbcode_artist">The Chemical Brothers</a>
         <a target='_blank' href="http://www.last.fm/music/+noredirect/Chemical+Brothers">Chemical Brothers on Last.fm</a>.
        */

        if (biography == null) {
            return null;
        }
        Pattern pattern = Pattern.compile("((This is mistagged for)|(Fix your tags to)).*class=\"bbcode_artist\">(.*?)</a>");
        Matcher matcher = pattern.matcher(biography);
        if (matcher.find()) {
            return matcher.group(4);
        }
        return null;
    }

    private String processWikiText(String text) {
        /*
         System of a Down is an Armenian American <a href="http://www.last.fm/tag/alternative%20metal" class="bbcode_tag" rel="tag">alternative metal</a> band,
         formed in 1994 in Los Angeles, California, USA. All four members are of Armenian descent, and are widely known for their outspoken views expressed in
         many of their songs confronting the Armenian Genocide of 1915 by the Ottoman Empire and the ongoing War on Terror by the US government. The band
         consists of <a href="http://www.last.fm/music/Serj+Tankian" class="bbcode_artist">Serj Tankian</a> (vocals), Daron Malakian (vocals, guitar),
         Shavo Odadjian (bass, vocals) and John Dolmayan (drums).
         <a href="http://www.last.fm/music/System+of+a+Down">Read more about System of a Down on Last.fm</a>.
         User-contributed text is available under the Creative Commons By-SA License and may also be available under the GNU FDL.
         */

        text = text.replaceAll("User-contributed text.*", "");
        text = text.replaceAll("<a ", "<a target='_blank' ");
        text = text.replace("\n", " ");
        text = StringUtils.trimToNull(text);

        if (text != null && text.startsWith("This is an incorrect tag")) {
            return null;
        }

        return text;
    }

    private String getArtistName(MediaFile mediaFile) {
        String artistName = mediaFile.getName();
        if (mediaFile.isAlbum() || mediaFile.isFile()) {
            artistName = mediaFile.getAlbumArtist() != null ? mediaFile.getAlbumArtist() : mediaFile.getArtist();
        }
        return artistName;
    }

    public void setMediaFileDao(MediaFileDao mediaFileDao) {
        this.mediaFileDao = mediaFileDao;
    }

    public void setMediaFileService(MediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
    }

    public void setArtistDao(ArtistDao artistDao) {
        this.artistDao = artistDao;
    }
}
