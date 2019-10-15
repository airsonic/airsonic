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
package org.airsonic.player.service.upnp;

import org.airsonic.player.domain.CoverArtScheme;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.service.*;
import org.fourthline.cling.support.contentdirectory.ContentDirectoryErrorCode;
import org.fourthline.cling.support.contentdirectory.ContentDirectoryException;
import org.fourthline.cling.support.model.*;
import org.fourthline.cling.support.model.item.Item;
import org.fourthline.cling.support.model.item.MusicTrack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Arrays;

/**
 * @author Allen Petersen
 * @author Sindre Mehus
 * @version $Id$
 */
@Service
public class DispatchingContentDirectory extends CustomContentDirectory {

    private static final Logger LOG = LoggerFactory.getLogger(DispatchingContentDirectory.class);

    public static final String CONTAINER_ID_ROOT = "0";
    public static final String CONTAINER_ID_PLAYLIST_PREFIX = "playlist";
    public static final String CONTAINER_ID_FOLDER_PREFIX = "folder";
    public static final String CONTAINER_ID_ALBUM_PREFIX = "album";
    public static final String CONTAINER_ID_ARTIST_PREFIX = "artist";
    public static final String CONTAINER_ID_ARTISTALBUM_PREFIX = "artistalbum";
    public static final String CONTAINER_ID_GENRE_PREFIX = "genre";
    public static final String CONTAINER_ID_RECENT_PREFIX = "recent";

    protected static final String SEPARATOR = "-";

    @Autowired
    private PlaylistUpnpProcessor playlistProcessor;
    @Autowired
    private MediaFileUpnpProcessor mediaFileProcessor;
    //@Autowired can't autowire because of the subclassing :P
    @Autowired//first checks type then field name to autowire
    private AlbumUpnpProcessor albumUpnpProcessor;
    //@Autowired can't autowire because of the subclassing :P
    @Autowired//first checks type then field name to autowire
    private RecentAlbumUpnpProcessor recentAlbumUpnpProcessor;
    @Autowired
    private ArtistUpnpProcessor artistProcessor;
    @Autowired
    private GenreUpnpProcessor genreProcessor;
    @Autowired
    private RootUpnpProcessor rootProcessor;

    @Autowired
    private MediaFileService mediaFileService;
    @Autowired
    private PlaylistService playlistService;

    @Autowired
    private MusicIndexService musicIndexService;

    @Autowired
    private SearchService searchService;


    @Override
    public BrowseResult browse(String objectId, BrowseFlag browseFlag,
                               String filter, long firstResult,
                               long maxResults, SortCriterion[] orderBy)
        throws ContentDirectoryException {

        LOG.info("UPnP request - objectId: " + objectId + ", browseFlag: " + browseFlag + ", filter: " + filter + ", firstResult: " + firstResult + ", maxResults: " + maxResults);

        if (objectId == null)
            throw new ContentDirectoryException(ContentDirectoryErrorCode.CANNOT_PROCESS, "objectId is null");

        // maxResult == 0 means all.
        if (maxResults == 0) {
            maxResults = Long.MAX_VALUE;
        }

        BrowseResult returnValue = null;
        try {
            String[] splitId = objectId.split(SEPARATOR);
            String browseRoot = splitId[0];
            String itemId = splitId.length == 1 ? null : splitId[1];

            UpnpContentProcessor processor = findProcessor(browseRoot);
            if (processor == null) {
                // if it's null then assume it's a file, and that the id
                // is all that's there.
                itemId = browseRoot;
                processor = getMediaFileProcessor();
            }

            if (itemId == null) {
                returnValue = browseFlag == BrowseFlag.METADATA ? processor.browseRootMetadata() : processor.browseRoot(filter, firstResult, maxResults, orderBy);
            } else {
                returnValue = browseFlag == BrowseFlag.METADATA ? processor.browseObjectMetadata(itemId) : processor.browseObject(itemId, filter, firstResult, maxResults, orderBy);
            }
            return returnValue;
        } catch (Throwable x) {
            LOG.error("UPnP error: " + x, x);
            throw new ContentDirectoryException(ContentDirectoryErrorCode.CANNOT_PROCESS, x.toString());
        }
    }

    @Override
    public BrowseResult search(String containerId,
                               String searchCriteria, String filter,
                               long firstResult, long maxResults,
                               SortCriterion[] orderBy) throws ContentDirectoryException {
        // i don't see a parser for upnp search criteria anywhere, so this will
        // have to do
        String upnpClass = searchCriteria.replaceAll("^.*upnp:class\\s+[\\S]+\\s+\"([\\S]*)\".*$", "$1");
        String titleSearch = searchCriteria.replaceAll("^.*dc:title\\s+[\\S]+\\s+\"([\\S]*)\".*$", "$1");
        BrowseResult returnValue = null;
        if ("object.container.person.musicArtist".equalsIgnoreCase(upnpClass)) {
            returnValue = getArtistProcessor().searchByName(titleSearch, firstResult, maxResults, orderBy);
        } else if ("object.item.audioItem".equalsIgnoreCase(upnpClass)) {
            returnValue = getMediaFileProcessor().searchByName(titleSearch, firstResult, maxResults, orderBy);
        } else if ("object.container.album.musicAlbum".equalsIgnoreCase(upnpClass)) {
            returnValue = getAlbumProcessor().searchByName(titleSearch, firstResult, maxResults, orderBy);
        }

        return returnValue != null ? returnValue : super.search(containerId, searchCriteria, filter, firstResult, maxResults, orderBy);
    }


    private UpnpContentProcessor findProcessor(String type) {
        switch (type) {
            case CONTAINER_ID_ROOT:
                return getRootProcessor();
            case CONTAINER_ID_PLAYLIST_PREFIX:
                return getPlaylistProcessor();
            case CONTAINER_ID_FOLDER_PREFIX:
                return getMediaFileProcessor();
            case CONTAINER_ID_ALBUM_PREFIX:
                return getAlbumProcessor();
            case CONTAINER_ID_RECENT_PREFIX:
                return getRecentAlbumProcessor();
            case CONTAINER_ID_ARTIST_PREFIX:
                return getArtistProcessor();
            case CONTAINER_ID_GENRE_PREFIX:
                return getGenreProcessor();
        }
        return null;
    }

    public Item createItem(MediaFile song) {
        MediaFile parent = mediaFileService.getParentOf(song);
        MusicTrack item = new MusicTrack();
        item.setId(String.valueOf(song.getId()));
        item.setParentID(String.valueOf(parent.getId()));
        item.setTitle(song.getTitle());
        item.setAlbum(song.getAlbumName());
        if (song.getArtist() != null) {
            item.setArtists(new PersonWithRole[]{new PersonWithRole(song.getArtist())});
        }
        Integer year = song.getYear();
        if (year != null) {
            item.setDate(year + "-01-01");
        }
        item.setOriginalTrackNumber(song.getTrackNumber());
        if (song.getGenre() != null) {
            item.setGenres(new String[]{song.getGenre()});
        }
        item.setResources(Arrays.asList(createResourceForSong(song)));
        item.setDescription(song.getComment());
        item.addProperty(new DIDLObject.Property.UPNP.ALBUM_ART_URI(getAlbumArtUrl(parent.getId())));

        return item;
    }

    public URI getAlbumArtUrl(int id) {
        return jwtSecurityService.addJWTToken(UriComponentsBuilder.fromUriString(getBaseUrl() + "/ext/coverArt.view").queryParam("id", id).queryParam("size", CoverArtScheme.LARGE.getSize())).build().encode().toUri();
    }

    public PlaylistUpnpProcessor getPlaylistProcessor() {
        return playlistProcessor;
    }
    public void setPlaylistProcessor(PlaylistUpnpProcessor playlistProcessor) {
        this.playlistProcessor = playlistProcessor;
    }

    public MediaFileUpnpProcessor getMediaFileProcessor() {
        return mediaFileProcessor;
    }
    public void setMediaFileProcessor(MediaFileUpnpProcessor mediaFileProcessor) {
        this.mediaFileProcessor = mediaFileProcessor;
    }

    public AlbumUpnpProcessor getAlbumProcessor() {
        return albumUpnpProcessor;
    }
    public void setAlbumProcessor(AlbumUpnpProcessor albumProcessor) {
        this.albumUpnpProcessor = albumProcessor;
    }

    public RecentAlbumUpnpProcessor getRecentAlbumProcessor() {
        return recentAlbumUpnpProcessor;
    }
    public void setRecentAlbumProcessor(RecentAlbumUpnpProcessor recentAlbumProcessor) {
        this.recentAlbumUpnpProcessor = recentAlbumProcessor;
    }

    public ArtistUpnpProcessor getArtistProcessor() {
        return artistProcessor;
    }
    public void setArtistProcessor(ArtistUpnpProcessor artistProcessor) {
        this.artistProcessor = artistProcessor;
    }

    public GenreUpnpProcessor getGenreProcessor() {
        return genreProcessor;
    }
    public void setGenreProcessor(GenreUpnpProcessor genreProcessor) {
        this.genreProcessor = genreProcessor;
    }

    public RootUpnpProcessor getRootProcessor() {
        return rootProcessor;
    }
    public void setRootProcessor(RootUpnpProcessor rootProcessor) {
        this.rootProcessor = rootProcessor;
    }

    public MediaFileService getMediaFileService() {
        return mediaFileService;
    }
    public void setMediaFileService(MediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
    }

    public SettingsService getSettingsService() {
        return settingsService;
    }

    public PlaylistService getPlaylistService() {
        return playlistService;
    }
    public void setPlaylistService(PlaylistService playlistService) {
        this.playlistService = playlistService;
    }

    public JWTSecurityService getJwtSecurityService() {
        return jwtSecurityService;
    }

    public MusicIndexService getMusicIndexService() {
        return this.musicIndexService;
    }
    public void setMusicIndexService(MusicIndexService musicIndexService) {
        this.musicIndexService = musicIndexService;
    }

    public SearchService getSearchService() {
        return this.searchService;
    }
    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }
}
