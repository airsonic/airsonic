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

package org.airsonic.player.service.search;

import org.airsonic.player.dao.AlbumDao;
import org.airsonic.player.dao.ArtistDao;
import org.airsonic.player.dao.MediaFileDao;
import org.airsonic.player.domain.*;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.util.FileUtil;
import org.airsonic.player.util.Util;
import org.apache.commons.io.FileUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.store.FSDirectory;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Function class that is strongly linked to the lucene index implementation.
 * Legacy has an implementation in SearchService.
 * 
 * If the index CRUD and search functionality are in the same class,
 * there is often a dependency conflict on the class used.
 * Although the interface of SearchService is left to maintain the legacy implementation,
 * it is desirable that methods of index operations other than search essentially use this class directly.
 */
@Component
public class IndexManager {

    private static final Logger LOG = LoggerFactory.getLogger(IndexManager.class);

    /**
     * Schema version of Airsonic index.
     * It may be incremented in the following cases:
     * 
     *  - Incompatible update case in Lucene index implementation
     *  - When schema definition is changed due to modification of AnalyzerFactory,
     *    DocumentFactory or the class that they use.
     *
     */
    private static final int INDEX_VERSION = 16;

    /**
     * Literal name of index top directory.
     */
    private static final String INDEX_ROOT_DIR_NAME = "index";

    /**
     * File for index directory.
     */
    private File rootIndexDirectory = new File(SettingsService.getAirsonicHome(), INDEX_ROOT_DIR_NAME.concat(Integer.toString(INDEX_VERSION)));

    /**
     * Returns the directory of the specified index
     */
    private Function<IndexType, File> getIndexDirectory = (indexType) ->
        new File(rootIndexDirectory, indexType.toString().toLowerCase());

    @Autowired
    private AnalyzerFactory analyzerFactory;

    @Autowired
    private DocumentFactory documentFactory;

    @Autowired
    private MediaFileDao mediaFileDao;

    @Autowired
    private ArtistDao artistDao;

    @Autowired
    private AlbumDao albumDao;

    private Map<IndexType, SearcherManager> searchers = new ConcurrentHashMap<>();

    private Map<IndexType, IndexWriter> writers = new ConcurrentHashMap<>();

    public void index(Album album) {
        Term primarykey = documentFactory.createPrimarykey(album);
        Document document = documentFactory.createAlbumId3Document(album);
        try {
            writers.get(IndexType.ALBUM_ID3).updateDocument(primarykey, document);
        } catch (Exception x) {
            LOG.error("Failed to create search index for {}", album, x);
        }
    }

    public void index(Artist artist, MusicFolder musicFolder) {
        Term primarykey = documentFactory.createPrimarykey(artist);
        Document document = documentFactory.createArtistId3Document(artist, musicFolder);
        try {
            writers.get(IndexType.ARTIST_ID3).updateDocument(primarykey, document);
        } catch (Exception x) {
            LOG.error("Failed to create search index for {}", artist, x);
        }
    }

    public void index(MediaFile mediaFile) {
        Term primarykey = documentFactory.createPrimarykey(mediaFile);
        try {
            if (mediaFile.isFile()) {
                Document document = documentFactory.createSongDocument(mediaFile);
                writers.get(IndexType.SONG).updateDocument(primarykey, document);
            } else if (mediaFile.isAlbum()) {
                Document document = documentFactory.createAlbumDocument(mediaFile);
                writers.get(IndexType.ALBUM).updateDocument(primarykey, document);
            } else {
                Document document = documentFactory.createArtistDocument(mediaFile);
                writers.get(IndexType.ARTIST).updateDocument(primarykey, document);
            }
        } catch (Exception x) {
            LOG.error("Failed to create search index for {}", mediaFile, x);
        }
    }

    public final void startIndexing() {
        EnumSet.allOf(IndexType.class).parallelStream().forEach(x -> {
            try {
                writers.put(x, createIndexWriter(x));
            } catch (IOException e) {
                LOG.error("Failed to create search index for {}", x, e);
            }
        });
    }

    private IndexWriter createIndexWriter(IndexType indexType) throws IOException {
        File indexDirectory = getIndexDirectory.apply(indexType);
        IndexWriterConfig config = new IndexWriterConfig(analyzerFactory.getAnalyzer());
        return new IndexWriter(FSDirectory.open(indexDirectory.toPath()), config);
    }

    public void expunge() {
        Term[] primarykeys = mediaFileDao.getArtistExpungeCandidates().stream()
                .map(m -> documentFactory.createPrimarykey(m))
                .toArray(i -> new Term[i]);
        try {
            writers.get(IndexType.ARTIST).deleteDocuments(primarykeys);
        } catch (IOException e) {
            LOG.error("Failed to delete artist doc.", e);
        }

        primarykeys = mediaFileDao.getAlbumExpungeCandidates().stream()
                .map(m -> documentFactory.createPrimarykey(m))
                .toArray(i -> new Term[i]);
        try {
            writers.get(IndexType.ALBUM).deleteDocuments(primarykeys);
        } catch (IOException e) {
            LOG.error("Failed to delete album doc.", e);
        }

        primarykeys = mediaFileDao.getSongExpungeCandidates().stream()
                .map(m -> documentFactory.createPrimarykey(m))
                .toArray(i -> new Term[i]);
        try {
            writers.get(IndexType.SONG).deleteDocuments(primarykeys);
        } catch (IOException e) {
            LOG.error("Failed to delete song doc.", e);
        }

        primarykeys = artistDao.getExpungeCandidates().stream()
                .map(m -> documentFactory.createPrimarykey(m))
                .toArray(i -> new Term[i]);
        try {
            writers.get(IndexType.ARTIST_ID3).deleteDocuments(primarykeys);
        } catch (IOException e) {
            LOG.error("Failed to delete artistId3 doc.", e);
        }

        primarykeys = albumDao.getExpungeCandidates().stream()
                .map(m -> documentFactory.createPrimarykey(m))
                .toArray(i -> new Term[i]);
        try {
            writers.get(IndexType.ALBUM_ID3).deleteDocuments(primarykeys);
        } catch (IOException e) {
            LOG.error("Failed to delete albumId3 doc.", e);
        }

    }

    /**
     * Close Writer of all indexes and update SearcherManager.
     * Called at the end of the Scan flow.
     */
    public void stopIndexing(MediaLibraryStatistics statistics) {
        EnumSet.allOf(IndexType.class).parallelStream().forEach(indexType -> stopIndexing(indexType, statistics));
    }

    /**
     * Close Writer of specified index and refresh SearcherManager.
     */
    private void stopIndexing(IndexType type, MediaLibraryStatistics statistics) {
        writers.computeIfPresent(type, (tw, w) -> {
            try (IndexWriter writer = w) {
                Map<String,String> userData = Util.objectToStringMap(statistics);
                writer.setLiveCommitData(userData.entrySet());
                boolean updated = (-1 != writer.commit());
                LOG.trace("Success to create or update search index : [{}]", tw);
                
                if (updated) {
                    searchers.computeIfPresent(tw, (ts, s) -> {
                        try {
                            s.maybeRefresh();
                            LOG.trace("SearcherManager has been refreshed : [{}]", ts);
                            return s;
                        } catch (IOException e) {
                            LOG.error("Failed to refresh SearcherManager : [{}]", ts, e);
                            return null; //remove from map
                        }
                    });
                }
            } catch (IOException e) {
                LOG.error("Failed to create search index for {}.", tw, e);
            }
            
            //remove from map
            return null;
        });
    }

    /**
     * Return the MediaLibraryStatistics saved on commit in the index. Ensures that each index reports the same data.
     * On invalid indices, returns null.
     */
    public @Nullable MediaLibraryStatistics getStatistics() {
        Set<MediaLibraryStatistics> stats = EnumSet.allOf(IndexType.class).parallelStream().map(t -> {
            IndexSearcher searcher = getSearcher(t);
            if (searcher == null) {
                LOG.trace("No index for type {}", t);
                return null;
            }
            IndexReader indexReader = searcher.getIndexReader();
            if (!(indexReader instanceof DirectoryReader)) {
                LOG.warn("Unexpected index type {} for {}", indexReader.getClass(), t);
                return null;
            }
            try {
                Map<String, String> userData = ((DirectoryReader) indexReader).getIndexCommit().getUserData();
                return Util.stringMapToValidObject(MediaLibraryStatistics.class, userData);
            } catch (IOException | IllegalArgumentException e) {
                LOG.debug("Exception encountered while fetching index commit data for {}", t, e);
                return null;
            }
        }).distinct().collect(Collectors.toSet());
        
        if (stats.size() > 1) {
            LOG.warn("Differing stats data for different indices: {}", stats.stream().map(x -> Util.objectToStringMap(x)).collect(Collectors.toSet()));
            return null;
        }
        
        return stats.stream().map(x -> Optional.ofNullable(x)).findAny().flatMap(x -> x).orElse(null);
    }

    /**
     * Return the IndexSearcher of the specified index.
     * At initial startup, it may return null
     * if the user performs any search before performing a scan.
     */
    public @Nullable IndexSearcher getSearcher(IndexType indexType) {
        return Optional.ofNullable(searchers.computeIfAbsent(indexType, k -> {
            File indexDirectory = getIndexDirectory.apply(k);
            try {
                if (indexDirectory.exists()) {
                    return new SearcherManager(FSDirectory.open(indexDirectory.toPath()), null);
                } else {
                    LOG.warn("{} does not exist. Please run a scan.", indexDirectory.getAbsolutePath());
                }
            } catch (IOException e) {
                LOG.error("Failed to initialize SearcherManager for {}", k, e);
            }
            
            return null;
        })).map(s -> {
            try {
                return s.acquire();
            } catch (IOException e) {
                LOG.warn("Failed to acquire IndexSearcher for {}.", indexType, e);
                return null;
            }
        }).orElse(null);
    }

    public void release(IndexType indexType, IndexSearcher indexSearcher) {
        searchers.compute(indexType, (k, v) -> {
            if (v == null) {
                // irregular case
                try {
                    indexSearcher.getIndexReader().close();
                } catch (Exception e) {
                    LOG.warn("Failed to release {} because it wasn't found. IndexSearcher has been closed.", k, e);
                }
                
                return null;
            }
            
            try {
                v.release(indexSearcher);
                return v;
            } catch (IOException e) {
                LOG.error("Failed to release IndexSearcher for {}.", k, e);
                return null;
            }
        });
    }

    /**
     * Check the version of the index and clean it up if necessary.
     * Legacy type indexes (files or directories starting with lucene) are deleted.
     * If there is no index directory, initialize the directory.
     * If the index directory exists and is not the current version,
     * initialize the directory.
     */
    public void deleteOldIndexFiles() {

        // Delete legacy files unconditionally
        Arrays.stream(SettingsService.getAirsonicHome()
                .listFiles((file, name) -> Pattern.compile("^lucene\\d+$").matcher(name).matches())).forEach(old -> {
                    if (FileUtil.exists(old)) {
                        LOG.info("Found legacy index file. Try to delete : {}", old.getAbsolutePath());
                        try {
                            if (old.isFile()) {
                                FileUtils.deleteQuietly(old);
                            } else {
                                FileUtils.deleteDirectory(old);
                            }
                        } catch (IOException e) {
                            // Log only if failed
                            LOG.warn("Failed to delete the legacy Index : {}", old.getAbsolutePath(), e);
                        }
                    }
                });

        // Delete if not old index version
        Arrays.stream(SettingsService.getAirsonicHome()
                .listFiles((file, name) -> Pattern.compile("^index\\d+$").matcher(name).matches()))
                .filter(dir -> !dir.getName().equals(rootIndexDirectory.getName()))
                .forEach(old -> {
                    if (FileUtil.exists(old)) {
                        LOG.info("Found old index file. Try to delete : {}", old.getAbsolutePath());
                        try {
                            if (old.isFile()) {
                                FileUtils.deleteQuietly(old);
                            } else {
                                FileUtils.deleteDirectory(old);
                            }
                        } catch (IOException e) {
                            // Log only if failed
                            LOG.warn("Failed to delete the old Index : {}", old.getAbsolutePath(), e);
                        }
                    }
                });

    }

    /**
     * Create a directory corresponding to the current index version.
     */
    public void initializeIndexDirectory() {
        // Check if Index is current version
        if (rootIndexDirectory.exists()) {
            // Index of current version already exists
            LOG.info("Index was found (index version {}). ", INDEX_VERSION);
        } else {
            if (rootIndexDirectory.mkdir()) {
                LOG.info("Index directory was created (index version {}). ", INDEX_VERSION);
            } else {
                LOG.warn("Failed to create index directory :  (index version {}). ", INDEX_VERSION);
            }
        }
    }

}
