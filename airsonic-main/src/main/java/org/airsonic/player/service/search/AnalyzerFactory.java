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

import org.apache.lucene.analysis.ASCIIFoldingFilter;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.util.Version;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Reader;

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

    /**
     * Return analyzer.
     * 
     * @return analyzer for index
     */
    public Analyzer getAnalyzer() {
        if (null == analyzer) {
            analyzer = new CustomAnalyzer();
        }
        return analyzer;
    }

    /**
     * Return analyzer.
     * 
     * @return analyzer for index
     */
    public Analyzer getQueryAnalyzer() {
        if (null == queryAnalyzer) {
            queryAnalyzer = new CustomAnalyzer();
        }
        return queryAnalyzer;
    }

    /*
     * The legacy CustomAnalyzer implementation is kept as it is.
     */
    private class CustomAnalyzer extends StandardAnalyzer {
        private CustomAnalyzer() {
            /*
             * Version.LUCENE_30
             * It is a transient description and will be deleted when upgrading the version.
             * SearchService variables are not used because the reference direction conflicts.
             */
            super(Version.LUCENE_30);
        }

        @Override
        public TokenStream tokenStream(String fieldName, Reader reader) {
            TokenStream result = super.tokenStream(fieldName, reader);
            return new ASCIIFoldingFilter(result);
        }

        @Override
        public TokenStream reusableTokenStream(String fieldName, Reader reader) throws IOException {
            class SavedStreams {
                StandardTokenizer tokenStream;
                TokenStream       filteredTokenStream;
            }

            SavedStreams streams = (SavedStreams) getPreviousTokenStream();
            if (streams == null) {
                streams = new SavedStreams();
                setPreviousTokenStream(streams);
                streams.tokenStream = new StandardTokenizer(Version.LUCENE_30, reader);
                streams.filteredTokenStream = new StandardFilter(streams.tokenStream);
                streams.filteredTokenStream = new LowerCaseFilter(streams.filteredTokenStream);
                streams.filteredTokenStream = new StopFilter(true, streams.filteredTokenStream,
                        STOP_WORDS_SET);
                streams.filteredTokenStream = new ASCIIFoldingFilter(streams.filteredTokenStream);
            } else {
                streams.tokenStream.reset(reader);
            }
            streams.tokenStream.setMaxTokenLength(DEFAULT_MAX_TOKEN_LENGTH);

            return streams.filteredTokenStream;
        }
    }

}
