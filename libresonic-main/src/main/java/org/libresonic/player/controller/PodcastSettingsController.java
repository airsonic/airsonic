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

 Copyright 2016 (C) Libresonic Authors
 Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
 */
package org.libresonic.player.controller;

import org.libresonic.player.command.PodcastSettingsCommand;
import org.libresonic.player.service.PodcastService;
import org.libresonic.player.service.SettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Controller for the page used to administrate the Podcast receiver.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/podcastSettings")
public class PodcastSettingsController {

    @Autowired
    private SettingsService settingsService;
    @Autowired
    private PodcastService podcastService;

    @RequestMapping(method = RequestMethod.GET)
    protected String formBackingObject(Model model) throws Exception {
        PodcastSettingsCommand command = new PodcastSettingsCommand();

        command.setInterval(String.valueOf(settingsService.getPodcastUpdateInterval()));
        command.setEpisodeRetentionCount(String.valueOf(settingsService.getPodcastEpisodeRetentionCount()));
        command.setEpisodeDownloadCount(String.valueOf(settingsService.getPodcastEpisodeDownloadCount()));
        command.setFolder(settingsService.getPodcastFolder());

        model.addAttribute("command",command);
        return "podcastSettings";
    }

    @RequestMapping(method = RequestMethod.POST)
    protected String doSubmitAction(@ModelAttribute PodcastSettingsCommand command, Model model) throws Exception {
        command.setToast(true);

        settingsService.setPodcastUpdateInterval(Integer.parseInt(command.getInterval()));
        settingsService.setPodcastEpisodeRetentionCount(Integer.parseInt(command.getEpisodeRetentionCount()));
        settingsService.setPodcastEpisodeDownloadCount(Integer.parseInt(command.getEpisodeDownloadCount()));
        settingsService.setPodcastFolder(command.getFolder());
        settingsService.save();

        podcastService.schedule();
        model.addAttribute("command",command);
        return "podcastSettings";
    }

}
