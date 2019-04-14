package org.airsonic.player.service;

import chameleon.playlist.*;
import org.airsonic.player.domain.InternetRadio;
import org.airsonic.player.domain.InternetRadioSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.*;

@Service
public class InternetRadioService {

    private static final Logger LOG = LoggerFactory.getLogger(InternetRadioService.class);

    private Map<Integer, List<InternetRadioSource>> cachedSources;

    public InternetRadioService() {
        this.cachedSources = new HashMap<>();
    }

    public void clearInternetRadioSourceCache() {
        cachedSources.clear();
    }

    public List<InternetRadioSource> getInternetRadioSources(InternetRadio radio) throws Exception {
        List<InternetRadioSource> sources;
        if (cachedSources.containsKey(radio.getId())) {
            LOG.debug("Got cached sources for internet radio {}!", radio.getStreamUrl());
            sources = cachedSources.get(radio.getId());
        } else {
            LOG.debug("Retrieving sources for internet radio {}...", radio.getStreamUrl());
            sources = retrieveInternetRadioSources(radio);
            if (sources.isEmpty()) {
                LOG.warn("No entries found when parsing external playlist.");
            }
            LOG.info("Retrieved playlist for internet radio {}, got {} sources.", radio.getStreamUrl(), sources.size());
            cachedSources.put(radio.getId(), sources);
        }
        return sources;
    }

    private List<InternetRadioSource> retrieveInternetRadioSources(InternetRadio radio) throws Exception {
        // Retrieve radio playlist and parse it
        URL playlistUrl = new URL(radio.getStreamUrl());
        SpecificPlaylist inputPlaylist = null;
        try {
            LOG.debug("Parsing internet radio playlist at {}...", playlistUrl.toString());
            inputPlaylist = SpecificPlaylistFactory.getInstance().readFrom(playlistUrl);
        } catch (Exception e) {
            LOG.error("Unable to parse internet radio playlist: {}", playlistUrl.toString(), e);
            throw e;
        }
        if (inputPlaylist == null) {
            LOG.error("Unsupported playlist format: {}", playlistUrl.toString());
            throw new Exception("Unsupported playlist format " + playlistUrl.toString());
        }

        // Retrieve stream URLs
        List<InternetRadioSource> entries = new ArrayList<>();
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
                String streamUrl = media.getSource().getURI().toString();
                LOG.debug("Got source media at {}", streamUrl);
                entries.add(new InternetRadioSource(
                    streamUrl
                ));
            }

            @Override
            public void endVisitMedia(Media media) throws Exception {

            }
        });

        return entries;
    }
}
