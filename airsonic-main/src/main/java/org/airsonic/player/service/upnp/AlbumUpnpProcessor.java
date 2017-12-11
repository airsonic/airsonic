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

  Copyright 2017 (C) Airsonic Authors
  Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
*/
package org.airsonic.player.service.upnp;

import org.airsonic.player.dao.AlbumDao;
import org.airsonic.player.dao.MediaFileDao;
import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.CoverArtScheme;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.service.SearchService;
import org.fourthline.cling.support.model.BrowseResult;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.PersonWithRole;
import org.fourthline.cling.support.model.SortCriterion;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.container.MusicAlbum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * @author Allen Petersen
 * @version $Id$
 */
@Service
public class AlbumUpnpProcessor extends UpnpContentProcessor <Album, MediaFile> {

    public static final String ALL_BY_ARTIST = "allByArtist";
    public static final String ALL_RECENT = "allRecent";

    @Autowired
    AlbumDao albumDao;

    @Autowired
    SearchService searchService;

    public AlbumUpnpProcessor() {
        setRootId(DispatchingContentDirectory.CONTAINER_ID_ALBUM_PREFIX);
        setRootTitle("Albums");
    }

    /**
     * Browses the top-level content of a type.
     */
    public BrowseResult browseRoot(String filter, long firstResult, long maxResults, SortCriterion[] orderBy) throws Exception {
        DIDLContent didl = new DIDLContent();

        List<MusicFolder> allFolders = getDispatchingContentDirectory().getSettingsService().getAllMusicFolders();
        List<Album> selectedItems = getAlbumDao().getAlphabeticalAlbums((int) firstResult, (int) maxResults, false, true, allFolders);
        for (Album item : selectedItems) {
            addItem(didl, item);
        }

        return createBrowseResult(didl, (int) didl.getCount(), getAllItemsSize());
    }
    public Container createContainer(Album album) throws Exception {
        MusicAlbum container = new MusicAlbum();

        if (album.getId() == -1) {
            container.setId(getRootId() + DispatchingContentDirectory.SEPARATOR + album.getComment());
        } else {
            container.setId(getRootId() + DispatchingContentDirectory.SEPARATOR + album.getId());
            container.setAlbumArtURIs(new URI[] { getAlbumArtURI(album.getId()) });
            container.setDescription(album.getComment());
        }
        container.setParentID(getRootId());
        container.setTitle(album.getName());
        // TODO: correct artist?
        if (album.getArtist() != null) {
            container.setArtists(getAlbumArtists(album.getArtist()));
        }
        return container;
    }

    public List<Album> getAllItems() {
        List<MusicFolder> allFolders = getDispatchingContentDirectory().getSettingsService().getAllMusicFolders();
        return getAlbumDao().getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, true, allFolders);
    }

    public Album getItemById(String id) throws Exception {
        Album returnValue = null;
        if (id.startsWith(ALL_BY_ARTIST) || id.equalsIgnoreCase(ALL_RECENT)) {
            returnValue = new Album();
            returnValue.setId(-1);
            returnValue.setComment(id);
        } else {
            returnValue = getAlbumDao().getAlbum(Integer.parseInt(id));
        }
        return returnValue;
    }

    public List<MediaFile> getChildren(Album album) throws Exception {
        List<MediaFile> allFiles = getMediaFileDao().getSongsForAlbum(album.getArtist(), album.getName());
        if (album.getId() == -1) {
            List<Album> albumList = null;
            if (album.getComment().startsWith(ALL_BY_ARTIST)) {
                ArtistUpnpProcessor ap = getDispatcher().getArtistProcessor();
                albumList =  ap.getChildren(ap.getItemById(album.getComment().replaceAll(ALL_BY_ARTIST + "_", "")));
            } else if (album.getComment().equalsIgnoreCase(ALL_RECENT)) {
                albumList = getDispatcher().getRecentAlbumProcessor().getAllItems();
            }
            for (Album a: albumList) {
                if (a.getId() != -1) {
                    allFiles.addAll(getMediaFileDao().getSongsForAlbum(a.getArtist(), a.getName()));
                }
            }
        } else {
            allFiles = getMediaFileDao().getSongsForAlbum(album.getArtist(), album.getName());
        }
        return allFiles;
    }

    @Override
    public int getAllItemsSize() throws Exception {
        List<MusicFolder> allFolders = getDispatchingContentDirectory().getSettingsService().getAllMusicFolders();
        return getAlbumDao().getAlbumCount(allFolders);
    }


    public void addChild(DIDLContent didl, MediaFile child) throws Exception {
        didl.addItem(getDispatcher().getMediaFileProcessor().createItem(child));
    }

    public URI getAlbumArtURI(int albumId) throws URISyntaxException {
        return getDispatcher().getJwtSecurityService().addJWTToken(UriComponentsBuilder.fromUriString(getDispatcher().getBaseUrl() + "/ext/coverArt.view").queryParam("id", albumId).queryParam("size", CoverArtScheme.LARGE.getSize())).build().encode().toUri();
    }

    public PersonWithRole[] getAlbumArtists(String artist) {
        return new PersonWithRole[] { new PersonWithRole(artist) };
    }

    public AlbumDao getAlbumDao() {
        return albumDao;
    }
    public void setAlbumDao(AlbumDao albumDao) {
        this.albumDao = albumDao;
    }

    public MediaFileDao getMediaFileDao() {
        return getDispatcher().getMediaFileProcessor().getMediaFileDao();
    }

}
