package org.airsonic.player.dao;

import org.airsonic.player.domain.PodcastChannel;
import org.airsonic.player.domain.PodcastEpisode;
import org.airsonic.player.domain.PodcastStatus;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit test of {@link PodcastDao}.
 *
 * @author Sindre Mehus
 */
public class PodcastDaoTestCase extends DaoTestCaseBean2 {

    @Autowired
    PodcastDao podcastDao;

    @Before
    public void setUp() {
        getJdbcTemplate().execute("delete from podcast_channel");
    }

    @Test
    public void testCreateChannel() {
        PodcastChannel channel = new PodcastChannel("http://foo");
        podcastDao.createChannel(channel);

        PodcastChannel newChannel = podcastDao.getAllChannels().get(0);
        assertNotNull("Wrong ID.", newChannel.getId());
        assertChannelEquals(channel, newChannel);
    }

    @Test
    public void testChannelId() {
        int channelId = podcastDao.createChannel(new PodcastChannel("http://foo"));

        assertEquals("Error in createChannel.", channelId + 1, podcastDao.createChannel(new PodcastChannel("http://foo")));
        assertEquals("Error in createChannel.", channelId + 2, podcastDao.createChannel(new PodcastChannel("http://foo")));
        assertEquals("Error in createChannel.", channelId + 3, podcastDao.createChannel(new PodcastChannel("http://foo")));

        podcastDao.deleteChannel(channelId + 1);
        assertEquals("Error in createChannel.", channelId + 4, podcastDao.createChannel(new PodcastChannel("http://foo")));

        podcastDao.deleteChannel(channelId + 4);
        assertEquals("Error in createChannel.", channelId + 5, podcastDao.createChannel(new PodcastChannel("http://foo")));
    }

    @Test
    public void testUpdateChannel() {
        PodcastChannel channel = new PodcastChannel("http://foo");
        podcastDao.createChannel(channel);
        channel = podcastDao.getAllChannels().get(0);

        channel.setUrl("http://bar");
        channel.setTitle("Title");
        channel.setDescription("Description");
        channel.setImageUrl("http://foo/bar.jpg");
        channel.setStatus(PodcastStatus.ERROR);
        channel.setErrorMessage("Something went terribly wrong.");

        podcastDao.updateChannel(channel);
        PodcastChannel newChannel = podcastDao.getAllChannels().get(0);

        assertEquals("Wrong ID.", channel.getId(), newChannel.getId());
        assertChannelEquals(channel, newChannel);
    }

    @Test
    public void testDeleteChannel() {
        assertEquals("Wrong number of channels.", 0, podcastDao.getAllChannels().size());

        PodcastChannel channel = new PodcastChannel("http://foo");
        podcastDao.createChannel(channel);
        assertEquals("Wrong number of channels.", 1, podcastDao.getAllChannels().size());

        podcastDao.createChannel(channel);
        assertEquals("Wrong number of channels.", 2, podcastDao.getAllChannels().size());

        podcastDao.deleteChannel(podcastDao.getAllChannels().get(0).getId());
        assertEquals("Wrong number of channels.", 1, podcastDao.getAllChannels().size());

        podcastDao.deleteChannel(podcastDao.getAllChannels().get(0).getId());
        assertEquals("Wrong number of channels.", 0, podcastDao.getAllChannels().size());
    }

    @Test
    public void testCreateEpisode() {
        int channelId = createChannel();
        PodcastEpisode episode = new PodcastEpisode(null, channelId, "http://bar", "path", "title", "description",
                new Date(), "12:34", null, null, PodcastStatus.NEW, null);
        podcastDao.createEpisode(episode);

        PodcastEpisode newEpisode = podcastDao.getEpisodes(channelId).get(0);
        assertNotNull("Wrong ID.", newEpisode.getId());
        assertEpisodeEquals(episode, newEpisode);
    }

    @Test
    public void testGetEpisode() {
        assertNull("Error in getEpisode()", podcastDao.getEpisode(23));

        int channelId = createChannel();
        PodcastEpisode episode = new PodcastEpisode(null, channelId, "http://bar", "path", "title", "description",
                new Date(), "12:34", 3276213L, 2341234L, PodcastStatus.NEW, "error");
        podcastDao.createEpisode(episode);

        int episodeId = podcastDao.getEpisodes(channelId).get(0).getId();
        PodcastEpisode newEpisode = podcastDao.getEpisode(episodeId);
        assertEpisodeEquals(episode, newEpisode);
    }

    @Test
    public void testGetEpisodes() {
        int channelId = createChannel();
        PodcastEpisode a = new PodcastEpisode(null, channelId, "a", null, null, null,
                new Date(3000), null, null, null, PodcastStatus.NEW, null);
        PodcastEpisode b = new PodcastEpisode(null, channelId, "b", null, null, null,
                new Date(1000), null, null, null, PodcastStatus.NEW, "error");
        PodcastEpisode c = new PodcastEpisode(null, channelId, "c", null, null, null,
                new Date(2000), null, null, null, PodcastStatus.NEW, null);
        PodcastEpisode d = new PodcastEpisode(null, channelId, "c", null, null, null,
                null, null, null, null, PodcastStatus.NEW, "");
        podcastDao.createEpisode(a);
        podcastDao.createEpisode(b);
        podcastDao.createEpisode(c);
        podcastDao.createEpisode(d);

        List<PodcastEpisode> episodes = podcastDao.getEpisodes(channelId);
        assertEquals("Error in getEpisodes().", 4, episodes.size());
        assertEpisodeEquals(d, episodes.get(0));
        assertEpisodeEquals(a, episodes.get(1));
        assertEpisodeEquals(c, episodes.get(2));
        assertEpisodeEquals(b, episodes.get(3));
    }


    @Test
    public void testUpdateEpisode() {
        int channelId = createChannel();
        PodcastEpisode episode = new PodcastEpisode(null, channelId, "http://bar", null, null, null,
                null, null, null, null, PodcastStatus.NEW, null);
        podcastDao.createEpisode(episode);
        episode = podcastDao.getEpisodes(channelId).get(0);

        episode.setUrl("http://bar");
        episode.setPath("c:/tmp");
        episode.setTitle("Title");
        episode.setDescription("Description");
        episode.setPublishDate(new Date());
        episode.setDuration("1:20");
        episode.setBytesTotal(87628374612L);
        episode.setBytesDownloaded(9086L);
        episode.setStatus(PodcastStatus.DOWNLOADING);
        episode.setErrorMessage("Some error");

        podcastDao.updateEpisode(episode);
        PodcastEpisode newEpisode = podcastDao.getEpisodes(channelId).get(0);
        assertEquals("Wrong ID.", episode.getId(), newEpisode.getId());
        assertEpisodeEquals(episode, newEpisode);
    }

    @Test
    public void testDeleteEpisode() {
        int channelId = createChannel();

        assertEquals("Wrong number of episodes.", 0, podcastDao.getEpisodes(channelId).size());

        PodcastEpisode episode = new PodcastEpisode(null, channelId, "http://bar", null, null, null,
                null, null, null, null, PodcastStatus.NEW, null);

        podcastDao.createEpisode(episode);
        assertEquals("Wrong number of episodes.", 1, podcastDao.getEpisodes(channelId).size());

        podcastDao.createEpisode(episode);
        assertEquals("Wrong number of episodes.", 2, podcastDao.getEpisodes(channelId).size());

        podcastDao.deleteEpisode(podcastDao.getEpisodes(channelId).get(0).getId());
        assertEquals("Wrong number of episodes.", 1, podcastDao.getEpisodes(channelId).size());

        podcastDao.deleteEpisode(podcastDao.getEpisodes(channelId).get(0).getId());
        assertEquals("Wrong number of episodes.", 0, podcastDao.getEpisodes(channelId).size());
    }


    @Test
    public void testCascadingDelete() {
        int channelId = createChannel();
        PodcastEpisode episode = new PodcastEpisode(null, channelId, "http://bar", null, null, null,
                null, null, null, null, PodcastStatus.NEW, null);
        podcastDao.createEpisode(episode);
        podcastDao.createEpisode(episode);
        assertEquals("Wrong number of episodes.", 2, podcastDao.getEpisodes(channelId).size());

        podcastDao.deleteChannel(channelId);
        assertEquals("Wrong number of episodes.", 0, podcastDao.getEpisodes(channelId).size());
    }

    private int createChannel() {
        PodcastChannel channel = new PodcastChannel("http://foo");
        podcastDao.createChannel(channel);
        channel = podcastDao.getAllChannels().get(0);
        return channel.getId();
    }

    private void assertChannelEquals(PodcastChannel expected, PodcastChannel actual) {
        assertEquals("Wrong URL.", expected.getUrl(), actual.getUrl());
        assertEquals("Wrong title.", expected.getTitle(), actual.getTitle());
        assertEquals("Wrong description.", expected.getDescription(), actual.getDescription());
        assertEquals("Wrong image URL.", expected.getImageUrl(), actual.getImageUrl());
        assertSame("Wrong status.", expected.getStatus(), actual.getStatus());
        assertEquals("Wrong error message.", expected.getErrorMessage(), actual.getErrorMessage());
    }

    private void assertEpisodeEquals(PodcastEpisode expected, PodcastEpisode actual) {
        assertEquals("Wrong URL.", expected.getUrl(), actual.getUrl());
        assertEquals("Wrong path.", expected.getPath(), actual.getPath());
        assertEquals("Wrong title.", expected.getTitle(), actual.getTitle());
        assertEquals("Wrong description.", expected.getDescription(), actual.getDescription());
        assertEquals("Wrong date.", expected.getPublishDate(), actual.getPublishDate());
        assertEquals("Wrong duration.", expected.getDuration(), actual.getDuration());
        assertEquals("Wrong bytes total.", expected.getBytesTotal(), actual.getBytesTotal());
        assertEquals("Wrong bytes downloaded.", expected.getBytesDownloaded(), actual.getBytesDownloaded());
        assertSame("Wrong status.", expected.getStatus(), actual.getStatus());
        assertEquals("Wrong error message.", expected.getErrorMessage(), actual.getErrorMessage());
    }

}
