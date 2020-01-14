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

import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.PlayStatus;
import org.airsonic.player.domain.Player;
import org.airsonic.player.domain.TransferStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Provides services for maintaining the list of stream, download and upload statuses.
 * <p/>
 * Note that for stream statuses, the last inactive status is also stored.
 *
 * @author Sindre Mehus
 * @see TransferStatus
 */
@Service
public class StatusService {

    @Autowired
    private MediaFileService mediaFileService;

    private final List<TransferStatus> streamStatuses = Collections.synchronizedList(new ArrayList<>());
    private final List<TransferStatus> downloadStatuses = Collections.synchronizedList(new ArrayList<>());
    private final List<TransferStatus> uploadStatuses = Collections.synchronizedList(new ArrayList<>());
    private final List<PlayStatus> remotePlays = Collections.synchronizedList(new ArrayList<>());

    // Maps from player ID to latest inactive stream status.
    private final Map<Integer, TransferStatus> inactiveStreamStatuses = new ConcurrentHashMap<>();

    public TransferStatus createStreamStatus(Player player) {
        return createStatus(player, streamStatuses);
    }

    public synchronized void removeStreamStatus(TransferStatus status) {
        // Move it to the map of inactive statuses
        status.setActive(false);
        inactiveStreamStatuses.put(status.getPlayer().getId(), status);
        streamStatuses.remove(status);
    }

    public List<TransferStatus> getAllStreamStatuses() {
        List<TransferStatus> result = new ArrayList<>(streamStatuses);

        // Add inactive status for those players that have no active status.
        Set<Integer> activePlayers = new HashSet<Integer>();
        for (TransferStatus status : streamStatuses) {
            activePlayers.add(status.getPlayer().getId());
        }

        for (Map.Entry<Integer, TransferStatus> entry : inactiveStreamStatuses.entrySet()) {
            if (!activePlayers.contains(entry.getKey())) {
                result.add(entry.getValue());
            }
        }
        return result;
    }

    public List<TransferStatus> getStreamStatusesForPlayer(Player player) {
        List<TransferStatus> result = new ArrayList<TransferStatus>();
        for (TransferStatus status : streamStatuses) {
            if (status.getPlayer().getId().equals(player.getId())) {
                result.add(status);
            }
        }

        // If no active statuses exists, add the inactive one.
        if (result.isEmpty()) {
            TransferStatus inactiveStatus = inactiveStreamStatuses.get(player.getId());
            if (inactiveStatus != null) {
                result.add(inactiveStatus);
            }
        }

        return result;
    }

    public TransferStatus createDownloadStatus(Player player) {
        return createStatus(player, downloadStatuses);
    }

    public void removeDownloadStatus(TransferStatus status) {
        downloadStatuses.remove(status);
    }

    public List<TransferStatus> getAllDownloadStatuses() {
        return new ArrayList<TransferStatus>(downloadStatuses);
    }

    public TransferStatus createUploadStatus(Player player) {
        return createStatus(player, uploadStatuses);
    }

    public void removeUploadStatus(TransferStatus status) {
        uploadStatuses.remove(status);
    }

    public List<TransferStatus> getAllUploadStatuses() {
        return new ArrayList<TransferStatus>(uploadStatuses);
    }

    public void addRemotePlay(PlayStatus playStatus) {
        remotePlays.removeIf(PlayStatus::isExpired);
        remotePlays.add(playStatus);
    }

    public synchronized List<PlayStatus> getPlayStatuses() {
        Map<Integer, PlayStatus> result = new LinkedHashMap<Integer, PlayStatus>();
        for (PlayStatus remotePlay : remotePlays) {
            if (!remotePlay.isExpired()) {
                result.put(remotePlay.getPlayer().getId(), remotePlay);
            }
        }

        List<TransferStatus> statuses = new ArrayList<TransferStatus>();
        statuses.addAll(inactiveStreamStatuses.values());
        statuses.addAll(streamStatuses);

        for (TransferStatus streamStatus : statuses) {
            Player player = streamStatus.getPlayer();
            File file = streamStatus.getFile();
            if (file == null) {
                continue;
            }
            MediaFile mediaFile = mediaFileService.getMediaFile(file);
            if (player == null || mediaFile == null) {
                continue;
            }
            Date time = new Date(System.currentTimeMillis() - streamStatus.getMillisSinceLastUpdate());
            result.put(player.getId(), new PlayStatus(mediaFile, player, time));
        }
        return new ArrayList<PlayStatus>(result.values());
    }

    private static TransferStatus createStatus(Player player, List<TransferStatus> statusList) {
        TransferStatus status = new TransferStatus(player);
        statusList.add(status);
        return status;
    }

    public void setMediaFileService(MediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
    }
}
