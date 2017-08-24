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
package org.airsonic.player.service;

import org.airsonic.player.dao.RatingDao;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.util.FileUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides services for user ratings.
 *
 * @author Sindre Mehus
 */
@Service
public class RatingService {

    @Autowired
    private RatingDao ratingDao;
    @Autowired
    private SecurityService securityService;
    @Autowired
    private MediaFileService mediaFileService;

    /**
     * Returns the highest rated albums.
     *
     * @param offset      Number of albums to skip.
     * @param count       Maximum number of albums to return.
     * @param musicFolders Only return albums in these folders.
     * @return The highest rated albums.
     */
    public List<MediaFile> getHighestRatedAlbums(int offset, int count, List<MusicFolder> musicFolders) {
        List<String> highestRated = ratingDao.getHighestRatedAlbums(offset, count, musicFolders);
        List<MediaFile> result = new ArrayList<MediaFile>();
        for (String path : highestRated) {
            File file = new File(path);
            if (FileUtil.exists(file) && securityService.isReadAllowed(file)) {
                result.add(mediaFileService.getMediaFile(path));
            }
        }
        return result;
    }

    /**
     * Sets the rating for a music file and a given user.
     *
     * @param username  The user name.
     * @param mediaFile The music file.
     * @param rating    The rating between 1 and 5, or <code>null</code> to remove the rating.
     */
    public void setRatingForUser(String username, MediaFile mediaFile, Integer rating) {
        ratingDao.setRatingForUser(username, mediaFile, rating);
    }

    /**
     * Returns the average rating for the given music file.
     *
     * @param mediaFile The music file.
     * @return The average rating, or <code>null</code> if no ratings are set.
     */
    public Double getAverageRating(MediaFile mediaFile) {
        return ratingDao.getAverageRating(mediaFile);
    }

    /**
     * Returns the rating for the given user and music file.
     *
     * @param username  The user name.
     * @param mediaFile The music file.
     * @return The rating, or <code>null</code> if no rating is set.
     */
    public Integer getRatingForUser(String username, MediaFile mediaFile) {
        return ratingDao.getRatingForUser(username, mediaFile);
    }

    public int getRatedAlbumCount(String username, List<MusicFolder> musicFolders) {
        return ratingDao.getRatedAlbumCount(username, musicFolders);
    }

    public void setRatingDao(RatingDao ratingDao) {
        this.ratingDao = ratingDao;
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    public void setMediaFileService(MediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
    }
}
