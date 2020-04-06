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

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.airsonic.player.dao.PodcastDao;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.PodcastChannel;
import org.airsonic.player.domain.PodcastEpisode;
import org.airsonic.player.domain.PodcastStatus;
import org.airsonic.player.service.metadata.MetaData;
import org.airsonic.player.service.metadata.MetaDataParser;
import org.airsonic.player.service.metadata.MetaDataParserFactory;
import org.airsonic.player.util.FileUtil;
import org.airsonic.player.util.StringUtil;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

import static org.airsonic.player.util.XMLUtil.createSAXBuilder;

/**
 * Provides services for Podcast reception.
 *
 * @author Sindre Mehus
 */
@Service
public class PodcastService {

    private static final Logger LOG = LoggerFactory.getLogger(PodcastService.class);
    private static final DateFormat[] RSS_DATE_FORMATS = {new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US),
        new SimpleDateFormat("dd MMM yyyy HH:mm:ss Z", Locale.US)};

    private static final Namespace[] ITUNES_NAMESPACES = {Namespace.getNamespace("http://www.itunes.com/DTDs/Podcast-1.0.dtd"),
        Namespace.getNamespace("http://www.itunes.com/dtds/podcast-1.0.dtd")};

    private final ExecutorService refreshExecutor;
    private final ExecutorService downloadExecutor;
    private final ScheduledExecutorService scheduledExecutor;
    private ScheduledFuture<?> scheduledRefresh;
    @Autowired
    private PodcastDao podcastDao;
    @Autowired
    private SettingsService settingsService;
    @Autowired
    private SecurityService securityService;
    @Autowired
    private MediaFileService mediaFileService;
    @Autowired
    private MetaDataParserFactory metaDataParserFactory;

    public PodcastService() {
        ThreadFactory threadFactory = r -> {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setDaemon(true);
            return t;
        };
        refreshExecutor = Executors.newFixedThreadPool(5, threadFactory);
        downloadExecutor = Executors.newFixedThreadPool(3, threadFactory);
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor(threadFactory);
    }

    @PostConstruct
    public synchronized void init() {
        try {
            // Clean up partial downloads.
            for (PodcastChannel channel : getAllChannels()) {
                for (PodcastEpisode episode : getEpisodes(channel.getId())) {
                    if (episode.getStatus() == PodcastStatus.DOWNLOADING) {
                        deleteEpisode(episode.getId(), false);
                        LOG.info("Deleted Podcast episode '" + episode.getTitle() + "' since download was interrupted.");
                    }
                }
            }
            schedule();
        } catch (Throwable x) {
            LOG.error("Failed to initialize PodcastService: " + x, x);
        }
    }

    public synchronized void schedule() {
        Runnable task = () -> {
            LOG.info("Starting scheduled Podcast refresh.");
            refreshAllChannels(true);
            LOG.info("Completed scheduled Podcast refresh.");
        };

        if (scheduledRefresh != null) {
            scheduledRefresh.cancel(true);
        }

        int hoursBetween = settingsService.getPodcastUpdateInterval();

        if (hoursBetween == -1) {
            LOG.info("Automatic Podcast update disabled.");
            return;
        }

        long periodMillis = hoursBetween * 60L * 60L * 1000L;
        long initialDelayMillis = 5L * 60L * 1000L;

        scheduledRefresh = scheduledExecutor.scheduleAtFixedRate(task, initialDelayMillis, periodMillis, TimeUnit.MILLISECONDS);
        Date firstTime = new Date(System.currentTimeMillis() + initialDelayMillis);
        LOG.info("Automatic Podcast update scheduled to run every " + hoursBetween + " hour(s), starting at " + firstTime);
    }

    /**
     * Creates a new Podcast channel.
     *
     * @param url The URL of the Podcast channel.
     */
    public void createChannel(String url) {
        url = sanitizeUrl(url);
        PodcastChannel channel = new PodcastChannel(url);
        int channelId = podcastDao.createChannel(channel);

        refreshChannels(Arrays.asList(getChannel(channelId)), true);
    }

    private String sanitizeUrl(String url) {
        return url.replace(" ", "%20");
    }

    /**
     * Returns a single Podcast channel.
     */
    public PodcastChannel getChannel(int channelId) {
        PodcastChannel channel = podcastDao.getChannel(channelId);
        if (channel.getTitle() != null)
            addMediaFileIdToChannels(Arrays.asList(channel));
        return channel;
    }

    /**
     * Returns all Podcast channels.
     *
     * @return Possibly empty list of all Podcast channels.
     */
    public List<PodcastChannel> getAllChannels() {
        return addMediaFileIdToChannels(podcastDao.getAllChannels());
    }

    private PodcastEpisode getEpisodeByUrl(String url) {
        PodcastEpisode episode = podcastDao.getEpisodeByUrl(url);
        if (episode == null) {
            return null;
        }
        List<PodcastEpisode> episodes = Arrays.asList(episode);
        episodes = filterAllowed(episodes);
        addMediaFileIdToEpisodes(episodes);
        return episodes.isEmpty() ? null : episodes.get(0);
    }

    /**
     * Returns all Podcast episodes for a given channel.
     *
     * @param channelId      The Podcast channel ID.
     * @return Possibly empty list of all Podcast episodes for the given channel, sorted in
     *         reverse chronological order (newest episode first).
     */
    public List<PodcastEpisode> getEpisodes(int channelId) {
        List<PodcastEpisode> episodes = filterAllowed(podcastDao.getEpisodes(channelId));
        return addMediaFileIdToEpisodes(episodes);
    }

    /**
     * Returns the N newest episodes.
     *
     * @return Possibly empty list of the newest Podcast episodes, sorted in
     *         reverse chronological order (newest episode first).
     */
    public List<PodcastEpisode> getNewestEpisodes(int count) {
        List<PodcastEpisode> episodes = addMediaFileIdToEpisodes(podcastDao.getNewestEpisodes(count));

        return Lists.newArrayList(Iterables.filter(episodes, episode -> {
            Integer mediaFileId = episode.getMediaFileId();
            if (mediaFileId == null) {
                return false;
            }
            MediaFile mediaFile = mediaFileService.getMediaFile(mediaFileId);
            return mediaFile != null && mediaFile.isPresent();
        }));
    }

    private List<PodcastEpisode> filterAllowed(List<PodcastEpisode> episodes) {
        List<PodcastEpisode> result = new ArrayList<>(episodes.size());
        for (PodcastEpisode episode : episodes) {
            if (episode.getPath() == null || securityService.isReadAllowed(new File(episode.getPath()))) {
                result.add(episode);
            }
        }
        return result;
    }

    public PodcastEpisode getEpisode(int episodeId, boolean includeDeleted) {
        PodcastEpisode episode = podcastDao.getEpisode(episodeId);
        if (episode == null) {
            return null;
        }
        if (episode.getStatus() == PodcastStatus.DELETED && !includeDeleted) {
            return null;
        }
        addMediaFileIdToEpisodes(Arrays.asList(episode));
        return episode;
    }

    private List<PodcastEpisode> addMediaFileIdToEpisodes(List<PodcastEpisode> episodes) {
        for (PodcastEpisode episode : episodes) {
            if (episode.getPath() != null) {
                MediaFile mediaFile = mediaFileService.getMediaFile(episode.getPath());
                if (mediaFile != null && mediaFile.isPresent()) {
                    episode.setMediaFileId(mediaFile.getId());
                }
            }
        }
        return episodes;
    }

    private List<PodcastChannel> addMediaFileIdToChannels(List<PodcastChannel> channels) {
        for (PodcastChannel channel : channels) {
            try {
                if (channel.getTitle() == null) {
                    LOG.warn("Podcast channel id {} has null title", channel.getId());
                    continue;
                }
                File dir = getChannelDirectory(channel);
                MediaFile mediaFile = mediaFileService.getMediaFile(dir);
                if (mediaFile != null) {
                    channel.setMediaFileId(mediaFile.getId());
                }
            } catch (Exception x) {
                LOG.warn("Failed to resolve media file ID for podcast channel '" + channel.getTitle() + "': " + x, x);
            }
        }
        return channels;
    }

    public void refreshChannel(int channelId, boolean downloadEpisodes) {
        refreshChannels(Arrays.asList(getChannel(channelId)), downloadEpisodes);
    }

    public void refreshAllChannels(boolean downloadEpisodes) {
        refreshChannels(getAllChannels(), downloadEpisodes);
    }

    private void refreshChannels(final List<PodcastChannel> channels, final boolean downloadEpisodes) {
        for (final PodcastChannel channel : channels) {
            Runnable task = () -> doRefreshChannel(channel, downloadEpisodes);
            refreshExecutor.submit(task);
        }
    }

    private void doRefreshChannel(PodcastChannel channel, boolean downloadEpisodes) {
        InputStream in = null;

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            channel.setStatus(PodcastStatus.DOWNLOADING);
            channel.setErrorMessage(null);
            podcastDao.updateChannel(channel);
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(2 * 60 * 1000) // 2 minutes
                    .setSocketTimeout(10 * 60 * 1000) // 10 minutes
                    .build();
            HttpGet method = new HttpGet(channel.getUrl());
            method.setConfig(requestConfig);

            try (CloseableHttpResponse response = client.execute(method)) {
                in = response.getEntity().getContent();

                Document document = createSAXBuilder().build(in);
                Element channelElement = document.getRootElement().getChild("channel");

                channel.setTitle(StringUtil.removeMarkup(channelElement.getChildTextTrim("title")));
                channel.setDescription(StringUtil.removeMarkup(channelElement.getChildTextTrim("description")));
                channel.setImageUrl(getChannelImageUrl(channelElement));
                channel.setStatus(PodcastStatus.COMPLETED);
                channel.setErrorMessage(null);
                podcastDao.updateChannel(channel);

                downloadImage(channel);
                refreshEpisodes(channel, channelElement.getChildren("item"));
            }
        } catch (Exception x) {
            LOG.warn("Failed to get/parse RSS file for Podcast channel " + channel.getUrl(), x);
            channel.setStatus(PodcastStatus.ERROR);
            channel.setErrorMessage(getErrorMessage(x));
            podcastDao.updateChannel(channel);
        } finally {
            FileUtil.closeQuietly(in);
        }

        if (downloadEpisodes) {
            for (final PodcastEpisode episode : getEpisodes(channel.getId())) {
                if (episode.getStatus() == PodcastStatus.NEW && episode.getUrl() != null) {
                    downloadEpisode(episode);
                }
            }
        }
    }

    private void downloadImage(PodcastChannel channel) {
        InputStream in = null;
        OutputStream out = null;
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            String imageUrl = channel.getImageUrl();
            if (imageUrl == null) {
                return;
            }

            File dir = getChannelDirectory(channel);
            MediaFile channelMediaFile = mediaFileService.getMediaFile(dir);
            File existingCoverArt = mediaFileService.getCoverArt(channelMediaFile);
            boolean imageFileExists = existingCoverArt != null && mediaFileService.getMediaFile(existingCoverArt) == null;
            if (imageFileExists) {
                return;
            }

            HttpGet method = new HttpGet(imageUrl);
            try (CloseableHttpResponse response = client.execute(method)) {
                in = response.getEntity().getContent();
                out = new FileOutputStream(new File(dir, "cover." + getCoverArtSuffix(response)));
                IOUtils.copy(in, out);
                mediaFileService.refreshMediaFile(channelMediaFile);
            }
        } catch (Exception x) {
            LOG.warn("Failed to download cover art for podcast channel '" + channel.getTitle() + "': " + x, x);
        } finally {
            FileUtil.closeQuietly(in);
            FileUtil.closeQuietly(out);
        }
    }

    private String getCoverArtSuffix(HttpResponse response) {
        String result = null;
        Header contentTypeHeader = response.getEntity().getContentType();
        if (contentTypeHeader != null && contentTypeHeader.getValue() != null) {
            ContentType contentType = ContentType.parse(contentTypeHeader.getValue());
            String mimeType = contentType.getMimeType();
            result = StringUtil.getSuffix(mimeType);
        }
        return result == null ? "jpeg" : result;
    }

    private String getChannelImageUrl(Element channelElement) {
        String result = getITunesAttribute(channelElement, "image", "href");
        if (result == null) {
            Element imageElement = channelElement.getChild("image");
            if (imageElement != null) {
                result = imageElement.getChildTextTrim("url");
            }
        }
        return result;
    }

    private String getErrorMessage(Exception x) {
        return x.getMessage() != null ? x.getMessage() : x.toString();
    }

    public void downloadEpisode(final PodcastEpisode episode) {
        Runnable task = () -> doDownloadEpisode(episode);
        downloadExecutor.submit(task);
    }

    private void refreshEpisodes(PodcastChannel channel, List<Element> episodeElements) {

        List<PodcastEpisode> episodes = new ArrayList<>();

        for (Element episodeElement : episodeElements) {

            String title = episodeElement.getChildTextTrim("title");
            String duration = formatDuration(getITunesElement(episodeElement, "duration"));
            String description = episodeElement.getChildTextTrim("description");
            if (StringUtils.isBlank(description)) {
                description = getITunesElement(episodeElement, "summary");
            }
            title = StringUtil.removeMarkup(title);
            description = StringUtil.removeMarkup(description);

            Element enclosure = episodeElement.getChild("enclosure");
            if (enclosure == null) {
                LOG.info("No enclosure found for episode " + title);
                continue;
            }

            String url = enclosure.getAttributeValue("url");
            url = sanitizeUrl(url);
            if (url == null) {
                LOG.info("No enclosure URL found for episode " + title);
                continue;
            }

            if (getEpisodeByUrl(url) == null) {
                Long length = null;
                try {
                    length = Long.valueOf(enclosure.getAttributeValue("length"));
                } catch (Exception x) {
                    LOG.warn("Failed to parse enclosure length.", x);
                }

                Date date = parseDate(episodeElement.getChildTextTrim("pubDate"));
                PodcastEpisode episode = new PodcastEpisode(null, channel.getId(), url, null, title, description, date,
                        duration, length, 0L, PodcastStatus.NEW, null);
                episodes.add(episode);
                LOG.info("Created Podcast episode " + title);
            }
        }

        // Sort episode in reverse chronological order (newest first)
        episodes.sort((a, b) -> {
            long timeA = a.getPublishDate() == null ? 0L : a.getPublishDate().getTime();
            long timeB = b.getPublishDate() == null ? 0L : b.getPublishDate().getTime();

            return Long.compare(timeB, timeA);
        });

        // Create episodes in database, skipping the proper number of episodes.
        int downloadCount = settingsService.getPodcastEpisodeDownloadCount();
        if (downloadCount == -1) {
            downloadCount = Integer.MAX_VALUE;
        }

        for (int i = 0; i < episodes.size(); i++) {
            PodcastEpisode episode = episodes.get(i);
            if (i >= downloadCount) {
                episode.setStatus(PodcastStatus.SKIPPED);
            }
            podcastDao.createEpisode(episode);
        }
    }

    private Date parseDate(String s) {
        for (DateFormat dateFormat : RSS_DATE_FORMATS) {
            try {
                return dateFormat.parse(s);
            } catch (Exception x) {
                // Ignored.
            }
        }
        LOG.warn("Failed to parse publish date: '" + s + "'.");
        return null;
    }

    private String formatDuration(String duration) {
        if (duration == null) return null;
        if (duration.matches("^\\d+$")) {
            long seconds = Long.valueOf(duration);
            if (seconds >= 3600)
                return String.format("%02d:%02d:%02d", seconds / 3600, seconds / 60, seconds % 60);
            else
                return String.format("%02d:%02d", seconds / 60, seconds % 60);
        } else {
            return duration;
        }
    }

    private String getITunesElement(Element element, String childName) {
        for (Namespace ns : ITUNES_NAMESPACES) {
            String value = element.getChildTextTrim(childName, ns);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String getITunesAttribute(Element element, String childName, String attributeName) {
        for (Namespace ns : ITUNES_NAMESPACES) {
            Element elem = element.getChild(childName, ns);
            if (elem != null) {
                return StringUtils.trimToNull(elem.getAttributeValue(attributeName));
            }
        }
        return null;
    }

    private void doDownloadEpisode(PodcastEpisode episode) {
        InputStream in = null;
        OutputStream out = null;

        if (isEpisodeDeleted(episode)) {
            LOG.info("Podcast " + episode.getUrl() + " was deleted. Aborting download.");
            return;
        }

        LOG.info("Starting to download Podcast from " + episode.getUrl());

        try (CloseableHttpClient client = HttpClients.createDefault()) {

            PodcastChannel channel = getChannel(episode.getChannelId());
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(2 * 60 * 1000) // 2 minutes
                    .setSocketTimeout(10 * 60 * 1000) // 10 minutes
                    // Workaround HttpClient circular redirects, which some feeds use (with query parameters)
                    .setCircularRedirectsAllowed(true)
                    // Workaround HttpClient not understanding latest RFC-compliant cookie 'expires' attributes
                    .setCookieSpec(CookieSpecs.STANDARD)
                    .build();
            HttpGet method = new HttpGet(episode.getUrl());
            method.setConfig(requestConfig);

            try (CloseableHttpResponse response = client.execute(method)) {
                in = response.getEntity().getContent();

                File file = getFile(channel, episode);
                out = new FileOutputStream(file);

                episode.setStatus(PodcastStatus.DOWNLOADING);
                episode.setBytesDownloaded(0L);
                episode.setErrorMessage(null);
                episode.setPath(file.getPath());
                podcastDao.updateEpisode(episode);

                byte[] buffer = new byte[4096];
                long bytesDownloaded = 0;
                int n;
                long nextLogCount = 30000L;

                while ((n = in.read(buffer)) != -1) {
                    out.write(buffer, 0, n);
                    bytesDownloaded += n;

                    if (bytesDownloaded > nextLogCount) {
                        episode.setBytesDownloaded(bytesDownloaded);
                        nextLogCount += 30000L;

                        // Abort download if episode was deleted by user.
                        if (isEpisodeDeleted(episode)) {
                            break;
                        }
                        podcastDao.updateEpisode(episode);
                    }
                }

                if (isEpisodeDeleted(episode)) {
                    LOG.info("Podcast " + episode.getUrl() + " was deleted. Aborting download.");
                    FileUtil.closeQuietly(out);
                    if (!file.delete()) {
                        LOG.warn("Unable to delete " + file);
                    }
                } else {
                    addMediaFileIdToEpisodes(Arrays.asList(episode));
                    episode.setBytesDownloaded(bytesDownloaded);
                    podcastDao.updateEpisode(episode);
                    LOG.info("Downloaded " + bytesDownloaded + " bytes from Podcast " + episode.getUrl());
                    FileUtil.closeQuietly(out);
                    updateTags(file, episode);
                    episode.setStatus(PodcastStatus.COMPLETED);
                    podcastDao.updateEpisode(episode);
                    deleteObsoleteEpisodes(channel);
                }
            }
        } catch (Exception x) {
            LOG.warn("Failed to download Podcast from " + episode.getUrl(), x);
            episode.setStatus(PodcastStatus.ERROR);
            episode.setErrorMessage(getErrorMessage(x));
            podcastDao.updateEpisode(episode);
        } finally {
            FileUtil.closeQuietly(in);
            FileUtil.closeQuietly(out);
        }
    }

    private boolean isEpisodeDeleted(PodcastEpisode episode) {
        episode = podcastDao.getEpisode(episode.getId());
        return episode == null || episode.getStatus() == PodcastStatus.DELETED;
    }

    private void updateTags(File file, PodcastEpisode episode) {
        try {
            MediaFile mediaFile = mediaFileService.getMediaFile(file, false);
            if (StringUtils.isNotBlank(episode.getTitle())) {
                MetaDataParser parser = metaDataParserFactory.getParser(file);
                if (!parser.isEditingSupported()) {
                    return;
                }
                MetaData metaData = parser.getRawMetaData(file);
                metaData.setTitle(episode.getTitle());
                parser.setMetaData(mediaFile, metaData);
                mediaFileService.refreshMediaFile(mediaFile);
            }
        } catch (Exception x) {
            LOG.warn("Failed to update tags for podcast " + episode.getUrl(), x);
        }
    }

    private synchronized void deleteObsoleteEpisodes(PodcastChannel channel) {
        int episodeCount = settingsService.getPodcastEpisodeRetentionCount();
        if (episodeCount == -1) {
            return;
        }

        List<PodcastEpisode> episodes = getEpisodes(channel.getId());

        // Don't do anything if other episodes of the same channel is currently downloading.
        for (PodcastEpisode episode : episodes) {
            if (episode.getStatus() == PodcastStatus.DOWNLOADING) {
                return;
            }
        }

        // Reverse array to get chronological order (oldest episodes first).
        Collections.reverse(episodes);

        int episodesToDelete = Math.max(0, episodes.size() - episodeCount);
        for (int i = 0; i < episodesToDelete; i++) {
            deleteEpisode(episodes.get(i).getId(), true);
            LOG.info("Deleted old Podcast episode " + episodes.get(i).getUrl());
        }
    }

    private synchronized File getFile(PodcastChannel channel, PodcastEpisode episode) {

        File channelDir = getChannelDirectory(channel);

        String filename = channel.getTitle() + " - " + episode.getPublishDate().toString() + " - " + episode.getTitle();
        filename = filename.substring(0, Math.min(filename.length(), 146));

        filename = StringUtil.fileSystemSafe(filename);
        String extension = FilenameUtils.getExtension(filename);
        filename = FilenameUtils.removeExtension(filename);
        if (StringUtils.isBlank(extension)) {
            extension = "mp3";
        }

        File file = new File(channelDir, filename + "." + extension);
        for (int i = 0; file.exists(); i++) {
            file = new File(channelDir, filename + i + "." + extension);
        }

        if (!securityService.isWriteAllowed(file)) {
            throw new SecurityException("Access denied to file " + file);
        }
        return file;
    }

    private File getChannelDirectory(PodcastChannel channel) {
        File podcastDir = new File(settingsService.getPodcastFolder());
        File channelDir = new File(podcastDir, StringUtil.fileSystemSafe(channel.getTitle()));

        if (!podcastDir.canWrite()) {
            throw new RuntimeException("The podcasts directory " + podcastDir + " isn't writeable.");
        }

        if (!channelDir.exists()) {
            boolean ok = channelDir.mkdirs();
            if (!ok) {
                throw new RuntimeException("Failed to create directory " + channelDir);
            }

            MediaFile mediaFile = mediaFileService.getMediaFile(channelDir);
            mediaFile.setComment(channel.getDescription());
            mediaFileService.updateMediaFile(mediaFile);
        }
        return channelDir;
    }

    /**
     * Deletes the Podcast channel with the given ID.
     *
     * @param channelId The Podcast channel ID.
     */
    public void deleteChannel(int channelId) {
        // Delete all associated episodes (in case they have files that need to be deleted).
        List<PodcastEpisode> episodes = getEpisodes(channelId);
        for (PodcastEpisode episode : episodes) {
            deleteEpisode(episode.getId(), false);
        }
        podcastDao.deleteChannel(channelId);
    }

    /**
     * Deletes the Podcast episode with the given ID.
     *
     * @param episodeId     The Podcast episode ID.
     * @param logicalDelete Whether to perform a logical delete by setting the
     *                      episode status to {@link PodcastStatus#DELETED}.
     */
    public void deleteEpisode(int episodeId, boolean logicalDelete) {
        PodcastEpisode episode = podcastDao.getEpisode(episodeId);
        if (episode == null) {
            return;
        }

        // Delete file.
        if (episode.getPath() != null) {
            File file = new File(episode.getPath());
            if (file.exists()) {
                file.delete();
                // TODO: Delete directory if empty?
            }
        }

        if (logicalDelete) {
            episode.setStatus(PodcastStatus.DELETED);
            episode.setErrorMessage(null);
            podcastDao.updateEpisode(episode);
        } else {
            podcastDao.deleteEpisode(episodeId);
        }
    }

    public void setPodcastDao(PodcastDao podcastDao) {
        this.podcastDao = podcastDao;
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    public void setMediaFileService(MediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
    }

    public void setMetaDataParserFactory(MetaDataParserFactory metaDataParserFactory) {
        this.metaDataParserFactory = metaDataParserFactory;
    }
}
