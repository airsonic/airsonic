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

import chameleon.playlist.SpecificPlaylist;
import chameleon.playlist.SpecificPlaylistFactory;
import chameleon.playlist.SpecificPlaylistProvider;
import org.airsonic.player.dao.MediaFileDao;
import org.airsonic.player.dao.PlaylistDao;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.PlayQueue;
import org.airsonic.player.domain.Playlist;
import org.airsonic.player.domain.User;
import org.airsonic.player.service.playlist.PlaylistExportHandler;
import org.airsonic.player.service.playlist.PlaylistImportHandler;
import org.airsonic.player.util.FileUtil;
import org.airsonic.player.util.Pair;
import org.airsonic.player.util.StringUtil;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

/**
 * Provides services for loading and saving playlists to and from persistent storage.
 *
 * @author Sindre Mehus
 * @see PlayQueue
 */
@Service
public class PlaylistService {

    private static final Logger LOG = LoggerFactory.getLogger(PlaylistService.class);
    @Autowired
    private MediaFileDao mediaFileDao;
    @Autowired
    private PlaylistDao playlistDao;
    @Autowired
    private SecurityService securityService;
    @Autowired
    private SettingsService settingsService;
    @Autowired
    private List<PlaylistExportHandler> exportHandlers;
    @Autowired
    private List<PlaylistImportHandler> importHandlers;

    public PlaylistService(
            MediaFileDao mediaFileDao,
            PlaylistDao playlistDao,
            SecurityService securityService,
            SettingsService settingsService,
            List<PlaylistExportHandler> exportHandlers,
            List<PlaylistImportHandler> importHandlers
    ) {
        Assert.notNull(mediaFileDao);
        Assert.notNull(playlistDao);
        Assert.notNull(securityService);
        Assert.notNull(settingsService);
        Assert.notNull(exportHandlers);
        Assert.notNull(importHandlers);
        this.mediaFileDao = mediaFileDao;
        this.playlistDao = playlistDao;
        this.securityService = securityService;
        this.settingsService = settingsService;
        this.exportHandlers = exportHandlers;
        this.importHandlers = importHandlers;
    }

    public List<Playlist> getAllPlaylists() {
        return sort(playlistDao.getAllPlaylists());
    }

    public List<Playlist> getReadablePlaylistsForUser(String username) {
        return sort(playlistDao.getReadablePlaylistsForUser(username));
    }

    public List<Playlist> getWritablePlaylistsForUser(String username) {

        // Admin users are allowed to modify all playlists that are visible to them.
        if (securityService.isAdmin(username)) {
            return getReadablePlaylistsForUser(username);
        }

        return sort(playlistDao.getWritablePlaylistsForUser(username));
    }

    private List<Playlist> sort(List<Playlist> playlists) {
        Collections.sort(playlists, new PlaylistComparator());
        return playlists;
    }

    public Playlist getPlaylist(int id) {
        return playlistDao.getPlaylist(id);
    }

    public List<String> getPlaylistUsers(int playlistId) {
        return playlistDao.getPlaylistUsers(playlistId);
    }

    public List<MediaFile> getFilesInPlaylist(int id) {
        return getFilesInPlaylist(id, false);
    }

    public List<MediaFile> getFilesInPlaylist(int id, boolean includeNotPresent) {
        List<MediaFile> files = mediaFileDao.getFilesInPlaylist(id);
        if (includeNotPresent) {
            return files;
        }
        List<MediaFile> presentFiles = new ArrayList<MediaFile>(files.size());
        for (MediaFile file : files) {
            if (file.isPresent()) {
                presentFiles.add(file);
            }
        }
        return presentFiles;
    }

    public void setFilesInPlaylist(int id, List<MediaFile> files) {
        playlistDao.setFilesInPlaylist(id, files);
    }

    public void createPlaylist(Playlist playlist) {
        playlistDao.createPlaylist(playlist);
    }

    public void addPlaylistUser(int playlistId, String username) {
        playlistDao.addPlaylistUser(playlistId, username);
    }

    public void deletePlaylistUser(int playlistId, String username) {
        playlistDao.deletePlaylistUser(playlistId, username);
    }

    public boolean isReadAllowed(Playlist playlist, String username) {
        if (username == null) {
            return false;
        }
        if (username.equals(playlist.getUsername()) || playlist.isShared()) {
            return true;
        }
        return playlistDao.getPlaylistUsers(playlist.getId()).contains(username);
    }

    public boolean isWriteAllowed(Playlist playlist, String username) {
        return username != null && username.equals(playlist.getUsername());
    }

    public void deletePlaylist(int id) {
        playlistDao.deletePlaylist(id);
    }

    public void updatePlaylist(Playlist playlist) {
        playlistDao.updatePlaylist(playlist);
    }

    public Playlist importPlaylist(
            String username, String playlistName, String fileName, InputStream inputStream, Playlist existingPlaylist
    ) throws Exception {

        // TODO: handle other encodings
        final SpecificPlaylist inputSpecificPlaylist = SpecificPlaylistFactory.getInstance().readFrom(inputStream, "UTF-8");
        if (inputSpecificPlaylist == null) {
            throw new Exception("Unsupported playlist " + fileName);
        }
        PlaylistImportHandler importHandler = getImportHandler(inputSpecificPlaylist);
        LOG.debug("Using " + importHandler.getClass().getSimpleName() + " playlist import handler");

        Pair<List<MediaFile>, List<String>> result = importHandler.handle(inputSpecificPlaylist);

        if (result.getFirst().isEmpty() && !result.getSecond().isEmpty()) {
            throw new Exception("No songs in the playlist were found.");
        }

        for (String error : result.getSecond()) {
            LOG.warn("File in playlist '" + fileName + "' not found: " + error);
        }
        Date now = new Date();
        Playlist playlist;
        if (existingPlaylist == null) {
            playlist = new Playlist();
            playlist.setUsername(username);
            playlist.setCreated(now);
            playlist.setChanged(now);
            playlist.setShared(true);
            playlist.setName(playlistName);
            playlist.setComment("Auto-imported from " + fileName);
            playlist.setImportedFrom(fileName);
            createPlaylist(playlist);
        } else {
            playlist = existingPlaylist;
        }

        setFilesInPlaylist(playlist.getId(), result.getFirst());

        return playlist;
    }

    public String getExportPlaylistExtension() {
        String format = settingsService.getPlaylistExportFormat();
        SpecificPlaylistProvider provider = SpecificPlaylistFactory.getInstance().findProviderById(format);
        return provider.getContentTypes()[0].getExtensions()[0];
    }

    public void exportPlaylist(int id, OutputStream out) throws Exception {
        String format = settingsService.getPlaylistExportFormat();
        SpecificPlaylistProvider provider = SpecificPlaylistFactory.getInstance().findProviderById(format);
        PlaylistExportHandler handler = getExportHandler(provider);
        SpecificPlaylist specificPlaylist = handler.handle(id, provider);
        specificPlaylist.writeTo(out, StringUtil.ENCODING_UTF8);
    }

    private PlaylistImportHandler getImportHandler(SpecificPlaylist playlist) {
        return importHandlers.stream()
                             .filter(handler -> handler.canHandle(playlist.getClass()))
                             .findFirst()
                             .orElseThrow(() -> new RuntimeException("No import handler for " + playlist.getClass()
                                                                                                        .getName()));

    }

    private PlaylistExportHandler getExportHandler(SpecificPlaylistProvider provider) {
        return exportHandlers.stream()
                             .filter(handler -> handler.canHandle(provider.getClass()))
                             .findFirst()
                             .orElseThrow(() -> new RuntimeException("No export handler for " + provider.getClass()
                                                                                                        .getName()));
    }

    public void importPlaylists() {
        try {
            LOG.info("Starting playlist import.");
            doImportPlaylists();
            LOG.info("Completed playlist import.");
        } catch (Throwable x) {
            LOG.warn("Failed to import playlists: " + x, x);
        }
    }

    private void doImportPlaylists() {
        String playlistFolderPath = settingsService.getPlaylistFolder();
        if (playlistFolderPath == null) {
            return;
        }
        File playlistFolder = new File(playlistFolderPath);
        if (!playlistFolder.exists()) {
            return;
        }

        List<Playlist> allPlaylists = playlistDao.getAllPlaylists();
        for (File file : playlistFolder.listFiles()) {
            try {
                importPlaylistIfUpdated(file, allPlaylists);
            } catch (Exception x) {
                LOG.warn("Failed to auto-import playlist " + file + ". " + x.getMessage());
            }
        }
    }

    private void importPlaylistIfUpdated(File file, List<Playlist> allPlaylists) throws Exception {

        String fileName = file.getName();
        Playlist existingPlaylist = null;
        for (Playlist playlist : allPlaylists) {
            if (fileName.equals(playlist.getImportedFrom())) {
                existingPlaylist = playlist;
                if (file.lastModified() <= playlist.getChanged().getTime()) {
                    // Already imported and not changed since.
                    return;
                }
            }
        }
        InputStream in = new FileInputStream(file);
        try {
            importPlaylist(User.USERNAME_ADMIN, FilenameUtils.getBaseName(fileName), fileName, in, existingPlaylist);
            LOG.info("Auto-imported playlist " + file);
        } finally {
            FileUtil.closeQuietly(in);
        }
    }

    private static class PlaylistComparator implements Comparator<Playlist> {
        @Override
        public int compare(Playlist p1, Playlist p2) {
            return p1.getName().compareTo(p2.getName());
        }
    }
}
