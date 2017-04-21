/*
 This file is part of Libresonic.

 Libresonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Libresonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Libresonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2016 (C) Libresonic Authors
 Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
 */
package org.libresonic.player.service;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.libresonic.player.dao.MediaFileDao;
import org.libresonic.player.dao.PlaylistDao;
import org.libresonic.player.domain.MediaFile;
import org.libresonic.player.domain.MusicFolder;
import org.libresonic.player.domain.Playlist;
import org.libresonic.player.domain.User;
import org.libresonic.player.util.Pair;
import org.libresonic.player.util.StringUtil;
import org.libresonic.player.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides services for loading and saving playlists to and from persistent storage.
 *
 * @author Sindre Mehus
 * @see org.libresonic.player.domain.PlayQueue
 */
public class PlaylistService {

    private static final Logger LOG = LoggerFactory.getLogger(PlaylistService.class);
    private MediaFileService mediaFileService;
    private MediaFileDao mediaFileDao;
    private PlaylistDao playlistDao;
    private SecurityService securityService;
    private SettingsService settingsService;

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

    public Playlist importPlaylist(String username, String playlistName, String fileName, String format,
            InputStream inputStream, Playlist existingPlaylist) throws Exception {
        PlaylistFormat playlistFormat = getPlaylistFormat(format);
        if (playlistFormat == null) {
            throw new Exception("Unsupported playlist format: " + format);
        }

        Pair<List<MediaFile>, List<String>> result = parseFiles(IOUtils.toByteArray(inputStream), playlistFormat);
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

    private Pair<List<MediaFile>, List<String>> parseFiles(byte[] playlist, PlaylistFormat playlistFormat) throws IOException {
        Pair<List<MediaFile>, List<String>> result = null;

        // Try with multiple encodings; use the one that finds the most files.
        String[] encodings = {StringUtil.ENCODING_LATIN, StringUtil.ENCODING_UTF8, Charset.defaultCharset().name()};
        for (String encoding : encodings) {
            Pair<List<MediaFile>, List<String>> files = parseFilesWithEncoding(playlist, playlistFormat, encoding);
            if (result == null || result.getFirst().size() < files.getFirst().size()) {
                result = files;
            }
        }
        return result;
    }

    private Pair<List<MediaFile>, List<String>> parseFilesWithEncoding(byte[] playlist, PlaylistFormat playlistFormat, String encoding) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(playlist), encoding));
        return playlistFormat.parse(reader, mediaFileService);
    }

    public void exportPlaylist(int id, OutputStream out) throws Exception {
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, StringUtil.ENCODING_UTF8));
        new M3UFormat().format(getFilesInPlaylist(id, true), writer);
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

    private void doImportPlaylists() throws Exception {
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
        String format = FilenameUtils.getExtension(file.getPath());
        if (getPlaylistFormat(format) == null) {
            return;
        }

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
            importPlaylist(User.USERNAME_ADMIN, FilenameUtils.getBaseName(fileName), fileName, format, in, existingPlaylist);
            LOG.info("Auto-imported playlist " + file);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    private PlaylistFormat getPlaylistFormat(String format) {
        if (format == null) {
            return null;
        }
        if (format.equalsIgnoreCase("m3u") || format.equalsIgnoreCase("m3u8")) {
            return new M3UFormat();
        }
        if (format.equalsIgnoreCase("pls")) {
            return new PLSFormat();
        }
        if (format.equalsIgnoreCase("xspf")) {
            return new XSPFFormat();
        }
        return null;
    }

    public void setPlaylistDao(PlaylistDao playlistDao) {
        this.playlistDao = playlistDao;
    }

    public void setMediaFileDao(MediaFileDao mediaFileDao) {
        this.mediaFileDao = mediaFileDao;
    }

    public void setMediaFileService(MediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    /**
     * Abstract superclass for playlist formats.
     */
    private abstract class PlaylistFormat {
        public abstract Pair<List<MediaFile>, List<String>> parse(BufferedReader reader, MediaFileService mediaFileService) throws IOException;

        public abstract void format(List<MediaFile> files, PrintWriter writer) throws IOException;


        protected MediaFile getMediaFile(String path) {
            try {
                File file = new File(path);
                if (!file.exists()) {
                    return null;
                }

                file = normalizePath(file);
                if (file == null) {
                    return null;
                }
                MediaFile mediaFile = mediaFileService.getMediaFile(file);
                if (mediaFile != null && mediaFile.exists()) {
                    return mediaFile;
                }
            } catch (SecurityException x) {
                // Ignored
            } catch (IOException x) {
                // Ignored
            }
            return null;
        }

        /**
         * Paths in an external playlist may not have the same upper/lower case as in the (case sensitive) media_file table.
         * This methods attempts to normalize the external path to match the one stored in the table.
         */
        private File normalizePath(File file) throws IOException {

            // Only relevant for Windows where paths are case insensitive.
            if (!Util.isWindows()) {
                return file;
            }

            // Find the most specific music folder.
            String canonicalPath = file.getCanonicalPath();
            MusicFolder containingMusicFolder = null;
            for (MusicFolder musicFolder : settingsService.getAllMusicFolders()) {
                String musicFolderPath = musicFolder.getPath().getPath();
                if (canonicalPath.toLowerCase().startsWith(musicFolderPath.toLowerCase())) {
                    if (containingMusicFolder == null || containingMusicFolder.getPath().length() < musicFolderPath.length()) {
                        containingMusicFolder = musicFolder;
                    }
                }
            }

            if (containingMusicFolder == null) {
                return null;
            }

            return new File(containingMusicFolder.getPath().getPath() + canonicalPath.substring(containingMusicFolder.getPath().getPath().length()));
            // TODO: Consider slashes.
        }
    }

    private class M3UFormat extends PlaylistFormat {
        public Pair<List<MediaFile>, List<String>> parse(BufferedReader reader, MediaFileService mediaFileService) throws IOException {
            List<MediaFile> ok = new ArrayList<MediaFile>();
            List<String> error = new ArrayList<String>();
            String line = reader.readLine();
            while (line != null) {
                if (!line.startsWith("#")) {
                    MediaFile file = getMediaFile(line);
                    if (file != null) {
                        ok.add(file);
                    } else {
                        error.add(line);
                    }
                }
                line = reader.readLine();
            }
            return new Pair<List<MediaFile>, List<String>>(ok, error);
        }

        public void format(List<MediaFile> files, PrintWriter writer) throws IOException {
            writer.println("#EXTM3U");
            for (MediaFile file : files) {
                writer.println(file.getPath());
            }
            if (writer.checkError()) {
                throw new IOException("Error when writing playlist");
            }
        }
    }

    /**
     * Implementation of PLS playlist format.
     */
    private class PLSFormat extends PlaylistFormat {
        public Pair<List<MediaFile>, List<String>> parse(BufferedReader reader, MediaFileService mediaFileService) throws IOException {
            List<MediaFile> ok = new ArrayList<MediaFile>();
            List<String> error = new ArrayList<String>();

            Pattern pattern = Pattern.compile("^File\\d+=(.*)$");
            String line = reader.readLine();
            while (line != null) {

                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    String path = matcher.group(1);
                    MediaFile file = getMediaFile(path);
                    if (file != null) {
                        ok.add(file);
                    } else {
                        error.add(path);
                    }
                }
                line = reader.readLine();
            }
            return new Pair<List<MediaFile>, List<String>>(ok, error);
        }

        public void format(List<MediaFile> files, PrintWriter writer) throws IOException {
            writer.println("[playlist]");
            int counter = 0;

            for (MediaFile file : files) {
                counter++;
                writer.println("File" + counter + '=' + file.getPath());
            }
            writer.println("NumberOfEntries=" + counter);
            writer.println("Version=2");

            if (writer.checkError()) {
                throw new IOException("Error when writing playlist.");
            }
        }
    }

    /**
     * Implementation of XSPF (http://www.xspf.org/) playlist format.
     */
    private class XSPFFormat extends PlaylistFormat {
        public Pair<List<MediaFile>, List<String>> parse(BufferedReader reader, MediaFileService mediaFileService) throws IOException {
            List<MediaFile> ok = new ArrayList<MediaFile>();
            List<String> error = new ArrayList<String>();

            SAXBuilder builder = new SAXBuilder();
            Document document;
            try {
                document = builder.build(reader);
            } catch (JDOMException x) {
                LOG.warn("Failed to parse XSPF playlist.", x);
                throw new IOException("Failed to parse XSPF playlist.");
            }

            Element root = document.getRootElement();
            Namespace ns = root.getNamespace();
            Element trackList = root.getChild("trackList", ns);
            List<?> tracks = trackList.getChildren("track", ns);

            for (Object obj : tracks) {
                Element track = (Element) obj;
                String location = track.getChildText("location", ns);
                if (location != null) {
                    MediaFile file = getMediaFile(location);
                    if (file != null) {
                        ok.add(file);
                    } else {
                        error.add(location);
                    }
                }
            }
            return new Pair<List<MediaFile>, List<String>>(ok, error);
        }

        public void format(List<MediaFile> files, PrintWriter writer) throws IOException {
            writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            writer.println("<playlist version=\"1\" xmlns=\"http://xspf.org/ns/0/\">");
            writer.println("    <trackList>");

            for (MediaFile file : files) {
                writer.println("        <track><location>file://" + StringEscapeUtils.escapeXml(file.getPath()) + "</location></track>");
            }
            writer.println("    </trackList>");
            writer.println("</playlist>");

            if (writer.checkError()) {
                throw new IOException("Error when writing playlist.");
            }
        }
    }

    private static class PlaylistComparator implements Comparator<Playlist> {
        @Override
        public int compare(Playlist p1, Playlist p2) {
            return p1.getName().compareTo(p2.getName());
        }
    }
}
