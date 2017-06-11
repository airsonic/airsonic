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

  Copyright 2017 (C) Libresonic Authors
  Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
*/
package org.libresonic.player.service.upnp;
import org.libresonic.player.domain.Album;
import org.libresonic.player.domain.MusicFolder;

import java.util.List;

/**
 * @author Allen Petersen
 * @version $Id$
 */
public class RecentAlbumUpnpProcessor extends AlbumUpnpProcessor {
    private final static int RECENT_COUNT = 50;

    public RecentAlbumUpnpProcessor() {
        setRootId(DispatchingContentDirectory.CONTAINER_ID_RECENT_PREFIX);
        setRootTitle("RecentAlbums");
    }

    public List<Album> getAllItems() {
        List<MusicFolder> allFolders = getDispatchingContentDirectory().getSettingsService().getAllMusicFolders();
        List<Album> recentAlbums = getAlbumDao().getNewestAlbums(0, RECENT_COUNT, allFolders);
        if (recentAlbums.size() > 1) {
            // if there is more than one recent album, add in an option to
            // view the tracks in all the recent albums together
            Album viewAll = new Album();
            viewAll.setName("- All Albums -");
            viewAll.setId(-1);
            viewAll.setComment(AlbumUpnpProcessor.ALL_RECENT);
            recentAlbums.add(0, viewAll);
        }
        return recentAlbums;
    }
}
