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

import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.util.FileUtil;
import org.apache.commons.lang.ObjectUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Provides database services for albums.
 *
 * @author Sindre Mehus
 */
@Repository
public class AlbumDao extends AbstractDao {
    private static final String INSERT_COLUMNS = "path, name, artist, song_count, duration_seconds, cover_art_path, " +
                                          "year, genre, play_count, last_played, comment, created, last_scanned, present, " +
                                          "folder_id, mb_release_id";

    private static final String QUERY_COLUMNS = "id, " + INSERT_COLUMNS;

    private final RowMapper<Album> rowMapper = new AlbumMapper();

    public Album getAlbum(int id) {
        return queryOne("select " + QUERY_COLUMNS + " from album where id=?", rowMapper, id);
    }

    /**
     * Returns the album with the given artist and album name.
     *
     * @param artistName The artist name.
     * @param albumName  The album name.
     * @return The album or null.
     */
    public Album getAlbum(String artistName, String albumName) {
        return queryOne("select " + QUERY_COLUMNS + " from album where artist=? and name=?", rowMapper, artistName, albumName);
    }

    /**
     * Returns the album that the given file (most likely) is part of.
     *
     * @param file The media file.
     * @return The album or null.
     */
    public Album getAlbumForFile(MediaFile file) {

        // First, get all albums with the correct album name (irrespective of artist).
        List<Album> candidates = query("select " + QUERY_COLUMNS + " from album where name=?", rowMapper, file.getAlbumName());
        if (candidates.isEmpty()) {
            return null;
        }

        // Look for album with the correct artist.
        for (Album candidate : candidates) {
            if (ObjectUtils.equals(candidate.getArtist(), file.getArtist()) && FileUtil.exists(candidate.getPath())) {
                return candidate;
            }
        }

        // Look for album with the same path as the file.
        for (Album candidate : candidates) {
            if (ObjectUtils.equals(candidate.getPath(), file.getParentPath())) {
                return candidate;
            }
        }

        // No appropriate album found.
        return null;
    }

    public List<Album> getAlbumsForArtist(final String artist, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = new HashMap<>();
        args.put("artist", artist);
        args.put("folders", MusicFolder.toIdList(musicFolders));
        return namedQuery("select " + QUERY_COLUMNS
                          + " from album where artist = :artist and present and folder_id in (:folders) " +
                          "order by name",
                          rowMapper, args);
    }

    /**
     * Creates or updates an album.
     *
     * @param album The album to create/update.
     */
    @Transactional
    public void createOrUpdateAlbum(Album album) {
        String sql = "update album set " +
                     "path=?," +
                     "song_count=?," +
                     "duration_seconds=?," +
                     "cover_art_path=?," +
                     "year=?," +
                     "genre=?," +
                     "play_count=?," +
                     "last_played=?," +
                     "comment=?," +
                     "created=?," +
                     "last_scanned=?," +
                     "present=?, " +
                     "folder_id=?, " +
                     "mb_release_id=? " +
                     "where artist=? and name=?";

        int n = update(sql, album.getPath(), album.getSongCount(), album.getDurationSeconds(), album.getCoverArtPath(), album.getYear(),
                       album.getGenre(), album.getPlayCount(), album.getLastPlayed(), album.getComment(), album.getCreated(),
                       album.getLastScanned(), album.isPresent(), album.getFolderId(), album.getMusicBrainzReleaseId(), album.getArtist(), album.getName());

        if (n == 0) {

            update("insert into album (" + INSERT_COLUMNS + ") values (" + questionMarks(INSERT_COLUMNS) + ")", album.getPath(),
                   album.getName(), album.getArtist(), album.getSongCount(), album.getDurationSeconds(),
                   album.getCoverArtPath(), album.getYear(), album.getGenre(), album.getPlayCount(), album.getLastPlayed(),
                   album.getComment(), album.getCreated(), album.getLastScanned(), album.isPresent(), album.getFolderId(), album.getMusicBrainzReleaseId());
        }

        int id = queryForInt("select id from album where artist=? and name=?", null, album.getArtist(), album.getName());
        album.setId(id);
    }

    /**
     * Returns albums in alphabetical order.
     *
     * @param offset       Number of albums to skip.
     * @param count        Maximum number of albums to return.
     * @param byArtist     Whether to sort by artist name
     * @param musicFolders Only return albums from these folders.
     * @param ignoreCase   Use case insensitive sorting
     * @return Albums in alphabetical order.
     */
    public List<Album> getAlphabeticalAlbums(final int offset, final int count, boolean byArtist, boolean ignoreCase, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = new HashMap<>();
        args.put("folders", MusicFolder.toIdList(musicFolders));
        args.put("count", count);
        args.put("offset", offset);
        String orderBy;
        if (ignoreCase) {
            orderBy = byArtist ? "LOWER(artist),  LOWER(name)" : "LOWER(name)";
        } else {
            orderBy = byArtist ? "artist, name" : "name";
        }

        return namedQuery("select " + QUERY_COLUMNS + " from album where present and folder_id in (:folders) " +
                          "order by " + orderBy + " limit :count offset :offset", rowMapper, args);
    }

    /**
     * Returns the count of albums in the given folders
     *
     * @param musicFolders Only return albums from these folders.
     * @return the count of present albums
     */
    public int getAlbumCount(final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return 0;
        }
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("folders", MusicFolder.toIdList(musicFolders));

        return getNamedParameterJdbcTemplate().queryForObject("select count(*) from album where present and folder_id in (:folders)", args, Integer.class);

    }

    /**
     * Returns the most frequently played albums.
     *
     * @param offset       Number of albums to skip.
     * @param count        Maximum number of albums to return.
     * @param musicFolders Only return albums from these folders.
     * @return The most frequently played albums.
     */
    public List<Album> getMostFrequentlyPlayedAlbums(final int offset, final int count, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = new HashMap<>();
        args.put("folders", MusicFolder.toIdList(musicFolders));
        args.put("count", count);
        args.put("offset", offset);
        return namedQuery("select " + QUERY_COLUMNS
                          + " from album where play_count > 0 and present and folder_id in (:folders) " +
                          "order by play_count desc limit :count offset :offset", rowMapper, args);
    }

    /**
     * Returns the most recently played albums.
     *
     * @param offset       Number of albums to skip.
     * @param count        Maximum number of albums to return.
     * @param musicFolders Only return albums from these folders.
     * @return The most recently played albums.
     */
    public List<Album> getMostRecentlyPlayedAlbums(final int offset, final int count, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = new HashMap<>();
        args.put("folders", MusicFolder.toIdList(musicFolders));
        args.put("count", count);
        args.put("offset", offset);
        return namedQuery("select " + QUERY_COLUMNS
                          + " from album where last_played is not null and present and folder_id in (:folders) " +
                          "order by last_played desc limit :count offset :offset", rowMapper, args);
    }

    /**
     * Returns the most recently added albums.
     *
     * @param offset       Number of albums to skip.
     * @param count        Maximum number of albums to return.
     * @param musicFolders Only return albums from these folders.
     * @return The most recently added albums.
     */
    public List<Album> getNewestAlbums(final int offset, final int count, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = new HashMap<>();
        args.put("folders", MusicFolder.toIdList(musicFolders));
        args.put("count", count);
        args.put("offset", offset);
        return namedQuery("select " + QUERY_COLUMNS + " from album where present and folder_id in (:folders) " +
                          "order by created desc limit :count offset :offset", rowMapper, args);
    }

    /**
     * Returns the most recently starred albums.
     *
     * @param offset       Number of albums to skip.
     * @param count        Maximum number of albums to return.
     * @param username     Returns albums starred by this user.
     * @param musicFolders Only return albums from these folders.
     * @return The most recently starred albums for this user.
     */
    public List<Album> getStarredAlbums(final int offset, final int count, final String username, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = new HashMap<>();
        args.put("folders", MusicFolder.toIdList(musicFolders));
        args.put("count", count);
        args.put("offset", offset);
        args.put("username", username);
        return namedQuery("select " + prefix(QUERY_COLUMNS, "album") + " from starred_album, album where album.id = starred_album.album_id and " +
                          "album.present and album.folder_id in (:folders) and starred_album.username = :username " +
                          "order by starred_album.created desc limit :count offset :offset",
                          rowMapper, args);
    }

    /**
     * Returns albums in a genre.
     *
     * @param offset       Number of albums to skip.
     * @param count        Maximum number of albums to return.
     * @param genre        The genre name.
     * @param musicFolders Only return albums from these folders.
     * @return Albums in the genre.
     */
    public List<Album> getAlbumsByGenre(final int offset, final int count, final String genre, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = new HashMap<>();
        args.put("folders", MusicFolder.toIdList(musicFolders));
        args.put("count", count);
        args.put("offset", offset);
        args.put("genre", genre);
        return namedQuery("select " + QUERY_COLUMNS + " from album where present and folder_id in (:folders) " +
                          "and genre = :genre limit :count offset :offset", rowMapper, args);
    }

    /**
     * Returns albums within a year range.
     *
     * @param offset       Number of albums to skip.
     * @param count        Maximum number of albums to return.
     * @param fromYear     The first year in the range.
     * @param toYear       The last year in the range.
     * @param musicFolders Only return albums from these folders.
     * @return Albums in the year range.
     */
    public List<Album> getAlbumsByYear(final int offset, final int count, final int fromYear, final int toYear,
                                       final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = new HashMap<>();
        args.put("folders", MusicFolder.toIdList(musicFolders));
        args.put("count", count);
        args.put("offset", offset);
        args.put("fromYear", fromYear);
        args.put("toYear", toYear);
        if (fromYear <= toYear) {
            return namedQuery("select " + QUERY_COLUMNS + " from album where present and folder_id in (:folders) " +
                              "and year between :fromYear and :toYear order by year limit :count offset :offset",
                              rowMapper, args);
        } else {
            return namedQuery("select " + QUERY_COLUMNS + " from album where present and folder_id in (:folders) " +
                              "and year between :toYear and :fromYear order by year desc limit :count offset :offset",
                              rowMapper, args);
        }
    }

    public void markNonPresent(Date lastScanned) {
        int minId = queryForInt("select min(id) from album where last_scanned < ? and present", 0, lastScanned);
        int maxId = queryForInt("select max(id) from album where last_scanned < ? and present", 0, lastScanned);

        final int batchSize = 1000;
        for (int id = minId; id <= maxId; id += batchSize) {
            update("update album set present=false where id between ? and ? and last_scanned < ? and present", id, id + batchSize, lastScanned);
        }
    }

    public List<Integer> getExpungeCandidates() {
        return queryForInts("select id from album where not present");
    }

    public void expunge() {
        int minId = queryForInt("select min(id) from album where not present", 0);
        int maxId = queryForInt("select max(id) from album where not present", 0);

        final int batchSize = 1000;
        for (int id = minId; id <= maxId; id += batchSize) {
            update("delete from album where id between ? and ? and not present", id, id + batchSize);
        }
    }

    public void starAlbum(int albumId, String username) {
        unstarAlbum(albumId, username);
        update("insert into starred_album(album_id, username, created) values (?,?,?)", albumId, username, new Date());
    }

    public void unstarAlbum(int albumId, String username) {
        update("delete from starred_album where album_id=? and username=?", albumId, username);
    }

    public Date getAlbumStarredDate(int albumId, String username) {
        return queryForDate("select created from starred_album where album_id=? and username=?", null, albumId, username);
    }

    private static class AlbumMapper implements RowMapper<Album> {
        public Album mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Album(
                    rs.getInt(1),
                    rs.getString(2),
                    rs.getString(3),
                    rs.getString(4),
                    rs.getInt(5),
                    rs.getInt(6),
                    rs.getString(7),
                    rs.getInt(8) == 0 ? null : rs.getInt(8),
                    rs.getString(9),
                    rs.getInt(10),
                    rs.getTimestamp(11),
                    rs.getString(12),
                    rs.getTimestamp(13),
                    rs.getTimestamp(14),
                    rs.getBoolean(15),
                    rs.getInt(16),
                    rs.getString(17));
        }
    }
}
