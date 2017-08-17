package org.airsonic.restapi.domain;

import javax.xml.bind.annotation.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "User", propOrder = {
        "folder"
})
public class User {

    @XmlElement(type = Integer.class)
    protected List<Integer> folder;
    @XmlAttribute(name = "username", required = true)
    protected String username;
    @XmlAttribute(name = "email")
    protected String email;
    @XmlAttribute(name = "scrobblingEnabled", required = true)
    protected boolean scrobblingEnabled;
    @XmlAttribute(name = "maxBitRate")
    protected Integer maxBitRate;
    @XmlAttribute(name = "adminRole", required = true)
    protected boolean adminRole;
    @XmlAttribute(name = "settingsRole", required = true)
    protected boolean settingsRole;
    @XmlAttribute(name = "downloadRole", required = true)
    protected boolean downloadRole;
    @XmlAttribute(name = "uploadRole", required = true)
    protected boolean uploadRole;
    @XmlAttribute(name = "playlistRole", required = true)
    protected boolean playlistRole;
    @XmlAttribute(name = "coverArtRole", required = true)
    protected boolean coverArtRole;
    @XmlAttribute(name = "commentRole", required = true)
    protected boolean commentRole;
    @XmlAttribute(name = "podcastRole", required = true)
    protected boolean podcastRole;
    @XmlAttribute(name = "streamRole", required = true)
    protected boolean streamRole;
    @XmlAttribute(name = "jukeboxRole", required = true)
    protected boolean jukeboxRole;
    @XmlAttribute(name = "shareRole", required = true)
    protected boolean shareRole;
    @XmlAttribute(name = "videoConversionRole", required = true)
    protected boolean videoConversionRole;
    @XmlAttribute(name = "avatarLastChanged")
    @XmlSchemaType(name = "dateTime")
    protected Date avatarLastChanged;

    public List<Integer> getFolder() {
        if (folder == null) {
            folder = new ArrayList<>();
        }
        return this.folder;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String value) {
        this.username = value;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String value) {
        this.email = value;
    }

    public boolean isScrobblingEnabled() {
        return scrobblingEnabled;
    }

    public void setScrobblingEnabled(boolean value) {
        this.scrobblingEnabled = value;
    }

    public Integer getMaxBitRate() {
        return maxBitRate;
    }

    public void setMaxBitRate(Integer value) {
        this.maxBitRate = value;
    }

    public boolean isAdminRole() {
        return adminRole;
    }

    public void setAdminRole(boolean value) {
        this.adminRole = value;
    }

    public boolean isSettingsRole() {
        return settingsRole;
    }

    public void setSettingsRole(boolean value) {
        this.settingsRole = value;
    }

    public boolean isDownloadRole() {
        return downloadRole;
    }

    public void setDownloadRole(boolean value) {
        this.downloadRole = value;
    }

    public boolean isUploadRole() {
        return uploadRole;
    }

    public void setUploadRole(boolean value) {
        this.uploadRole = value;
    }

    public boolean isPlaylistRole() {
        return playlistRole;
    }

    public void setPlaylistRole(boolean value) {
        this.playlistRole = value;
    }

    public boolean isCoverArtRole() {
        return coverArtRole;
    }

    public void setCoverArtRole(boolean value) {
        this.coverArtRole = value;
    }

    public boolean isCommentRole() {
        return commentRole;
    }

    public void setCommentRole(boolean value) {
        this.commentRole = value;
    }

    public boolean isPodcastRole() {
        return podcastRole;
    }

    public void setPodcastRole(boolean value) {
        this.podcastRole = value;
    }

    public boolean isStreamRole() {
        return streamRole;
    }

    public void setStreamRole(boolean value) {
        this.streamRole = value;
    }

    public boolean isJukeboxRole() {
        return jukeboxRole;
    }

    public void setJukeboxRole(boolean value) {
        this.jukeboxRole = value;
    }

    public boolean isShareRole() {
        return shareRole;
    }

    public void setShareRole(boolean value) {
        this.shareRole = value;
    }

    public boolean isVideoConversionRole() {
        return videoConversionRole;
    }

    public void setVideoConversionRole(boolean value) {
        this.videoConversionRole = value;
    }

    public Date getAvatarLastChanged() {
        return avatarLastChanged;
    }

    public void setAvatarLastChanged(Date value) {
        this.avatarLastChanged = value;
    }

}
