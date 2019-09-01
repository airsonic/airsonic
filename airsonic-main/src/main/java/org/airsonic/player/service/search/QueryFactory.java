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

import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.RandomSearchCriteria;
import org.airsonic.player.domain.SearchCriteria;
import org.apache.lucene.analysis.ASCIIFoldingFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.util.NumericUtils;
import org.apache.lucene.util.Version;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.airsonic.player.service.search.IndexType.ALBUM_ID3;
import static org.airsonic.player.service.search.IndexType.ARTIST_ID3;

/**
 * Factory class of Lucene Query.
 * This class is an extract of the functionality that was once part of SearchService.
 * It is for maintainability and verification.
 * Each corresponds to the SearchService method.
 * The API syntax for query generation depends on the lucene version.
 * verification with query grammar is possible.
 * On the other hand, the generated queries are relatively small by version.
 * Therefore, test cases of this class are useful for large version upgrades.
 **/
@Component
public class QueryFactory {

    @Autowired
    private AnalyzerFactory analyzerFactory;

    private String analyzeQuery(String query) throws IOException {
        StringBuilder result = new StringBuilder();
        /*
         * Version.LUCENE_30
         * It is a transient description and will be deleted when upgrading the version.
         * SearchService variables are not used because the reference direction conflicts.
         */
        @SuppressWarnings("resource")
        ASCIIFoldingFilter filter = new ASCIIFoldingFilter(
                new StandardTokenizer(Version.LUCENE_30, new StringReader(query)));
        TermAttribute termAttribute = filter.getAttribute(TermAttribute.class);
        while (filter.incrementToken()) {
            result.append(termAttribute.term()).append("* ");
        }
        return result.toString();
    }

    /**
     * Normalize the genre string.
     * 
     * @param genre genre string
     * @return genre string normalized
     * @deprecated should be resolved with tokenizer or filter
     */
    @Deprecated
    private String normalizeGenre(String genre) {
        return genre.toLowerCase().replace(" ", "").replace("-", "");
    }

    /**
     * Query generation expression extracted from
     * {@link org.airsonic.player.service.SearchService#search(SearchCriteria, List, IndexType)}.
     * 
     * @param criteria criteria
     * @param musicFolders musicFolders
     * @param indexType {@link IndexType}
     * @return Query
     * @throws IOException When parsing of MultiFieldQueryParser fails
     * @throws ParseException When parsing of MultiFieldQueryParser fails
     */
    public Query search(SearchCriteria criteria, List<MusicFolder> musicFolders,
            IndexType indexType) throws ParseException, IOException {
        /*
         * Version.LUCENE_30
         * It is a transient description and will be deleted when upgrading the version.
         * SearchService variables are not used because the reference direction conflicts.
         */
        MultiFieldQueryParser queryParser = new MultiFieldQueryParser(Version.LUCENE_30,
                indexType.getFields(), analyzerFactory.getQueryAnalyzer(), indexType.getBoosts());

        BooleanQuery query = new BooleanQuery();
        query.add(queryParser.parse(analyzeQuery(criteria.getQuery())), BooleanClause.Occur.MUST);
        List<SpanTermQuery> musicFolderQueries = new ArrayList<SpanTermQuery>();
        for (MusicFolder musicFolder : musicFolders) {
            if (indexType == ALBUM_ID3 || indexType == ARTIST_ID3) {
                musicFolderQueries.add(new SpanTermQuery(new Term(FieldNames.FOLDER_ID,
                        NumericUtils.intToPrefixCoded(musicFolder.getId()))));
            } else {
                musicFolderQueries.add(new SpanTermQuery(
                        new Term(FieldNames.FOLDER, musicFolder.getPath().getPath())));
            }
        }
        query.add(
                new SpanOrQuery(
                        musicFolderQueries.toArray(new SpanQuery[musicFolderQueries.size()])),
                BooleanClause.Occur.MUST);
        return query;
    }

    /**
     * Query generation expression extracted from
     * {@link org.airsonic.player.service.SearchService#getRandomSongs(RandomSearchCriteria)}.
     * 
     * @param criteria criteria
     * @return Query
     */
    public Query getRandomSongs(RandomSearchCriteria criteria) {
        BooleanQuery query = new BooleanQuery();
        query.add(new TermQuery(
                new Term(FieldNames.MEDIA_TYPE, MediaFile.MediaType.MUSIC.name().toLowerCase())),
                BooleanClause.Occur.MUST);
        if (criteria.getGenre() != null) {
            String genre = normalizeGenre(criteria.getGenre());
            query.add(new TermQuery(new Term(FieldNames.GENRE, genre)), BooleanClause.Occur.MUST);
        }
        if (criteria.getFromYear() != null || criteria.getToYear() != null) {
            NumericRangeQuery<Integer> rangeQuery = NumericRangeQuery.newIntRange(FieldNames.YEAR,
                    criteria.getFromYear(), criteria.getToYear(), true, true);
            query.add(rangeQuery, BooleanClause.Occur.MUST);
        }

        List<SpanTermQuery> musicFolderQueries = new ArrayList<SpanTermQuery>();
        for (MusicFolder musicFolder : criteria.getMusicFolders()) {
            musicFolderQueries.add(new SpanTermQuery(
                    new Term(FieldNames.FOLDER, musicFolder.getPath().getPath())));
        }
        query.add(
                new SpanOrQuery(
                        musicFolderQueries.toArray(new SpanQuery[musicFolderQueries.size()])),
                BooleanClause.Occur.MUST);
        return query;
    }

    /**
     * Query generation expression extracted from
     * {@link org.airsonic.player.service.SearchService#searchByName( String, String, int, int, List, Class)}.
     * 
     * @param fieldName {@link FieldNames}
     * @return Query
     * @throws ParseException When parsing of QueryParser fails
     */
    public Query searchByName(String fieldName, String name) throws ParseException {
        /*
         * Version.LUCENE_30
         * It is a transient description and will be deleted when upgrading the version.
         * SearchService variables are not used because the reference direction conflicts.
         */
        QueryParser queryParser = new QueryParser(Version.LUCENE_30, fieldName,
                analyzerFactory.getQueryAnalyzer());
        Query query = queryParser.parse(name + "*");
        return query;
    }

    /**
     * Query generation expression extracted from
     * {@link org.airsonic.player.service.SearchService#getRandomAlbums(int, List)}.
     * 
     * @param musicFolders musicFolders
     * @return Query
     */
    public Query getRandomAlbums(List<MusicFolder> musicFolders) {
        List<SpanTermQuery> musicFolderQueries = new ArrayList<SpanTermQuery>();
        for (MusicFolder musicFolder : musicFolders) {
            musicFolderQueries.add(new SpanTermQuery(
                    new Term(FieldNames.FOLDER, musicFolder.getPath().getPath())));
        }
        Query query = new SpanOrQuery(
                musicFolderQueries.toArray(new SpanQuery[musicFolderQueries.size()]));
        return query;
    }

    /**
     * Query generation expression extracted from
     * {@link org.airsonic.player.service.SearchService#getRandomAlbumsId3(int, List)}.
     * 
     * @param musicFolders musicFolders
     * @return Query
     */
    public Query getRandomAlbumsId3(List<MusicFolder> musicFolders) {

        List<SpanTermQuery> musicFolderQueries = new ArrayList<SpanTermQuery>();
        for (MusicFolder musicFolder : musicFolders) {
            musicFolderQueries.add(new SpanTermQuery(new Term(FieldNames.FOLDER_ID,
                    NumericUtils.intToPrefixCoded(musicFolder.getId()))));
        }
        Query query = new SpanOrQuery(
                musicFolderQueries.toArray(new SpanQuery[musicFolderQueries.size()]));
        return query;
    }

}
