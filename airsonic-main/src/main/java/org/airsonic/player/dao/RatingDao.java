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
package org.airsonic.player.dao;

import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides database services for ratings.
 *
 * @author Sindre Mehus
 */
@Repository("musicFileInfoDao")
public class RatingDao extends AbstractDao {

    /**
     * Returns paths for the highest rated albums.
     *
     * @param offset      Number of albums to skip.
     * @param count       Maximum number of albums to return.
     * @param musicFolders Only return albums in these folders.
     * @return Paths for the highest rated albums.
     */
    public List<String> getHighestRatedAlbums(final int offset, final int count, final List<MusicFolder> musicFolders) {
        if (count < 1 || musicFolders.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Object> args = new HashMap<String, Object>() {{
            put("type", MediaFile.MediaType.ALBUM.name());
            put("folders", MusicFolder.toPathList(musicFolders));
            put("count", count);
            put("offset", offset);
        }};

        String sql = "select user_rating.path from user_rating, media_file " +
                     "where user_rating.path=media_file.path and media_file.present and media_file.type = :type and media_file.folder in (:folders) " +
                     "group by user_rating.path " +
                     "order by avg(rating) desc limit :count offset :offset";
        return namedQueryForStrings(sql, args);
    }

    /**
     * Sets the rating for a media file and a given user.
     *
     * @param username  The user name.
     * @param mediaFile The media file.
     * @param rating    The rating between 1 and 5, or <code>null</code> to remove the rating.
     */
    public void setRatingForUser(String username, MediaFile mediaFile, Integer rating) {
        if (rating != null && (rating < 1 || rating > 5)) {
            return;
        }

        update("delete from user_rating where username=? and path=?", username, mediaFile.getPath());
        if (rating != null) {
            update("insert into user_rating values(?, ?, ?)", username, mediaFile.getPath(), rating);
        }
    }

    /**
     * Returns the average rating for the given media file.
     *
     * @param mediaFile The media file.
     * @return The average rating, or <code>null</code> if no ratings are set.
     */
    public Double getAverageRating(MediaFile mediaFile) {
        try {
            return (Double) getJdbcTemplate().queryForObject("select avg(rating) from user_rating where path=?", new Object[]{mediaFile.getPath()}, Double.class);
        } catch (EmptyResultDataAccessException x) {
            return null;
        }
    }

    /**
     * Returns the rating for the given user and media file.
     *
     * @param username  The user name.
     * @param mediaFile The media file.
     * @return The rating, or <code>null</code> if no rating is set.
     */
    public Integer getRatingForUser(String username, MediaFile mediaFile) {
        try {
            return getJdbcTemplate().queryForObject("select rating from user_rating where username=? and path=?", new Object[]{username, mediaFile.getPath()},Integer.class);
        } catch (EmptyResultDataAccessException x) {
            return null;
        }
    }

    public int getRatedAlbumCount(final String username, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return 0;
        }
        Map<String, Object> args = new HashMap<String, Object>() {{
            put("type", MediaFile.MediaType.ALBUM.name());
            put("folders", MusicFolder.toPathList(musicFolders));
            put("username", username);
        }};

        return namedQueryForInt("select count(*) from user_rating, media_file " +
                                "where media_file.path = user_rating.path " +
                                "and media_file.type = :type " +
                                "and media_file.present " +
                                "and media_file.folder in (:folders) " +
                                "and user_rating.username = :username",
                                0, args);
    }
}
