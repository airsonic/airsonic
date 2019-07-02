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

import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.Artist;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.util.FileUtil;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

import static org.airsonic.player.service.search.IndexType.ALBUM;
import static org.airsonic.player.service.search.IndexType.ALBUM_ID3;
import static org.airsonic.player.service.search.IndexType.ARTIST;
import static org.airsonic.player.service.search.IndexType.ARTIST_ID3;
import static org.airsonic.player.service.search.IndexType.SONG;

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

    @Autowired
    private AnalyzerFactory analyzerFactory;

    @Autowired
    private DocumentFactory documentFactory;

    private IndexWriter artistWriter;
    private IndexWriter artistId3Writer;
    private IndexWriter albumWriter;
    private IndexWriter albumId3Writer;
    private IndexWriter songWriter;

    public void index(Album album) {
        try {
            albumId3Writer.addDocument(documentFactory.createAlbumId3Document(album));
        } catch (Exception x) {
            LOG.error("Failed to create search index for " + album, x);
        }
    }

    public void index(Artist artist, MusicFolder musicFolder) {
        try {
            artistId3Writer
                    .addDocument(documentFactory.createArtistId3Document(artist, musicFolder));
        } catch (Exception x) {
            LOG.error("Failed to create search index for " + artist, x);
        }
    }

    public void index(MediaFile mediaFile) {
        try {
            if (mediaFile.isFile()) {
                songWriter.addDocument(documentFactory.createSongDocument(mediaFile));
            } else if (mediaFile.isAlbum()) {
                albumWriter.addDocument(documentFactory.createAlbumDocument(mediaFile));
            } else {
                artistWriter.addDocument(documentFactory.createArtistDocument(mediaFile));
            }
        } catch (Exception x) {
            LOG.error("Failed to create search index for " + mediaFile, x);
        }
    }

    private static final String LUCENE_DIR = "lucene2";

    public IndexReader createIndexReader(IndexType indexType) throws IOException {
        File dir = getIndexDirectory(indexType);
        return IndexReader.open(FSDirectory.open(dir), true);
    }

    /**
     * It is static as an intermediate response of the transition period.
     * (It is called before injection because it is called by SearchService constructor)
     * 
     * @return
     */
    private static File getIndexRootDirectory() {
        return new File(SettingsService.getAirsonicHome(), LUCENE_DIR);
    }

    /**
     * Make it public as an interim response of the transition period.
     * (It is called before the injection because it is called in the SearchService constructor.)
     * 
     * @param indexType
     * @return
     * @deprecated It should not be called from outside.
     */
    @Deprecated
    public static File getIndexDirectory(IndexType indexType) {
        return new File(getIndexRootDirectory(), indexType.toString().toLowerCase());
    }

    private IndexWriter createIndexWriter(IndexType indexType) throws IOException {
        File dir = getIndexDirectory(indexType);
        return new IndexWriter(FSDirectory.open(dir), analyzerFactory.getAnalyzer(), true,
                new IndexWriter.MaxFieldLength(10));
    }

    public final void startIndexing() {
        try {
            artistWriter = createIndexWriter(ARTIST);
            artistId3Writer = createIndexWriter(ARTIST_ID3);
            albumWriter = createIndexWriter(ALBUM);
            albumId3Writer = createIndexWriter(ALBUM_ID3);
            songWriter = createIndexWriter(SONG);
        } catch (Exception x) {
            LOG.error("Failed to create search index.", x);
        }
    }

    public void stopIndexing() {
        try {
            artistWriter.optimize();
            artistId3Writer.optimize();
            albumWriter.optimize();
            albumId3Writer.optimize();
            songWriter.optimize();
        } catch (Exception x) {
            LOG.error("Failed to create search index.", x);
        } finally {
            FileUtil.closeQuietly(artistId3Writer);
            FileUtil.closeQuietly(artistWriter);
            FileUtil.closeQuietly(albumWriter);
            FileUtil.closeQuietly(albumId3Writer);
            FileUtil.closeQuietly(songWriter);
        }
    }

}
