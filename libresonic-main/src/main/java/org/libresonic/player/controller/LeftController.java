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

 Copyright 2009 (C) Sindre Mehus
 */
package org.libresonic.player.controller;

import java.io.File;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.ParameterizableViewController;
import org.springframework.web.servlet.support.RequestContextUtils;

import org.libresonic.player.domain.InternetRadio;
import org.libresonic.player.domain.MediaLibraryStatistics;
import org.libresonic.player.domain.MusicFolder;
import org.libresonic.player.domain.MusicFolderContent;
import org.libresonic.player.domain.UserSettings;
import org.libresonic.player.service.MediaScannerService;
import org.libresonic.player.service.MusicIndexService;
import org.libresonic.player.service.PlayerService;
import org.libresonic.player.service.SecurityService;
import org.libresonic.player.service.SettingsService;
import org.libresonic.player.util.FileUtil;
import org.libresonic.player.util.StringUtil;

/**
 * Controller for the left index frame.
 *
 * @author Sindre Mehus
 */
public class LeftController extends ParameterizableViewController {

    // Update this time if you want to force a refresh in clients.
    private static final Calendar LAST_COMPATIBILITY_TIME = Calendar.getInstance();
    static {
        LAST_COMPATIBILITY_TIME.set(2012, Calendar.MARCH, 6, 0, 0, 0);
        LAST_COMPATIBILITY_TIME.set(Calendar.MILLISECOND, 0);
    }

    private MediaScannerService mediaScannerService;
    private SettingsService settingsService;
    private SecurityService securityService;
    private MusicIndexService musicIndexService;
    private PlayerService playerService;

    /**
     * Note: This class intentionally does not implement org.springframework.web.servlet.mvc.LastModified
     * as we don't need browser-side caching of left.jsp.  This method is only used by RESTController.
     */
    public long getLastModified(HttpServletRequest request) {
        saveSelectedMusicFolder(request);

        if (mediaScannerService.isScanning()) {
            return -1L;
        }

        long lastModified = LAST_COMPATIBILITY_TIME.getTimeInMillis();
        String username = securityService.getCurrentUsername(request);

        // When was settings last changed?
        lastModified = Math.max(lastModified, settingsService.getSettingsChanged());

        // When was music folder(s) on disk last changed?
        List<MusicFolder> allMusicFolders = settingsService.getMusicFoldersForUser(username);
        MusicFolder selectedMusicFolder = settingsService.getSelectedMusicFolder(username);
        if (selectedMusicFolder != null) {
            File file = selectedMusicFolder.getPath();
            lastModified = Math.max(lastModified, FileUtil.lastModified(file));
        } else {
            for (MusicFolder musicFolder : allMusicFolders) {
                File file = musicFolder.getPath();
                lastModified = Math.max(lastModified, FileUtil.lastModified(file));
            }
        }

        // When was music folder table last changed?
        for (MusicFolder musicFolder : allMusicFolders) {
            lastModified = Math.max(lastModified, musicFolder.getChanged().getTime());
        }

        // When was internet radio table last changed?
        for (InternetRadio internetRadio : settingsService.getAllInternetRadios()) {
            lastModified = Math.max(lastModified, internetRadio.getChanged().getTime());
        }

        // When was user settings last changed?
        UserSettings userSettings = settingsService.getUserSettings(username);
        lastModified = Math.max(lastModified, userSettings.getChanged().getTime());

        return lastModified;
    }

    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
        boolean musicFolderChanged = saveSelectedMusicFolder(request);
        Map<String, Object> map = new HashMap<String, Object>();

        MediaLibraryStatistics statistics = mediaScannerService.getStatistics();
        Locale locale = RequestContextUtils.getLocale(request);

        boolean refresh = ServletRequestUtils.getBooleanParameter(request, "refresh", false);
        if (refresh) {
            settingsService.clearMusicFolderCache();
        }

        String username = securityService.getCurrentUsername(request);
        List<MusicFolder> allMusicFolders = settingsService.getMusicFoldersForUser(username);
        MusicFolder selectedMusicFolder = settingsService.getSelectedMusicFolder(username);
        List<MusicFolder> musicFoldersToUse = selectedMusicFolder == null ? allMusicFolders : Arrays.asList(selectedMusicFolder);
        UserSettings userSettings = settingsService.getUserSettings(username);
        MusicFolderContent musicFolderContent = musicIndexService.getMusicFolderContent(musicFoldersToUse, refresh);

        map.put("player", playerService.getPlayer(request, response));
        map.put("scanning", mediaScannerService.isScanning());
        map.put("musicFolders", allMusicFolders);
        map.put("selectedMusicFolder", selectedMusicFolder);
        map.put("radios", settingsService.getAllInternetRadios());
        map.put("shortcuts", musicIndexService.getShortcuts(musicFoldersToUse));
        map.put("partyMode", userSettings.isPartyModeEnabled());
        map.put("organizeByFolderStructure", settingsService.isOrganizeByFolderStructure());
        map.put("musicFolderChanged", musicFolderChanged);

        if (statistics != null) {
            map.put("statistics", statistics);
            long bytes = statistics.getTotalLengthInBytes();
            long hours = statistics.getTotalDurationInSeconds() / 3600L;
            map.put("hours", hours);
            map.put("bytes", StringUtil.formatBytes(bytes, locale));
        }

        map.put("indexedArtists", musicFolderContent.getIndexedArtists());
        map.put("singleSongs", musicFolderContent.getSingleSongs());
        map.put("indexes", musicFolderContent.getIndexedArtists().keySet());
        map.put("user", securityService.getCurrentUser(request));

        ModelAndView result = super.handleRequestInternal(request, response);
        result.addObject("model", map);
        return result;
    }

    private boolean saveSelectedMusicFolder(HttpServletRequest request) {
        if (request.getParameter("musicFolderId") == null) {
            return false;
        }
        int musicFolderId = Integer.parseInt(request.getParameter("musicFolderId"));

        // Note: UserSettings.setChanged() is intentionally not called. This would break browser caching
        // of the left frame.
        UserSettings settings = settingsService.getUserSettings(securityService.getCurrentUsername(request));
        settings.setSelectedMusicFolderId(musicFolderId);
        settingsService.updateUserSettings(settings);

        return true;
    }

    public void setMediaScannerService(MediaScannerService mediaScannerService) {
        this.mediaScannerService = mediaScannerService;
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    public void setMusicIndexService(MusicIndexService musicIndexService) {
        this.musicIndexService = musicIndexService;
    }

    public void setPlayerService(PlayerService playerService) {
        this.playerService = playerService;
    }

}
