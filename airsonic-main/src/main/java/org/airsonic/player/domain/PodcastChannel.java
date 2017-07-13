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
 * A Podcast channel. Each channel contain several episodes.
 *
 * @author Sindre Mehus
 * @see PodcastEpisode
 */
public class PodcastChannel {

    private Integer id;
    private String url;
    private String title;
    private String description;
    private String imageUrl;
    private PodcastStatus status;
    private String errorMessage;
    private Integer mediaFileId;

    public PodcastChannel(Integer id, String url, String title, String description, String imageUrl,
                          PodcastStatus status, String errorMessage) {
        this.id = id;
        this.url = url;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.status = status;
        this.errorMessage = errorMessage;
    }

    public PodcastChannel(String url) {
        this.url = url;
        status = PodcastStatus.NEW;
    }

    public Integer getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public PodcastStatus getStatus() {
        return status;
    }

    public void setStatus(PodcastStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void setMediaFileId(Integer mediaFileId) {
        this.mediaFileId = mediaFileId;
    }

    public Integer getMediaFileId() {
        return mediaFileId;
    }
}