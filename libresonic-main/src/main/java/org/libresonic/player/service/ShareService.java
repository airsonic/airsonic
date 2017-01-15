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
package org.libresonic.player.service;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.libresonic.player.Logger;
import org.libresonic.player.dao.ShareDao;
import org.libresonic.player.domain.MediaFile;
import org.libresonic.player.domain.MusicFolder;
import org.libresonic.player.domain.Share;
import org.libresonic.player.domain.User;

import javax.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Provides services for sharing media.
 *
 * @author Sindre Mehus
 * @see Share
 */
public class ShareService {

    private static final Logger LOG = Logger.getLogger(ShareService.class);

    private ShareDao shareDao;
    private SecurityService securityService;
    private SettingsService settingsService;
    private MediaFileService mediaFileService;

    public List<Share> getAllShares() {
        return shareDao.getAllShares();
    }

    public List<Share> getSharesForUser(User user) {
        List<Share> result = new ArrayList<Share>();
        for (Share share : getAllShares()) {
            if (user.isAdminRole() || ObjectUtils.equals(user.getUsername(), share.getUsername())) {
                result.add(share);
            }
        }
        return result;
    }

    public Share getShareById(int id) {
        return shareDao.getShareById(id);
    }

    public Share getShareByName(String name) {
        return shareDao.getShareByName(name);
    }

    public List<MediaFile> getSharedFiles(int id, List<MusicFolder> musicFolders) {
        List<MediaFile> result = new ArrayList<MediaFile>();
        for (String path : shareDao.getSharedFiles(id, musicFolders)) {
            try {
                MediaFile mediaFile = mediaFileService.getMediaFile(path);
                if (mediaFile != null) {
                    result.add(mediaFile);
                }
            } catch (Exception x) {
                // Ignored
            }
        }
        return result;
    }

    public Share createShare(HttpServletRequest request, List<MediaFile> files) throws Exception {

        Share share = new Share();
        share.setName(RandomStringUtils.random(5, "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"));
        share.setCreated(new Date());
        share.setUsername(securityService.getCurrentUsername(request));

        Calendar expires = Calendar.getInstance();
        expires.add(Calendar.YEAR, 1);
        share.setExpires(expires.getTime());

        shareDao.createShare(share);
        for (MediaFile file : files) {
            shareDao.createSharedFiles(share.getId(), file.getPath());
        }
        LOG.info("Created share '" + share.getName() + "' with " + files.size() + " file(s).");

        return share;
    }

    public void updateShare(Share share) {
        shareDao.updateShare(share);
    }

    public void deleteShare(int id) {
        shareDao.deleteShare(id);
    }

    public String getShareBaseUrl() {
        return settingsService.getUrlRedirectUrl() + "/share/";
    }

    public String getShareUrl(Share share) {
        return getShareBaseUrl() + share.getName();
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    public void setShareDao(ShareDao shareDao) {
        this.shareDao = shareDao;
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setMediaFileService(MediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
    }
}