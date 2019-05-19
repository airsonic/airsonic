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
import java.net.HttpURLConnection;
import java.net.URL;
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
     * The maximum number of redirects for a remote playlist response.
     */
    private static final int PLAYLIST_REMOTE_MAX_REDIRECTS = 20;

    /**
     * A list of cached source URLs for remote playlists.
     */
    private final Map<Integer, List<InternetRadioSource>> cachedSources;

    /**
     * Generic exception class for playlists.
     */
    private class PlaylistException extends Exception {
        public PlaylistException(String message) { super(message); }
    }

    /**
     * Exception thrown when the remote playlist is too large to be parsed completely.
     */
    private class PlaylistTooLarge extends PlaylistException {
        public PlaylistTooLarge(String message) { super(message); }
    }

    /**
     * Exception thrown when the remote playlist format cannot be determined.
     */
    private class PlaylistFormatUnsupported extends PlaylistException {
        public PlaylistFormatUnsupported(String message) {
            super(message);
        }
    }

    /**
     * Exception thrown when too many redirects occurred when retrieving a remote playlist.
     */
    private class PlaylistHasTooManyRedirects extends PlaylistException {
        public PlaylistHasTooManyRedirects(String message) { super(message); }
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
        return retrieveInternetRadioSources(
            radio,
            PLAYLIST_REMOTE_MAX_LENGTH,
            PLAYLIST_REMOTE_MAX_BYTE_SIZE,
            PLAYLIST_REMOTE_MAX_REDIRECTS
        );
    }

    /**
     * Retrieve a list of sources from the given internet radio.
     *
     * @param radio an internet radio
     * @param maxCount the maximum number of items to read from the remote playlist, or 0 if unlimited
     * @param maxByteSize maximum size of the response, in bytes, or 0 if unlimited
     * @param maxRedirects maximum number of redirects, or 0 if unlimited
     * @return a list of internet radio sources
     * @throws Exception
     */
    private List<InternetRadioSource> retrieveInternetRadioSources(InternetRadio radio, int maxCount, long maxByteSize, int maxRedirects) throws Exception {
        // Retrieve the remote playlist
        String playlistUrl = radio.getStreamUrl();
        LOG.debug("Parsing internet radio playlist at {}...", playlistUrl);
        SpecificPlaylist inputPlaylist = retrievePlaylist(new URL(playlistUrl), maxByteSize, maxRedirects);

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
     * @param url URL to the remote playlist
     * @param maxByteSize maximum size of the response, in bytes, or 0 if unlimited
     * @param maxRedirects maximum number of redirects, or 0 if unlimited
     * @return the remote playlist data
     */
    protected SpecificPlaylist retrievePlaylist(URL url, long maxByteSize, int maxRedirects) throws IOException, PlaylistException {

        SpecificPlaylist playlist;
        HttpURLConnection urlConnection = connectToURLWithRedirects(url, maxRedirects);
        try (InputStream in = urlConnection.getInputStream()) {
            String contentEncoding = urlConnection.getContentEncoding();
            if (maxByteSize > 0) {
                playlist = SpecificPlaylistFactory.getInstance().readFrom(new BoundedInputStream(in, maxByteSize), contentEncoding);
            } else {
                playlist = SpecificPlaylistFactory.getInstance().readFrom(in, contentEncoding);
            }
        }
        finally {
            urlConnection.disconnect();
        }
        if (playlist == null) {
            throw new PlaylistFormatUnsupported("Unsupported playlist format " + url.toString());
        }
        return playlist;
    }

    /**
     * Start a new connection to a remote URL, and follow redirects.
     *
     * @param url the remote URL
     * @param maxRedirects maximum number of redirects, or 0 if unlimited
     * @return an open connection
     */
    protected HttpURLConnection connectToURLWithRedirects(URL url, int maxRedirects) throws IOException, PlaylistException {

        int redirectCount = 0;
        URL currentURL = url;

        // Start a first connection.
        HttpURLConnection connection = connectToURL(currentURL);

        // While it redirects, follow redirects in new connections.
        while (connection.getResponseCode() == HttpURLConnection.HTTP_MOVED_PERM ||
               connection.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP ||
               connection.getResponseCode() == HttpURLConnection.HTTP_SEE_OTHER) {

            // Check if redirect count is not too large.
            redirectCount += 1;
            if (maxRedirects > 0 && redirectCount > maxRedirects) {
                connection.disconnect();
                throw new PlaylistHasTooManyRedirects(String.format("Too many redirects (%d) for URL %s", redirectCount, url));
            }

            // Reconnect to the new URL.
            currentURL = new URL(connection.getHeaderField("Location"));
            connection.disconnect();
            connection = connectToURL(currentURL);
        }

        // Return the last connection that did not redirect.
        return connection;
    }

    /**
     * Start a new connection to a remote URL.
     *
     * @param url the remote URL
     * @return an open connection
     */
    protected HttpURLConnection connectToURL(URL url) throws IOException {
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setAllowUserInteraction(false);
        urlConnection.setConnectTimeout(10000);
        urlConnection.setDoInput(true);
        urlConnection.setDoOutput(false);
        urlConnection.setReadTimeout(60000);
        urlConnection.setUseCaches(true);
        urlConnection.connect();
        return urlConnection;
    }
}
