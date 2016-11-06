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

import java.text.NumberFormat;
import java.util.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.taglibs.standard.lang.jstl.IntegerDivideOperator;
import org.libresonic.player.domain.*;
import org.libresonic.player.service.*;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.ParameterizableViewController;

/**
 * Controller for the creating a random play queue.
 *
 * @author Sindre Mehus
 */
public class RandomPlayQueueController extends ParameterizableViewController {

    private PlayerService playerService;
    private List<ReloadFrame> reloadFrames;
    private MediaFileService mediaFileService;
    private SecurityService securityService;
    private SettingsService settingsService;

    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {

        int size = ServletRequestUtils.getRequiredIntParameter(request, "size");

        String genre;
        Integer fromYear = null;
        Integer toYear = null;
        Integer minAlbumRating = null;
        Integer maxAlbumRating = null;
        Date minLastPlayedDate = null;
        Date maxLastPlayedDate = null;
        boolean doesShowStarredSongs = false;
        boolean doesShowUnstarredSongs = false;

        // Handle the genre filter
        genre = request.getParameter("genre");
        if (StringUtils.equalsIgnoreCase("any", genre)) {
            genre = null;
        }

        // Handle the release year filter
        String year = request.getParameter("year");
        if (!StringUtils.equalsIgnoreCase("any", year)) {
            String[] tmp = StringUtils.split(year);
            fromYear = Integer.parseInt(tmp[0]);
            toYear = Integer.parseInt(tmp[1]);
        }

        // Handle the song rating filter
        String songRating = request.getParameter("songRating");
        if (StringUtils.equalsIgnoreCase("any", songRating)) {
            doesShowStarredSongs = true;
            doesShowUnstarredSongs = true;
        } else if (StringUtils.equalsIgnoreCase("starred", songRating)) {
            doesShowStarredSongs = true;
            doesShowUnstarredSongs = false;
        } else if (StringUtils.equalsIgnoreCase("unstarred", songRating)) {
            doesShowStarredSongs = false;
            doesShowUnstarredSongs = true;
        }

        // Handle the last played date filter
        String lastPlayedValue = request.getParameter("lastPlayedValue");
        String lastPlayedComp = request.getParameter("lastPlayedComp");
        Calendar lastPlayed = Calendar.getInstance();
        lastPlayed.setTime(new Date());
        switch (lastPlayedValue) {
            case "any":
                lastPlayed = null;
                break;
            case "1day":
                lastPlayed.add(Calendar.DAY_OF_YEAR, -1);
                break;
            case "1week":
                lastPlayed.add(Calendar.WEEK_OF_YEAR, -1);
                break;
            case "1month":
                lastPlayed.add(Calendar.MONTH, -1);
                break;
            case "3months":
                lastPlayed.add(Calendar.MONTH, -3);
                break;
            case "6months":
                lastPlayed.add(Calendar.MONTH, -6);
                break;
            case "1year":
                lastPlayed.add(Calendar.YEAR, -1);
                break;
        }
        if (lastPlayed != null) {
            switch (lastPlayedComp) {
                case "lt":
                    minLastPlayedDate = null;
                    maxLastPlayedDate = lastPlayed.getTime();
                    break;
                case "gt":
                    minLastPlayedDate = lastPlayed.getTime();
                    maxLastPlayedDate = null;
                    break;
            }
        }

        // Handle the album rating filter
        Integer albumRatingValue = null;
        try { albumRatingValue = Integer.parseInt(request.getParameter("albumRatingValue")); }
        catch (NumberFormatException e) { }
        String albumRatingComp = request.getParameter("albumRatingComp");
        if (albumRatingValue != null) {
            switch (albumRatingComp) {
                case "lt":
                    minAlbumRating = null;
                    maxAlbumRating = albumRatingValue - 1;
                    break;
                case "gt":
                    minAlbumRating = albumRatingValue + 1;
                    maxAlbumRating = null;
                    break;
                case "le":
                    minAlbumRating = null;
                    maxAlbumRating = albumRatingValue;
                    break;
                case "ge":
                    minAlbumRating = albumRatingValue;
                    maxAlbumRating = null;
                    break;
                case "eq":
                    minAlbumRating = albumRatingValue;
                    maxAlbumRating = albumRatingValue;
                    break;
            }
        }

        // Handle the format filter
        String format = request.getParameter("format");
        if (StringUtils.equalsIgnoreCase(format, "any")) format = null;

        // Handle the music folder filter
        List<MusicFolder> musicFolders = getMusicFolders(request);

        // Search the database using these criteria
        RandomSearchCriteria criteria = new RandomSearchCriteria(
                size,
                genre,
                fromYear,
                toYear,
                musicFolders,
                minLastPlayedDate,
                maxLastPlayedDate,
                minAlbumRating,
                maxAlbumRating,
                doesShowStarredSongs,
                doesShowUnstarredSongs,
                format
        );
        User user = securityService.getCurrentUser(request);
        Player player = playerService.getPlayer(request, response);
        PlayQueue playQueue = player.getPlayQueue();
        playQueue.addFiles(false, mediaFileService.getRandomSongs(criteria, user.getUsername()));

        if (request.getParameter("autoRandom") != null) {
            playQueue.setRandomSearchCriteria(criteria);
        }

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("reloadFrames", reloadFrames);

        ModelAndView result = super.handleRequestInternal(request, response);
        result.addObject("model", map);
        return result;
    }

    private List<MusicFolder> getMusicFolders(HttpServletRequest request) throws ServletRequestBindingException {
        String username = securityService.getCurrentUsername(request);
        Integer selectedMusicFolderId = ServletRequestUtils.getRequiredIntParameter(request, "musicFolderId");
        if (selectedMusicFolderId == -1) {
            selectedMusicFolderId = null;
        }
        return settingsService.getMusicFoldersForUser(username, selectedMusicFolderId);
    }

    public void setPlayerService(PlayerService playerService) {
        this.playerService = playerService;
    }

    public void setReloadFrames(List<ReloadFrame> reloadFrames) {
        this.reloadFrames = reloadFrames;
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setMediaFileService(MediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
    }
}
