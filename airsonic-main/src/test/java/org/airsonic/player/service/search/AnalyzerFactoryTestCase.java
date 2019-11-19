
package org.airsonic.player.service.search;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

/**
 * Test case for Analyzer.
 * These cases have the purpose of observing the current situation
 * and observing the impact of upgrading Lucene.
 */
public class AnalyzerFactoryTestCase {

    private AnalyzerFactory analyzerFactory = new AnalyzerFactory();

    /**
     * Test for the number of character separators per field.
     */
    @Test
    public void testTokenCounts() {

        /*
         * Analyzer used in legacy uses the same Tokenizer for all fields.
         * (Some fields are converted to their own input string for integrity.)
         * As a result, specifications for strings are scattered and difficult to understand.
         * Using PerFieldAnalyzerWrapper,
         * it is possible to use different Analyzer (Tokenizer/Filter) for each field.
         * This allows consistent management of parsing definitions.
         * It is also possible to apply definitions such as "id3 delimiters Tokenizer" to specific fields.
         */

        // The number of words excluding articles is 7.
        String query = "The quick brown fox jumps over the lazy dog.";

        Arrays.stream(IndexType.values()).flatMap(i -> Arrays.stream(i.getFields())).forEach(n -> {
            List<String> terms = toTermString(n, query);
            switch (n) {

                /*
                 * In the legacy, these field divide input into 7. It is not necessary to delimit
                 * this field originally.
                 */
                case FieldNames.FOLDER:
                case FieldNames.MEDIA_TYPE:
                case FieldNames.GENRE:
                    assertEquals("oneTokenFields : " + n, 7, terms.size());
                    break;

                /*
                 * These should be divided into 7.
                 */
                case FieldNames.ARTIST:
                case FieldNames.ALBUM:
                case FieldNames.TITLE:
                    assertEquals("multiTokenFields : " + n, 7, terms.size());
                    break;
                /*
                 * ID, FOLDER_ID, YEAR
                 * This is not a problem because the input value does not contain a delimiter.
                 */
                default:
                    assertEquals("oneTokenFields : " + n, 7, terms.size());
                    break;
            }
        });

    }

    /**
     * Detailed tests on Punctuation.
     * In addition to the common delimiters, there are many delimiters.
     */
    @Test
    public void testPunctuation1() {

        String query = "B︴C";
        String expected = "b︴c";

        /*
         * XXX 3.x -> 8.x :
         * The definition of punctuation has changed.
         */
        Arrays.stream(IndexType.values()).flatMap(i -> Arrays.stream(i.getFields())).forEach(n -> {
            List<String> terms = toTermString(n, query);
            switch (n) {

                /*
                 * In the legacy, these field divide input into 2.
                 * It is not necessary to delimit
                 * this field originally.
                 */
                case FieldNames.FOLDER:
                case FieldNames.GENRE:
                case FieldNames.MEDIA_TYPE:
                    assertEquals("tokenized : " + n, 1, terms.size());
                    assertEquals("tokenized : " + n, expected, terms.get(0));
                    break;

                /*
                 * What should the fields of this be?
                 * Generally discarded.
                 */
                case FieldNames.ARTIST:
                case FieldNames.ALBUM:
                case FieldNames.TITLE:
                    assertEquals("tokenized : " + n, 1, terms.size());
                    assertEquals("tokenized : " + n, expected, terms.get(0));
                    break;
                /*
                 * ID, FOLDER_ID, YEAR
                 * This is not a problem because the input value does not contain a delimiter.
                 */
                default:
                    assertEquals("tokenized : " + n, 2, terms.size());
                    break;
            }
        });
    }

    /*
     * Detailed tests on Punctuation.
     * Many of the symbols are delimiters or target to be removed.
     */
    @Test
    public void testPunctuation2() {

        String query = "{'“『【【】】[︴○◎@ $〒→+]";
        Arrays.stream(IndexType.values()).flatMap(i -> Arrays.stream(i.getFields())).forEach(n -> {
            List<String> terms = toTermString(n, query);
            switch (n) {
                case FieldNames.FOLDER:
                case FieldNames.MEDIA_TYPE:
                case FieldNames.GENRE:
                case FieldNames.ARTIST:
                case FieldNames.ALBUM:
                case FieldNames.TITLE:
                    assertEquals("removed : " + n, 0, terms.size());
                    break;
                default:
                    assertEquals("removed : " + n, 0, terms.size());
            }
        });
    }

    /**
     * Detailed tests on Stopward.
     * 
     * @see org.apache.lucene.analysis.core.StopAnalyzer#ENGLISH_STOP_WORDS_SET
     */
    @Test
    public void testStopward() {

        /*
         * Legacy behavior is to remove ENGLISH_STOP_WORDS_SET from the Token stream.
         * (Putting whether or not it matches the specification of the music search.)
         */

        /*
         * article.
         * This is included in ENGLISH_STOP_WORDS_SET.
         */
        String queryArticle = "a an the";

        /*
         * The default set as index stop word.
         * But these are not included in ENGLISH_STOP_WORDS_SET.
         */
        String queryArticle4Index = "el la los las le les";

        /*
         * Non-article in the ENGLISH_STOP_WORDS_SET.
         * Stopwords are essential for newspapers and documents,
         * but offten they are over-processed for song titles.
         * For example, "we will rock you" can not be searched by "will".
         */
        String queryStop = "and are as at be but by for if in into is it no not of on " //
                + "or such that their then there these they this to was will with";

        Arrays.stream(IndexType.values()).flatMap(i -> Arrays.stream(i.getFields())).forEach(n -> {
            List<String> articleTerms = toTermString(n, queryArticle);
            List<String> indexArticleTerms = toTermString(n, queryArticle4Index);
            List<String> stopedTerms = toTermString(n, queryStop);

            switch (n) {

                case FieldNames.FOLDER:
                case FieldNames.MEDIA_TYPE:
                case FieldNames.GENRE:
                case FieldNames.ARTIST:
                case FieldNames.ALBUM:
                case FieldNames.TITLE:

                    // It is removed because it is included in ENGLISH_STOP_WORDS_SET.
                    assertEquals("article : " + n, 0, articleTerms.size());
                    // Not removed because it is not included in ENGLISH_STOP_WORDS_SET.
                    assertEquals("sonic server index article: " + n, 6, indexArticleTerms.size());
                    // It is removed because it is included in ENGLISH_STOP_WORDS_SET.
                    assertEquals("non-article stop words : " + n, 0, stopedTerms.size());
                    break;

                // Legacy has common behavior for all fields.
                default:
                    assertEquals("article : " + n, 0, articleTerms.size());
                    assertEquals("sonic server index article: " + n, 6, indexArticleTerms.size());
                    assertEquals("non-article stop words : " + n, 0, stopedTerms.size());
                    break;
            }
        });

    }

    /**
     * Simple test on FullWidth.
     */
    @Test
    public void testFullWidth() {
        String query = "ＦＵＬＬ－ＷＩＤＴＨ";
        List<String> terms = toTermString(query);
        assertEquals(2, terms.size());
        assertEquals("full", terms.get(0));
        assertEquals("width", terms.get(1));
    }

    /**
     * Combined case of Stop and full-width.
     */
    @Test
    public void testStopwardAndFullWidth() {

        /*
         * Stop word is removed.
         */
        String queryHalfWidth = "THIS IS FULL-WIDTH SENTENCES.";
        List<String> terms = toTermString(queryHalfWidth);
        assertEquals(3, terms.size());
        assertEquals("full", terms.get(0));
        assertEquals("width", terms.get(1));
        assertEquals("sentences", terms.get(2));

        /*
         * Legacy can avoid Stopward if it is full width.
         * It is unclear whether it is a specification or not.
         * (Problems due to a defect in filter application order?
         * or
         * Is it popular in English speaking countries?)
         */
        String queryFullWidth = "ＴＨＩＳ　ＩＳ　ＦＵＬＬ－ＷＩＤＴＨ　ＳＥＮＴＥＮＣＥＳ.";
        terms = toTermString(queryFullWidth);
        /*
         * XXX 3.x -> 8.x :
         * 
         * This is not a change due to the library but an intentional change.
         * The filter order has been changed properly
         * as it is probably not a deliberate specification.
         */
        assertEquals(3, terms.size());
        assertEquals("full", terms.get(0));
        assertEquals("width", terms.get(1));
        assertEquals("sentences", terms.get(2));

    }

    /**
     * Tests on ligature and diacritical marks.
     * In UAX#29, determination of non-practical word boundaries is not considered.
     * Languages ​​that use special strings require "practical word" sample.
     * Unit testing with only ligature and diacritical marks is not possible.
     */
    @Test
    public void testAsciiFoldingStop() {

        String queryLigature = "Cæsar";
        String expectedLigature = "caesar";

        String queryDiacritical = "Café";
        String expectedDiacritical = "cafe";

        Arrays.stream(IndexType.values()).flatMap(i -> Arrays.stream(i.getFields())).forEach(n -> {
            List<String> termsLigature = toTermString(n, queryLigature);
            List<String> termsDiacritical = toTermString(n, queryDiacritical);
            switch (n) {

                /*
                 * It is decomposed into the expected string.
                 */
                case FieldNames.FOLDER:
                case FieldNames.MEDIA_TYPE:
                case FieldNames.GENRE:
                case FieldNames.ARTIST:
                case FieldNames.ALBUM:
                case FieldNames.TITLE:
                    assertEquals("Cæsar : " + n, 1, termsLigature.size());
                    assertEquals("Cæsar : " + n, expectedLigature, termsLigature.get(0));
                    assertEquals("Café : " + n, 1, termsDiacritical.size());
                    assertEquals("Café : " + n, expectedDiacritical, termsDiacritical.get(0));
                    break;

                // Legacy has common behavior for all fields.
                default:
                    assertEquals("Cæsar : " + n, 1, termsLigature.size());
                    assertEquals("Cæsar : " + n, expectedLigature, termsLigature.get(0));
                    assertEquals("Café : " + n, 1, termsDiacritical.size());
                    assertEquals("Café : " + n, expectedDiacritical, termsDiacritical.get(0));
                    break;

            }
        });

    }

    /**
     * Detailed tests on LowerCase.
     */
    @Test
    public void testLowerCase() {

        // Filter operation check only. Verify only some settings.
        String query = "ABCDEFG";
        String expected = "abcdefg";

        Arrays.stream(IndexType.values()).flatMap(i -> Arrays.stream(i.getFields())).forEach(n -> {
            List<String> terms = toTermString(n, query);
            switch (n) {

                /*
                 * In legacy, it is converted to lower. (over-processed?)
                 */
                case FieldNames.FOLDER:
                case FieldNames.MEDIA_TYPE:
                    assertEquals("lower : " + n, 1, terms.size());
                    assertEquals("lower : " + n, expected, terms.get(0));
                    break;

                /*
                 * These are searchable fields in lower case.
                 */
                case FieldNames.GENRE:
                case FieldNames.ARTIST:
                case FieldNames.ALBUM:
                case FieldNames.TITLE:
                    assertEquals("lower : " + n, 1, terms.size());
                    assertEquals("lower : " + n, expected, terms.get(0));
                    break;

                // Legacy has common behavior for all fields.
                default:
                    assertEquals("lower : " + n, 1, terms.size());
                    assertEquals("lower : " + n, expected, terms.get(0));
                    break;

            }
        });
    }

    /**
     * Detailed tests on EscapeRequires.
     * The reserved string is discarded unless it is purposely Escape.
     * This is fine as a search specification(if it is considered as a kind of reserved stop word).
     * However, in the case of file path, it may be a problem.
     */
    @Test
    public void testLuceneEscapeRequires() {

        String queryEscapeRequires = "+-&&||!(){}[]^\"~*?:\\/";
        String queryFileUsable = "+-&&!(){}[]^~";

        Arrays.stream(IndexType.values()).flatMap(i -> Arrays.stream(i.getFields())).forEach(n -> {
            List<String> terms = toTermString(n, queryEscapeRequires);
            switch (n) {

                /*
                 * Will be removed. (Can not distinguish the directory of a particular pattern?)
                 */
                case FieldNames.FOLDER:
                    assertEquals("escape : " + n, 0, terms.size());
                    terms = toTermString(n, queryFileUsable);
                    assertEquals("escape : " + n, 0, terms.size());
                    break;

                /*
                 * Will be removed.
                 */
                case FieldNames.MEDIA_TYPE:
                case FieldNames.GENRE:
                case FieldNames.ARTIST:
                case FieldNames.ALBUM:
                case FieldNames.TITLE:
                    assertEquals("escape : " + n, 0, terms.size());
                    break;

                // Will be removed.
                default:
                    assertEquals("escape : " + n, 0, terms.size());
                    break;

            }
        });

    }

    /**
     * Create an example that makes UAX 29 differences easy to understand.
     */
    @Test
    public void testUax29() {

        /*
         * Case using test resource name
         */

        // Semicolon, comma and hyphen.
        String query = "Bach: Goldberg Variations, BWV 988 - Aria";
        List<String> terms = toTermString(query);
        assertEquals(6, terms.size());
        assertEquals("bach", terms.get(0));
        assertEquals("goldberg", terms.get(1));
        assertEquals("variations", terms.get(2));
        assertEquals("bwv", terms.get(3));
        assertEquals("988", terms.get(4));
        assertEquals("aria", terms.get(5));

        // Underscores around words, ascii and semicolon.
        query = "_ID3_ARTIST_ Céline Frisch: Café Zimmermann";
        terms = toTermString(query);
        assertEquals(5, terms.size());

        /*
         * XXX 3.x -> 8.x : _id3_artist_　in UAX#29.
         * Since the effect is large, trim with Filter.
         */
        assertEquals("id3_artist", terms.get(0));
        assertEquals("celine", terms.get(1));
        assertEquals("frisch", terms.get(2));
        assertEquals("cafe", terms.get(3));
        assertEquals("zimmermann", terms.get(4));

        // Underscores around words and slashes.
        query = "_ID3_ARTIST_ Sarah Walker/Nash Ensemble";
        terms = toTermString(query);
        assertEquals(5, terms.size());

        /*
         * XXX 3.x -> 8.x : _id3_artist_　in UAX#29.
         * Since the effect is large, trim with Filter.
         */
        assertEquals("id3_artist", terms.get(0));
        assertEquals("sarah", terms.get(1));
        assertEquals("walker", terms.get(2));
        assertEquals("nash", terms.get(3));
        assertEquals("ensemble", terms.get(4));
        
        // Space
        assertEquals(asList("abc", "def"), toTermString(" ABC DEF "));
        assertEquals(asList("abc1", "def"), toTermString(" ABC1 DEF "));

        // trim and delimiter
        assertEquals(asList("abc", "def"), toTermString("+ABC+DEF+"));
        assertEquals(asList("abc", "def"), toTermString("|ABC|DEF|"));
        assertEquals(asList("abc", "def"), toTermString("!ABC!DEF!"));
        assertEquals(asList("abc", "def"), toTermString("(ABC(DEF("));
        assertEquals(asList("abc", "def"), toTermString(")ABC)DEF)"));
        assertEquals(asList("abc", "def"), toTermString("{ABC{DEF{"));
        assertEquals(asList("abc", "def"), toTermString("}ABC}DEF}"));
        assertEquals(asList("abc", "def"), toTermString("[ABC[DEF["));
        assertEquals(asList("abc", "def"), toTermString("]ABC]DEF]"));
        assertEquals(asList("abc", "def"), toTermString("^ABC^DEF^"));
        assertEquals(asList("abc", "def"), toTermString("\\ABC\\DEF\\"));
        assertEquals(asList("abc", "def"), toTermString("\"ABC\"DEF\""));
        assertEquals(asList("abc", "def"), toTermString("~ABC~DEF~"));
        assertEquals(asList("abc", "def"), toTermString("*ABC*DEF*"));
        assertEquals(asList("abc", "def"), toTermString("?ABC?DEF?"));
        assertEquals(asList("abc:def"), toTermString(":ABC:DEF:"));             // XXX 3.x -> 8.x : abc def -> abc:def
        assertEquals(asList("abc", "def"), toTermString("-ABC-DEF-"));
        assertEquals(asList("abc", "def"), toTermString("/ABC/DEF/"));
        /*
         * XXX 3.x -> 8.x : _abc_def_　in UAX#29.
         * Since the effect is large, trim with Filter.
         */
        assertEquals(asList("abc_def"), toTermString("_ABC_DEF_"));             // XXX 3.x -> 8.x : abc def -> abc_def
        assertEquals(asList("abc", "def"), toTermString(",ABC,DEF,"));
        assertEquals(asList("abc.def"), toTermString(".ABC.DEF."));
        assertEquals(asList("abc", "def"), toTermString("&ABC&DEF&"));          // XXX 3.x -> 8.x : abc&def -> abc def
        assertEquals(asList("abc", "def"), toTermString("@ABC@DEF@"));          // XXX 3.x -> 8.x : abc@def -> abc def
        assertEquals(asList("abc'def"), toTermString("'ABC'DEF'"));

        // trim and delimiter and number
        assertEquals(asList("abc1", "def"), toTermString("+ABC1+DEF+"));
        assertEquals(asList("abc1", "def"), toTermString("|ABC1|DEF|"));
        assertEquals(asList("abc1", "def"), toTermString("!ABC1!DEF!"));
        assertEquals(asList("abc1", "def"), toTermString("(ABC1(DEF("));
        assertEquals(asList("abc1", "def"), toTermString(")ABC1)DEF)"));
        assertEquals(asList("abc1", "def"), toTermString("{ABC1{DEF{"));
        assertEquals(asList("abc1", "def"), toTermString("}ABC1}DEF}"));
        assertEquals(asList("abc1", "def"), toTermString("[ABC1[DEF["));
        assertEquals(asList("abc1", "def"), toTermString("]ABC1]DEF]"));
        assertEquals(asList("abc1", "def"), toTermString("^ABC1^DEF^"));
        assertEquals(asList("abc1", "def"), toTermString("\\ABC1\\DEF\\"));
        assertEquals(asList("abc1", "def"), toTermString("\"ABC1\"DEF\""));
        assertEquals(asList("abc1", "def"), toTermString("~ABC1~DEF~"));
        assertEquals(asList("abc1", "def"), toTermString("*ABC1*DEF*"));
        assertEquals(asList("abc1", "def"), toTermString("?ABC1?DEF?"));
        assertEquals(asList("abc1", "def"), toTermString(":ABC1:DEF:"));
        assertEquals(asList("abc1", "def"), toTermString(",ABC1,DEF,"));        // XXX 3.x -> 8.x : abc1,def -> abc1 def
        assertEquals(asList("abc1", "def"), toTermString("-ABC1-DEF-"));        // XXX 3.x -> 8.x : abc1-def -> abc1 def
        assertEquals(asList("abc1", "def"), toTermString("/ABC1/DEF/"));        // XXX 3.x -> 8.x : abc1/def -> abc1 def
        /*
         * XXX 3.x -> 8.x : _abc1_def_　in UAX#29.
         * Since the effect is large, trim with Filter.
         */
        assertEquals(asList("abc1_def"), toTermString("_ABC1_DEF_"));
        assertEquals(asList("abc1", "def"), toTermString(".ABC1.DEF."));        // XXX 3.x -> 8.x : abc1.def -> abc1 def
        assertEquals(asList("abc1", "def"), toTermString("&ABC1&DEF&"));
        assertEquals(asList("abc1", "def"), toTermString("@ABC1@DEF@"));
        assertEquals(asList("abc1", "def"), toTermString("'ABC1'DEF'"));

    }

    /**
     * Special handling of single quotes.
     */
    @Test
    public void testSingleQuotes() {

        /*
         * A somewhat cultural that seems to be related to a specific language.
         */
        String query = "This is Airsonic's analysis.";
        List<String> terms = toTermString(query);
        assertEquals(2, terms.size());
        assertEquals("airsonic", terms.get(0));
        assertEquals("analysis", terms.get(1));

        /*
         * XXX 3.x -> 8.x :
         * we ve -> we've
         */
        query = "We’ve been here before.";
        terms = toTermString(query);
        assertEquals(4, terms.size());
        assertEquals("we've", terms.get(0));
        assertEquals("been", terms.get(1));
        assertEquals("here", terms.get(2));
        assertEquals("before", terms.get(3));

        query = "LʼHomme";
        terms = toTermString(query);
        assertEquals(1, terms.size());
        assertEquals("lʼhomme", terms.get(0));

        query = "L'Homme";
        terms = toTermString(query);
        assertEquals(1, terms.size());
        assertEquals("l'homme", terms.get(0));

        query = "aujourd'hui";
        terms = toTermString(query);
        assertEquals(1, terms.size());
        assertEquals("aujourd'hui", terms.get(0));

        query = "fo'c'sle";
        terms = toTermString(query);
        assertEquals(1, terms.size());
        assertEquals("fo'c'sle", terms.get(0));

    }

    /*
     * There is also a filter that converts the tense to correspond to the search by the present
     * tense.
     */
    @Test
    public void testPastParticiple() {

        /*
         * Confirming no conversion to present tense.
         */
        String query = "This is formed with a form of the verb \"have\" and a past participl.";
        List<String> terms = toTermString(query);
        assertEquals(6, terms.size());
        assertEquals("formed", terms.get(0));// leave passive / not "form"
        assertEquals("form", terms.get(1));
        assertEquals("verb", terms.get(2));
        assertEquals("have", terms.get(3));
        assertEquals("past", terms.get(4));
        assertEquals("participl", terms.get(5));

    }

    /*
     * There are also filters that convert plurals to singular.
     */
    @Test
    public void testNumeral() {

        /*
         * Confirming no conversion to singular.
         */

        String query = "books boxes cities leaves men glasses";
        List<String> terms = toTermString(query);
        assertEquals(6, terms.size());
        assertEquals("books", terms.get(0));// leave numeral / not singular
        assertEquals("boxes", terms.get(1));
        assertEquals("cities", terms.get(2));
        assertEquals("leaves", terms.get(3));
        assertEquals("men", terms.get(4));
        assertEquals("glasses", terms.get(5));
    }

    @Test
    public void testGenre() {

        /*
         * Confirming no conversion to singular.
         */

        String query = "{}";
        List<String> terms = toQueryTermString(FieldNames.GENRE, query);
        assertEquals(1, terms.size());
        assertEquals("{ }", terms.get(0));
    }
    
    private List<String> toTermString(String str) {
        return toTermString(null, str);
    }

    private List<String> toTermString(String field, String str) {
        List<String> result = new ArrayList<>();
        try {
            TokenStream stream = analyzerFactory.getAnalyzer().tokenStream(field,
                    new StringReader(str));
            stream.reset();
            while (stream.incrementToken()) {
                result.add(stream.getAttribute(CharTermAttribute.class).toString()
                        .replaceAll("^term\\=", ""));
            }
            stream.close();
        } catch (IOException e) {
            LoggerFactory.getLogger(AnalyzerFactoryTestCase.class)
                    .error("Error during Token processing.", e);
        }
        return result;
    }

    /*
     * Should be added in later versions.
     */
    public void testWildCard() {
    }

    @SuppressWarnings("unused")
    private List<String> toQueryTermString(String field, String str) {
        List<String> result = new ArrayList<>();
        try {
            TokenStream stream = analyzerFactory.getQueryAnalyzer().tokenStream(field,
                    new StringReader(str));
            stream.reset();
            while (stream.incrementToken()) {
                result.add(stream.getAttribute(CharTermAttribute.class).toString()
                        .replaceAll("^term\\=", ""));
            }
            stream.close();
        } catch (IOException e) {
            LoggerFactory.getLogger(AnalyzerFactoryTestCase.class)
                    .error("Error during Token processing.", e);
        }
        return result;
    }

}
