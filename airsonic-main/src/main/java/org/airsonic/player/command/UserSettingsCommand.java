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
package org.airsonic.player.command;

import org.airsonic.player.controller.UserSettingsController;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.TranscodeScheme;
import org.airsonic.player.domain.User;

import java.util.List;

/**
 * Command used in {@link UserSettingsController}.
 *
 * @author Sindre Mehus
 */
public class UserSettingsCommand {
    private String username;
    private boolean isAdminRole;
    private boolean isDownloadRole;
    private boolean isUploadRole;
    private boolean isCoverArtRole;
    private boolean isCommentRole;
    private boolean isPodcastRole;
    private boolean isStreamRole;
    private boolean isJukeboxRole;
    private boolean isSettingsRole;
    private boolean isShareRole;

    private List<User> users;
    private boolean isAdmin;
    private boolean isPasswordChange;
    private boolean isNewUser;
    private boolean isDeleteUser;
    private String password;
    private String confirmPassword;
    private String email;
    private boolean isLdapAuthenticated;
    private boolean isLdapEnabled;
    private List<MusicFolder> allMusicFolders;
    private int[] allowedMusicFolderIds;

    private String transcodeSchemeName;
    private EnumHolder[] transcodeSchemeHolders;
    private boolean transcodingSupported;
    private String transcodeDirectory;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isAdminRole() {
        return isAdminRole;
    }

    public void setAdminRole(boolean adminRole) {
        isAdminRole = adminRole;
    }

    public boolean isDownloadRole() {
        return isDownloadRole;
    }

    public void setDownloadRole(boolean downloadRole) {
        isDownloadRole = downloadRole;
    }

    public boolean isUploadRole() {
        return isUploadRole;
    }

    public void setUploadRole(boolean uploadRole) {
        isUploadRole = uploadRole;
    }

    public boolean isCoverArtRole() {
        return isCoverArtRole;
    }

    public void setCoverArtRole(boolean coverArtRole) {
        isCoverArtRole = coverArtRole;
    }

    public boolean isCommentRole() {
        return isCommentRole;
    }

    public void setCommentRole(boolean commentRole) {
        isCommentRole = commentRole;
    }

    public boolean isPodcastRole() {
        return isPodcastRole;
    }

    public void setPodcastRole(boolean podcastRole) {
        isPodcastRole = podcastRole;
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

    public boolean isSettingsRole() {
        return isSettingsRole;
    }

    public void setSettingsRole(boolean settingsRole) {
        isSettingsRole = settingsRole;
    }

    public boolean isShareRole() {
        return isShareRole;
    }

    public void setShareRole(boolean shareRole) {
        isShareRole = shareRole;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }

    public boolean isPasswordChange() {
        return isPasswordChange;
    }

    public void setPasswordChange(boolean passwordChange) {
        isPasswordChange = passwordChange;
    }

    public boolean isNewUser() {
        return isNewUser;
    }

    public void setNewUser(boolean isNewUser) {
        this.isNewUser = isNewUser;
    }

    public boolean isDeleteUser() {
        return isDeleteUser;
    }

    public void setDeleteUser(boolean deleteUser) {
        this.isDeleteUser = deleteUser;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isLdapAuthenticated() {
        return isLdapAuthenticated;
    }

    public void setLdapAuthenticated(boolean ldapAuthenticated) {
        isLdapAuthenticated = ldapAuthenticated;
    }

    public boolean isLdapEnabled() {
        return isLdapEnabled;
    }

    public void setLdapEnabled(boolean ldapEnabled) {
        isLdapEnabled = ldapEnabled;
    }

    public List<MusicFolder> getAllMusicFolders() {
        return allMusicFolders;
    }

    public void setAllMusicFolders(List<MusicFolder> allMusicFolders) {
        this.allMusicFolders = allMusicFolders;
    }

    public int[] getAllowedMusicFolderIds() {
        return allowedMusicFolderIds;
    }

    public void setAllowedMusicFolderIds(int[] allowedMusicFolderIds) {
        this.allowedMusicFolderIds = allowedMusicFolderIds;
    }

    public String getTranscodeSchemeName() {
        return transcodeSchemeName;
    }

    public void setTranscodeSchemeName(String transcodeSchemeName) {
        this.transcodeSchemeName = transcodeSchemeName;
    }

    public EnumHolder[] getTranscodeSchemeHolders() {
        return transcodeSchemeHolders;
    }

    public void setTranscodeSchemes(TranscodeScheme[] transcodeSchemes) {
        transcodeSchemeHolders = new EnumHolder[transcodeSchemes.length];
        for (int i = 0; i < transcodeSchemes.length; i++) {
            TranscodeScheme scheme = transcodeSchemes[i];
            transcodeSchemeHolders[i] = new EnumHolder(scheme.name(), scheme.toString());
        }
    }

    public boolean isTranscodingSupported() {
        return transcodingSupported;
    }

    public void setTranscodingSupported(boolean transcodingSupported) {
        this.transcodingSupported = transcodingSupported;
    }

    public String getTranscodeDirectory() {
        return transcodeDirectory;
    }

    public void setTranscodeDirectory(String transcodeDirectory) {
        this.transcodeDirectory = transcodeDirectory;
    }

    public void setUser(User user) {
        username = user == null ? null : user.getUsername();
        isAdminRole = user != null && user.isAdminRole();
        isDownloadRole = user != null && user.isDownloadRole();
        isUploadRole = user != null && user.isUploadRole();
        isCoverArtRole = user != null && user.isCoverArtRole();
        isCommentRole = user != null && user.isCommentRole();
        isPodcastRole = user != null && user.isPodcastRole();
        isStreamRole = user != null && user.isStreamRole();
        isJukeboxRole = user != null && user.isJukeboxRole();
        isSettingsRole = user != null && user.isSettingsRole();
        isShareRole = user != null && user.isShareRole();
        isLdapAuthenticated = user != null && user.isLdapAuthenticated();
        setNewUser(false);
    }

}