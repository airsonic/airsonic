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
package net.sourceforge.subsonic.controller;

import org.springframework.web.servlet.mvc.SimpleFormController;
import net.sourceforge.subsonic.service.SettingsService;
import net.sourceforge.subsonic.service.PodcastService;
import net.sourceforge.subsonic.command.PodcastSettingsCommand;

import javax.servlet.http.HttpServletRequest;

/**
 * Controller for the page used to administrate the Podcast receiver.
 *
 * @author Sindre Mehus
 */
public class PodcastSettingsController extends SimpleFormController {

    private SettingsService settingsService;
    private PodcastService podcastService;

    protected Object formBackingObject(HttpServletRequest request) throws Exception {
        PodcastSettingsCommand command = new PodcastSettingsCommand();

        command.setInterval(String.valueOf(settingsService.getPodcastUpdateInterval()));
        command.setEpisodeRetentionCount(String.valueOf(settingsService.getPodcastEpisodeRetentionCount()));
        command.setEpisodeDownloadCount(String.valueOf(settingsService.getPodcastEpisodeDownloadCount()));
        command.setFolder(settingsService.getPodcastFolder());
        return command;
    }

    protected void doSubmitAction(Object comm) throws Exception {
        PodcastSettingsCommand command = (PodcastSettingsCommand) comm;
        command.setToast(true);

        settingsService.setPodcastUpdateInterval(Integer.parseInt(command.getInterval()));
        settingsService.setPodcastEpisodeRetentionCount(Integer.parseInt(command.getEpisodeRetentionCount()));
        settingsService.setPodcastEpisodeDownloadCount(Integer.parseInt(command.getEpisodeDownloadCount()));
        settingsService.setPodcastFolder(command.getFolder());
        settingsService.save();

        podcastService.schedule();
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setPodcastService(PodcastService podcastService) {
        this.podcastService = podcastService;
    }
}
