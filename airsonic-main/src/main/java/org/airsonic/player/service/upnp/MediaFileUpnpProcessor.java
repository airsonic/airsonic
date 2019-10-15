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

import org.airsonic.player.dao.MediaFileDao;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.service.MediaFileService;
import org.fourthline.cling.support.model.BrowseResult;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.DIDLObject;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.container.MusicAlbum;
import org.fourthline.cling.support.model.item.Item;
import org.fourthline.cling.support.model.item.MusicTrack;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Allen Petersen
 * @version $Id$
 */
@Service
public class MediaFileUpnpProcessor extends UpnpContentProcessor <MediaFile, MediaFile> {

    @Autowired
    MediaFileDao mediaFileDao;

    public MediaFileUpnpProcessor() {
        setRootId(DispatchingContentDirectory.CONTAINER_ID_FOLDER_PREFIX);
        setRootTitle("Folders");
    }

    @Override
    // overriding for the case of browsing a file
    public BrowseResult browseObjectMetadata(String id) throws Exception {
        MediaFile item = getItemById(id);
        DIDLContent didl = new DIDLContent();
        addChild(didl, item);
        return createBrowseResult(didl, 1, 1);
    }

    public Container createContainer(MediaFile item) {
        MusicAlbum container = new MusicAlbum();
        if (item.isAlbum()) {
            container.setAlbumArtURIs(new URI[] { getDispatcher().getAlbumProcessor().getAlbumArtURI(item.getId())});

            if (item.getArtist() != null) {
                container.setArtists(getDispatcher().getAlbumProcessor().getAlbumArtists(item.getArtist()));
            }
            container.setDescription(item.getComment());
        }
        container.setId(DispatchingContentDirectory.CONTAINER_ID_FOLDER_PREFIX + DispatchingContentDirectory.SEPARATOR + item.getId());
        container.setTitle(item.getName());
        List<MediaFile> children = getChildren(item);
        container.setChildCount(children.size());

        if (! getMediaFileService().isRoot(item)) {
            MediaFile parent = getMediaFileService().getParentOf(item);
            if (parent != null) {
                container.setParentID(String.valueOf(parent.getId()));
            }
        } else {
            container.setParentID(DispatchingContentDirectory.CONTAINER_ID_FOLDER_PREFIX);
        }
        return container;
    }

    public List<MediaFile> getAllItems() {
        List<MusicFolder> allFolders = getDispatcher().getSettingsService().getAllMusicFolders();
        List<MediaFile> returnValue = new ArrayList<MediaFile>();
        if (allFolders.size() == 1) {
            // if there's only one root folder just return it
            return getChildren(getMediaFileService().getMediaFile(allFolders.get(0).getPath()));
        } else {
            for (MusicFolder folder : allFolders) {
                returnValue.add(getMediaFileService().getMediaFile(folder.getPath()));
            }
        }
        return returnValue;
    }

    public MediaFile getItemById(String id) {
        return getMediaFileService().getMediaFile(Integer.parseInt(id));
    }

    public List<MediaFile> getChildren(MediaFile item) {
        List<MediaFile> children = getMediaFileService().getChildrenOf(item, true, true, true);
        children.sort((MediaFile o1, MediaFile o2) -> o1.getPath().replaceAll("\\W", "").compareToIgnoreCase(o2.getPath().replaceAll("\\W", "")));
        return children;
    }

    public void addItem(DIDLContent didl, MediaFile item) {
        if (item.isFile()) {
            didl.addItem(createItem(item));
        } else {
            didl.addContainer(createContainer(item));
        }
    }

    public void addChild(DIDLContent didl, MediaFile child) {
        if (child.isFile()) {
            didl.addItem(createItem(child));
        } else {
            didl.addContainer(createContainer(child));
        }
    }

    public Item createItem(MediaFile song) {
        MediaFile parent = getMediaFileService().getParentOf(song);
        MusicTrack item = new MusicTrack();
        item.setId(String.valueOf(song.getId()));
        item.setParentID(String.valueOf(parent.getId()));
        item.setTitle(song.getTitle());
        item.setAlbum(song.getAlbumName());
        if (song.getArtist() != null) {
            item.setArtists(getDispatcher().getAlbumProcessor().getAlbumArtists(song.getArtist()));
        }
        Integer year = song.getYear();
        if (year != null) {
            item.setDate(year + "-01-01");
        }
        item.setOriginalTrackNumber(song.getTrackNumber());
        if (song.getGenre() != null) {
            item.setGenres(new String[]{song.getGenre()});
        }
        item.setResources(Arrays.asList(getDispatcher().createResourceForSong(song)));
        item.setDescription(song.getComment());
        item.addProperty(new DIDLObject.Property.UPNP.ALBUM_ART_URI(getDispatcher().getAlbumProcessor().getAlbumArtURI(parent.getId())));

        return item;
    }

    public MediaFileService getMediaFileService() {
        return getDispatchingContentDirectory().getMediaFileService();
    }

    public MediaFileDao getMediaFileDao() {
        return mediaFileDao;
    }

    public void setMediaFileDao(MediaFileDao mediaFileDao) {
        this.mediaFileDao = mediaFileDao;
    }

}
