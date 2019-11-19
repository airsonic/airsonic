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
package org.airsonic.player.controller;

import com.github.biconou.AudioPlayer.AudioSystemUtils;
import org.airsonic.player.command.PlayerSettingsCommand;
import org.airsonic.player.domain.*;
import org.airsonic.player.service.PlayerService;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.TranscodingService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Controller for the player settings page.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/playerSettings")
public class PlayerSettingsController  {

    @Autowired
    private PlayerService playerService;
    @Autowired
    private SecurityService securityService;
    @Autowired
    private TranscodingService transcodingService;

    @GetMapping
    protected String displayForm() {
        return "playerSettings";
    }

    @ModelAttribute
    protected void formBackingObject(HttpServletRequest request, Model model) throws Exception {

        handleRequestParameters(request);
        List<Player> players = getPlayers(request);

        User user = securityService.getCurrentUser(request);
        PlayerSettingsCommand command = new PlayerSettingsCommand();
        Player player = null;
        Integer playerId = ServletRequestUtils.getIntParameter(request, "id");
        if (playerId != null) {
            player = playerService.getPlayerById(playerId);
        } else if (!players.isEmpty()) {
            player = players.get(0);
        }

        if (player != null) {
            command.setPlayerId(player.getId());
            command.setName(player.getName());
            command.setDescription(player.toString());
            command.setType(player.getType());
            command.setLastSeen(player.getLastSeen());
            command.setDynamicIp(player.isDynamicIp());
            command.setAutoControlEnabled(player.isAutoControlEnabled());
            command.setM3uBomEnabled(player.isM3uBomEnabled());
            command.setTranscodeSchemeName(player.getTranscodeScheme().name());
            command.setTechnologyName(player.getTechnology().name());
            command.setAllTranscodings(transcodingService.getAllTranscodings());
            List<Transcoding> activeTranscodings = transcodingService.getTranscodingsForPlayer(player);
            int[] activeTranscodingIds = new int[activeTranscodings.size()];
            for (int i = 0; i < activeTranscodings.size(); i++) {
                activeTranscodingIds[i] = activeTranscodings.get(i).getId();
            }
            command.setActiveTranscodingIds(activeTranscodingIds);
        }

        command.setTranscodingSupported(transcodingService.isDownsamplingSupported(null));
        command.setTranscodeDirectory(transcodingService.getTranscodeDirectory().getPath());
        command.setTranscodeSchemes(TranscodeScheme.values());
        command.setTechnologies(PlayerTechnology.values());
        command.setPlayers(players.toArray(new Player[players.size()]));
        command.setAdmin(user.isAdminRole());

        command.setJavaJukeboxMixers(Arrays.stream(AudioSystemUtils.listAllMixers()).map(info -> info.getName()).toArray(String[]::new));
        if (player != null) {
            command.setJavaJukeboxMixer(player.getJavaJukeboxMixer());
        }
        model.addAttribute("command",command);
    }

    @PostMapping
    protected String doSubmitAction(@ModelAttribute("command") PlayerSettingsCommand command, RedirectAttributes redirectAttributes) {
        Player player = playerService.getPlayerById(command.getPlayerId());
        if (player != null) {
            player.setAutoControlEnabled(command.isAutoControlEnabled());
            player.setM3uBomEnabled(command.isM3uBomEnabled());
            player.setDynamicIp(command.isDynamicIp());
            player.setName(StringUtils.trimToNull(command.getName()));
            player.setTranscodeScheme(TranscodeScheme.valueOf(command.getTranscodeSchemeName()));
            player.setTechnology(PlayerTechnology.valueOf(command.getTechnologyName()));
            player.setJavaJukeboxMixer(command.getJavaJukeboxMixer());

            playerService.updatePlayer(player);
            transcodingService.setTranscodingsForPlayer(player, command.getActiveTranscodingIds());

            redirectAttributes.addFlashAttribute("settings_reload", true);
            redirectAttributes.addFlashAttribute("settings_toast", true);
            return "redirect:playerSettings.view";
        } else {
            return "redirect:notFound";
        }
    }

    private List<Player> getPlayers(HttpServletRequest request) {
        User user = securityService.getCurrentUser(request);
        String username = user.getUsername();
        List<Player> players = playerService.getAllPlayers();
        List<Player> authorizedPlayers = new ArrayList<Player>();

        for (Player player : players) {
            // Only display authorized players.
            if (user.isAdminRole() || username.equals(player.getUsername())) {
                authorizedPlayers.add(player);
            }
        }
        return authorizedPlayers;
    }

    private void handleRequestParameters(HttpServletRequest request) throws Exception {
        if (request.getParameter("delete") != null) {
            playerService.removePlayerById(ServletRequestUtils.getIntParameter(request, "delete"));
        } else if (request.getParameter("clone") != null) {
            playerService.clonePlayer(ServletRequestUtils.getIntParameter(request, "clone"));
        }
    }

}
