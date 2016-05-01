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
package net.sourceforge.subsonic.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import net.sourceforge.subsonic.domain.PodcastChannel;
import net.sourceforge.subsonic.domain.PodcastEpisode;
import net.sourceforge.subsonic.domain.PodcastStatus;

/**
 * Provides database services for Podcast channels and episodes.
 *
 * @author Sindre Mehus
 */
public class PodcastDao extends AbstractDao {

    private static final String CHANNEL_COLUMNS = "id, url, title, description, image_url, status, error_message";
    private static final String EPISODE_COLUMNS = "id, channel_id, url, path, title, description, publish_date, " +
            "duration, bytes_total, bytes_downloaded, status, error_message";

    private PodcastChannelRowMapper channelRowMapper = new PodcastChannelRowMapper();
    private PodcastEpisodeRowMapper episodeRowMapper = new PodcastEpisodeRowMapper();

    /**
     * Creates a new Podcast channel.
     *
     * @param channel The Podcast channel to create.
     * @return The ID of the newly created channel.
     */
    public synchronized int createChannel(PodcastChannel channel) {
        String sql = "insert into podcast_channel (" + CHANNEL_COLUMNS + ") values (" + questionMarks(CHANNEL_COLUMNS) + ")";
        update(sql, null, channel.getUrl(), channel.getTitle(), channel.getDescription(), channel.getImageUrl(),
                channel.getStatus().name(), channel.getErrorMessage());

        return getJdbcTemplate().queryForInt("select max(id) from podcast_channel");
    }

    /**
     * Returns all Podcast channels.
     *
     * @return Possibly empty list of all Podcast channels.
     */
    public List<PodcastChannel> getAllChannels() {
        String sql = "select " + CHANNEL_COLUMNS + " from podcast_channel";
        return query(sql, channelRowMapper);
    }

    /**
     * Returns a single Podcast channel.
     */
    public PodcastChannel getChannel(int channelId) {
        String sql = "select " + CHANNEL_COLUMNS + " from podcast_channel where id=?";
        return queryOne(sql, channelRowMapper, channelId);
    }

    /**
     * Updates the given Podcast channel.
     *
     * @param channel The Podcast channel to update.
     */
    public void updateChannel(PodcastChannel channel) {
        String sql = "update podcast_channel set url=?, title=?, description=?, image_url=?, status=?, error_message=? where id=?";
        update(sql, channel.getUrl(), channel.getTitle(), channel.getDescription(), channel.getImageUrl(),
                channel.getStatus().name(), channel.getErrorMessage(), channel.getId());
    }

    /**
     * Deletes the Podcast channel with the given ID.
     *
     * @param id The Podcast channel ID.
     */
    public void deleteChannel(int id) {
        String sql = "delete from podcast_channel where id=?";
        update(sql, id);
    }

    /**
     * Creates a new Podcast episode.
     *
     * @param episode The Podcast episode to create.
     */
    public void createEpisode(PodcastEpisode episode) {
        String sql = "insert into podcast_episode (" + EPISODE_COLUMNS + ") values (" + questionMarks(EPISODE_COLUMNS) + ")";
        update(sql, null, episode.getChannelId(), episode.getUrl(), episode.getPath(),
                episode.getTitle(), episode.getDescription(), episode.getPublishDate(),
                episode.getDuration(), episode.getBytesTotal(), episode.getBytesDownloaded(),
                episode.getStatus().name(), episode.getErrorMessage());
    }

    /**
     * Returns all Podcast episodes for a given channel.
     *
     * @return Possibly empty list of all Podcast episodes for the given channel, sorted in
     *         reverse chronological order (newest episode first).
     */
    public List<PodcastEpisode> getEpisodes(int channelId) {
        String sql = "select " + EPISODE_COLUMNS + " from podcast_episode where channel_id = ? " +
                     "and status != ? order by publish_date desc";
        return query(sql, episodeRowMapper, channelId, PodcastStatus.DELETED);
    }

    /**
     * Returns the N newest episodes.
     *
     * @return Possibly empty list of the newest Podcast episodes, sorted in
     *         reverse chronological order (newest episode first).
     */
    public List<PodcastEpisode> getNewestEpisodes(int count) {
        String sql = "select " + EPISODE_COLUMNS + " from podcast_episode where status = ? and publish_date is not null " +
                     "order by publish_date desc limit ?";
        return query(sql, episodeRowMapper, PodcastStatus.COMPLETED, count);
    }

    /**
     * Returns the Podcast episode with the given ID.
     *
     * @param episodeId The Podcast episode ID.
     * @return The episode or <code>null</code> if not found.
     */
    public PodcastEpisode getEpisode(int episodeId) {
        String sql = "select " + EPISODE_COLUMNS + " from podcast_episode where id=?";
        return queryOne(sql, episodeRowMapper, episodeId);
    }

    public PodcastEpisode getEpisodeByUrl(String url) {
        String sql = "select " + EPISODE_COLUMNS + " from podcast_episode where url=?";
        return queryOne(sql, episodeRowMapper, url);
    }

    /**
     * Updates the given Podcast episode.
     *
     * @param episode The Podcast episode to update.
     * @return The number of episodes updated (zero or one).
     */
    public int updateEpisode(PodcastEpisode episode) {
        String sql = "update podcast_episode set url=?, path=?, title=?, description=?, publish_date=?, duration=?, " +
                "bytes_total=?, bytes_downloaded=?, status=?, error_message=? where id=?";
        return update(sql, episode.getUrl(), episode.getPath(), episode.getTitle(),
                episode.getDescription(), episode.getPublishDate(), episode.getDuration(),
                episode.getBytesTotal(), episode.getBytesDownloaded(), episode.getStatus().name(),
                episode.getErrorMessage(), episode.getId());
    }

    /**
     * Deletes the Podcast episode with the given ID.
     *
     * @param id The Podcast episode ID.
     */
    public void deleteEpisode(int id) {
        String sql = "delete from podcast_episode where id=?";
        update(sql, id);
    }

    private static class PodcastChannelRowMapper implements RowMapper {
        public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new PodcastChannel(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5),
                                      PodcastStatus.valueOf(rs.getString(6)), rs.getString(7));
        }
    }

    private static class PodcastEpisodeRowMapper implements ParameterizedRowMapper<PodcastEpisode> {
        public PodcastEpisode mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new PodcastEpisode(rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getString(4), rs.getString(5),
                    rs.getString(6), rs.getTimestamp(7), rs.getString(8), (Long) rs.getObject(9),
                    (Long) rs.getObject(10), PodcastStatus.valueOf(rs.getString(11)), rs.getString(12));
        }
    }
}
