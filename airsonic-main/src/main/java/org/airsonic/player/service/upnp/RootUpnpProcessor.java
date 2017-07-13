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

import org.airsonic.player.domain.MediaLibraryStatistics;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.WriteStatus;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.container.StorageFolder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Allen Petersen
 * @version $Id$
 */
@Component
public class RootUpnpProcessor extends UpnpContentProcessor <Container, Container> {
    public Container createRootContainer() {
        StorageFolder root = new StorageFolder();
        root.setId(DispatchingContentDirectory.CONTAINER_ID_ROOT);
        root.setParentID("-1");

        MediaLibraryStatistics statistics = getDispatchingContentDirectory().getSettingsService().getMediaLibraryStatistics();
        // returning large storageUsed values doesn't play nicely with
        // some upnp clients
        //root.setStorageUsed(statistics == null ? 0 : statistics.getTotalLengthInBytes());
        root.setStorageUsed(-1L);
        root.setTitle("Airsonic Media");
        root.setRestricted(true);
        root.setSearchable(true);
        root.setWriteStatus(WriteStatus.NOT_WRITABLE);

        root.setChildCount(6);
        return root;
    }

    public Container createContainer(Container item) {
        // the items are the containers in this case.
        return item;
    }

    public List<Container> getAllItems() throws Exception {
        ArrayList<Container> allItems = new ArrayList<Container>();
        allItems.add(getDispatchingContentDirectory().getAlbumProcessor().createRootContainer());
        allItems.add(getDispatchingContentDirectory().getArtistProcessor().createRootContainer());
        allItems.add(getDispatchingContentDirectory().getMediaFileProcessor().createRootContainer());
        allItems.add(getDispatchingContentDirectory().getGenreProcessor().createRootContainer());
        allItems.add(getDispatchingContentDirectory().getPlaylistProcessor().createRootContainer());
        allItems.add(getDispatchingContentDirectory().getRecentAlbumProcessor().createRootContainer());
        return allItems;
    }

    public Container getItemById(String id) {
        return createRootContainer();
    }

    public List<Container> getChildren(Container item) throws Exception {
        return getAllItems();
    }

    public void addChild(DIDLContent didl, Container child) {
        // special case; root doesn't have object instances
    }
}
