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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.view.RedirectView;

import net.sourceforge.subsonic.domain.CoverArtScheme;
import net.sourceforge.subsonic.domain.MediaFile;
import net.sourceforge.subsonic.domain.MediaFileComparator;
import net.sourceforge.subsonic.domain.MusicFolder;
import net.sourceforge.subsonic.domain.Player;
import net.sourceforge.subsonic.domain.UserSettings;
import net.sourceforge.subsonic.service.AdService;
import net.sourceforge.subsonic.service.MediaFileService;
import net.sourceforge.subsonic.service.PlayerService;
import net.sourceforge.subsonic.service.RatingService;
import net.sourceforge.subsonic.service.SecurityService;
import net.sourceforge.subsonic.service.SettingsService;

/**
 * Controller for the main page.
 *
 * @author Sindre Mehus
 */
public class MainController extends AbstractController {

    private SecurityService securityService;
    private PlayerService playerService;
    private SettingsService settingsService;
    private RatingService ratingService;
    private MediaFileService mediaFileService;
    private AdService adService;

    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Map<String, Object> map = new HashMap<String, Object>();

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

        List<MediaFile> children = mediaFiles.size() == 1 ? mediaFileService.getChildrenOf(dir, true, true, true) : getMultiFolderChildren(mediaFiles);
        List<MediaFile> files = new ArrayList<MediaFile>();
        List<MediaFile> subDirs = new ArrayList<MediaFile>();
        for (MediaFile child : children) {
            if (child.isFile()) {
                files.add(child);
            } else {
                subDirs.add(child);
            }
        }

        UserSettings userSettings = settingsService.getUserSettings(username);

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
        map.put("showAd", !settingsService.isLicenseValid() && adService.showAd());
        map.put("viewAsList", isViewAsList(request, userSettings));
        if (dir.isAlbum()) {
            map.put("sieblingAlbums", getSieblingAlbums(dir));
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

        ModelAndView result = new ModelAndView(view);
        result.addObject("model", map);
        return result;
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
        List<MediaFile> mediaFiles = new ArrayList<MediaFile>();
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
        SortedSet<MediaFile> result = new TreeSet<MediaFile>(new MediaFileComparator(settingsService.isSortAlbumsByYear()));
        for (MediaFile mediaFile : mediaFiles) {
            if (mediaFile.isFile()) {
                mediaFile = mediaFileService.getParentOf(mediaFile);
            }
            result.addAll(mediaFileService.getChildrenOf(mediaFile, true, true, true));
        }
        return new ArrayList<MediaFile>(result);
    }

    private List<MediaFile> getAncestors(MediaFile dir) throws IOException {
        LinkedList<MediaFile> result = new LinkedList<MediaFile>();

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
        List<MediaFile> result = new ArrayList<MediaFile>();

        MediaFile parent = mediaFileService.getParentOf(dir);
        if (!mediaFileService.isRoot(parent)) {
            List<MediaFile> sieblings = mediaFileService.getChildrenOf(parent, false, true, true);
            for (MediaFile siebling : sieblings) {
                if (siebling.isAlbum() && !siebling.equals(dir)) {
                    result.add(siebling);
                }
            }
        }
        return result;
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    public void setPlayerService(PlayerService playerService) {
        this.playerService = playerService;
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setRatingService(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    public void setAdService(AdService adService) {
        this.adService = adService;
    }

    public void setMediaFileService(MediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
    }
}
