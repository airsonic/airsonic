package org.airsonic.player.service;

import chameleon.playlist.*;
import org.airsonic.player.domain.InternetRadio;
import org.airsonic.player.domain.InternetRadioSource;
import org.apache.commons.io.input.BoundedInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

@Service
public class InternetRadioService {

    private static final Logger LOG = LoggerFactory.getLogger(InternetRadioService.class);

    /**
     * The maximum number of source URLs in a remote playlist.
     */
    private static final int PLAYLIST_REMOTE_MAX_LENGTH = 250;

    /**
     * The maximum size, in bytes, for a remote playlist response.
     */
    private static final long PLAYLIST_REMOTE_MAX_BYTE_SIZE = 100 * 1024;  // 100 kB

    /**
     * A list of cached source URLs for remote playlists.
     */
    private Map<Integer, List<InternetRadioSource>> cachedSources;

    /**
     * Exception thrown when the remote playlist is too large to be parsed completely.
     */
    private class PlaylistTooLarge extends Exception {
        public PlaylistTooLarge(String message) {
            super(message);
        }
    }
    /**
     * Exception thrown when the remote playlist format cannot be determined.
     */
    private class PlaylistFormatUnsupported extends Exception {
        public PlaylistFormatUnsupported(String message) {
            super(message);
        }
    }

    public InternetRadioService() {
        this.cachedSources = new HashMap<>();
    }

    /**
     * Clear the radio source cache.
     */
    public void clearInternetRadioSourceCache() {
        cachedSources.clear();
    }

    /**
     * Clear the radio source cache for the given radio id
     * @param internetRadioId a radio id
     */
    public void clearInternetRadioSourceCache(Integer internetRadioId) {
        if (internetRadioId != null) {
            cachedSources.remove(internetRadioId);
        }
    }

    /**
     * Retrieve a list of sources for the given internet radio.
     *
     * This method caches the sources using the InternetRadio.getId
     * method as a key, until clearInternetRadioSourceCache is called.
     *
     * @param radio an internet radio
     * @return a list of internet radio sources
     */
    public List<InternetRadioSource> getInternetRadioSources(InternetRadio radio) {
        List<InternetRadioSource> sources;
        if (cachedSources.containsKey(radio.getId())) {
            LOG.debug("Got cached sources for internet radio {}!", radio.getStreamUrl());
            sources = cachedSources.get(radio.getId());
        } else {
            LOG.debug("Retrieving sources for internet radio {}...", radio.getStreamUrl());
            try {
                sources = retrieveInternetRadioSources(radio);
                if (sources.isEmpty()) {
                    LOG.warn("No entries found for internet radio {}.", radio.getStreamUrl());
                } else {
                    LOG.info("Retrieved playlist for internet radio {}, got {} sources.", radio.getStreamUrl(), sources.size());
                }
            } catch (Exception e) {
                LOG.error("Failed to retrieve sources for internet radio {}.", radio.getStreamUrl(), e);
                sources = new ArrayList<>();
            }
            cachedSources.put(radio.getId(), sources);
        }
        return sources;
    }

    /**
     * Retrieve a list of sources from the given internet radio
     *
     * This method uses a default maximum limit of PLAYLIST_REMOTE_MAX_LENGTH sources.
     *
     * @param radio an internet radio
     * @return a list of internet radio sources
     * @throws Exception
     */
    private List<InternetRadioSource> retrieveInternetRadioSources(InternetRadio radio) throws Exception {
        return retrieveInternetRadioSources(radio, PLAYLIST_REMOTE_MAX_LENGTH, PLAYLIST_REMOTE_MAX_BYTE_SIZE);
    }

    /**
     * Retrieve a list of sources from the given internet radio.
     *
     * @param radio an internet radio
     * @param maxCount the maximum number of items to read from the remote playlist, or 0 if unlimited
     * @return a list of internet radio sources
     * @throws Exception
     */
    private List<InternetRadioSource> retrieveInternetRadioSources(InternetRadio radio, int maxCount, long maxByteSize) throws Exception {
        // Retrieve the remote playlist
        String playlistUrl = radio.getStreamUrl();
        LOG.debug("Parsing internet radio playlist at {}...", playlistUrl);
        SpecificPlaylist inputPlaylist = retrievePlaylist(new URL(playlistUrl), maxByteSize);

        // Retrieve stream URLs
        List<InternetRadioSource> entries = new ArrayList<>();
        try {
            inputPlaylist.toPlaylist().acceptDown(new PlaylistVisitor() {
                @Override
                public void beginVisitPlaylist(Playlist playlist) throws Exception {

                }

                @Override
                public void endVisitPlaylist(Playlist playlist) throws Exception {

                }

                @Override
                public void beginVisitParallel(Parallel parallel) throws Exception {

                }

                @Override
                public void endVisitParallel(Parallel parallel) throws Exception {

                }

                @Override
                public void beginVisitSequence(Sequence sequence) throws Exception {

                }

                @Override
                public void endVisitSequence(Sequence sequence) throws Exception {

                }

                @Override
                public void beginVisitMedia(Media media) throws Exception {
                    // Since we're dealing with remote content, we place a hard
                    // limit on the maximum number of items to load from the playlist,
                    // in order to avoid parsing erroneous data.
                    if (maxCount > 0 && entries.size() >= maxCount) {
                        throw new PlaylistTooLarge("Remote playlist has too many sources (maximum " + maxCount + ")");
                    }
                    String streamUrl = media.getSource().getURI().toString();
                    LOG.debug("Got source media at {}", streamUrl);
                    entries.add(new InternetRadioSource(streamUrl));
                }

                @Override
                public void endVisitMedia(Media media) throws Exception {

                }
            });
        } catch (PlaylistTooLarge e) {
            // Ignore if playlist is too large, but truncate the rest and log a warning.
            LOG.warn(e.getMessage());
        }

        return entries;
    }

    /**
     * Retrieve playlist data from a given URL.
     *
     * This throws an ec
     *
     * @param url URL to the remote playlist
     * @param maxByteSize maximum size of the response, in bytes, or 0 if unlimited
     * @return the remote playlist data
     */
    private SpecificPlaylist retrievePlaylist(URL url, long maxByteSize) throws IOException, PlaylistFormatUnsupported {
        URLConnection urlConnection = url.openConnection();
        urlConnection.setAllowUserInteraction(false);
        urlConnection.setConnectTimeout(10000);
        urlConnection.setDoInput(true);
        urlConnection.setDoOutput(false);
        urlConnection.setReadTimeout(60000);
        urlConnection.setUseCaches(true);
        urlConnection.connect();
        String contentEncoding = urlConnection.getContentEncoding();
        SpecificPlaylist playlist = null;
        try (InputStream in = urlConnection.getInputStream()) {
            if (maxByteSize > 0) {
                playlist = SpecificPlaylistFactory.getInstance().readFrom(new BoundedInputStream(in, maxByteSize), contentEncoding);
            } else {
                playlist = SpecificPlaylistFactory.getInstance().readFrom(in, contentEncoding);
            }
        }
        if (playlist == null) {
            throw new PlaylistFormatUnsupported("Unsupported playlist format " + url.toString());
        }
        return playlist;
    }
}
