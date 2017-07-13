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
package org.airsonic.player.domain;

import java.util.Date;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
public class Artist {

    private int id;
    private String name;
    private String coverArtPath;
    private int albumCount;
    private Date lastScanned;
    private boolean present;
    private Integer folderId;

    public Artist() {
    }

    public Artist(int id, String name, String coverArtPath, int albumCount, Date lastScanned, boolean present, Integer folderId) {
        this.id = id;
        this.name = name;
        this.coverArtPath = coverArtPath;
        this.albumCount = albumCount;
        this.lastScanned = lastScanned;
        this.present = present;
        this.folderId = folderId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCoverArtPath() {
        return coverArtPath;
    }

    public void setCoverArtPath(String coverArtPath) {
        this.coverArtPath = coverArtPath;
    }

    public int getAlbumCount() {
        return albumCount;
    }

    public void setAlbumCount(int albumCount) {
        this.albumCount = albumCount;
    }

    public Date getLastScanned() {
        return lastScanned;
    }

    public void setLastScanned(Date lastScanned) {
        this.lastScanned = lastScanned;
    }

    public boolean isPresent() {
        return present;
    }

    public void setPresent(boolean present) {
        this.present = present;
    }

    public void setFolderId(Integer folderId) {
        this.folderId = folderId;
    }

    public Integer getFolderId() {
        return folderId;
    }
}
