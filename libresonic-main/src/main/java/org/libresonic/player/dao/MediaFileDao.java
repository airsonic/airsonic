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
package org.libresonic.player.dao;

import org.apache.commons.lang.StringUtils;
import org.libresonic.player.domain.Genre;
import org.libresonic.player.domain.MediaFile;
import org.libresonic.player.domain.MusicFolder;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static org.libresonic.player.domain.MediaFile.MediaType;
import static org.libresonic.player.domain.MediaFile.MediaType.*;

/**
 * Provides database services for media files.
 *
 * @author Sindre Mehus
 */
public class MediaFileDao extends AbstractDao {

    private static final String COLUMNS = "id, path, folder, type, format, title, album, artist, album_artist, disc_number, " +
                                          "track_number, year, genre, bit_rate, variable_bit_rate, duration_seconds, file_size, width, height, cover_art_path, " +
                                          "parent_path, play_count, last_played, comment, created, changed, last_scanned, children_last_updated, present, version";
    private static final String GENRE_COLUMNS = "name, song_count, album_count";

    public static final int VERSION = 4;

    private final RowMapper rowMapper = new MediaFileMapper();
    private final RowMapper musicFileInfoRowMapper = new MusicFileInfoMapper();
    private final RowMapper genreRowMapper = new GenreMapper();

    /**
     * Returns the media file for the given path.
     *
     * @param path The path.
     * @return The media file or null.
     */
    public MediaFile getMediaFile(String path) {
        return queryOne("select " + COLUMNS + " from media_file where path=?", rowMapper, path);
    }

    /**
     * Returns the media file for the given ID.
     *
     * @param id The ID.
     * @return The media file or null.
     */
    public MediaFile getMediaFile(int id) {
        return queryOne("select " + COLUMNS + " from media_file where id=?", rowMapper, id);
    }

    /**
     * Returns the media file that are direct children of the given path.
     *
     * @param path The path.
     * @return The list of children.
     */
    public List<MediaFile> getChildrenOf(String path) {
        return query("select " + COLUMNS + " from media_file where parent_path=? and present", rowMapper, path);
    }

    public List<MediaFile> getFilesInPlaylist(int playlistId) {
        return query("select " + prefix(COLUMNS, "media_file") + " from playlist_file, media_file where " +
                     "media_file.id = playlist_file.media_file_id and " +
                     "playlist_file.playlist_id = ? " +
                     "order by playlist_file.id", rowMapper, playlistId);
    }

    public List<MediaFile> getSongsForAlbum(String artist, String album) {
        return query("select " + COLUMNS + " from media_file where album_artist=? and album=? and present " +
                     "and type in (?,?,?) order by disc_number, track_number", rowMapper,
                     artist, album, MUSIC.name(), AUDIOBOOK.name(), PODCAST.name());
    }

    public List<MediaFile> getVideos(final int count, final int offset, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = new HashMap<String, Object>() {{
            put("type", VIDEO.name());
            put("folders", MusicFolder.toPathList(musicFolders));
            put("count", count);
            put("offset", offset);
        }};
        return namedQuery("select " + COLUMNS + " from media_file where type = :type and present and folder in (:folders) " +
                          "order by title limit :count offset :offset", rowMapper, args);
    }

    public MediaFile getArtistByName(final String name, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return null;
        }
        Map<String, Object> args = new HashMap<String, Object>() {{
            put("type", DIRECTORY.name());
            put("name", name);
            put("folders", MusicFolder.toPathList(musicFolders));
        }};
        return namedQueryOne("select " + COLUMNS + " from media_file where type = :type and artist = :name " +
                             "and present and folder in (:folders)", rowMapper, args);
    }

    /**
     * Creates or updates a media file.
     *
     * @param file The media file to create/update.
     */
    public synchronized void createOrUpdateMediaFile(MediaFile file) {
        String sql = "update media_file set " +
                     "folder=?," +
                     "type=?," +
                     "format=?," +
                     "title=?," +
                     "album=?," +
                     "artist=?," +
                     "album_artist=?," +
                     "disc_number=?," +
                     "track_number=?," +
                     "year=?," +
                     "genre=?," +
                     "bit_rate=?," +
                     "variable_bit_rate=?," +
                     "duration_seconds=?," +
                     "file_size=?," +
                     "width=?," +
                     "height=?," +
                     "cover_art_path=?," +
                     "parent_path=?," +
                     "play_count=?," +
                     "last_played=?," +
                     "comment=?," +
                     "changed=?," +
                     "last_scanned=?," +
                     "children_last_updated=?," +
                     "present=?, " +
                     "version=? " +
                     "where path=?";

        int n = update(sql,
                       file.getFolder(), file.getMediaType().name(), file.getFormat(), file.getTitle(), file.getAlbumName(), file.getArtist(),
                       file.getAlbumArtist(), file.getDiscNumber(), file.getTrackNumber(), file.getYear(), file.getGenre(), file.getBitRate(),
                       file.isVariableBitRate(), file.getDurationSeconds(), file.getFileSize(), file.getWidth(), file.getHeight(),
                       file.getCoverArtPath(), file.getParentPath(), file.getPlayCount(), file.getLastPlayed(), file.getComment(),
                       file.getChanged(), file.getLastScanned(), file.getChildrenLastUpdated(), file.isPresent(), VERSION, file.getPath());

        if (n == 0) {

            // Copy values from obsolete table music_file_info.
            MediaFile musicFileInfo = getMusicFileInfo(file.getPath());
            if (musicFileInfo != null) {
                file.setComment(musicFileInfo.getComment());
                file.setLastPlayed(musicFileInfo.getLastPlayed());
                file.setPlayCount(musicFileInfo.getPlayCount());
            }

            update("insert into media_file (" + COLUMNS + ") values (" + questionMarks(COLUMNS) + ")", null,
                   file.getPath(), file.getFolder(), file.getMediaType().name(), file.getFormat(), file.getTitle(), file.getAlbumName(), file.getArtist(),
                   file.getAlbumArtist(), file.getDiscNumber(), file.getTrackNumber(), file.getYear(), file.getGenre(), file.getBitRate(),
                   file.isVariableBitRate(), file.getDurationSeconds(), file.getFileSize(), file.getWidth(), file.getHeight(),
                   file.getCoverArtPath(), file.getParentPath(), file.getPlayCount(), file.getLastPlayed(), file.getComment(),
                   file.getCreated(), file.getChanged(), file.getLastScanned(),
                   file.getChildrenLastUpdated(), file.isPresent(), VERSION);
        }

        int id = queryForInt("select id from media_file where path=?", null, file.getPath());
        file.setId(id);
    }

    private MediaFile getMusicFileInfo(String path) {
        return queryOne("select play_count, last_played, comment from music_file_info where path=?", musicFileInfoRowMapper, path);
    }

    public void deleteMediaFile(String path) {
        update("update media_file set present=false, children_last_updated=? where path=?", new Date(0L), path);
    }

    public List<Genre> getGenres(boolean sortByAlbum) {
        String orderBy = sortByAlbum ? "album_count" : "song_count";
        return query("select " + GENRE_COLUMNS + " from genre order by " + orderBy + " desc", genreRowMapper);
    }

    public void updateGenres(List<Genre> genres) {
        update("delete from genre");
        for (Genre genre : genres) {
            update("insert into genre(" + GENRE_COLUMNS + ") values(?, ?, ?)",
                   genre.getName(), genre.getSongCount(), genre.getAlbumCount());
        }
    }

    /**
     * Returns the most frequently played albums.
     *
     * @param offset       Number of albums to skip.
     * @param count        Maximum number of albums to return.
     * @param musicFolders Only return albums in these folders.
     * @return The most frequently played albums.
     */
    public List<MediaFile> getMostFrequentlyPlayedAlbums(final int offset, final int count, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = new HashMap<String, Object>() {{
            put("type", ALBUM.name());
            put("folders", MusicFolder.toPathList(musicFolders));
            put("count", count);
            put("offset", offset);
        }};

        return namedQuery("select " + COLUMNS + " from media_file where type = :type and play_count > 0 and present and folder in (:folders) " +
                          "order by play_count desc limit :count offset :offset", rowMapper, args);
    }

    /**
     * Returns the most recently played albums.
     *
     * @param offset       Number of albums to skip.
     * @param count        Maximum number of albums to return.
     * @param musicFolders Only return albums in these folders.
     * @return The most recently played albums.
     */
    public List<MediaFile> getMostRecentlyPlayedAlbums(final int offset, final int count, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = new HashMap<String, Object>() {{
            put("type", ALBUM.name());
            put("folders", MusicFolder.toPathList(musicFolders));
            put("count", count);
            put("offset", offset);
        }};
        return namedQuery("select " + COLUMNS + " from media_file where type = :type and last_played is not null and present " +
                          "and folder in (:folders) order by last_played desc limit :count offset :offset", rowMapper, args);
    }

    /**
     * Returns the most recently added albums.
     *
     * @param offset       Number of albums to skip.
     * @param count        Maximum number of albums to return.
     * @param musicFolders Only return albums in these folders.
     * @return The most recently added albums.
     */
    public List<MediaFile> getNewestAlbums(final int offset, final int count, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = new HashMap<String, Object>() {{
            put("type", ALBUM.name());
            put("folders", MusicFolder.toPathList(musicFolders));
            put("count", count);
            put("offset", offset);
        }};

        return namedQuery("select " + COLUMNS + " from media_file where type = :type and folder in (:folders) and present " +
                          "order by created desc limit :count offset :offset", rowMapper, args);
    }

    /**
     * Returns albums in alphabetical order.
     *
     * @param offset       Number of albums to skip.
     * @param count        Maximum number of albums to return.
     * @param byArtist     Whether to sort by artist name
     * @param musicFolders Only return albums in these folders.
     * @return Albums in alphabetical order.
     */
    public List<MediaFile> getAlphabeticalAlbums(final int offset, final int count, boolean byArtist, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = new HashMap<String, Object>() {{
            put("type", ALBUM.name());
            put("folders", MusicFolder.toPathList(musicFolders));
            put("count", count);
            put("offset", offset);
        }};

        String orderBy = byArtist ? "artist, album" : "album";
        return namedQuery("select " + COLUMNS + " from media_file where type = :type and folder in (:folders) and present " +
                          "order by " + orderBy + " limit :count offset :offset", rowMapper, args);
    }

    /**
     * Returns albums within a year range.
     *
     * @param offset       Number of albums to skip.
     * @param count        Maximum number of albums to return.
     * @param fromYear     The first year in the range.
     * @param toYear       The last year in the range.
     * @param musicFolders Only return albums in these folders.
     * @return Albums in the year range.
     */
    public List<MediaFile> getAlbumsByYear(final int offset, final int count, final int fromYear, final int toYear,
                                           final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = new HashMap<String, Object>() {{
            put("type", ALBUM.name());
            put("folders", MusicFolder.toPathList(musicFolders));
            put("fromYear", fromYear);
            put("toYear", toYear);
            put("count", count);
            put("offset", offset);
        }};

        if (fromYear <= toYear) {
            return namedQuery("select " + COLUMNS + " from media_file where type = :type and folder in (:folders) and present " +
                              "and year between :fromYear and :toYear order by year limit :count offset :offset",
                              rowMapper, args);
        } else {
            return namedQuery("select " + COLUMNS + " from media_file where type = :type and folder in (:folders) and present " +
                              "and year between :toYear and :fromYear order by year desc limit :count offset :offset",
                              rowMapper, args);
        }
    }

    /**
     * Returns albums in a genre.
     *
     * @param offset       Number of albums to skip.
     * @param count        Maximum number of albums to return.
     * @param genre        The genre name.
     * @param musicFolders Only return albums in these folders.
     * @return Albums in the genre.
     */
    public List<MediaFile> getAlbumsByGenre(final int offset, final int count, final String genre,
                                            final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = new HashMap<String, Object>() {{
            put("type", ALBUM.name());
            put("genre", genre);
            put("folders", MusicFolder.toPathList(musicFolders));
            put("count", count);
            put("offset", offset);
        }};
        return namedQuery("select " + COLUMNS + " from media_file where type = :type and folder in (:folders) " +
                          "and present and genre = :genre limit :count offset :offset", rowMapper, args);
    }

    public List<MediaFile> getSongsByGenre(final String genre, final int offset, final int count, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = new HashMap<String, Object>() {{
            put("types", Arrays.asList(MUSIC.name(), PODCAST.name(), AUDIOBOOK.name()));
            put("genre", genre);
            put("count", count);
            put("offset", offset);
            put("folders", MusicFolder.toPathList(musicFolders));
        }};
        return namedQuery("select " + COLUMNS + " from media_file where type in (:types) and genre = :genre " +
                          "and present and folder in (:folders) limit :count offset :offset",
                          rowMapper, args);
    }

    public List<MediaFile> getSongsByArtist(String artist, int offset, int count) {
        return query("select " + COLUMNS + " from media_file where type in (?,?,?) and artist=? and present limit ? offset ?",
                     rowMapper, MUSIC.name(), PODCAST.name(), AUDIOBOOK.name(), artist, count, offset);
    }

    public MediaFile getSongByArtistAndTitle(final String artist, final String title, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty() || StringUtils.isBlank(title) || StringUtils.isBlank(artist)) {
            return null;
        }
        Map<String, Object> args = new HashMap<String, Object>() {{
            put("artist", artist);
            put("title", title);
            put("type", MUSIC.name());
            put("folders", MusicFolder.toPathList(musicFolders));
        }};
        return namedQueryOne("select " + COLUMNS + " from media_file where artist = :artist " +
                             "and title = :title and type = :type and present and folder in (:folders)" ,
                             rowMapper, args);
    }

    /**
     * Returns the most recently starred albums.
     *
     * @param offset       Number of albums to skip.
     * @param count        Maximum number of albums to return.
     * @param username     Returns albums starred by this user.
     * @param musicFolders Only return albums in these folders.
     * @return The most recently starred albums for this user.
     */
    public List<MediaFile> getStarredAlbums(final int offset, final int count, final String username,
                                            final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = new HashMap<String, Object>() {{
            put("type", ALBUM.name());
            put("folders", MusicFolder.toPathList(musicFolders));
            put("username", username);
            put("count", count);
            put("offset", offset);
        }};
        return namedQuery("select " + prefix(COLUMNS, "media_file") + " from starred_media_file, media_file where media_file.id = starred_media_file.media_file_id and " +
                          "media_file.present and media_file.type = :type and media_file.folder in (:folders) and starred_media_file.username = :username " +
                          "order by starred_media_file.created desc limit :count offset :offset",
                          rowMapper, args);
    }

    /**
     * Returns the most recently starred directories.
     *
     * @param offset       Number of directories to skip.
     * @param count        Maximum number of directories to return.
     * @param username     Returns directories starred by this user.
     * @param musicFolders Only return albums in these folders.
     * @return The most recently starred directories for this user.
     */
    public List<MediaFile> getStarredDirectories(final int offset, final int count, final String username,
                                                 final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = new HashMap<String, Object>() {{
            put("type", DIRECTORY.name());
            put("folders", MusicFolder.toPathList(musicFolders));
            put("username", username);
            put("count", count);
            put("offset", offset);
        }};
        return namedQuery("select " + prefix(COLUMNS, "media_file") + " from starred_media_file, media_file " +
                          "where media_file.id = starred_media_file.media_file_id and " +
                          "media_file.present and media_file.type = :type and starred_media_file.username = :username and " +
                          "media_file.folder in (:folders) " +
                          "order by starred_media_file.created desc limit :count offset :offset",
                          rowMapper, args);
    }

    /**
     * Returns the most recently starred files.
     *
     * @param offset       Number of files to skip.
     * @param count        Maximum number of files to return.
     * @param username     Returns files starred by this user.
     * @param musicFolders Only return albums in these folders.
     * @return The most recently starred files for this user.
     */
    public List<MediaFile> getStarredFiles(final int offset, final int count, final String username,
                                           final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = new HashMap<String, Object>() {{
            put("types", Arrays.asList(MUSIC.name(), PODCAST.name(), AUDIOBOOK.name(), VIDEO.name()));
            put("folders", MusicFolder.toPathList(musicFolders));
            put("username", username);
            put("count", count);
            put("offset", offset);
        }};
        return namedQuery("select " + prefix(COLUMNS, "media_file") + " from starred_media_file, media_file where media_file.id = starred_media_file.media_file_id and " +
                          "media_file.present and media_file.type in (:types) and starred_media_file.username = :username and " +
                          "media_file.folder in (:folders) " +
                          "order by starred_media_file.created desc limit :count offset :offset",
                          rowMapper, args);
    }

    public int getAlbumCount(final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return 0;
        }
        Map<String, Object> args = new HashMap<String, Object>() {{
            put("type", ALBUM.name());
            put("folders", MusicFolder.toPathList(musicFolders));
        }};
        return namedQueryForInt("select count(*) from media_file where type = :type and folder in (:folders) and present", 0, args);
    }

    public int getPlayedAlbumCount(final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return 0;
        }
        Map<String, Object> args = new HashMap<String, Object>() {{
            put("type", ALBUM.name());
            put("folders", MusicFolder.toPathList(musicFolders));
        }};
        return namedQueryForInt("select count(*) from media_file where type = :type " +
                                "and play_count > 0 and present and folder in (:folders)", 0, args);
    }

    public int getStarredAlbumCount(final String username, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return 0;
        }
        Map<String, Object> args = new HashMap<String, Object>() {{
            put("type", ALBUM.name());
            put("folders", MusicFolder.toPathList(musicFolders));
            put("username", username);
        }};
        return namedQueryForInt("select count(*) from starred_media_file, media_file " +
                                "where media_file.id = starred_media_file.media_file_id " +
                                "and media_file.type = :type " +
                                "and media_file.present " +
                                "and media_file.folder in (:folders) " +
                                "and starred_media_file.username = :username",
                                0, args);
    }

    public void starMediaFile(int id, String username) {
        unstarMediaFile(id, username);
        update("insert into starred_media_file(media_file_id, username, created) values (?,?,?)", id, username, new Date());
    }

    public void unstarMediaFile(int id, String username) {
        update("delete from starred_media_file where media_file_id=? and username=?", id, username);
    }

    public Date getMediaFileStarredDate(int id, String username) {
        return queryForDate("select created from starred_media_file where media_file_id=? and username=?", null, id, username);
    }

    public void markPresent(String path, Date lastScanned) {
        update("update media_file set present=?, last_scanned=? where path=?", true, lastScanned, path);
    }

    public void markNonPresent(Date lastScanned) {
        int minId = queryForInt("select top 1 id from media_file where last_scanned != ? and present", 0, lastScanned);
        int maxId = queryForInt("select max(id) from media_file where last_scanned != ? and present", 0, lastScanned);

        final int batchSize = 1000;
        Date childrenLastUpdated = new Date(0L);  // Used to force a children rescan if file is later resurrected.
        for (int id = minId; id <= maxId; id += batchSize) {
            update("update media_file set present=false, children_last_updated=? where id between ? and ? and last_scanned != ? and present",
                   childrenLastUpdated, id, id + batchSize, lastScanned);
        }
    }

    public void expunge() {
        int minId = queryForInt("select top 1 id from media_file where not present", 0);
        int maxId = queryForInt("select max(id) from media_file where not present", 0);

        final int batchSize = 1000;
        for (int id = minId; id <= maxId; id += batchSize) {
            update("delete from media_file where id between ? and ? and not present", id, id + batchSize);
        }
        update("checkpoint");
    }

    private static class MediaFileMapper implements RowMapper<MediaFile> {
        public MediaFile mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new MediaFile(
                    rs.getInt(1),
                    rs.getString(2),
                    rs.getString(3),
                    MediaType.valueOf(rs.getString(4)),
                    rs.getString(5),
                    rs.getString(6),
                    rs.getString(7),
                    rs.getString(8),
                    rs.getString(9),
                    rs.getInt(10) == 0 ? null : rs.getInt(10),
                    rs.getInt(11) == 0 ? null : rs.getInt(11),
                    rs.getInt(12) == 0 ? null : rs.getInt(12),
                    rs.getString(13),
                    rs.getInt(14) == 0 ? null : rs.getInt(14),
                    rs.getBoolean(15),
                    rs.getInt(16) == 0 ? null : rs.getInt(16),
                    rs.getLong(17) == 0 ? null : rs.getLong(17),
                    rs.getInt(18) == 0 ? null : rs.getInt(18),
                    rs.getInt(19) == 0 ? null : rs.getInt(19),
                    rs.getString(20),
                    rs.getString(21),
                    rs.getInt(22),
                    rs.getTimestamp(23),
                    rs.getString(24),
                    rs.getTimestamp(25),
                    rs.getTimestamp(26),
                    rs.getTimestamp(27),
                    rs.getTimestamp(28),
                    rs.getBoolean(29),
                    rs.getInt(30));
        }
    }

    private static class MusicFileInfoMapper implements RowMapper<MediaFile> {
        public MediaFile mapRow(ResultSet rs, int rowNum) throws SQLException {
            MediaFile file = new MediaFile();
            file.setPlayCount(rs.getInt(1));
            file.setLastPlayed(rs.getTimestamp(2));
            file.setComment(rs.getString(3));
            return file;
        }
    }

    private static class GenreMapper implements RowMapper<Genre> {
        public Genre mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Genre(rs.getString(1), rs.getInt(2), rs.getInt(3));
        }
    }
}
