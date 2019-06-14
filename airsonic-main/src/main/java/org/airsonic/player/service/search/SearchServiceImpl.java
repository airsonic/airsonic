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

import com.google.common.collect.Lists;
import org.airsonic.player.dao.AlbumDao;
import org.airsonic.player.dao.ArtistDao;
import org.airsonic.player.domain.*;
import org.airsonic.player.service.MediaFileService;
import org.airsonic.player.service.SearchService;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.util.FileUtil;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.airsonic.player.service.search.IndexType.*;

/**
 * Performs Lucene-based searching and indexing.
 *
 * @author Sindre Mehus
 * @version $Id$
 * @see MediaScannerService
 */
@Service
public class SearchServiceImpl implements SearchService {

  private static final Logger LOG = LoggerFactory.getLogger(SearchServiceImpl.class);

  private static final String  LUCENE_DIR     = "lucene2";

  @Autowired
  private MediaFileService mediaFileService;
  @Autowired
  private ArtistDao        artistDao;
  @Autowired
  private AlbumDao         albumDao;
  @Autowired
  private DocumentFactory documentFactory;
  @Autowired
  private AnalyzerFactory analyzerFactory;
  @Autowired
  private QueryFactory queryFactory;

  private IndexWriter artistWriter;
  private IndexWriter artistId3Writer;
  private IndexWriter albumWriter;
  private IndexWriter albumId3Writer;
  private IndexWriter songWriter;

  public SearchServiceImpl() {
    removeLocks();
  }

  @Override
  public void startIndexing() {
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

  @Override
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

  @Override
  public void index(Artist artist, MusicFolder musicFolder) {
    try {
      artistId3Writer.addDocument(documentFactory.createDocument(artist, musicFolder));
    } catch (Exception x) {
      LOG.error("Failed to create search index for " + artist, x);
    }
  }

  @Override
  public void index(Album album) {
    try {
      albumId3Writer.addDocument(documentFactory.createDocument(album));
    } catch (Exception x) {
      LOG.error("Failed to create search index for " + album, x);
    }
  }

  @Override
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

  @Override
  public SearchResult search(SearchCriteria criteria, List<MusicFolder> musicFolders,
      IndexType indexType) {
    SearchResult result = new SearchResult();
    int offset = criteria.getOffset();
    int count = criteria.getCount();
    result.setOffset(offset);

    if (count <= 0)
      return result;

    IndexReader reader = null;
    try {
      reader = createIndexReader(indexType);
      Searcher searcher = new IndexSearcher(reader);
      Query query = queryFactory.search(criteria, musicFolders, indexType);
      TopDocs topDocs = searcher.search(query, null, offset + count);
      result.setTotalHits(topDocs.totalHits);

      int start = Math.min(offset, topDocs.totalHits);
      int end = Math.min(start + count, topDocs.totalHits);
      for (int i = start; i < end; i++) {
        Document doc = searcher.doc(topDocs.scoreDocs[i].doc);
        switch (indexType) {
          case SONG:
          case ARTIST:
          case ALBUM:
            MediaFile mediaFile = mediaFileService.getMediaFile(Integer.valueOf(doc.get(FieldNames.ID)));
            addIfNotNull(mediaFile, result.getMediaFiles());
            break;
          case ARTIST_ID3:
            Artist artist = artistDao.getArtist(Integer.valueOf(doc.get(FieldNames.ID)));
            addIfNotNull(artist, result.getArtists());
            break;
          case ALBUM_ID3:
            Album album = albumDao.getAlbum(Integer.valueOf(doc.get(FieldNames.ID)));
            addIfNotNull(album, result.getAlbums());
            break;
          default:
            break;
        }
      }

    } catch (Throwable x) {
      LOG.error("Failed to execute Lucene search.", x);
    } finally {
      FileUtil.closeQuietly(reader);
    }
    return result;
  }

  @Override
  public List<MediaFile> getRandomSongs(RandomSearchCriteria criteria) {
    List<MediaFile> result = new ArrayList<MediaFile>();

    IndexReader reader = null;
    try {
      reader = createIndexReader(SONG);
      Searcher searcher = new IndexSearcher(reader);
      Query query = queryFactory.getRandomSongs(criteria);
      TopDocs topDocs = searcher.search(query, null, Integer.MAX_VALUE);
      List<ScoreDoc> scoreDocs = Lists.newArrayList(topDocs.scoreDocs);
      Random random = new Random(System.currentTimeMillis());

      while (!scoreDocs.isEmpty() && result.size() < criteria.getCount()) {
        int index = random.nextInt(scoreDocs.size());
        Document doc = searcher.doc(scoreDocs.remove(index).doc);
        int id = Integer.valueOf(doc.get(FieldNames.ID));
        try {
          addIfNotNull(mediaFileService.getMediaFile(id), result);
        } catch (Exception x) {
          LOG.warn("Failed to get media file " + id);
        }
      }

    } catch (Throwable x) {
      LOG.error("Failed to search or random songs.", x);
    } finally {
      FileUtil.closeQuietly(reader);
    }
    return result;
  }

  @Override
  public List<MediaFile> getRandomAlbums(int count, List<MusicFolder> musicFolders) {
    List<MediaFile> result = new ArrayList<MediaFile>();

    IndexReader reader = null;
    try {
      reader = createIndexReader(ALBUM);
      Searcher searcher = new IndexSearcher(reader);
      Query query = queryFactory.getRandomAlbums(musicFolders);
      TopDocs topDocs = searcher.search(query, null, Integer.MAX_VALUE);
      List<ScoreDoc> scoreDocs = Lists.newArrayList(topDocs.scoreDocs);
      Random random = new Random(System.currentTimeMillis());

      while (!scoreDocs.isEmpty() && result.size() < count) {
        int index = random.nextInt(scoreDocs.size());
        Document doc = searcher.doc(scoreDocs.remove(index).doc);
        int id = Integer.valueOf(doc.get(FieldNames.ID));
        try {
          addIfNotNull(mediaFileService.getMediaFile(id), result);
        } catch (Exception x) {
          LOG.warn("Failed to get media file " + id, x);
        }
      }

    } catch (Throwable x) {
      LOG.error("Failed to search for random albums.", x);
    } finally {
      FileUtil.closeQuietly(reader);
    }
    return result;
  }

  @Override
  public List<Album> getRandomAlbumsId3(int count, List<MusicFolder> musicFolders) {
    List<Album> result = new ArrayList<Album>();

    IndexReader reader = null;
    try {
      reader = createIndexReader(ALBUM_ID3);
      Searcher searcher = new IndexSearcher(reader);
      Query query = queryFactory.getRandomAlbumsId3(musicFolders);
      TopDocs topDocs = searcher.search(query, null, Integer.MAX_VALUE);
      List<ScoreDoc> scoreDocs = Lists.newArrayList(topDocs.scoreDocs);
      Random random = new Random(System.currentTimeMillis());

      while (!scoreDocs.isEmpty() && result.size() < count) {
        int index = random.nextInt(scoreDocs.size());
        Document doc = searcher.doc(scoreDocs.remove(index).doc);
        int id = Integer.valueOf(doc.get(FieldNames.ID));
        try {
          addIfNotNull(albumDao.getAlbum(id), result);
        } catch (Exception x) {
          LOG.warn("Failed to get album file " + id, x);
        }
      }

    } catch (Throwable x) {
      LOG.error("Failed to search for random albums.", x);
    } finally {
      FileUtil.closeQuietly(reader);
    }
    return result;
  }

  @Override
  public <T> ParamSearchResult<T> searchByName(String name, int offset, int count,
      List<MusicFolder> folderList, Class<T> clazz) {
    IndexType indexType = null;
    String field = null;
    if (clazz.isAssignableFrom(Album.class)) {
      indexType = IndexType.ALBUM_ID3;
      field = FieldNames.ALBUM;
    } else if (clazz.isAssignableFrom(Artist.class)) {
      indexType = IndexType.ARTIST_ID3;
      field = FieldNames.ARTIST;
    } else if (clazz.isAssignableFrom(MediaFile.class)) {
      indexType = IndexType.SONG;
      field = FieldNames.TITLE;
    }
    ParamSearchResult<T> result = new ParamSearchResult<T>();
    // we only support album, artist, and song for now
    if (indexType == null || field == null) {
      return result;
    }

    result.setOffset(offset);

    IndexReader reader = null;

    try {
      reader = createIndexReader(indexType);
      Searcher searcher = new IndexSearcher(reader);
      Query query = queryFactory.searchByName(field, name);
      Sort sort = new Sort(new SortField(field, SortField.STRING));
      TopDocs topDocs = searcher.search(query, null, offset + count, sort);
      result.setTotalHits(topDocs.totalHits);
      int start = Math.min(offset, topDocs.totalHits);
      int end = Math.min(start + count, topDocs.totalHits);
      for (int i = start; i < end; i++) {
        Document doc = searcher.doc(topDocs.scoreDocs[i].doc);
        switch (indexType) {
          case SONG:
            MediaFile mediaFile = mediaFileService.getMediaFile(Integer.valueOf(doc.get(FieldNames.ID)));
            addIfNotNull(clazz.cast(mediaFile), result.getItems());
            break;
          case ARTIST_ID3:
            Artist artist = artistDao.getArtist(Integer.valueOf(doc.get(FieldNames.ID)));
            addIfNotNull(clazz.cast(artist), result.getItems());
            break;
          case ALBUM_ID3:
            Album album = albumDao.getAlbum(Integer.valueOf(doc.get(FieldNames.ID)));
            addIfNotNull(clazz.cast(album), result.getItems());
            break;
          default:
            break;
        }
      }
    } catch (Throwable x) {
      LOG.error("Failed to execute Lucene search.", x);
    } finally {
      FileUtil.closeQuietly(reader);
    }
    return result;
  }

  private <T> void addIfNotNull(T value, List<T> list) {
    if (value != null) {
      list.add(value);
    }
  }

  private IndexWriter createIndexWriter(IndexType indexType) throws IOException {
    File dir = getIndexDirectory(indexType);
    return new IndexWriter(FSDirectory.open(dir), analyzerFactory.getAnalyzer(), true,
        new IndexWriter.MaxFieldLength(10));
  }

  private IndexReader createIndexReader(IndexType indexType) throws IOException {
    File dir = getIndexDirectory(indexType);
    return IndexReader.open(FSDirectory.open(dir), true);
  }

  private File getIndexRootDirectory() {
    return new File(SettingsService.getAirsonicHome(), LUCENE_DIR);
  }

  private File getIndexDirectory(IndexType indexType) {
    return new File(getIndexRootDirectory(), indexType.toString().toLowerCase());
  }

  /**
   * Locks are managed automatically by the framework.
   * @deprecated It becomes unnecessary at the time of version upgrade.
   */
  @Deprecated
  private void removeLocks() {
    for (IndexType indexType : IndexType.values()) {
      Directory dir = null;
      try {
        dir = FSDirectory.open(getIndexDirectory(indexType));
        if (IndexWriter.isLocked(dir)) {
          IndexWriter.unlock(dir);
          LOG.info("Removed Lucene lock file in " + dir);
        }
      } catch (Exception x) {
        LOG.warn("Failed to remove Lucene lock file in " + dir, x);
      } finally {
        FileUtil.closeQuietly(dir);
      }
    }
  }


}
