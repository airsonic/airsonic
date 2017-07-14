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

import org.airsonic.player.domain.*;
import org.airsonic.player.service.*;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for the main page.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/main")
public class MainController  {

    @Autowired
    private SecurityService securityService;
    @Autowired
    private PlayerService playerService;
    @Autowired
    private SettingsService settingsService;
    @Autowired
    private RatingService ratingService;
    @Autowired
    private MediaFileService mediaFileService;

    @RequestMapping(method = RequestMethod.GET)
    protected ModelAndView handleRequestInternal(@RequestParam(name = "showAll", required = false) Boolean showAll,
                                                 HttpServletRequest request,
                                                 HttpServletResponse response) throws Exception {
        Map<String, Object> map = new HashMap<>();

        Player player = playerService.getPlayer(request, response);
        List<MediaFile> mediaFiles = getMediaFiles(request);

        if (mediaFiles.isEmpty()) {
            return new ModelAndView(new RedirectView("notFound.view"));
        }

        MediaFile dir = mediaFiles.get(0);
        if (dir.isFile()) {
            dir = mediaFileService.getParentOf(dir);
        }

        // Redirect if root directory.
        if (mediaFileService.isRoot(dir)) {
            return new ModelAndView(new RedirectView("home.view?"));
        }

        String username = securityService.getCurrentUsername(request);
        if (!securityService.isFolderAccessAllowed(dir, username)) {
            return new ModelAndView(new RedirectView("accessDenied.view"));
        }

        UserSettings userSettings = settingsService.getUserSettings(username);

        List<MediaFile> children = mediaFiles.size() == 1 ? mediaFileService.getChildrenOf(dir,
                true,
                true,
                true) : getMultiFolderChildren(mediaFiles);
        List<MediaFile> files = new ArrayList<>();
        List<MediaFile> subDirs = new ArrayList<>();
        for (MediaFile child : children) {
            if (child.isFile()) {
                files.add(child);
            } else {
                subDirs.add(child);
            }
        }

        int userPaginationPreference = userSettings.getPaginationSize();

        if(userPaginationPreference <= 0) {
            showAll = true;
        }

        boolean thereIsMoreSubDirs = trimToSize(showAll, subDirs, userPaginationPreference);
        boolean thereIsMoreSAlbums = false;

        mediaFileService.populateStarredDate(dir, username);
        mediaFileService.populateStarredDate(children, username);

        map.put("dir", dir);
        map.put("files", files);
        map.put("subDirs", subDirs);
        map.put("ancestors", getAncestors(dir));
        map.put("coverArtSizeMedium", CoverArtScheme.MEDIUM.getSize());
        map.put("coverArtSizeLarge", CoverArtScheme.LARGE.getSize());
        map.put("player", player);
        map.put("user", securityService.getCurrentUser(request));
        map.put("visibility", userSettings.getMainVisibility());
        map.put("showAlbumYear", settingsService.isSortAlbumsByYear());
        map.put("showArtistInfo", userSettings.isShowArtistInfoEnabled());
        map.put("partyMode", userSettings.isPartyModeEnabled());
        map.put("brand", settingsService.getBrand());
        map.put("viewAsList", isViewAsList(request, userSettings));
        if (dir.isAlbum()) {
            List<MediaFile> sieblingAlbums = getSieblingAlbums(dir);
            thereIsMoreSAlbums = trimToSize(showAll, sieblingAlbums, userPaginationPreference);
            map.put("sieblingAlbums", sieblingAlbums);
            map.put("artist", guessArtist(children));
            map.put("album", guessAlbum(children));
        }

        try {
            MediaFile parent = mediaFileService.getParentOf(dir);
            map.put("parent", parent);
            map.put("navigateUpAllowed", !mediaFileService.isRoot(parent));
        } catch (SecurityException x) {
            // Happens if Podcast directory is outside music folder.
        }
        map.put("thereIsMore", (thereIsMoreSubDirs || thereIsMoreSAlbums) && !BooleanUtils.isTrue(showAll));

        Integer userRating = ratingService.getRatingForUser(username, dir);
        Double averageRating = ratingService.getAverageRating(dir);

        if (userRating == null) {
            userRating = 0;
        }

        if (averageRating == null) {
            averageRating = 0.0D;
        }

        map.put("userRating", 10 * userRating);
        map.put("averageRating", Math.round(10.0D * averageRating));
        map.put("starred", mediaFileService.getMediaFileStarredDate(dir.getId(), username) != null);

        String view;
        if (isVideoOnly(children)) {
            view = "videoMain";
        } else if (dir.isAlbum()) {
            view = "albumMain";
        } else {
            view = "artistMain";
        }

        return new ModelAndView(view, "model", map);
}

    private <T> boolean trimToSize(Boolean showAll, List<T> list, int userPaginationPreference) {
        boolean trimmed = false;
        if(!BooleanUtils.isTrue(showAll)) {
            if(list.size() > userPaginationPreference) {
                trimmed = true;
                list.subList(userPaginationPreference, list.size()).clear();
            }
        }
        return trimmed;
    }

    private boolean isViewAsList(HttpServletRequest request, UserSettings userSettings) {
        boolean viewAsList = ServletRequestUtils.getBooleanParameter(request, "viewAsList", userSettings.isViewAsList());
        if (viewAsList != userSettings.isViewAsList()) {
            userSettings.setViewAsList(viewAsList);
            userSettings.setChanged(new Date());
            settingsService.updateUserSettings(userSettings);
        }
        return viewAsList;
    }

    private boolean isVideoOnly(List<MediaFile> children) {
        boolean videoFound = false;
        for (MediaFile child : children) {
            if (child.isAudio()) {
                return false;
            }
            if (child.isVideo()) {
                videoFound = true;
            }
        }
        return videoFound;
    }

    private List<MediaFile> getMediaFiles(HttpServletRequest request) {
        List<MediaFile> mediaFiles = new ArrayList<>();
        for (String path : ServletRequestUtils.getStringParameters(request, "path")) {
            MediaFile mediaFile = mediaFileService.getMediaFile(path);
            if (mediaFile != null) {
                mediaFiles.add(mediaFile);
            }
        }
        for (int id : ServletRequestUtils.getIntParameters(request, "id")) {
            MediaFile mediaFile = mediaFileService.getMediaFile(id);
            if (mediaFile != null) {
                mediaFiles.add(mediaFile);
            }
        }
        return mediaFiles;
    }

    private String guessArtist(List<MediaFile> children) {
        for (MediaFile child : children) {
            if (child.isFile() && child.getArtist() != null) {
                return child.getArtist();
            }
        }
        return null;
    }

    private String guessAlbum(List<MediaFile> children) {
        for (MediaFile child : children) {
            if (child.isFile() && child.getArtist() != null) {
                return child.getAlbumName();
            }
        }
        return null;
    }

    private List<MediaFile> getMultiFolderChildren(List<MediaFile> mediaFiles) throws IOException {
        SortedSet<MediaFile> result = new TreeSet<>(new MediaFileComparator(settingsService.isSortAlbumsByYear()));
        for (MediaFile mediaFile : mediaFiles) {
            if (mediaFile.isFile()) {
                mediaFile = mediaFileService.getParentOf(mediaFile);
            }
            result.addAll(mediaFileService.getChildrenOf(mediaFile, true, true, true));
        }
        return new ArrayList<>(result);
    }

    private List<MediaFile> getAncestors(MediaFile dir) throws IOException {
        LinkedList<MediaFile> result = new LinkedList<>();

        try {
            MediaFile parent = mediaFileService.getParentOf(dir);
            while (parent != null && !mediaFileService.isRoot(parent)) {
                result.addFirst(parent);
                parent = mediaFileService.getParentOf(parent);
            }
        } catch (SecurityException x) {
            // Happens if Podcast directory is outside music folder.
        }
        return result;
    }

    private List<MediaFile> getSieblingAlbums(MediaFile dir) {
        List<MediaFile> result = new ArrayList<>();

        MediaFile parent = mediaFileService.getParentOf(dir);
        if (!mediaFileService.isRoot(parent)) {
            List<MediaFile> sieblings = mediaFileService.getChildrenOf(parent, false, true, true);
            result.addAll(sieblings.stream().filter(siebling -> siebling.isAlbum() && !siebling.equals(dir)).collect(Collectors.toList()));
        }
        return result;
    }

}
