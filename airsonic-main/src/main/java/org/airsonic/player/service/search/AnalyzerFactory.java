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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.cjk.CJKWidthFilterFactory;
import org.apache.lucene.analysis.core.KeywordTokenizerFactory;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.StopFilterFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer.Builder;
import org.apache.lucene.analysis.en.EnglishPossessiveFilterFactory;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilterFactory;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.pattern.PatternReplaceFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.util.ObjectUtils.isEmpty;

/**
 * Analyzer provider.
 * This class is a division of what was once part of SearchService and added functionality.
 * This class provides Analyzer which is used at index generation
 * and QueryAnalyzer which analyzes the specified query at search time.
 * Analyzer can be closed but is a reuse premise.
 * It is held in this class.
 */
@Component
public final class AnalyzerFactory {

    private Analyzer analyzer;

    private Analyzer queryAnalyzer;

    /*
     * XXX 3.x -> 8.x : Convert UAX#29 Underscore Analysis to Legacy Analysis
     * 
     * Because changes in underscores before and after words
     * have a major effect on user's forward match search.
     * 
     * @see AnalyzerFactoryTestCase
     */
    private void addTokenFilterForUnderscoreRemovalAroundToken(Builder builder) throws IOException {
        builder
            .addTokenFilter(PatternReplaceFilterFactory.class,
                    "pattern", "^\\_", "replacement", "", "replace", "all")
            .addTokenFilter(PatternReplaceFilterFactory.class,
                    "pattern", "\\_$", "replacement", "", "replace", "all");
    }

    /*
     * XXX 3.x -> 8.x : Handle brackets correctly
     * 
     * Process the input value of Genre search for search of domain value.
     * 
     * The tag parser performs special character conversion
     * when converting input values ​​from a file.
     * Therefore, the domain value may be different from the original value.
     * This filter allows searching by user readable value (file tag value).
     * 
     * @see org.jaudiotagger.tag.id3.framebody.FrameBodyTCON#convertID3v23GenreToGeneric
     * (TCON stands for Genre with ID3 v2.3-v2.4)
     * Such processing exists because brackets in the Gener string have a special meaning.
     */
    private void addTokenFilterForTokenToDomainValue(Builder builder) throws IOException {
        builder
            .addTokenFilter(PatternReplaceFilterFactory.class,
                    "pattern", "\\(", "replacement", "", "replace", "all")
            .addTokenFilter(PatternReplaceFilterFactory.class,
                    "pattern", "\\)$", "replacement", "", "replace", "all")
            .addTokenFilter(PatternReplaceFilterFactory.class,
                    "pattern", "\\)", "replacement", " ", "replace", "all")
            .addTokenFilter(PatternReplaceFilterFactory.class,
                    "pattern", "\\{\\}", "replacement", "\\{ \\}", "replace", "all")
            .addTokenFilter(PatternReplaceFilterFactory.class,
                    "pattern", "\\[\\]", "replacement", "\\[ \\]", "replace", "all");
    }

    private Builder createDefaultAnalyzerBuilder() throws IOException {
        Builder builder = CustomAnalyzer.builder()
                .withTokenizer(StandardTokenizerFactory.class)
                .addTokenFilter(CJKWidthFilterFactory.class)
                .addTokenFilter(ASCIIFoldingFilterFactory.class, "preserveOriginal", "false")
                .addTokenFilter(LowerCaseFilterFactory.class)
                .addTokenFilter(StopFilterFactory.class)
                .addTokenFilter(EnglishPossessiveFilterFactory.class);
        addTokenFilterForUnderscoreRemovalAroundToken(builder);
        return builder;
    }

    private Builder createKeywordAnalyzerBuilder() throws IOException {
        return CustomAnalyzer.builder()
                .withTokenizer(KeywordTokenizerFactory.class);
    }

    private Builder createGenreAnalyzerBuilder() throws IOException {
        Builder builder = createKeywordAnalyzerBuilder();
        addTokenFilterForTokenToDomainValue(builder);
        return builder;
    }

    /**
     * Returns the Analyzer to use when generating the index.
     * 
     * Whether this analyzer is applied to input values ​​depends on
     * the definition of the document's fields.
     * 
     * @return analyzer for index
     * @see DocumentFactory
     */
    public Analyzer getAnalyzer() throws IOException {
        if (isEmpty(analyzer)) {
            try {
                analyzer = createDefaultAnalyzerBuilder().build();
            } catch (IOException e) {
                throw new IOException("Error when initializing Analyzer.", e);
            }
        }
        return analyzer;
    }

    /**
     * Returns the analyzer to use when generating a query for index search.
     * 
     * String processing handled by QueryFactory
     * is limited to Lucene's modifier.
     * 
     * The processing of the operands is expressed
     * in the AnalyzerFactory implementation.
     * Rules for tokenizing/converting input values ​
     * should not be described in QueryFactory.
     * 
     * @return analyzer for query
     * @see QueryFactory
     */
    public Analyzer getQueryAnalyzer() throws IOException {
        if (isEmpty(queryAnalyzer)) {
            try {

                Analyzer defaultAnalyzer = createDefaultAnalyzerBuilder().build();
                Analyzer genreAnalyzer = createGenreAnalyzerBuilder().build();

                Map<String, Analyzer> fieldAnalyzers = new HashMap<>();
                fieldAnalyzers.put(FieldNames.GENRE, genreAnalyzer);

                queryAnalyzer = new PerFieldAnalyzerWrapper(defaultAnalyzer, fieldAnalyzers);

            } catch (IOException e) {
                throw new IOException("Error when initializing Analyzer.", e);
            }
        }
        return queryAnalyzer;
    }

}
