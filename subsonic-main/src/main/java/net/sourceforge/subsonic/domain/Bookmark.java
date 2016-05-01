/*
 This file is part of Subsonic.

 Subsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Subsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Subsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2009 (C) Sindre Mehus
 */
package net.sourceforge.subsonic.domain;

import java.util.Date;

/**
 * A bookmark within a media file, for a given user.
 *
 * @author Sindre Mehus
 * @version $Id$
 */
public class Bookmark {

    private int id;
    private int mediaFileId;
    private long positionMillis;
    private String username;
    private String comment;
    private Date created;
    private Date changed;

    public Bookmark(int id, int mediaFileId, long positionMillis, String username, String comment, Date created, Date changed) {
        this.id = id;
        this.mediaFileId = mediaFileId;
        this.positionMillis = positionMillis;
        this.username = username;
        this.comment = comment;
        this.created = created;
        this.changed = changed;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getMediaFileId() {
        return mediaFileId;
    }

    public void setMediaFileId(int mediaFileId) {
        this.mediaFileId = mediaFileId;
    }

    public long getPositionMillis() {
        return positionMillis;
    }

    public void setPositionMillis(long positionMillis) {
        this.positionMillis = positionMillis;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getChanged() {
        return changed;
    }

    public void setChanged(Date changed) {
        this.changed = changed;
    }
}
