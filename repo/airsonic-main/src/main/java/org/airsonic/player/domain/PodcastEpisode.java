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
 * A Podcast episode belonging to a channel.
 *
 * @author Sindre Mehus
 * @see PodcastChannel
 */
public class PodcastEpisode {

    private Integer id;
    private Integer mediaFileId;
    private Integer channelId;
    private String url;
    private String path;
    private String title;
    private String description;
    private Date publishDate;
    private String duration;
    private Long bytesTotal;
    private Long bytesDownloaded;
    private PodcastStatus status;
    private String errorMessage;

    public PodcastEpisode(Integer id, Integer channelId, String url, String path, String title,
                          String description, Date publishDate, String duration, Long length, Long bytesDownloaded,
                          PodcastStatus status, String errorMessage) {
        this.id = id;
        this.channelId = channelId;
        this.url = url;
        this.path = path;
        this.title = title;
        this.description = description;
        this.publishDate = publishDate;
        this.duration = duration;
        this.bytesTotal = length;
        this.bytesDownloaded = bytesDownloaded;
        this.status = status;
        this.errorMessage = errorMessage;
    }

    public Integer getId() {
        return id;
    }

    public Integer getChannelId() {
        return channelId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
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

    public Date getPublishDate() {
        return publishDate;
    }

    public void setPublishDate(Date publishDate) {
        this.publishDate = publishDate;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public Long getBytesTotal() {
        return bytesTotal;
    }

    public void setBytesTotal(Long bytesTotal) {
        this.bytesTotal = bytesTotal;
    }

    public Long getBytesDownloaded() {
        return bytesDownloaded;
    }

    public Double getCompletionRate() {
        if (bytesTotal == null || bytesTotal == 0) {
            return null;
        }
        if (bytesDownloaded == null) {
            return 0.0;
        }

        double d = bytesDownloaded;
        double t = bytesTotal;
        return d / t;
    }

    public void setBytesDownloaded(Long bytesDownloaded) {
        this.bytesDownloaded = bytesDownloaded;
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

    public Integer getMediaFileId() {
        return mediaFileId;
    }

    public void setMediaFileId(Integer mediaFileId) {
        this.mediaFileId = mediaFileId;
    }
}
