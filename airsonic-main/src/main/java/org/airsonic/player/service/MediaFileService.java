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

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.airsonic.player.dao.AlbumDao;
import org.airsonic.player.dao.MediaFileDao;
import org.airsonic.player.domain.*;
import org.airsonic.player.service.metadata.JaudiotaggerParser;
import org.airsonic.player.service.metadata.MetaData;
import org.airsonic.player.service.metadata.MetaDataParser;
import org.airsonic.player.service.metadata.MetaDataParserFactory;
import org.airsonic.player.util.FileUtil;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

/**
 * Provides services for instantiating and caching media files and cover art.
 *
 * @author Sindre Mehus
 */
@Service
public class MediaFileService {

    private static final Logger LOG = LoggerFactory.getLogger(MediaFileService.class);

    @Autowired
    private Ehcache mediaFileMemoryCache;
    @Autowired
    private SecurityService securityService;
    @Autowired
    private SettingsService settingsService;
    @Autowired
    private MediaFileDao mediaFileDao;
    @Autowired
    private AlbumDao albumDao;
    @Autowired
    private JaudiotaggerParser parser;
    @Autowired
    private MetaDataParserFactory metaDataParserFactory;
    private boolean memoryCacheEnabled = true;

    /**
     * Returns a media file instance for the given file.  If possible, a cached value is returned.
     *
     * @param file A file on the local file system.
     * @return A media file instance, or null if not found.
     * @throws SecurityException If access is denied to the given file.
     */
    public MediaFile getMediaFile(File file) {
        return getMediaFile(file, settingsService.isFastCacheEnabled());
    }

    /**
     * Returns a media file instance for the given file.  If possible, a cached value is returned.
     *
     * @param file A file on the local file system.
     * @return A media file instance, or null if not found.
     * @throws SecurityException If access is denied to the given file.
     */
    public MediaFile getMediaFile(File file, boolean useFastCache) {

        // Look in fast memory cache first.
        MediaFile result = getFromMemoryCache(file);
        if (result != null) {
            return result;
        }

        if (!securityService.isReadAllowed(file)) {
            throw new SecurityException("Access denied to file " + file);
        }

        // Secondly, look in database.
        result = mediaFileDao.getMediaFile(file.getPath());
        if (result != null) {
            result = checkLastModified(result, useFastCache);
            putInMemoryCache(file, result);
            return result;
        }

        if (!FileUtil.exists(file)) {
            return null;
        }
        // Not found in database, must read from disk.
        result = createMediaFile(file);

        // Put in cache and database.
        putInMemoryCache(file, result);
        mediaFileDao.createOrUpdateMediaFile(result);

        return result;
    }

    private MediaFile checkLastModified(MediaFile mediaFile, boolean useFastCache) {
        if (useFastCache || (mediaFile.getVersion() >= MediaFileDao.VERSION && mediaFile.getChanged().getTime() >= FileUtil.lastModified(mediaFile.getFile()))) {
            return mediaFile;
        }
        mediaFile = createMediaFile(mediaFile.getFile());
        mediaFileDao.createOrUpdateMediaFile(mediaFile);
        return mediaFile;
    }

    /**
     * Returns a media file instance for the given path name. If possible, a cached value is returned.
     *
     * @param pathName A path name for a file on the local file system.
     * @return A media file instance.
     * @throws SecurityException If access is denied to the given file.
     */
    public MediaFile getMediaFile(String pathName) {
        return getMediaFile(new File(pathName));
    }

    // TODO: Optimize with memory caching.
    public MediaFile getMediaFile(int id) {
        MediaFile mediaFile = mediaFileDao.getMediaFile(id);
        if (mediaFile == null) {
            return null;
        }

        if (!securityService.isReadAllowed(mediaFile.getFile())) {
            throw new SecurityException("Access denied to file " + mediaFile);
        }

        return checkLastModified(mediaFile, settingsService.isFastCacheEnabled());
    }

    public MediaFile getParentOf(MediaFile mediaFile) {
        if (mediaFile.getParentPath() == null) {
            return null;
        }
        return getMediaFile(mediaFile.getParentPath());
    }

    /**
     * Returns all media files that are children of a given media file.
     *
     * @param includeFiles       Whether files should be included in the result.
     * @param includeDirectories Whether directories should be included in the result.
     * @param sort               Whether to sort files in the same directory.
     * @return All children media files.
     */
    public List<MediaFile> getChildrenOf(MediaFile parent, boolean includeFiles, boolean includeDirectories, boolean sort) {
        return getChildrenOf(parent, includeFiles, includeDirectories, sort, settingsService.isFastCacheEnabled());
    }

    /**
     * Returns all media files that are children of a given media file.
     *
     * @param includeFiles       Whether files should be included in the result.
     * @param includeDirectories Whether directories should be included in the result.
     * @param sort               Whether to sort files in the same directory.
     * @return All children media files.
     */
    public List<MediaFile> getChildrenOf(MediaFile parent, boolean includeFiles, boolean includeDirectories, boolean sort, boolean useFastCache) {

        if (!parent.isDirectory()) {
            return Collections.emptyList();
        }

        // Make sure children are stored and up-to-date in the database.
        if (!useFastCache) {
            updateChildren(parent);
        }

        List<MediaFile> result = new ArrayList<MediaFile>();
        for (MediaFile child : mediaFileDao.getChildrenOf(parent.getPath())) {
            child = checkLastModified(child, useFastCache);
            if (child.isDirectory() && includeDirectories) {
                result.add(child);
            }
            if (child.isFile() && includeFiles) {
                result.add(child);
            }
        }

        if (sort) {
            Comparator<MediaFile> comparator = new MediaFileComparator(settingsService.isSortAlbumsByYear());
            // Note: Intentionally not using Collections.sort() since it can be problematic on Java 7.
            // http://www.oracle.com/technetwork/java/javase/compatibility-417013.html#jdk7
            Set<MediaFile> set = new TreeSet<MediaFile>(comparator);
            set.addAll(result);
            result = new ArrayList<MediaFile>(set);
        }

        return result;
    }

    /**
     * Returns whether the given file is the root of a media folder.
     *
     * @see MusicFolder
     */
    public boolean isRoot(MediaFile mediaFile) {
        for (MusicFolder musicFolder : settingsService.getAllMusicFolders(false, true)) {
            if (mediaFile.getPath().equals(musicFolder.getPath().getPath())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns all genres in the music collection.
     *
     * @param sortByAlbum Whether to sort by album count, rather than song count.
     * @return Sorted list of genres.
     */
    public List<Genre> getGenres(boolean sortByAlbum) {
        return mediaFileDao.getGenres(sortByAlbum);
    }

    /**
     * Returns the most frequently played albums.
     *
     * @param offset       Number of albums to skip.
     * @param count        Maximum number of albums to return.
     * @param musicFolders Only return albums in these folders.
     * @return The most frequently played albums.
     */
    public List<MediaFile> getMostFrequentlyPlayedAlbums(int offset, int count, List<MusicFolder> musicFolders) {
        return mediaFileDao.getMostFrequentlyPlayedAlbums(offset, count, musicFolders);
    }

    /**
     * Returns the most recently played albums.
     *
     * @param offset       Number of albums to skip.
     * @param count        Maximum number of albums to return.
     * @param musicFolders Only return albums in these folders.
     * @return The most recently played albums.
     */
    public List<MediaFile> getMostRecentlyPlayedAlbums(int offset, int count, List<MusicFolder> musicFolders) {
        return mediaFileDao.getMostRecentlyPlayedAlbums(offset, count, musicFolders);
    }

    /**
     * Returns the most recently added albums.
     *
     * @param offset       Number of albums to skip.
     * @param count        Maximum number of albums to return.
     * @param musicFolders Only return albums in these folders.
     * @return The most recently added albums.
     */
    public List<MediaFile> getNewestAlbums(int offset, int count, List<MusicFolder> musicFolders) {
        return mediaFileDao.getNewestAlbums(offset, count, musicFolders);
    }

    /**
     * Returns the most recently starred albums.
     *
     * @param offset       Number of albums to skip.
     * @param count        Maximum number of albums to return.
     * @param username     Returns albums starred by this user.
     * @param musicFolders Only return albums from these folders.
     * @return The most recently starred albums for this user.
     */
    public List<MediaFile> getStarredAlbums(int offset, int count, String username, List<MusicFolder> musicFolders) {
        return mediaFileDao.getStarredAlbums(offset, count, username, musicFolders);
    }

    /**
     * Returns albums in alphabetical order.
     *
     * @param offset       Number of albums to skip.
     * @param count        Maximum number of albums to return.
     * @param byArtist     Whether to sort by artist name
     * @param musicFolders Only return albums in these folders.
     * @return Albums in alphabetical order.
     */
    public List<MediaFile> getAlphabeticalAlbums(int offset, int count, boolean byArtist, List<MusicFolder> musicFolders) {
        return mediaFileDao.getAlphabeticalAlbums(offset, count, byArtist, musicFolders);
    }

    /**
     * Returns albums within a year range.
     *
     * @param offset       Number of albums to skip.
     * @param count        Maximum number of albums to return.
     * @param fromYear     The first year in the range.
     * @param toYear       The last year in the range.
     * @param musicFolders Only return albums in these folders.
     * @return Albums in the year range.
     */
    public List<MediaFile> getAlbumsByYear(int offset, int count, int fromYear, int toYear, List<MusicFolder> musicFolders) {
        return mediaFileDao.getAlbumsByYear(offset, count, fromYear, toYear, musicFolders);
    }

    /**
     * Returns albums in a genre.
     *
     * @param offset       Number of albums to skip.
     * @param count        Maximum number of albums to return.
     * @param genre        The genre name.
     * @param musicFolders Only return albums in these folders.
     * @return Albums in the genre.
     */
    public List<MediaFile> getAlbumsByGenre(int offset, int count, String genre, List<MusicFolder> musicFolders) {
        return mediaFileDao.getAlbumsByGenre(offset, count, genre, musicFolders);
    }

    /**
     * Returns random songs for the given parent.
     *
     * @param parent The parent.
     * @param count  Max number of songs to return.
     * @return Random songs.
     */
    public List<MediaFile> getRandomSongsForParent(MediaFile parent, int count) {
        List<MediaFile> children = getDescendantsOf(parent, false);
        removeVideoFiles(children);

        if (children.isEmpty()) {
            return children;
        }
        Collections.shuffle(children);
        return children.subList(0, Math.min(count, children.size()));
    }

    /**
     * Returns random songs matching search criteria.
     *
     */
    public List<MediaFile> getRandomSongs(RandomSearchCriteria criteria, String username) {
        return mediaFileDao.getRandomSongs(criteria, username);
    }

    /**
     * Removes video files from the given list.
     */
    public void removeVideoFiles(List<MediaFile> files) {
        Iterator<MediaFile> iterator = files.iterator();
        while (iterator.hasNext()) {
            MediaFile file = iterator.next();
            if (file.isVideo()) {
                iterator.remove();
            }
        }
    }

    public Date getMediaFileStarredDate(int id, String username) {
        return mediaFileDao.getMediaFileStarredDate(id, username);
    }

    public void populateStarredDate(List<MediaFile> mediaFiles, String username) {
        for (MediaFile mediaFile : mediaFiles) {
            populateStarredDate(mediaFile, username);
        }
    }

    public void populateStarredDate(MediaFile mediaFile, String username) {
        Date starredDate = mediaFileDao.getMediaFileStarredDate(mediaFile.getId(), username);
        mediaFile.setStarredDate(starredDate);
    }

    private void updateChildren(MediaFile parent) {

        // Check timestamps.
        if (parent.getChildrenLastUpdated().getTime() >= parent.getChanged().getTime()) {
            return;
        }

        List<MediaFile> storedChildren = mediaFileDao.getChildrenOf(parent.getPath());
        Map<String, MediaFile> storedChildrenMap = new HashMap<String, MediaFile>();
        for (MediaFile child : storedChildren) {
            storedChildrenMap.put(child.getPath(), child);
        }

        List<File> children = filterMediaFiles(FileUtil.listFiles(parent.getFile()));
        for (File child : children) {
            if (storedChildrenMap.remove(child.getPath()) == null) {
                // Add children that are not already stored.
                mediaFileDao.createOrUpdateMediaFile(createMediaFile(child));
            }
        }

        // Delete children that no longer exist on disk.
        for (String path : storedChildrenMap.keySet()) {
            mediaFileDao.deleteMediaFile(path);
        }

        // Update timestamp in parent.
        parent.setChildrenLastUpdated(parent.getChanged());
        parent.setPresent(true);
        mediaFileDao.createOrUpdateMediaFile(parent);
    }

    public List<File> filterMediaFiles(File[] candidates) {
        List<File> result = new ArrayList<File>();
        for (File candidate : candidates) {
            String suffix = FilenameUtils.getExtension(candidate.getName()).toLowerCase();
            if (!isExcluded(candidate) && (FileUtil.isDirectory(candidate) || isAudioFile(suffix) || isVideoFile(suffix))) {
                result.add(candidate);
            }
        }
        return result;
    }

    private boolean isAudioFile(String suffix) {
        for (String s : settingsService.getMusicFileTypesAsArray()) {
            if (suffix.equals(s.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private boolean isVideoFile(String suffix) {
        for (String s : settingsService.getVideoFileTypesAsArray()) {
            if (suffix.equals(s.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns whether the given file is excluded.
     *
     * @param file The child file in question.
     * @return Whether the child file is excluded.
     */
    private boolean isExcluded(File file) {
        if (settingsService.getIgnoreSymLinks() && Files.isSymbolicLink(file.toPath())) {
            LOG.info("excluding symbolic link " + file.toPath());
            return true;
        }
        String name = file.getName();
        if (settingsService.getExcludePattern() != null && settingsService.getExcludePattern().matcher(name).find()) {
            LOG.info("excluding file which matches exclude pattern " + settingsService.getExcludePatternString() + ": " + file.toPath());
            return true;
        }

        // Exclude all hidden files starting with a single "." or "@eaDir" (thumbnail dir created on Synology devices).
        return (name.startsWith(".") && !name.startsWith("..")) || name.startsWith("@eaDir") || name.equals("Thumbs.db");
    }

    private MediaFile createMediaFile(File file) {

        MediaFile existingFile = mediaFileDao.getMediaFile(file.getPath());

        MediaFile mediaFile = new MediaFile();
        Date lastModified = new Date(FileUtil.lastModified(file));
        mediaFile.setPath(file.getPath());
        mediaFile.setFolder(securityService.getRootFolderForFile(file));
        mediaFile.setParentPath(file.getParent());
        mediaFile.setChanged(lastModified);
        mediaFile.setLastScanned(new Date());
        mediaFile.setPlayCount(existingFile == null ? 0 : existingFile.getPlayCount());
        mediaFile.setLastPlayed(existingFile == null ? null : existingFile.getLastPlayed());
        mediaFile.setComment(existingFile == null ? null : existingFile.getComment());
        mediaFile.setChildrenLastUpdated(new Date(0));
        mediaFile.setCreated(lastModified);
        mediaFile.setMediaType(MediaFile.MediaType.DIRECTORY);
        mediaFile.setPresent(true);

        if (file.isFile()) {

            MetaDataParser parser = metaDataParserFactory.getParser(file);
            if (parser != null) {
                MetaData metaData = parser.getMetaData(file);
                mediaFile.setArtist(metaData.getArtist());
                mediaFile.setAlbumArtist(metaData.getAlbumArtist());
                mediaFile.setAlbumName(metaData.getAlbumName());
                mediaFile.setTitle(metaData.getTitle());
                mediaFile.setDiscNumber(metaData.getDiscNumber());
                mediaFile.setTrackNumber(metaData.getTrackNumber());
                mediaFile.setGenre(metaData.getGenre());
                mediaFile.setYear(metaData.getYear());
                mediaFile.setDurationSeconds(metaData.getDurationSeconds());
                mediaFile.setBitRate(metaData.getBitRate());
                mediaFile.setVariableBitRate(metaData.getVariableBitRate());
                mediaFile.setHeight(metaData.getHeight());
                mediaFile.setWidth(metaData.getWidth());
                mediaFile.setMusicBrainzReleaseId(metaData.getMusicBrainzReleaseId());
            }
            String format = StringUtils.trimToNull(StringUtils.lowerCase(FilenameUtils.getExtension(mediaFile.getPath())));
            mediaFile.setFormat(format);
            mediaFile.setFileSize(FileUtil.length(file));
            mediaFile.setMediaType(getMediaType(mediaFile));

        } else {

            // Is this an album?
            if (!isRoot(mediaFile)) {
                File[] children = FileUtil.listFiles(file);
                File firstChild = null;
                for (File child : filterMediaFiles(children)) {
                    if (FileUtil.isFile(child)) {
                        firstChild = child;
                        break;
                    }
                }

                if (firstChild != null) {
                    mediaFile.setMediaType(MediaFile.MediaType.ALBUM);

                    // Guess artist/album name, year and genre.
                    MetaDataParser parser = metaDataParserFactory.getParser(firstChild);
                    if (parser != null) {
                        MetaData metaData = parser.getMetaData(firstChild);
                        mediaFile.setArtist(metaData.getAlbumArtist());
                        mediaFile.setAlbumName(metaData.getAlbumName());
                        mediaFile.setYear(metaData.getYear());
                        mediaFile.setGenre(metaData.getGenre());
                    }

                    // Look for cover art.
                    try {
                        File coverArt = findCoverArt(children);
                        if (coverArt != null) {
                            mediaFile.setCoverArtPath(coverArt.getPath());
                        }
                    } catch (IOException x) {
                        LOG.error("Failed to find cover art.", x);
                    }

                } else {
                    mediaFile.setArtist(file.getName());
                }
            }
        }

        return mediaFile;
    }

    private MediaFile.MediaType getMediaType(MediaFile mediaFile) {
        if (isVideoFile(mediaFile.getFormat())) {
            return MediaFile.MediaType.VIDEO;
        }
        String path = mediaFile.getPath().toLowerCase();
        String genre = StringUtils.trimToEmpty(mediaFile.getGenre()).toLowerCase();
        if (path.contains("podcast") || genre.contains("podcast")) {
            return MediaFile.MediaType.PODCAST;
        }
        if (path.contains("audiobook") || genre.contains("audiobook") || path.contains("audio book") || genre.contains("audio book")) {
            return MediaFile.MediaType.AUDIOBOOK;
        }
        return MediaFile.MediaType.MUSIC;
    }

    public void refreshMediaFile(MediaFile mediaFile) {
        mediaFile = createMediaFile(mediaFile.getFile());
        mediaFileDao.createOrUpdateMediaFile(mediaFile);
        mediaFileMemoryCache.remove(mediaFile.getFile());
    }

    private void putInMemoryCache(File file, MediaFile mediaFile) {
        if (memoryCacheEnabled) {
            mediaFileMemoryCache.put(new Element(file, mediaFile));
        }
    }

    private MediaFile getFromMemoryCache(File file) {
        if (!memoryCacheEnabled) {
            return null;
        }
        Element element = mediaFileMemoryCache.get(file);
        return element == null ? null : (MediaFile) element.getObjectValue();
    }

    public void setMemoryCacheEnabled(boolean memoryCacheEnabled) {
        this.memoryCacheEnabled = memoryCacheEnabled;
        if (!memoryCacheEnabled) {
            mediaFileMemoryCache.removeAll();
        }
    }

    /**
     * Returns a cover art image for the given media file.
     */
    public File getCoverArt(MediaFile mediaFile) {
        if (mediaFile.getCoverArtFile() != null) {
            return mediaFile.getCoverArtFile();
        }
        MediaFile parent = getParentOf(mediaFile);
        return parent == null ? null : parent.getCoverArtFile();
    }

    /**
     * Finds a cover art image for the given directory, by looking for it on the disk.
     */
    private File findCoverArt(File[] candidates) throws IOException {
        for (String mask : settingsService.getCoverArtFileTypesAsArray()) {
            for (File candidate : candidates) {
                if (candidate.isFile() && candidate.getName().toUpperCase().endsWith(mask.toUpperCase()) && !candidate.getName().startsWith(".")) {
                    return candidate;
                }
            }
        }

        // Look for embedded images in audiofiles. (Only check first audio file encountered).
        for (File candidate : candidates) {
            if (parser.isApplicable(candidate)) {
                if (parser.isImageAvailable(getMediaFile(candidate))) {
                    return candidate;
                } else {
                    return null;
                }
            }
        }
        return null;
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setMediaFileMemoryCache(Ehcache mediaFileMemoryCache) {
        this.mediaFileMemoryCache = mediaFileMemoryCache;
    }

    public void setMediaFileDao(MediaFileDao mediaFileDao) {
        this.mediaFileDao = mediaFileDao;
    }

    /**
     * Returns all media files that are children, grand-children etc of a given media file.
     * Directories are not included in the result.
     *
     * @param sort Whether to sort files in the same directory.
     * @return All descendant music files.
     */
    public List<MediaFile> getDescendantsOf(MediaFile ancestor, boolean sort) {

        if (ancestor.isFile()) {
            return Arrays.asList(ancestor);
        }

        List<MediaFile> result = new ArrayList<MediaFile>();

        for (MediaFile child : getChildrenOf(ancestor, true, true, sort)) {
            if (child.isDirectory()) {
                result.addAll(getDescendantsOf(child, sort));
            } else {
                result.add(child);
            }
        }
        return result;
    }

    public void setMetaDataParserFactory(MetaDataParserFactory metaDataParserFactory) {
        this.metaDataParserFactory = metaDataParserFactory;
    }

    public void updateMediaFile(MediaFile mediaFile) {
        mediaFileDao.createOrUpdateMediaFile(mediaFile);
    }

    /**
     * Increments the play count and last played date for the given media file and its
     * directory and album.
     */
    public void incrementPlayCount(MediaFile file) {
        Date now = new Date();
        file.setLastPlayed(now);
        file.setPlayCount(file.getPlayCount() + 1);
        updateMediaFile(file);

        MediaFile parent = getParentOf(file);
        if (!isRoot(parent)) {
            parent.setLastPlayed(now);
            parent.setPlayCount(parent.getPlayCount() + 1);
            updateMediaFile(parent);
        }

        Album album = albumDao.getAlbum(file.getAlbumArtist(), file.getAlbumName());
        if (album != null) {
            album.setLastPlayed(now);
            album.setPlayCount(album.getPlayCount() + 1);
            albumDao.createOrUpdateAlbum(album);
        }
    }

    public int getAlbumCount(List<MusicFolder> musicFolders) {
        return mediaFileDao.getAlbumCount(musicFolders);
    }

    public int getPlayedAlbumCount(List<MusicFolder> musicFolders) {
        return mediaFileDao.getPlayedAlbumCount(musicFolders);
    }

    public int getStarredAlbumCount(String username, List<MusicFolder> musicFolders) {
        return mediaFileDao.getStarredAlbumCount(username, musicFolders);
    }

    public void clearMemoryCache() {
        mediaFileMemoryCache.removeAll();
    }

    public void setAlbumDao(AlbumDao albumDao) {
        this.albumDao = albumDao;
    }

    public void setParser(JaudiotaggerParser parser) {
        this.parser = parser;
    }
}
