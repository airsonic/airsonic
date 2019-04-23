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

import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.Share;
import org.airsonic.player.domain.User;
import org.airsonic.player.service.MediaFileService;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.service.ShareService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;

import java.util.*;

/**
 * Controller for the page used to administrate the set of shared media.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/shareSettings")
public class ShareSettingsController {

    @Autowired
    private ShareService shareService;
    @Autowired
    private SecurityService securityService;
    @Autowired
    private MediaFileService mediaFileService;
    @Autowired
    private SettingsService settingsService;


    @GetMapping
    public String doGet(HttpServletRequest request, Model model) throws Exception {

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("shareInfos", getShareInfos(request));
        map.put("user", securityService.getCurrentUser(request));

        model.addAttribute("model", map);
        return "shareSettings";
    }

    @PostMapping
    public String doPost(HttpServletRequest request, RedirectAttributes redirectAttributes) throws Exception {
        handleParameters(request);

        redirectAttributes.addFlashAttribute("settings_toast", true);

        return "redirect:shareSettings.view";
    }

    private void handleParameters(HttpServletRequest request) {
        User user = securityService.getCurrentUser(request);
        for (Share share : shareService.getSharesForUser(user)) {
            int id = share.getId();

            String description = getParameter(request, "description", id);
            boolean delete = getParameter(request, "delete", id) != null;
            String expireIn = getParameter(request, "expireIn", id);

            if (delete) {
                shareService.deleteShare(id);
            } else {
                if (expireIn != null) {
                    share.setExpires(parseExpireIn(expireIn));
                }
                share.setDescription(description);
                shareService.updateShare(share);
            }
        }

        boolean deleteExpired = ServletRequestUtils.getBooleanParameter(request, "deleteExpired", false);
        if (deleteExpired) {
            Date now = new Date();
            for (Share share : shareService.getSharesForUser(user)) {
                Date expires = share.getExpires();
                if (expires != null && expires.before(now)) {
                    shareService.deleteShare(share.getId());
                }
            }
        }
    }

    private List<ShareInfo> getShareInfos(HttpServletRequest request) {
        List<ShareInfo> result = new ArrayList<ShareInfo>();
        User user = securityService.getCurrentUser(request);
        List<MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(user.getUsername());

        for (Share share : shareService.getSharesForUser(user)) {
            List<MediaFile> files = shareService.getSharedFiles(share.getId(), musicFolders);
            if (!files.isEmpty()) {
                MediaFile file = files.get(0);
                result.add(new ShareInfo(shareService.getShareUrl(request, share), share, file.isDirectory() ? file :
                        mediaFileService
                        .getParentOf(file)));
            }
        }
        return result;
    }


    private String getParameter(HttpServletRequest request, String name, int id) {
        return StringUtils.trimToNull(request.getParameter(name + "[" + id + "]"));
    }

    private Date parseExpireIn(String expireIn) {
        int days = Integer.parseInt(expireIn);
        if (days == 0) {
            return null;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, days);
        return calendar.getTime();
    }

    public static class ShareInfo {
        private final String shareUrl;
        private final Share share;
        private final MediaFile dir;

        public ShareInfo(String shareUrl, Share share, MediaFile dir) {
            this.shareUrl = shareUrl;
            this.share = share;
            this.dir = dir;
        }

        public Share getShare() {
            return share;
        }

        public String getShareUrl() {
            return shareUrl;
        }

        public MediaFile getDir() {
            return dir;
        }
    }
}
