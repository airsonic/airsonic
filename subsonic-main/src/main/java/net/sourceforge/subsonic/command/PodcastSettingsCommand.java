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
package net.sourceforge.subsonic.command;

import net.sourceforge.subsonic.controller.PodcastSettingsController;

/**
 * Command used in {@link PodcastSettingsController}.
 *
 * @author Sindre Mehus
 */
public class PodcastSettingsCommand {

    private String interval;
    private String folder;
    private String episodeRetentionCount;
    private String episodeDownloadCount;
    private boolean toast;

    public String getInterval() {
        return interval;
    }

    public void setInterval(String interval) {
        this.interval = interval;
    }

    public String getFolder() {
        return folder;
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }

    public String getEpisodeRetentionCount() {
        return episodeRetentionCount;
    }

    public void setEpisodeRetentionCount(String episodeRetentionCount) {
        this.episodeRetentionCount = episodeRetentionCount;
    }

    public String getEpisodeDownloadCount() {
        return episodeDownloadCount;
    }

    public void setEpisodeDownloadCount(String episodeDownloadCount) {
        this.episodeDownloadCount = episodeDownloadCount;
    }

    public boolean isToast() {
        return toast;
    }

    public void setToast(boolean toast) {
        this.toast = toast;
    }
}
