package org.airsonic.player.service;

import chameleon.playlist.*;
import org.airsonic.player.domain.InternetRadio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Service
public class InternetRadioService {

    private static final Logger LOG = LoggerFactory.getLogger(InternetRadioService.class);

    public List<String> getStreamUrls(InternetRadio radio) throws Exception {

        // Retrieve radio playlist and parse it
        URL playlistUrl = new URL(radio.getStreamUrl());
        SpecificPlaylist inputPlaylist = null;
        try {
            LOG.info("Parsing playlist at {}...", playlistUrl.toString());
            inputPlaylist = SpecificPlaylistFactory.getInstance().readFrom(playlistUrl);
        } catch (Exception e) {
            LOG.error("Unable to parse playlist: {}", playlistUrl.toString(), e);
            throw e;
        }
        if (inputPlaylist == null) {
            LOG.error("Unsupported playlist format: {}", playlistUrl.toString());
            throw new Exception("Unsupported playlist format " + playlistUrl.toString());
        }

        // Retrieve stream URLs
        List<String> entries = new ArrayList<>();
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
                LOG.info("Got source media at {}...", streamUrl);
                entries.add(streamUrl);
            }

            @Override
            public void endVisitMedia(Media media) throws Exception {

            }
        });

        if (entries.isEmpty()) {
            LOG.warn("No entries found when parsing external playlist.");
        }

        return entries;
    }
}
