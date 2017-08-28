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

import org.airsonic.player.domain.*;
import org.airsonic.player.service.MediaFileService;
import org.airsonic.player.service.PlaylistService;
import org.airsonic.player.util.Util;
import org.fourthline.cling.support.contentdirectory.ContentDirectoryErrorCode;
import org.fourthline.cling.support.contentdirectory.ContentDirectoryException;
import org.fourthline.cling.support.model.*;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.container.MusicAlbum;
import org.fourthline.cling.support.model.container.PlaylistContainer;
import org.fourthline.cling.support.model.container.StorageFolder;
import org.fourthline.cling.support.model.item.Item;
import org.fourthline.cling.support.model.item.MusicTrack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
@Service
public class FolderBasedContentDirectory extends CustomContentDirectory {

    private static final Logger LOG = LoggerFactory.getLogger(FolderBasedContentDirectory.class);
    private static final String CONTAINER_ID_PLAYLIST_ROOT = "playlists";
    private static final String CONTAINER_ID_PLAYLIST_PREFIX = "playlist-";
    private static final String CONTAINER_ID_FOLDER_PREFIX = "folder-";
    @Autowired
    private MediaFileService mediaFileService;
    @Autowired
    private PlaylistService playlistService;

    @Override
    public BrowseResult browse(String objectId, BrowseFlag browseFlag, String filter, long firstResult,
            long maxResults, SortCriterion[] orderby) throws ContentDirectoryException {

        LOG.info("UPnP request - objectId: " + objectId + ", browseFlag: " + browseFlag + ", filter: " + filter +
                ", firstResult: " + firstResult + ", maxResults: " + maxResults);

        // maxResult == 0 means all.
        if (maxResults == 0) {
            maxResults = Integer.MAX_VALUE;
        }

        try {
            if (CONTAINER_ID_ROOT.equals(objectId)) {
                return browseFlag == BrowseFlag.METADATA ? browseRootMetadata() : browseRoot(firstResult, maxResults);
            }
            if (CONTAINER_ID_PLAYLIST_ROOT.equals(objectId)) {
                return browseFlag == BrowseFlag.METADATA ? browsePlaylistRootMetadata() : browsePlaylistRoot(firstResult, maxResults);
            }
            if (objectId.startsWith(CONTAINER_ID_PLAYLIST_PREFIX)) {
                int playlistId = Integer.parseInt(objectId.replace(CONTAINER_ID_PLAYLIST_PREFIX, ""));
                Playlist playlist = playlistService.getPlaylist(playlistId);
                return browseFlag == BrowseFlag.METADATA ? browsePlaylistMetadata(playlist) : browsePlaylist(playlist, firstResult, maxResults);
            }

            int mediaFileId = Integer.parseInt(objectId.replace(CONTAINER_ID_FOLDER_PREFIX, ""));
            MediaFile mediaFile = mediaFileService.getMediaFile(mediaFileId);
            return browseFlag == BrowseFlag.METADATA ? browseMediaFileMetadata(mediaFile) : browseMediaFile(mediaFile, firstResult, maxResults);

        } catch (Throwable x) {
            LOG.error("UPnP error: " + x, x);
            throw new ContentDirectoryException(ContentDirectoryErrorCode.CANNOT_PROCESS, x.toString());
        }
    }

    private BrowseResult browseRootMetadata() throws Exception {
        StorageFolder root = new StorageFolder();
        root.setId(CONTAINER_ID_ROOT);
        root.setParentID("-1");

        MediaLibraryStatistics statistics = settingsService.getMediaLibraryStatistics();
        root.setStorageUsed(statistics == null ? 0 : statistics.getTotalLengthInBytes());
        root.setTitle("Airsonic Media");
        root.setRestricted(true);
        root.setSearchable(false);
        root.setWriteStatus(WriteStatus.NOT_WRITABLE);

        List<MusicFolder> musicFolders = settingsService.getAllMusicFolders();
        root.setChildCount(musicFolders.size() + 1);  // +1 for playlists

        DIDLContent didl = new DIDLContent();
        didl.addContainer(root);
        return createBrowseResult(didl, 1, 1);
    }

    private BrowseResult browsePlaylistRootMetadata() throws Exception {
        DIDLContent didl = new DIDLContent();
        didl.addContainer(createPlaylistRootContainer());
        return createBrowseResult(didl, 1, 1);
    }

    private BrowseResult browsePlaylistRoot(long firstResult, long maxResults) throws Exception {
        DIDLContent didl = new DIDLContent();
        List<Playlist> allPlaylists = playlistService.getAllPlaylists();
        List<Playlist> selectedPlaylists = Util.subList(allPlaylists, firstResult, maxResults);
        for (Playlist playlist : selectedPlaylists) {
            didl.addContainer(createPlaylistContainer(playlist));
        }

        return createBrowseResult(didl, selectedPlaylists.size(), allPlaylists.size());
    }

    private BrowseResult browsePlaylistMetadata(Playlist playlist) throws Exception {
        DIDLContent didl = new DIDLContent();
        didl.addContainer(createPlaylistContainer(playlist));
        return createBrowseResult(didl, 1, 1);
    }

    private BrowseResult browsePlaylist(Playlist playlist, long firstResult, long maxResults) throws Exception {
        List<MediaFile> allChildren = playlistService.getFilesInPlaylist(playlist.getId());
        List<MediaFile> selectedChildren = Util.subList(allChildren, firstResult, maxResults);

        DIDLContent didl = new DIDLContent();
        for (MediaFile child : selectedChildren) {
            addContainerOrItem(didl, child);
        }
        return createBrowseResult(didl, selectedChildren.size(), allChildren.size());
    }

    private BrowseResult browseRoot(long firstResult, long maxResults) throws Exception {
        DIDLContent didl = new DIDLContent();
        List<MusicFolder> allFolders = settingsService.getAllMusicFolders();
        List<MusicFolder> selectedFolders = Util.subList(allFolders, firstResult, maxResults);
        for (MusicFolder folder : selectedFolders) {
            MediaFile mediaFile = mediaFileService.getMediaFile(folder.getPath());
            addContainerOrItem(didl, mediaFile);
        }

        if (maxResults > selectedFolders.size()) {
            didl.addContainer(createPlaylistRootContainer());
        }

        return createBrowseResult(didl, (int) didl.getCount(), allFolders.size() + 1);
    }

    private BrowseResult browseMediaFileMetadata(MediaFile mediaFile) throws Exception {
        DIDLContent didl = new DIDLContent();
        didl.addContainer(createContainer(mediaFile));
        return createBrowseResult(didl, 1, 1);
    }

    private BrowseResult browseMediaFile(MediaFile mediaFile, long firstResult, long maxResults) throws Exception {
        List<MediaFile> allChildren = mediaFileService.getChildrenOf(mediaFile, true, true, true);
        List<MediaFile> selectedChildren = Util.subList(allChildren, firstResult, maxResults);

        DIDLContent didl = new DIDLContent();
        for (MediaFile child : selectedChildren) {
            addContainerOrItem(didl, child);
        }
        return createBrowseResult(didl, selectedChildren.size(), allChildren.size());
    }

    private void addContainerOrItem(DIDLContent didl, MediaFile mediaFile) throws Exception {
        if (mediaFile.isFile()) {
            didl.addItem(createItem(mediaFile));
        } else {
            didl.addContainer(createContainer(mediaFile));
        }
    }

    private Item createItem(MediaFile song) throws Exception {
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
        item.addProperty(new DIDLObject.Property.UPNP.ALBUM_ART_URI(getAlbumArtUrl(parent)));

        return item;
    }

    private Container createContainer(MediaFile mediaFile) throws Exception {
        Container container = mediaFile.isAlbum() ? createAlbumContainer(mediaFile) : new MusicAlbum();
        container.setId(CONTAINER_ID_FOLDER_PREFIX + mediaFile.getId());
        container.setTitle(mediaFile.getName());
        List<MediaFile> children = mediaFileService.getChildrenOf(mediaFile, true, true, false);
        container.setChildCount(children.size());

        container.setParentID(CONTAINER_ID_ROOT);
        if (!mediaFileService.isRoot(mediaFile)) {
            MediaFile parent = mediaFileService.getParentOf(mediaFile);
            if (parent != null) {
                container.setParentID(String.valueOf(parent.getId()));
            }
        }
        return container;
    }

    private Container createAlbumContainer(MediaFile album) throws Exception {
        MusicAlbum container = new MusicAlbum();
        container.setAlbumArtURIs(new URI[]{getAlbumArtUrl(album)});

        // TODO: correct artist?
        if (album.getArtist() != null) {
            container.setArtists(new PersonWithRole[]{new PersonWithRole(album.getArtist())});
        }
        container.setDescription(album.getComment());

        return container;
    }

    private Container createPlaylistRootContainer() {
        Container container = new StorageFolder();
        container.setId(CONTAINER_ID_PLAYLIST_ROOT);
        container.setTitle("Playlists");

        List<Playlist> playlists = playlistService.getAllPlaylists();
        container.setChildCount(playlists.size());
        container.setParentID(CONTAINER_ID_ROOT);
        return container;
    }

    private Container createPlaylistContainer(Playlist playlist) {
        PlaylistContainer container = new PlaylistContainer();
        container.setId(CONTAINER_ID_PLAYLIST_PREFIX + playlist.getId());
        container.setParentID(CONTAINER_ID_PLAYLIST_ROOT);
        container.setTitle(playlist.getName());
        container.setDescription(playlist.getComment());
        container.setChildCount(playlistService.getFilesInPlaylist(playlist.getId()).size());

        return container;
    }

    private URI getAlbumArtUrl(MediaFile album) throws URISyntaxException {
        return jwtSecurityService.addJWTToken(UriComponentsBuilder.fromUriString(getBaseUrl() + "/ext/coverArt.view")
                .queryParam("id", album.getId())
                .queryParam("size", CoverArtScheme.LARGE.getSize()))
                .build()
                .encode()
                .toUri();
    }

    public void setMediaFileService(MediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
    }

    public void setPlaylistService(PlaylistService playlistService) {
        this.playlistService = playlistService;
    }
}
