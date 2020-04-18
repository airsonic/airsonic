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

/**
 * Represent a user.
 *
 * @author Sindre Mehus
 */
public class User {

    public static final String USERNAME_GUEST = "guest";

    private final String username;
    private String password;
    private String email;
    private boolean ldapAuthenticated;
    private long bytesStreamed;
    private long bytesDownloaded;
    private long bytesUploaded;

    private boolean isAdminRole;
    private boolean isSettingsRole;
    private boolean isDownloadRole;
    private boolean isUploadRole;
    private boolean isPlaylistRole;
    private boolean isCoverArtRole;
    private boolean isCommentRole;
    private boolean isPodcastRole;
    private boolean isStreamRole;
    private boolean isJukeboxRole;
    private boolean isShareRole;

    public User(String username, String password, String email, boolean ldapAuthenticated,
                long bytesStreamed, long bytesDownloaded, long bytesUploaded) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.ldapAuthenticated = ldapAuthenticated;
        this.bytesStreamed = bytesStreamed;
        this.bytesDownloaded = bytesDownloaded;
        this.bytesUploaded = bytesUploaded;
    }

    public User(String username, String password, String email) {
        this(username, password, email, false, 0, 0, 0);
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isLdapAuthenticated() {
        return ldapAuthenticated;
    }

    public void setLdapAuthenticated(boolean ldapAuthenticated) {
        this.ldapAuthenticated = ldapAuthenticated;
    }

    public long getBytesStreamed() {
        return bytesStreamed;
    }

    public void setBytesStreamed(long bytesStreamed) {
        this.bytesStreamed = bytesStreamed;
    }

    public long getBytesDownloaded() {
        return bytesDownloaded;
    }

    public void setBytesDownloaded(long bytesDownloaded) {
        this.bytesDownloaded = bytesDownloaded;
    }

    public long getBytesUploaded() {
        return bytesUploaded;
    }

    public void setBytesUploaded(long bytesUploaded) {
        this.bytesUploaded = bytesUploaded;
    }

    public boolean isAdminRole() {
        return isAdminRole;
    }

    public void setAdminRole(boolean isAdminRole) {
        this.isAdminRole = isAdminRole;
    }

    public boolean isSettingsRole() {
        return isAdminRole || isSettingsRole;
    }

    public void setSettingsRole(boolean isSettingsRole) {
        this.isSettingsRole = isSettingsRole;
    }

    public boolean isCommentRole() {
        return isCommentRole;
    }

    public void setCommentRole(boolean isCommentRole) {
        this.isCommentRole = isCommentRole;
    }

    public boolean isDownloadRole() {
        return isDownloadRole;
    }

    public void setDownloadRole(boolean isDownloadRole) {
        this.isDownloadRole = isDownloadRole;
    }

    public boolean isUploadRole() {
        return isUploadRole;
    }

    public void setUploadRole(boolean isUploadRole) {
        this.isUploadRole = isUploadRole;
    }

    public boolean isPlaylistRole() {
        return isPlaylistRole;
    }

    public void setPlaylistRole(boolean isPlaylistRole) {
        this.isPlaylistRole = isPlaylistRole;
    }

    public boolean isCoverArtRole() {
        return isCoverArtRole;
    }

    public void setCoverArtRole(boolean isCoverArtRole) {
        this.isCoverArtRole = isCoverArtRole;
    }

    public boolean isPodcastRole() {
        return isPodcastRole;
    }

    public void setPodcastRole(boolean isPodcastRole) {
        this.isPodcastRole = isPodcastRole;
    }

    public boolean isStreamRole() {
        return isStreamRole;
    }

    public void setStreamRole(boolean streamRole) {
        isStreamRole = streamRole;
    }

    public boolean isJukeboxRole() {
        return isJukeboxRole;
    }

    public void setJukeboxRole(boolean jukeboxRole) {
        isJukeboxRole = jukeboxRole;
    }

    public boolean isShareRole() {
        return isShareRole;
    }

    public void setShareRole(boolean shareRole) {
        isShareRole = shareRole;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(username);

        if (isAdminRole) {
            result.append(" [admin]");
        }
        if (isSettingsRole) {
            result.append(" [settings]");
        }
        if (isDownloadRole) {
            result.append(" [download]");
        }
        if (isUploadRole) {
            result.append(" [upload]");
        }
        if (isPlaylistRole) {
            result.append(" [playlist]");
        }
        if (isCoverArtRole) {
            result.append(" [coverart]");
        }
        if (isCommentRole) {
            result.append(" [comment]");
        }
        if (isPodcastRole) {
            result.append(" [podcast]");
        }
        if (isStreamRole) {
            result.append(" [stream]");
        }
        if (isJukeboxRole) {
            result.append(" [jukebox]");
        }
        if (isShareRole) {
            result.append(" [share]");
        }

        return result.toString();
    }
}
