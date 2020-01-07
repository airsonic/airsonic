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
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static org.springframework.util.ObjectUtils.isEmpty;

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
    private static final int INDEX_VERSION = 17;

    /**
     * Literal name of index top directory.
     */
    private static final String INDEX_ROOT_DIR_NAME = "index";

    private static final String MEDIA_STATISTICS_KEY = "stats";

    /**
     * File supplier for index directory.
     */
    private Supplier<File> rootIndexDirectory = () ->
        new File(SettingsService.getAirsonicHome(), INDEX_ROOT_DIR_NAME.concat(Integer.toString(INDEX_VERSION)));

    /**
     * Returns the directory of the specified index
     */
    private Function<IndexType, File> getIndexDirectory = (indexType) ->
        new File(rootIndexDirectory.get(), indexType.toString().toLowerCase());

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

    private EnumMap<IndexType, SearcherManager> searchers = new EnumMap<>(IndexType.class);

    private EnumMap<IndexType, IndexWriter> writers = new EnumMap<>(IndexType.class);

    public void index(Album album) {
        Term primarykey = documentFactory.createPrimarykey(album);
        Document document = documentFactory.createAlbumId3Document(album);
        try {
            writers.get(IndexType.ALBUM_ID3).updateDocument(primarykey, document);
        } catch (Exception x) {
            LOG.error("Failed to create search index for " + album, x);
        }
    }

    public void index(Artist artist, MusicFolder musicFolder) {
        Term primarykey = documentFactory.createPrimarykey(artist);
        Document document = documentFactory.createArtistId3Document(artist, musicFolder);
        try {
            writers.get(IndexType.ARTIST_ID3).updateDocument(primarykey, document);
        } catch (Exception x) {
            LOG.error("Failed to create search index for " + artist, x);
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
            LOG.error("Failed to create search index for " + mediaFile, x);
        }
    }

    public final void startIndexing() {
        try {
            for (IndexType IndexType : IndexType.values()) {
                writers.put(IndexType, createIndexWriter(IndexType));
            }
        } catch (IOException e) {
            LOG.error("Failed to create search index.", e);
        }
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
        Arrays.asList(IndexType.values()).forEach(indexType -> stopIndexing(indexType, statistics));
    }

    /**
     * Close Writer of specified index and refresh SearcherManager.
     */
    private void stopIndexing(IndexType type, MediaLibraryStatistics statistics) {

        boolean isUpdate = false;
        // close
        IndexWriter indexWriter = writers.get(type);
        try {
            Map<String,String> userData = Util.objectToStringMap(statistics);
            indexWriter.setLiveCommitData(userData.entrySet());
            isUpdate = -1 != indexWriter.commit();
            indexWriter.close();
            writers.remove(type);
            LOG.trace("Success to create or update search index : [" + type + "]");
        } catch (IOException e) {
            LOG.error("Failed to create search index.", e);
        } finally {
            FileUtil.closeQuietly(indexWriter);
        }

        // refresh reader as index may have been written
        if (isUpdate && searchers.containsKey(type)) {
            try {
                searchers.get(type).maybeRefresh();
                LOG.trace("SearcherManager has been refreshed : [" + type + "]");
            } catch (IOException e) {
                LOG.error("Failed to refresh SearcherManager : [" + type + "]", e);
                searchers.remove(type);
            }
        }

    }

    /**
     * Return the MediaLibraryStatistics saved on commit in the index. Ensures that each index reports the same data.
     * On invalid indices, returns null.
     */
    public @Nullable MediaLibraryStatistics getStatistics() {
        MediaLibraryStatistics stats = null;
        for (IndexType indexType : IndexType.values()) {
            IndexSearcher searcher = getSearcher(indexType);
            if (searcher == null) {
                LOG.trace("No index for type " + indexType);
                return null;
            }
            IndexReader indexReader = searcher.getIndexReader();
            if (!(indexReader instanceof DirectoryReader)) {
                LOG.warn("Unexpected index type " + indexReader.getClass());
                return null;
            }
            try {
                Map<String, String> userData = ((DirectoryReader) indexReader).getIndexCommit().getUserData();
                MediaLibraryStatistics currentStats = Util.stringMapToValidObject(MediaLibraryStatistics.class,
                        userData);
                if (stats == null) {
                    stats = currentStats;
                } else {
                    if (!Objects.equals(stats, currentStats)) {
                        LOG.warn("Index type " + indexType + " had differing stats data");
                        return null;
                    }
                }
            } catch (IOException | IllegalArgumentException e) {
                LOG.debug("Exception encountered while fetching index commit data", e);
                return null;
            }
        }
        return stats;
    }

    /**
     * Return the IndexSearcher of the specified index.
     * At initial startup, it may return null
     * if the user performs any search before performing a scan.
     */
    public @Nullable IndexSearcher getSearcher(IndexType indexType) {
        if (!searchers.containsKey(indexType)) {
            File indexDirectory = getIndexDirectory.apply(indexType);
            try {
                if (indexDirectory.exists()) {
                    SearcherManager manager = new SearcherManager(FSDirectory.open(indexDirectory.toPath()), null);
                    searchers.put(indexType, manager);
                } else {
                    LOG.warn("{} does not exist. Please run a scan.", indexDirectory.getAbsolutePath());
                }
            } catch (IndexNotFoundException e) {
                LOG.debug("Index {} does not exist in {}, likely not yet created.", indexType.toString(), indexDirectory.getAbsolutePath());
            } catch (IOException e) {
                LOG.warn("Failed to initialize SearcherManager.", e);
            }
        }
        try {
            SearcherManager manager = searchers.get(indexType);
            if (!isEmpty(manager)) {
                return searchers.get(indexType).acquire();
            }
        } catch (Exception e) {
            LOG.warn("Failed to acquire IndexSearcher.", e);
        }
        return null;
    }

    public void release(IndexType indexType, IndexSearcher indexSearcher) {
        if (searchers.containsKey(indexType)) {
            try {
                searchers.get(indexType).release(indexSearcher);
            } catch (IOException e) {
                LOG.error("Failed to release IndexSearcher.", e);
                searchers.remove(indexType);
            }
        } else {
            // irregular case
            try {
                indexSearcher.getIndexReader().close();
            } catch (Exception e) {
                LOG.warn("Failed to release. IndexSearcher has been closed.", e);
            }
        }
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
                            LOG.warn("Failed to delete the legacy Index : ".concat(old.getAbsolutePath()), e);
                        }
                    }
                });

        // Delete if not old index version
        Arrays.stream(SettingsService.getAirsonicHome()
                .listFiles((file, name) -> Pattern.compile("^index\\d+$").matcher(name).matches()))
                .filter(dir -> !dir.getName().equals(rootIndexDirectory.get().getName()))
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
                            LOG.warn("Failed to delete the old Index : ".concat(old.getAbsolutePath()), e);
                        }
                    }
                });

    }

    /**
     * Create a directory corresponding to the current index version.
     */
    public void initializeIndexDirectory() {
        // Check if Index is current version
        if (rootIndexDirectory.get().exists()) {
            // Index of current version already exists
            LOG.info("Index was found (index version {}). ", INDEX_VERSION);
        } else {
            if (rootIndexDirectory.get().mkdir()) {
                LOG.info("Index directory was created (index version {}). ", INDEX_VERSION);
            } else {
                LOG.warn("Failed to create index directory :  (index version {}). ", INDEX_VERSION);
            }
        }
    }

}
