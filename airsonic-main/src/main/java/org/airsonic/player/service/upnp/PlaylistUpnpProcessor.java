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

import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.Playlist;
import org.airsonic.player.service.PlaylistService;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.container.PlaylistContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Allen Petersen
 * @version $Id$
 */
@Component
public class PlaylistUpnpProcessor extends UpnpContentProcessor <Playlist, MediaFile> {
    @Autowired
    private PlaylistService playlistService;

    public PlaylistUpnpProcessor() {
        setRootId(DispatchingContentDirectory.CONTAINER_ID_PLAYLIST_PREFIX);
        setRootTitle("Playlists");
    }

    public Container createContainer(Playlist item) {
        PlaylistContainer container = new PlaylistContainer();
        container.setId(getRootId() + DispatchingContentDirectory.SEPARATOR + item.getId());
        container.setParentID(getRootId());
        container.setTitle(item.getName());
        container.setDescription(item.getComment());
        container.setChildCount(getPlaylistService().getFilesInPlaylist(item.getId()).size());

        return container;
    }

    public List<Playlist> getAllItems() {
        return getPlaylistService().getAllPlaylists();
    }

    public Playlist getItemById(String id) throws Exception {
        return getDispatcher().getPlaylistService().getPlaylist(Integer.parseInt(id));
    }

    public List<MediaFile> getChildren(Playlist item) {
        return getPlaylistService().getFilesInPlaylist(item.getId());
    }

    public void addChild(DIDLContent didl, MediaFile child) throws Exception {
        didl.addItem(getDispatchingContentDirectory().createItem(child));
    }

    public PlaylistService getPlaylistService() {
        return this.playlistService;
    }
    public void setPlaylistService(PlaylistService playlistService) {
        this.playlistService = playlistService;
    }

}
