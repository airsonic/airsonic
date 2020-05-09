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
import org.airsonic.player.dao.MediaFileDao;
import org.airsonic.player.domain.*;
import org.airsonic.player.service.search.IndexManager;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Provides services for scanning the music library.
 *
 * @author Sindre Mehus
 */
@Service
public class MediaScannerService {

    private static final Logger LOG = LoggerFactory.getLogger(MediaScannerService.class);

    private volatile boolean scanning;

    private ScheduledExecutorService scheduler;

    @Autowired
    private SettingsService settingsService;
    @Autowired
    private IndexManager indexManager;
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
    @Value("${MediaScannerParallelism:#{T(java.lang.Runtime).getRuntime().availableProcessors() + 1}}")
    private int scannerParallelism;

    private AtomicInteger scanCount = new AtomicInteger(0);

    @PostConstruct
    public void init() {
        indexManager.initializeIndexDirectory();
        schedule();
    }

    public void initNoSchedule() throws IOException {
        indexManager.deleteOldIndexFiles();
    }

    /**
     * Schedule background execution of media library scanning.
     */
    public synchronized void schedule() {
        if (scheduler != null) {
            scheduler.shutdown();
        }

        long daysBetween = settingsService.getIndexCreationInterval();
        int hour = settingsService.getIndexCreationHour();

        if (daysBetween == -1) {
            LOG.info("Automatic media scanning disabled.");
            return;
        }

        scheduler = Executors.newSingleThreadScheduledExecutor();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextRun = now.withHour(hour).withMinute(0).withSecond(0);
        if (now.compareTo(nextRun) > 0)
            nextRun = nextRun.plusDays(1);

        long initialDelay = ChronoUnit.MILLIS.between(now, nextRun);

        scheduler.scheduleAtFixedRate(() -> scanLibrary(), initialDelay, TimeUnit.DAYS.toMillis(daysBetween), TimeUnit.MILLISECONDS);

        LOG.info("Automatic media library scanning scheduled to run every {} day(s), starting at {}", daysBetween, nextRun);

        // In addition, create index immediately if it doesn't exist on disk.
        if (neverScanned()) {
            LOG.info("Media library never scanned. Doing it now.");
            scanLibrary();
        }
    }

    boolean neverScanned() {
        return indexManager.getStatistics() == null;
    }

    /**
     * Returns whether the media library is currently being scanned.
     */
    public boolean isScanning() {
        return scanning;
    }

    private void setScanning(boolean scanning) {
        this.scanning = scanning;
    }

    /**
     * Returns the number of files scanned so far.
     */
    public int getScanCount() {
        return scanCount.get();
    }

    private static ForkJoinWorkerThreadFactory mediaScannerThreadFactory = new ForkJoinWorkerThreadFactory() {
        @Override
        public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
            final ForkJoinWorkerThread worker = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
            worker.setName("MediaLibraryScanner-" + worker.getPoolIndex());
            worker.setPriority(Thread.MIN_PRIORITY);
            return worker;
        }
    };

    /**
     * Scans the media library.
     * The scanning is done asynchronously, i.e., this method returns immediately.
     */
    public synchronized void scanLibrary() {
        if (isScanning()) {
            return;
        }
        setScanning(true);

        ForkJoinPool pool = new ForkJoinPool(scannerParallelism, mediaScannerThreadFactory, null, true);

        CompletableFuture.runAsync(() -> doScanLibrary(pool), pool)
                .thenRunAsync(() -> playlistService.importPlaylists(), pool)
                .thenRunAsync(() -> mediaFileDao.checkpoint(), pool)
                .whenComplete((r, t) -> {
                    pool.shutdown();
                    setScanning(false);
                });
    }

    private void doScanLibrary(ForkJoinPool pool) {
        LOG.info("Starting to scan media library.");
        MediaLibraryStatistics statistics = new MediaLibraryStatistics(
                DateUtils.truncate(new Date(), Calendar.SECOND));
        LOG.debug("New last scan date is {}", statistics.getScanDate());

        try {
            // Maps from artist name to album count.
            Map<String, AtomicInteger> albumCount = new ConcurrentHashMap<>();
            Map<String, Artist> artists = new ConcurrentHashMap<>();
            Map<String, Album> albums = new ConcurrentHashMap<>();
            Map<String, Boolean> encountered = new ConcurrentHashMap<>();
            Genres genres = new Genres();

            scanCount.set(0);

            mediaFileService.setMemoryCacheEnabled(false);
            indexManager.startIndexing();

            // Recurse through all files on disk.
            settingsService.getAllMusicFolders()
                .parallelStream()
                .forEach(musicFolder -> scanFile(mediaFileService.getMediaFile(musicFolder.getPath(), false), musicFolder, statistics, albumCount, artists, albums, genres, encountered, false));

            // Scan podcast folder.
            Path podcastFolder = Paths.get(settingsService.getPodcastFolder());
            if (Files.exists(podcastFolder)) {
                scanFile(mediaFileService.getMediaFile(podcastFolder.toFile()), new MusicFolder(podcastFolder.toFile(), null, true, null),
                         statistics, albumCount, artists, albums, genres, encountered, true);
            }

            LOG.info("Scanned media library with {} entries.", scanCount.get());

            // Update statistics
            statistics.incrementArtists(albumCount.size());
            statistics.incrementAlbums(albumCount.values().parallelStream().mapToInt(x -> x.get()).sum());

            LOG.info("Persisting albums");
            CompletableFuture<Void> albumPersistence = CompletableFuture
                    .allOf(albums.values().parallelStream()
                            .map(a -> CompletableFuture.runAsync(() -> albumDao.createOrUpdateAlbum(a), pool))
                            .toArray(CompletableFuture[]::new))
                    .thenRunAsync(() -> {
                        LOG.debug("Marking non-present albums.");
                        albumDao.markNonPresent(statistics.getScanDate());
                    }, pool)
                    .thenRunAsync(() -> LOG.info("Album persistence complete"), pool);

            LOG.info("Persisting artists");
            CompletableFuture<Void> artistPersistence = CompletableFuture
                    .allOf(artists.values().parallelStream()
                            .map(a -> CompletableFuture.runAsync(() -> artistDao.createOrUpdateArtist(a), pool))
                            .toArray(CompletableFuture[]::new))
                    .thenRunAsync(() -> {
                        LOG.debug("Marking non-present artists.");
                        artistDao.markNonPresent(statistics.getScanDate());
                    }, pool)
                    .thenRunAsync(() -> LOG.info("Artist persistence complete"), pool);

            LOG.info("Marking present files");
            CompletableFuture<Void> mediaFilePersistence = CompletableFuture
                    .runAsync(() -> mediaFileDao.markPresent(encountered.keySet(), statistics.getScanDate()), pool)
                    .thenRunAsync(() -> {
                        LOG.debug("Marking non-present files.");
                        mediaFileDao.markNonPresent(statistics.getScanDate());
                    }, pool)
                    .thenRunAsync(() -> LOG.info("File marking complete"), pool);

            LOG.info("Persisting genres");
            CompletableFuture<Void> genrePersistence = CompletableFuture
                    .runAsync(() -> {
                        LOG.debug("Updating genres");
                        mediaFileDao.updateGenres(genres.getGenres());
                    }, pool)
                    .thenRunAsync(() -> LOG.info("Genre persistence complete"), pool);

            CompletableFuture.allOf(albumPersistence, artistPersistence, mediaFilePersistence, genrePersistence).join();

            LOG.debug("Completed media library scan.");

        } catch (Throwable x) {
            LOG.error("Failed to scan media library.", x);
        } finally {
            mediaFileService.setMemoryCacheEnabled(true);
            indexManager.stopIndexing(statistics);
            LOG.info("Completed media library scan in {}s.", ChronoUnit.SECONDS.between(statistics.getScanDate().toInstant(), Instant.now()));
        }
    }

    private void scanFile(MediaFile file, MusicFolder musicFolder, MediaLibraryStatistics statistics,
                          Map<String, AtomicInteger> albumCount, Map<String, Artist> artists, Map<String, Album> albums, Genres genres, Map<String, Boolean> encountered, boolean isPodcast) {
        if (scanCount.incrementAndGet() % 250 == 0) {
            LOG.info("Scanned media library with {} entries.", scanCount.get());
        }

        LOG.trace("Scanning file {}", file.getPath());

        // Update the root folder if it has changed.
        if (!musicFolder.getPath().toString().equals(file.getFolder())) {
            file.setFolder(musicFolder.getPath().toString());
            mediaFileDao.createOrUpdateMediaFile(file);
        }

        indexManager.index(file);

        if (file.isDirectory()) {
            mediaFileService.getChildrenOf(file, true, true, false, false)
                .parallelStream()
                .forEach(child -> scanFile(child, musicFolder, statistics, albumCount, artists, albums, genres, encountered, isPodcast));
        } else {
            if (!isPodcast) {
                updateAlbum(file, musicFolder, statistics.getScanDate(), albumCount, albums);
                updateArtist(file, musicFolder, statistics.getScanDate(), albumCount, artists);
            }
            statistics.incrementSongs(1);
        }

        updateGenres(file, genres);
        encountered.putIfAbsent(file.getPath(), Boolean.TRUE);

        if (file.getDurationSeconds() != null) {
            statistics.incrementTotalDurationInSeconds(file.getDurationSeconds());
        }
        if (file.getFileSize() != null) {
            statistics.incrementTotalLengthInBytes(file.getFileSize());
        }
    }

    private void updateGenres(MediaFile file, Genres genres) {
        String genre = file.getGenre();
        if (genre == null) {
            return;
        }
        if (file.isAlbum()) {
            genres.incrementAlbumCount(genre);
        } else if (file.isAudio()) {
            genres.incrementSongCount(genre);
        }
    }

    private void updateAlbum(MediaFile file, MusicFolder musicFolder, Date lastScanned, Map<String, AtomicInteger> albumCount, Map<String, Album> albums) {
        String artist = file.getAlbumArtist() != null ? file.getAlbumArtist() : file.getArtist();
        if (file.getAlbumName() == null || artist == null || file.getParentPath() == null || !file.isAudio()) {
            return;
        }

        final AtomicBoolean firstEncounter = new AtomicBoolean(false);
        Album album = albums.compute(file.getAlbumName() + "|" + artist, (k,v) -> {
            Album a = v;

            if (a == null) {
                a = albumDao.getAlbumForFile(file);
            }

            if (a == null) {
                a = new Album();
                a.setPath(file.getParentPath());
                a.setName(file.getAlbumName());
                a.setArtist(artist);
                a.setCreated(file.getChanged());
            }

            firstEncounter.set(!lastScanned.equals(a.getLastScanned()));

            if (file.getDurationSeconds() != null) {
                a.incrementDurationSeconds(file.getDurationSeconds());
            }
            if (file.isAudio()) {
                a.incrementSongCount();
            }

            a.setLastScanned(lastScanned);
            a.setPresent(true);

            return a;
        });

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

        if (firstEncounter.get()) {
            album.setFolderId(musicFolder.getId());
            albumCount.computeIfAbsent(artist, k -> new AtomicInteger(0)).incrementAndGet();
            indexManager.index(album);
        }

        // Update the file's album artist, if necessary.
        if (!ObjectUtils.equals(album.getArtist(), file.getAlbumArtist())) {
            file.setAlbumArtist(album.getArtist());
            mediaFileDao.createOrUpdateMediaFile(file);
        }
    }

    private void updateArtist(MediaFile file, MusicFolder musicFolder, Date lastScanned, Map<String, AtomicInteger> albumCount, Map<String, Artist> artists) {
        if (file.getAlbumArtist() == null || !file.isAudio()) {
            return;
        }

        final AtomicBoolean firstEncounter = new AtomicBoolean(false);

        Artist artist = artists.compute(file.getAlbumArtist(), (k,v) -> {
            Artist a = v;

            if (a == null) {
                a = artistDao.getArtist(k);
            }

            if (a == null) {
                a = new Artist();
                a.setName(k);
            }

            int n = Math.max(Optional.ofNullable(albumCount.get(a.getName())).map(x -> x.get()).orElse(0), Optional.ofNullable(a.getAlbumCount()).orElse(0));
            a.setAlbumCount(n);

            firstEncounter.set(!lastScanned.equals(a.getLastScanned()));

            a.setLastScanned(lastScanned);
            a.setPresent(true);

            return a;
        });

        if (artist.getCoverArtPath() == null) {
            MediaFile parent = mediaFileService.getParentOf(file);
            if (parent != null) {
                artist.setCoverArtPath(parent.getCoverArtPath());
            }
        }

        if (firstEncounter.get()) {
            artist.setFolderId(musicFolder.getId());
            indexManager.index(artist, musicFolder);
        }
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
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

    public void setPlaylistService(PlaylistService playlistService) {
        this.playlistService = playlistService;
    }
}
