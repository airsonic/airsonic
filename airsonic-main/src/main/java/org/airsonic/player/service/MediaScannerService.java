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

import org.airsonic.player.dao.AlbumDao;
import org.airsonic.player.dao.ArtistDao;
import org.airsonic.player.dao.GenreDao;
import org.airsonic.player.dao.MediaFileDao;
import org.airsonic.player.domain.*;
import org.airsonic.player.util.FileUtil;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

import java.io.File;
import java.util.*;

/**
 * Provides services for scanning the music library.
 *
 * @author Sindre Mehus
 */
@Service
public class MediaScannerService {

    private static final int INDEX_VERSION = 15;
    private static final Logger LOG = LoggerFactory.getLogger(MediaScannerService.class);

    private MediaLibraryStatistics statistics;

    private boolean scanning;
    private Timer timer;
    @Autowired
    private SettingsService settingsService;
    @Autowired
    private SearchService searchService;
    @Autowired
    private PlaylistService playlistService;
    @Autowired
    private MediaFileService mediaFileService;
    @Autowired
    private MediaFileDao mediaFileDao;
    @Autowired
    private ArtistDao artistDao;
    @Autowired
    private AlbumDao albumDao;
    @Autowired
    private GenreDao genreDao;
    private int scanCount;

    @PostConstruct
    public void init() {
        deleteOldIndexFiles();
        statistics = settingsService.getMediaLibraryStatistics();
        schedule();
    }

    public void initNoSchedule() {
        deleteOldIndexFiles();
        statistics = settingsService.getMediaLibraryStatistics();
    }

    /**
     * Schedule background execution of media library scanning.
     */
    public synchronized void schedule() {
        if (timer != null) {
            timer.cancel();
        }
        timer = new Timer(true);

        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                scanLibrary();
            }
        };

        long daysBetween = settingsService.getIndexCreationInterval();
        int hour = settingsService.getIndexCreationHour();

        if (daysBetween == -1) {
            LOG.info("Automatic media scanning disabled.");
            return;
        }

        Date now = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);

        if (cal.getTime().before(now)) {
            cal.add(Calendar.DATE, 1);
        }

        Date firstTime = cal.getTime();
        long period = daysBetween * 24L * 3600L * 1000L;
        timer.schedule(task, firstTime, period);

        LOG.info("Automatic media library scanning scheduled to run every " + daysBetween + " day(s), starting at " + firstTime);

        // In addition, create index immediately if it doesn't exist on disk.
        if (settingsService.getLastScanned() == null) {
            LOG.info("Media library never scanned. Doing it now.");
            scanLibrary();
        }
    }

    /**
     * Returns whether the media library is currently being scanned.
     */
    public synchronized boolean isScanning() {
        return scanning;
    }

    /**
     * Returns the number of files scanned so far.
     */
    public int getScanCount() {
        return scanCount;
    }

    /**
     * Scans the media library.
     * The scanning is done asynchronously, i.e., this method returns immediately.
     */
    public synchronized void scanLibrary() {
        if (isScanning()) {
            return;
        }
        scanning = true;

        Thread thread = new Thread("MediaLibraryScanner") {
            @Override
            public void run() {
                doScanLibrary();
                playlistService.importPlaylists();
            }
        };

        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    private void doScanLibrary() {
        LOG.info("Starting to scan media library.");
        Date lastScanned = DateUtils.truncate(new Date(), Calendar.SECOND);
        LOG.debug("New last scan date is " + lastScanned);

        try {

            // Maps from artist name to album count.
            Map<String, Integer> albumCount = new HashMap<String, Integer>();

            Map<String, Genre> genres = new HashMap<>();

            scanCount = 0;
            statistics.reset();

            mediaFileService.setMemoryCacheEnabled(false);
            searchService.startIndexing();

            mediaFileService.clearMemoryCache();

            genreDao.resetCounts();

            // Recurse through all files on disk.
            for (MusicFolder musicFolder : settingsService.getAllMusicFolders()) {
                MediaFile root = mediaFileService.getMediaFile(musicFolder.getPath(), false);
                scanFile(root, musicFolder, lastScanned, albumCount, genres, false);
            }

            // Scan podcast folder.
            File podcastFolder = new File(settingsService.getPodcastFolder());
            if (podcastFolder.exists()) {
                scanFile(mediaFileService.getMediaFile(podcastFolder), new MusicFolder(podcastFolder, null, true, null),
                         lastScanned, albumCount, genres, true);
            }

            LOG.info("Scanned media library with " + scanCount + " entries.");

            LOG.info("Marking non-present files.");
            mediaFileDao.markNonPresent(lastScanned);
            LOG.info("Marking non-present artists.");
            artistDao.markNonPresent(lastScanned);
            LOG.info("Marking non-present albums.");
            albumDao.markNonPresent(lastScanned);

            // Update statistics
            statistics.incrementArtists(albumCount.size());
            for (Integer albums : albumCount.values()) {
                statistics.incrementAlbums(albums);
            }

            settingsService.setMediaLibraryStatistics(statistics);
            settingsService.setLastScanned(lastScanned);
            settingsService.save(false);
            LOG.info("Completed media library scan.");

        } catch (Throwable x) {
            LOG.error("Failed to scan media library.", x);
        } finally {
            mediaFileService.setMemoryCacheEnabled(true);
            searchService.stopIndexing();
            scanning = false;
        }
    }

    private void scanFile(MediaFile file, MusicFolder musicFolder, Date lastScanned,
                          Map<String, Integer> albumCount, Map<String, Genre> genreCache, boolean isPodcast) {
        scanCount++;
        if (scanCount % 250 == 0) {
            LOG.info("Scanned media library with " + scanCount + " entries.");
        }

        LOG.trace("Scanning file {}", file.getPath());

        // Update the root folder if it has changed.
        if (!musicFolder.getPath().getPath().equals(file.getFolder())) {
            file.setFolder(musicFolder.getPath().getPath());
            mediaFileDao.createOrUpdateMediaFile(file);
        }

        if (file.isDirectory()) {
            searchService.index(file, Collections.emptyList());

            for (MediaFile child : mediaFileService.getChildrenOf(file, true, false, false, false)) {
                scanFile(child, musicFolder, lastScanned, albumCount, genreCache, isPodcast);
            }
            for (MediaFile child : mediaFileService.getChildrenOf(file, false, true, false, false)) {
                scanFile(child, musicFolder, lastScanned, albumCount, genreCache, isPodcast);
            }
        } else {
            List<Genre> genres = getGenres(file, genreCache);
            for (Genre genre : genres) {
                genreDao.addGenre(file, genre);
            }

            searchService.index(file, genres);

            if (!isPodcast) {
                updateAlbum(file, musicFolder, lastScanned, albumCount, genres);
                updateArtist(file, musicFolder, lastScanned, albumCount);
            }
            statistics.incrementSongs(1);
        }

        mediaFileDao.markPresent(file.getPath(), lastScanned);
        artistDao.markPresent(file.getAlbumArtist(), lastScanned);

        if (file.getDurationSeconds() != null) {
            statistics.incrementTotalDurationInSeconds(file.getDurationSeconds());
        }
        if (file.getFileSize() != null) {
            statistics.incrementTotalLengthInBytes(file.getFileSize());
        }
    }

    private List<Genre> getGenres(MediaFile file, Map<String, Genre> cache) {
        if (StringUtils.isNotBlank(file.getGenre())) {
            // Split the genres by the configured delimiter
            String[] split = file.getGenre().split(settingsService.getMultiGenreDelimiter());

            List<Genre> genres = new ArrayList<>(split.length);
            for (String genreStr : split) {
                String name = genreStr.trim();
                if (!name.isEmpty()) {
                    // First see if we have the genre cached locally
                    Genre genre = cache.get(name);
                    if (genre == null) {
                        // The genre is not cached locally, check the DB
                        genre = genreDao.getGenre(name);
                        if (genre == null) {
                            // We need to create the genre in the DB
                            genre = genreDao.create(name);
                        }
                    }
                    if (genre != null) {
                        genres.add(genre);
                    }
                }
            }

            return genres;
        }
        else {
            return Collections.emptyList();
        }
    }

    private void updateAlbum(MediaFile file, MusicFolder musicFolder, Date lastScanned, Map<String, Integer> albumCount, List<Genre> genres) {
        String artist = file.getAlbumArtist() != null ? file.getAlbumArtist() : file.getArtist();
        if (file.getAlbumName() == null || artist == null || file.getParentPath() == null || !file.isAudio()) {
            return;
        }

        Album album = albumDao.getAlbumForFile(file);
        if (album == null) {
            album = new Album();
            album.setPath(file.getParentPath());
            album.setName(file.getAlbumName());
            album.setArtist(artist);
            album.setCreated(file.getChanged());
        }
        if (file.getMusicBrainzReleaseId() != null) {
            album.setMusicBrainzReleaseId(file.getMusicBrainzReleaseId());
        }
        if (file.getYear() != null) {
            album.setYear(file.getYear());
        }
        if (file.getGenre() != null) {
            album.setGenre(file.getGenre());
        }
        MediaFile parent = mediaFileService.getParentOf(file);
        if (parent != null && parent.getCoverArtPath() != null) {
            album.setCoverArtPath(parent.getCoverArtPath());
        }

        boolean firstEncounter = !lastScanned.equals(album.getLastScanned());
        if (firstEncounter) {
            album.setFolderId(musicFolder.getId());
            album.setDurationSeconds(0);
            album.setSongCount(0);
            Integer n = albumCount.get(artist);
            albumCount.put(artist, n == null ? 1 : n + 1);
        }
        if (file.getDurationSeconds() != null) {
            album.setDurationSeconds(album.getDurationSeconds() + file.getDurationSeconds());
        }
        if (file.isAudio()) {
            album.setSongCount(album.getSongCount() + 1);
        }
        album.setLastScanned(lastScanned);
        album.setPresent(true);
        albumDao.createOrUpdateAlbum(album);
        if (firstEncounter) {
            searchService.index(album);
        }

        // Update the album genres
        for (Genre genre : genres) {
            genreDao.addGenre(album, genre);
        }

        // Update the file's album artist, if necessary.
        if (!ObjectUtils.equals(album.getArtist(), file.getAlbumArtist())) {
            file.setAlbumArtist(album.getArtist());
            mediaFileDao.createOrUpdateMediaFile(file);
        }
    }

    private void updateArtist(MediaFile file, MusicFolder musicFolder, Date lastScanned, Map<String, Integer> albumCount) {
        if (file.getAlbumArtist() == null || !file.isAudio()) {
            return;
        }

        Artist artist = artistDao.getArtist(file.getAlbumArtist());
        if (artist == null) {
            artist = new Artist();
            artist.setName(file.getAlbumArtist());
        }
        if (artist.getCoverArtPath() == null) {
            MediaFile parent = mediaFileService.getParentOf(file);
            if (parent != null) {
                artist.setCoverArtPath(parent.getCoverArtPath());
            }
        }
        boolean firstEncounter = !lastScanned.equals(artist.getLastScanned());

        if (firstEncounter) {
            artist.setFolderId(musicFolder.getId());
        }
        Integer n = albumCount.get(artist.getName());
        artist.setAlbumCount(n == null ? 0 : n);

        artist.setLastScanned(lastScanned);
        artist.setPresent(true);
        artistDao.createOrUpdateArtist(artist);

        if (firstEncounter) {
            searchService.index(artist, musicFolder);
        }
    }

    /**
     * Returns media library statistics, including the number of artists, albums and songs.
     *
     * @return Media library statistics.
     */
    public MediaLibraryStatistics getStatistics() {
        return statistics;
    }

    /**
     * Deletes old versions of the index file.
     */
    private void deleteOldIndexFiles() {
        for (int i = 2; i < INDEX_VERSION; i++) {
            File file = getIndexFile(i);
            try {
                if (FileUtil.exists(file)) {
                    if (file.delete()) {
                        LOG.info("Deleted old index file: " + file.getPath());
                    }
                }
            } catch (Exception x) {
                LOG.warn("Failed to delete old index file: " + file.getPath(), x);
            }
        }
    }

    /**
     * Returns the index file for the given index version.
     *
     * @param version The index version.
     * @return The index file for the given index version.
     */
    private File getIndexFile(int version) {
        File home = SettingsService.getAirsonicHome();
        return new File(home, "airsonic" + version + ".index");
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

    public void setMediaFileService(MediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
    }

    public void setMediaFileDao(MediaFileDao mediaFileDao) {
        this.mediaFileDao = mediaFileDao;
    }

    public void setArtistDao(ArtistDao artistDao) {
        this.artistDao = artistDao;
    }

    public void setAlbumDao(AlbumDao albumDao) {
        this.albumDao = albumDao;
    }

    public void setGenreDao(GenreDao genreDao) {
        this.genreDao = genreDao;
    }

    public void setPlaylistService(PlaylistService playlistService) {
        this.playlistService = playlistService;
    }
}
