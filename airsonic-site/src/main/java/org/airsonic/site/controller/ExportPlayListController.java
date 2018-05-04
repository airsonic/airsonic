package org.airsonic.site.controller;

import org.airsonic.player.domain.Playlist;
import org.airsonic.player.service.PlaylistService;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Spring MVC Controller that serves the login page.
 */
@Controller
@RequestMapping("/exportPlaylist")
public class ExportPlayListController {


    private static final Logger LOG = LoggerFactory.getLogger(ExportPlayListController.class);

    @Autowired
    private PlaylistService playlistService;
    @Autowired
    private SecurityService securityService;

    @RequestMapping(method = { RequestMethod.GET })
    public ModelAndView exportPlaylist(HttpServletRequest request, HttpServletResponse response) throws Exception {

        int id = ServletRequestUtils.getRequiredIntParameter(request, "id");
        Playlist playlist = playlistService.getPlaylist(id);
        if (!playlistService.isReadAllowed(playlist, securityService.getCurrentUsername(request))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return null;

        }
        response.setContentType("application/x-download");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + StringUtil.fileSystemSafe(playlist.getName()) + ".m3u8\"");

        playlistService.exportPlaylist(id, response.getOutputStream());
        return null;
    }


}
