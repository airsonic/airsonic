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

import org.airsonic.player.domain.Artist;
import org.airsonic.player.domain.MusicFolder;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Provides database services for artists.
 *
 * @author Sindre Mehus
 */
@Repository
public class ArtistDao extends AbstractDao {
    private static final String INSERT_COLUMNS = "name, cover_art_path, album_count, last_scanned, present, folder_id";
    private static final String QUERY_COLUMNS = "id, " + INSERT_COLUMNS;

    private final RowMapper rowMapper = new ArtistMapper();

    /**
     * Returns the artist with the given name.
     *
     * @param artistName The artist name.
     * @return The artist or null.
     */
    public Artist getArtist(String artistName) {
        return queryOne("select " + QUERY_COLUMNS + " from artist where name=?", rowMapper, artistName);
    }

    /**
     * Returns the artist with the given name.
     *
     * @param artistName   The artist name.
     * @param musicFolders Only return artists that have at least one album in these folders.
     * @return The artist or null.
     */
    public Artist getArtist(final String artistName, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return null;
        }
        Map<String, Object> args = new HashMap<>();
        args.put("name", artistName);
        args.put("folders", MusicFolder.toIdList(musicFolders));

        return namedQueryOne("select " + QUERY_COLUMNS + " from artist where name = :name and folder_id in (:folders)",
                             rowMapper, args);
    }

    /**
     * Returns the artist with the given ID.
     *
     * @param id The artist ID.
     * @return The artist or null.
     */
    public Artist getArtist(int id) {
        return queryOne("select " + QUERY_COLUMNS + " from artist where id=?", rowMapper, id);
    }

    /**
     * Creates or updates an artist.
     *
     * @param artist The artist to create/update.
     */
    @Transactional
    public void createOrUpdateArtist(Artist artist) {
        String sql = "update artist set " +
                     "cover_art_path=?," +
                     "album_count=?," +
                     "last_scanned=?," +
                     "present=?," +
                     "folder_id=? " +
                     "where name=?";

        int n = update(sql, artist.getCoverArtPath(), artist.getAlbumCount(), artist.getLastScanned(), artist.isPresent(), artist.getFolderId(), artist.getName());

        if (n == 0) {
            update("insert into artist (" + INSERT_COLUMNS + ") values (" + questionMarks(INSERT_COLUMNS) + ")",
                   artist.getName(), artist.getCoverArtPath(), artist.getAlbumCount(), artist.getLastScanned(), artist.isPresent(), artist.getFolderId());
        }

        int id = queryForInt("select id from artist where name=?", null, artist.getName());
        artist.setId(id);
    }

    /**
     * Returns artists in alphabetical order.
     *
     * @param offset       Number of artists to skip.
     * @param count        Maximum number of artists to return.
     * @param musicFolders Only return artists that have at least one album in these folders.
     * @return Artists in alphabetical order.
     */
    public List<Artist> getAlphabetialArtists(final int offset, final int count, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = new HashMap<>();
        args.put("folders", MusicFolder.toIdList(musicFolders));
        args.put("count", count);
        args.put("offset", offset);

        return namedQuery("select " + QUERY_COLUMNS + " from artist where present and folder_id in (:folders) " +
                          "order by name limit :count offset :offset", rowMapper, args);
    }

    /**
     * Returns the most recently starred artists.
     *
     * @param offset       Number of artists to skip.
     * @param count        Maximum number of artists to return.
     * @param username     Returns artists starred by this user.
     * @param musicFolders Only return artists that have at least one album in these folders.
     * @return The most recently starred artists for this user.
     */
    public List<Artist> getStarredArtists(final int offset, final int count, final String username,
                                          final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = new HashMap<>();
        args.put("folders", MusicFolder.toIdList(musicFolders));
        args.put("username", username);
        args.put("count", count);
        args.put("offset", offset);

        return namedQuery("select " + prefix(QUERY_COLUMNS, "artist") + " from starred_artist, artist " +
                          "where artist.id = starred_artist.artist_id and " +
                          "artist.present and starred_artist.username = :username and " +
                          "artist.folder_id in (:folders) " +
                          "order by starred_artist.created desc limit :count offset :offset",
                          rowMapper, args);
    }

    public void markPresent(String artistName, Date lastScanned) {
        update("update artist set present=?, last_scanned=? where name=?", true, lastScanned, artistName);
    }

    public void markNonPresent(Date lastScanned) {
        int minId = queryForInt("select min(id) from artist where last_scanned != ? and present", 0, lastScanned);
        int maxId = queryForInt("select max(id) from artist where last_scanned != ? and present", 0, lastScanned);

        final int batchSize = 1000;
        for (int id = minId; id <= maxId; id += batchSize) {
            update("update artist set present=false where id between ? and ? and last_scanned != ? and present", id, id + batchSize, lastScanned);
        }
    }

    public void expunge() {
        int minId = queryForInt("select min(id) from artist where not present", 0);
        int maxId = queryForInt("select max(id) from artist where not present", 0);

        final int batchSize = 1000;
        for (int id = minId; id <= maxId; id += batchSize) {
            update("delete from artist where id between ? and ? and not present", id, id + batchSize);
        }
    }

    public void starArtist(int artistId, String username) {
        unstarArtist(artistId, username);
        update("insert into starred_artist(artist_id, username, created) values (?,?,?)", artistId, username, new Date());
    }

    public void unstarArtist(int artistId, String username) {
        update("delete from starred_artist where artist_id=? and username=?", artistId, username);
    }

    public Date getArtistStarredDate(int artistId, String username) {
        return queryForDate("select created from starred_artist where artist_id=? and username=?", null, artistId, username);
    }

    private static class ArtistMapper implements RowMapper<Artist> {
        public Artist mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Artist(
                    rs.getInt(1),
                    rs.getString(2),
                    rs.getString(3),
                    rs.getInt(4),
                    rs.getTimestamp(5),
                    rs.getBoolean(6),
                    rs.getInt(7));
        }
    }
}
