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

import com.google.common.collect.Lists;
import org.airsonic.player.dao.AlbumDao;
import org.airsonic.player.dao.ArtistDao;
import org.airsonic.player.domain.*;
import org.airsonic.player.util.FileUtil;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.StopFilterFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilterFactory;
import org.apache.lucene.analysis.miscellaneous.LimitTokenCountAnalyzer;
import org.apache.lucene.analysis.pattern.PatternReplaceFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.SortField.Type;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.NumericUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.airsonic.player.service.SearchService.IndexType.*;

/**
 * Performs Lucene-based searching and indexing.
 *
 * @author Sindre Mehus
 * @version $Id$
 * @see MediaScannerService
 */
@Service
public class SearchService {

    private static final Logger LOG = LoggerFactory.getLogger(SearchService.class);

    private static final String FIELD_ID = "id";
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_ALBUM = "album";
    private static final String FIELD_ARTIST = "artist";
    private static final String FIELD_GENRE = "genre";
    private static final String FIELD_YEAR = "year";
    private static final String FIELD_MEDIA_TYPE = "mediaType";
    private static final String FIELD_FOLDER = "folder";
    private static final String FIELD_FOLDER_ID = "folderId";

    private static final String LUCENE_DIR = "lucene7";

    private static Analyzer analyzer;
    private static Analyzer queryAnalyzer;
    static {
        try {
            analyzer = CustomAnalyzer.builder()
                    .withTokenizer(StandardTokenizerFactory.class)
                    .addTokenFilter(LowerCaseFilterFactory.class)
                    .addTokenFilter(StopFilterFactory.class, "ignoreCase", "true")
                    .addTokenFilter(ASCIIFoldingFilterFactory.class, "preserveOriginal", "false")
                    .build();

            queryAnalyzer = CustomAnalyzer.builder()
                    .withTokenizer(StandardTokenizerFactory.class)
                    .addTokenFilter(LowerCaseFilterFactory.class)
                    .addTokenFilter(StopFilterFactory.class, "ignoreCase", "true")
                    .addTokenFilter(ASCIIFoldingFilterFactory.class, "preserveOriginal", "false")
                    .addTokenFilter(PatternReplaceFilterFactory.class, "pattern", "(.*)", "replacement", "$1*", "replace", "all")
                    .build();
        } catch (IOException e) {
            LOG.error("Failed to create custom analyzer", e);
        }
    }


    @Autowired
    private MediaFileService mediaFileService;
    @Autowired
    private ArtistDao artistDao;
    @Autowired
    private AlbumDao albumDao;

    private IndexWriter artistWriter;
    private IndexWriter artistId3Writer;
    private IndexWriter albumWriter;
    private IndexWriter albumId3Writer;
    private IndexWriter songWriter;

    public SearchService() {
    }

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

    public void index(MediaFile mediaFile) {
        try {
            if (mediaFile.isFile()) {
                songWriter.addDocument(SONG.createDocument(mediaFile));
            } else if (mediaFile.isAlbum()) {
                albumWriter.addDocument(ALBUM.createDocument(mediaFile));
            } else {
                artistWriter.addDocument(ARTIST.createDocument(mediaFile));
            }
        } catch (Exception x) {
            LOG.error("Failed to create search index for " + mediaFile, x);
        }
    }

    public void index(Artist artist, MusicFolder musicFolder) {
        try {
            artistId3Writer.addDocument(ARTIST_ID3.createDocument(artist, musicFolder));
        } catch (Exception x) {
            LOG.error("Failed to create search index for " + artist, x);
        }
    }

    public void index(Album album) {
        try {
            albumId3Writer.addDocument(ALBUM_ID3.createDocument(album));
        } catch (Exception x) {
            LOG.error("Failed to create search index for " + album, x);
        }
    }

    public void stopIndexing() {
        try {
            artistWriter.close();
            artistId3Writer.close();
            albumWriter.close();
            albumId3Writer.close();
            songWriter.close();
        } catch (Exception e) {
            LOG.error("Failed to close the search index.", e);
        }
    }

    public SearchResult search(SearchCriteria criteria, List<MusicFolder> musicFolders, IndexType indexType) {
        SearchResult result = new SearchResult();
        int offset = criteria.getOffset();
        int count = criteria.getCount();
        result.setOffset(offset);

        if (count <= 0) return result;

        IndexReader reader = null;
        try {
            reader = createIndexReader(indexType);
            IndexSearcher searcher = new IndexSearcher(reader);

            MultiFieldQueryParser queryParser = new MultiFieldQueryParser(
                    indexType.getFields(),
                    queryAnalyzer,
                    indexType.getBoosts());

            BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder()
                    .add(queryParser.parse(criteria.getQuery()), BooleanClause.Occur.MUST);

            List<SpanTermQuery> musicFolderQueries = new ArrayList<>();
            for (MusicFolder musicFolder : musicFolders) {
                if (indexType == ALBUM_ID3 || indexType == ARTIST_ID3) {
                    byte[] bytes = new byte[Integer.BYTES];
                    NumericUtils.intToSortableBytes(musicFolder.getId(), bytes, 0);
                    BytesRef ref = new BytesRef(bytes);
                    musicFolderQueries.add(new SpanTermQuery(new Term(FIELD_FOLDER_ID, ref)));
                } else {
                    musicFolderQueries.add(new SpanTermQuery(new Term(FIELD_FOLDER, musicFolder.getPath().getPath())));
                }
            }
            queryBuilder.add(new SpanOrQuery(musicFolderQueries.toArray(new SpanQuery[musicFolderQueries.size()])), BooleanClause.Occur.MUST);

            TopDocs topDocs = searcher.search(queryBuilder.build(), offset + count);
            result.setTotalHits((int)topDocs.totalHits);

            int start = Math.min(offset, (int)topDocs.totalHits);
            int end = Math.min(start + count, (int)topDocs.totalHits);
            for (int i = start; i < end; i++) {
                Document doc = searcher.doc(topDocs.scoreDocs[i].doc);
                switch (indexType) {
                    case SONG:
                    case ARTIST:
                    case ALBUM:
                        MediaFile mediaFile = mediaFileService.getMediaFile(Integer.valueOf(doc.get(FIELD_ID)));
                        addIfNotNull(mediaFile, result.getMediaFiles());
                        break;
                    case ARTIST_ID3:
                        Artist artist = artistDao.getArtist(Integer.valueOf(doc.get(FIELD_ID)));
                        addIfNotNull(artist, result.getArtists());
                        break;
                    case ALBUM_ID3:
                        Album album = albumDao.getAlbum(Integer.valueOf(doc.get(FIELD_ID)));
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

    /**
     * Returns a number of random songs.
     *
     * @param criteria Search criteria.
     * @return List of random songs.
     */
    public List<MediaFile> getRandomSongs(RandomSearchCriteria criteria) {
        List<MediaFile> result = new ArrayList<>();

        IndexReader reader = null;
        try {
            reader = createIndexReader(SONG);
            IndexSearcher searcher = new IndexSearcher(reader);

            BooleanQuery.Builder query = new BooleanQuery.Builder();
            query.add(new TermQuery(new Term(FIELD_MEDIA_TYPE, MediaFile.MediaType.MUSIC.name().toLowerCase())), BooleanClause.Occur.MUST);
            if (criteria.getGenre() != null) {
                String genre = normalizeGenre(criteria.getGenre());
                query.add(new TermQuery(new Term(FIELD_GENRE, genre)), BooleanClause.Occur.MUST);
            }
            if (criteria.getFromYear() != null || criteria.getToYear() != null) {
                Query rangeQuery = IntPoint.newRangeQuery(FIELD_YEAR, criteria.getFromYear(), criteria.getToYear());
                query.add(rangeQuery, BooleanClause.Occur.MUST);
            }

            List<SpanTermQuery> musicFolderQueries = new ArrayList<>();
            for (MusicFolder musicFolder : criteria.getMusicFolders()) {
                musicFolderQueries.add(new SpanTermQuery(new Term(FIELD_FOLDER, musicFolder.getPath().getPath())));
            }
            query.add(new SpanOrQuery(musicFolderQueries.toArray(new SpanQuery[musicFolderQueries.size()])), BooleanClause.Occur.MUST);

            TopDocs topDocs = searcher.search(query.build(), Integer.MAX_VALUE);
            List<ScoreDoc> scoreDocs = Lists.newArrayList(topDocs.scoreDocs);
            Random random = new Random(System.currentTimeMillis());

            while (!scoreDocs.isEmpty() && result.size() < criteria.getCount()) {
                int index = random.nextInt(scoreDocs.size());
                Document doc = searcher.doc(scoreDocs.remove(index).doc);
                int id = Integer.valueOf(doc.get(FIELD_ID));
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

    private static String normalizeGenre(String genre) {
        return genre.toLowerCase().replace(" ", "").replace("-", "");
    }

    /**
     * Returns a number of random albums.
     *
     * @param count        Number of albums to return.
     * @param musicFolders Only return albums from these folders.
     * @return List of random albums.
     */
    public List<MediaFile> getRandomAlbums(int count, List<MusicFolder> musicFolders) {
        List<MediaFile> result = new ArrayList<>();

        IndexReader reader = null;
        try {
            reader = createIndexReader(ALBUM);
            IndexSearcher searcher = new IndexSearcher(reader);

            List<SpanTermQuery> musicFolderQueries = new ArrayList<>();
            for (MusicFolder musicFolder : musicFolders) {
                musicFolderQueries.add(new SpanTermQuery(new Term(FIELD_FOLDER, musicFolder.getPath().getPath())));
            }
            Query query = new SpanOrQuery(musicFolderQueries.toArray(new SpanQuery[musicFolderQueries.size()]));

            TopDocs topDocs = searcher.search(query, Integer.MAX_VALUE);
            List<ScoreDoc> scoreDocs = Lists.newArrayList(topDocs.scoreDocs);
            Random random = new Random(System.currentTimeMillis());

            while (!scoreDocs.isEmpty() && result.size() < count) {
                int index = random.nextInt(scoreDocs.size());
                Document doc = searcher.doc(scoreDocs.remove(index).doc);
                int id = Integer.valueOf(doc.get(FIELD_ID));
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

    /**
     * Returns a number of random albums, using ID3 tag.
     *
     * @param count        Number of albums to return.
     * @param musicFolders Only return albums from these folders.
     * @return List of random albums.
     */
    public List<Album> getRandomAlbumsId3(int count, List<MusicFolder> musicFolders) {
        List<Album> result = new ArrayList<>();

        IndexReader reader = null;
        try {
            reader = createIndexReader(ALBUM_ID3);
            IndexSearcher searcher = new IndexSearcher(reader);

            List<SpanTermQuery> musicFolderQueries = new ArrayList<SpanTermQuery>();
            for (MusicFolder musicFolder : musicFolders) {
                byte[] bytes = new byte[Integer.BYTES];
                NumericUtils.intToSortableBytes(musicFolder.getId(), bytes, 0);
                BytesRef ref = new BytesRef(bytes);
                musicFolderQueries.add(new SpanTermQuery(new Term(FIELD_FOLDER_ID, ref)));
            }
            Query query = new SpanOrQuery(musicFolderQueries.toArray(new SpanQuery[musicFolderQueries.size()]));
            TopDocs topDocs = searcher.search(query, Integer.MAX_VALUE);
            List<ScoreDoc> scoreDocs = Lists.newArrayList(topDocs.scoreDocs);
            Random random = new Random(System.currentTimeMillis());

            while (!scoreDocs.isEmpty() && result.size() < count) {
                int index = random.nextInt(scoreDocs.size());
                Document doc = searcher.doc(scoreDocs.remove(index).doc);
                int id = Integer.valueOf(doc.get(FIELD_ID));
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

    public <T> ParamSearchResult<T> searchByName(String name, int offset, int count, List<MusicFolder> folderList, Class<T> clazz) {
        IndexType indexType = null;
        String field = null;
        if (clazz.isAssignableFrom(Album.class)) {
            indexType = IndexType.ALBUM_ID3;
            field = FIELD_ALBUM;
        } else if (clazz.isAssignableFrom(Artist.class)) {
            indexType = IndexType.ARTIST_ID3;
            field = FIELD_ARTIST;
        } else if (clazz.isAssignableFrom(MediaFile.class)) {
            indexType = IndexType.SONG;
            field = FIELD_TITLE;
        }
        ParamSearchResult<T> result = new ParamSearchResult<>();
        // we only support album, artist, and song for now
        if (indexType == null || field == null) {
            return result;
        }

        result.setOffset(offset);

        IndexReader reader = null;

        try {
            reader = createIndexReader(indexType);
            IndexSearcher searcher = new IndexSearcher(reader);
            QueryParser queryParser = new QueryParser(field, queryAnalyzer);

            Query q = queryParser.parse(name );

            Sort sort = new Sort(new SortField(field, Type.STRING));
            TopDocs topDocs = searcher.search(q, offset + count, sort);
            result.setTotalHits((int) topDocs.totalHits);

            int start = Math.min(offset, (int) topDocs.totalHits);
            int end = Math.min(start + count, (int) topDocs.totalHits);
            for (int i = start; i < end; i++) {
                Document doc = searcher.doc(topDocs.scoreDocs[i].doc);
                switch (indexType) {
                case SONG:
                    MediaFile mediaFile = mediaFileService.getMediaFile(Integer.valueOf(doc.get(FIELD_ID)));
                    addIfNotNull(clazz.cast(mediaFile), result.getItems());
                    break;
                case ARTIST_ID3:
                    Artist artist = artistDao.getArtist(Integer.valueOf(doc.get(FIELD_ID)));
                    addIfNotNull(clazz.cast(artist), result.getItems());
                    break;
                case ALBUM_ID3:
                    Album album = albumDao.getAlbum(Integer.valueOf(doc.get(FIELD_ID)));
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
        IndexWriterConfig iwc = new IndexWriterConfig(new LimitTokenCountAnalyzer(analyzer, 10));
        return new IndexWriter(FSDirectory.open(dir.toPath()), iwc);
    }

    private IndexReader createIndexReader(IndexType indexType) throws IOException {
        File dir = getIndexDirectory(indexType);
        return DirectoryReader.open(FSDirectory.open(dir.toPath()));
    }

    private File getIndexRootDirectory() {
        return new File(SettingsService.getAirsonicHome(), LUCENE_DIR);
    }

    private File getIndexDirectory(IndexType indexType) {
        return new File(getIndexRootDirectory(), indexType.toString().toLowerCase());
    }

    public void setMediaFileService(MediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
    }

    public void setArtistDao(ArtistDao artistDao) {
        this.artistDao = artistDao;
    }

    public void setAlbumDao(AlbumDao albumDao) {
        this.albumDao = albumDao;
    }

    public static enum IndexType {

        SONG(new String[]{FIELD_TITLE, FIELD_ARTIST}, FIELD_TITLE) {
            @Override
            public Document createDocument(MediaFile mediaFile) {
                Document doc = new Document();
                doc.add(new StoredField(FIELD_ID, mediaFile.getId()));
                doc.add(new IntPoint(FIELD_ID, mediaFile.getId()));

                FieldType indexableType = new FieldType();
                indexableType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);

                FieldType omitNormsType = new FieldType(indexableType);
                omitNormsType.setOmitNorms(true);

                doc.add(new Field(FIELD_MEDIA_TYPE, mediaFile.getMediaType().name(), omitNormsType));

                if (mediaFile.getTitle() != null) {
                    doc.add(new StoredField(FIELD_TITLE, mediaFile.getTitle()));
                }
                if (mediaFile.getArtist() != null) {
                    doc.add(new StoredField(FIELD_ARTIST, mediaFile.getArtist()));
                }
                if (mediaFile.getGenre() != null) {
                    doc.add(new Field(FIELD_GENRE, normalizeGenre(mediaFile.getGenre()), indexableType));
                }
                if (mediaFile.getYear() != null) {
                    doc.add(new IntPoint(FIELD_YEAR, mediaFile.getYear()));
                }
                if (mediaFile.getFolder() != null) {
                    doc.add(new Field(FIELD_FOLDER, mediaFile.getFolder(), omitNormsType));
                }

                return doc;
            }
        },

        ALBUM(new String[]{FIELD_ALBUM, FIELD_ARTIST, FIELD_FOLDER}, FIELD_ALBUM) {
            @Override
            public Document createDocument(MediaFile mediaFile) {
                Document doc = new Document();
                doc.add(new StoredField(FIELD_ID, mediaFile.getId()));
                doc.add(new IntPoint(FIELD_ID, mediaFile.getId()));

                FieldType omitNormsType = new FieldType();
                omitNormsType.setOmitNorms(true);
                omitNormsType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);

                if (mediaFile.getArtist() != null) {
                    doc.add(new StoredField(FIELD_ARTIST, mediaFile.getArtist()));
                }
                if (mediaFile.getAlbumName() != null) {
                    doc.add(new StoredField(FIELD_ALBUM, mediaFile.getAlbumName()));
                }
                if (mediaFile.getFolder() != null) {
                    doc.add(new Field(FIELD_FOLDER, mediaFile.getFolder(), omitNormsType));
                }

                return doc;
            }
        },

        ALBUM_ID3(new String[]{FIELD_ALBUM, FIELD_ARTIST, FIELD_FOLDER_ID}, FIELD_ALBUM) {
            @Override
            public Document createDocument(Album album) {
                Document doc = new Document();
                doc.add(new StoredField(FIELD_ID, album.getId()));
                doc.add(new IntPoint(FIELD_ID, album.getId()));

                if (album.getArtist() != null) {
                    doc.add(new StoredField(FIELD_ARTIST, album.getArtist()));
                }
                if (album.getName() != null) {
                    doc.add(new StoredField(FIELD_ALBUM, album.getName()));
                }
                if (album.getFolderId() != null) {
                    doc.add(new IntPoint(FIELD_FOLDER_ID, album.getFolderId()));
                }

                return doc;
            }
        },

        ARTIST(new String[]{FIELD_ARTIST, FIELD_FOLDER}, null) {
            @Override
            public Document createDocument(MediaFile mediaFile) {
                Document doc = new Document();
                doc.add(new StoredField(FIELD_ID, mediaFile.getId()));
                doc.add(new IntPoint(FIELD_ID, mediaFile.getId()));

                FieldType omitNormsType = new FieldType();
                omitNormsType.setOmitNorms(true);
                omitNormsType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);

                if (mediaFile.getArtist() != null) {
                    doc.add(new StoredField(FIELD_ARTIST, mediaFile.getArtist()));
                }
                if (mediaFile.getFolder() != null) {
                    doc.add(new Field(FIELD_FOLDER, mediaFile.getFolder(), omitNormsType));
                }

                return doc;
            }
        },

        ARTIST_ID3(new String[]{FIELD_ARTIST}, null) {
            @Override
            public Document createDocument(Artist artist, MusicFolder musicFolder) {
                Document doc = new Document();
                doc.add(new StoredField(FIELD_ID, artist.getId()));
                doc.add(new IntPoint(FIELD_ID, artist.getId()));
                doc.add(new StoredField(FIELD_ARTIST, artist.getName()));
                doc.add(new IntPoint(FIELD_FOLDER_ID, musicFolder.getId()));

                return doc;
            }
        };

        private final String[] fields;
        private final Map<String, Float> boosts;

        private IndexType(String[] fields, String boostedField) {
            this.fields = fields;
            boosts = new HashMap<>();
            if (boostedField != null) {
                boosts.put(boostedField, 2.0F);
            }
        }

        public String[] getFields() {
            return fields;
        }

        protected Document createDocument(MediaFile mediaFile) {
            throw new UnsupportedOperationException();
        }

        protected Document createDocument(Artist artist, MusicFolder musicFolder) {
            throw new UnsupportedOperationException();
        }

        protected Document createDocument(Album album) {
            throw new UnsupportedOperationException();
        }

        public Map<String, Float> getBoosts() {
            return boosts;
        }
    }
}


