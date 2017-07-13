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
package org.airsonic.player.io;

import org.airsonic.player.domain.*;
import org.airsonic.player.service.AudioScrobblerService;
import org.airsonic.player.service.MediaFileService;
import org.airsonic.player.service.SearchService;
import org.airsonic.player.service.TranscodingService;
import org.airsonic.player.service.sonos.SonosHelper;
import org.airsonic.player.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Implementation of {@link InputStream} which reads from a {@link PlayQueue}.
 *
 * @author Sindre Mehus
 */
public class PlayQueueInputStream extends InputStream {

    private static final Logger LOG = LoggerFactory.getLogger(PlayQueueInputStream.class);

    private final Player player;
    private final TransferStatus status;
    private final Integer maxBitRate;
    private final String preferredTargetFormat;
    private final VideoTranscodingSettings videoTranscodingSettings;
    private final TranscodingService transcodingService;
    private final AudioScrobblerService audioScrobblerService;
    private final MediaFileService mediaFileService;
    private MediaFile currentFile;
    private InputStream currentInputStream;
    private SearchService searchService;

    public PlayQueueInputStream(Player player, TransferStatus status, Integer maxBitRate, String preferredTargetFormat,
                                VideoTranscodingSettings videoTranscodingSettings, TranscodingService transcodingService,
                                AudioScrobblerService audioScrobblerService, MediaFileService mediaFileService, SearchService searchService) {
        this.player = player;
        this.status = status;
        this.maxBitRate = maxBitRate;
        this.preferredTargetFormat = preferredTargetFormat;
        this.videoTranscodingSettings = videoTranscodingSettings;
        this.transcodingService = transcodingService;
        this.audioScrobblerService = audioScrobblerService;
        this.mediaFileService = mediaFileService;
        this.searchService = searchService;
    }

    @Override
    public int read() throws IOException {
        byte[] b = new byte[1];
        int n = read(b);
        return n == -1 ? -1 : b[0];
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        prepare();
        if (currentInputStream == null || player.getPlayQueue().getStatus() == PlayQueue.Status.STOPPED) {
            return -1;
        }

        int n = currentInputStream.read(b, off, len);

        // If end of song reached, skip to next song and call read() again.
        if (n == -1) {
            player.getPlayQueue().next();
            close();
            return read(b, off, len);
        } else {
            status.addBytesTransfered(n);
        }
        return n;
    }

    private void prepare() throws IOException {
        PlayQueue playQueue = player.getPlayQueue();

        // If playlist is in auto-random mode, populate it with new random songs.
        if (playQueue.getIndex() == -1 && playQueue.getRandomSearchCriteria() != null) {
            populateRandomPlaylist(playQueue);
        }

        MediaFile result;
        synchronized (playQueue) {
            result = playQueue.getCurrentFile();
        }
        MediaFile file = result;
        if (file == null) {
            close();
        } else if (!file.equals(currentFile)) {
            close();
            LOG.info(player.getUsername() + " listening to \"" + FileUtil.getShortPath(file.getFile()) + "\"");
            mediaFileService.incrementPlayCount(file);

            // Don't scrobble REST players (except Sonos)
            if (player.getClientId() == null || player.getClientId().equals(SonosHelper.AIRSONIC_CLIENT_ID)) {
                audioScrobblerService.register(file, player.getUsername(), false, null);
            }

            TranscodingService.Parameters parameters = transcodingService.getParameters(file, player, maxBitRate, preferredTargetFormat, videoTranscodingSettings);
            currentInputStream = transcodingService.getTranscodedInputStream(parameters);
            currentFile = file;
            status.setFile(currentFile.getFile());
        }
    }

    private void populateRandomPlaylist(PlayQueue playQueue) throws IOException {
        List<MediaFile> files = searchService.getRandomSongs(playQueue.getRandomSearchCriteria());
        playQueue.addFiles(false, files);
        LOG.info("Recreated random playlist with " + playQueue.size() + " songs.");
    }

    @Override
    public void close() throws IOException {
        try {
            if (currentInputStream != null) {
                currentInputStream.close();
            }
        } finally {
            // Don't scrobble REST players (except Sonos)
            if (player.getClientId() == null || player.getClientId().equals(SonosHelper.AIRSONIC_CLIENT_ID)) {
                audioScrobblerService.register(currentFile, player.getUsername(), true, null);
            }
            currentInputStream = null;
            currentFile = null;
        }
    }
}
