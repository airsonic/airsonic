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
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.Term;
import org.apache.lucene.util.BytesRef;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.stereotype.Component;

import java.util.function.BiConsumer;

import static org.springframework.util.ObjectUtils.isEmpty;

/**
 * A factory that generates the documents to be stored in the index.
 */
@Component
public class DocumentFactory {

    private static final FieldType TYPE_ID;

    private static final FieldType TYPE_ID_NO_STORE;

    private static final FieldType TYPE_KEY;

    static {

        TYPE_ID = new FieldType();
        TYPE_ID.setIndexOptions(IndexOptions.DOCS);
        TYPE_ID.setTokenized(false);
        TYPE_ID.setOmitNorms(true);
        TYPE_ID.setStored(true);
        TYPE_ID.freeze();

        TYPE_ID_NO_STORE = new FieldType();
        TYPE_ID_NO_STORE.setIndexOptions(IndexOptions.DOCS);
        TYPE_ID_NO_STORE.setTokenized(false);
        TYPE_ID_NO_STORE.setOmitNorms(true);
        TYPE_ID_NO_STORE.setStored(false);
        TYPE_ID_NO_STORE.freeze();

        TYPE_KEY = new FieldType();
        TYPE_KEY.setIndexOptions(IndexOptions.DOCS);
        TYPE_KEY.setTokenized(false);
        TYPE_KEY.setOmitNorms(true);
        TYPE_KEY.setStored(false);
        TYPE_KEY.freeze();

    }

    @FunctionalInterface
    private interface Consumer<T, U, V> {
        void accept(T t, U u, V v);

    }

    ;

    private BiConsumer<@NonNull Document, @NonNull Integer> fieldId = (doc, value) -> {
        doc.add(new StoredField(FieldNames.ID, Integer.toString(value), TYPE_ID));
    };

    private BiConsumer<@NonNull Document, @NonNull Integer> fieldFolderId = (doc, value) -> {
        doc.add(new StoredField(FieldNames.FOLDER_ID, Integer.toString(value), TYPE_ID_NO_STORE));
    };

    private Consumer<@NonNull Document, @NonNull String, @NonNull String> fieldKey = (doc, field, value) -> {
        doc.add(new StoredField(field, value, TYPE_KEY));
    };

    private BiConsumer<@NonNull  Document, @NonNull String> fieldMediatype = (doc, value) ->
        fieldKey.accept(doc, FieldNames.MEDIA_TYPE, value);

    private BiConsumer<@NonNull Document, @NonNull String> fieldFolderPath = (doc, value) -> 
        fieldKey.accept(doc, FieldNames.FOLDER, value);

    private BiConsumer<@NonNull Document, @Nullable String> fieldGenre = (doc, value) -> {
        if (isEmpty(value)) {
            return;
        }
        fieldKey.accept(doc, FieldNames.GENRE, value);
    };

    private Consumer<@NonNull Document, @NonNull String, @Nullable Integer> fieldYear = (doc, fieldName, value) -> {
        if (isEmpty(value)) {
            return;
        }
        doc.add(new IntPoint(fieldName, value));
    };

    private Consumer<@NonNull Document, @NonNull String, @Nullable String> fieldWords = (doc, fieldName, value) -> {
        if (isEmpty(value)) {
            return;
        }
        doc.add(new TextField(fieldName, value, Store.NO));
        doc.add(new SortedDocValuesField(fieldName, new BytesRef(value)));
    };

    public final Term createPrimarykey(Integer id) {
        return new Term(FieldNames.ID, Integer.toString(id));
    };

    public final Term createPrimarykey(Album album) {
        return createPrimarykey(album.getId());
    };

    public final Term createPrimarykey(Artist artist) {
        return createPrimarykey(artist.getId());
    };

    public final Term createPrimarykey(MediaFile mediaFile) {
        return createPrimarykey(mediaFile.getId());
    };

    /**
     * Create a document.
     * 
     * @param mediaFile target of document
     * @return document
     * @since legacy
     */
    public Document createAlbumDocument(MediaFile mediaFile) {
        Document doc = new Document();
        fieldId.accept(doc, mediaFile.getId());
        fieldWords.accept(doc, FieldNames.ARTIST, mediaFile.getArtist());
        fieldWords.accept(doc, FieldNames.ALBUM, mediaFile.getAlbumName());
        fieldFolderPath.accept(doc, mediaFile.getFolder());
        return doc;
    }

    /**
     * Create a document.
     * 
     * @param mediaFile target of document
     * @return document
     * @since legacy
     */
    public Document createArtistDocument(MediaFile mediaFile) {
        Document doc = new Document();
        fieldId.accept(doc, mediaFile.getId());
        fieldWords.accept(doc, FieldNames.ARTIST, mediaFile.getArtist());
        fieldFolderPath.accept(doc, mediaFile.getFolder());
        return doc;
    }

    /**
     * Create a document.
     * 
     * @param album target of document
     * @return document
     * @since legacy
     */
    public Document createAlbumId3Document(Album album) {
        Document doc = new Document();
        fieldId.accept(doc, album.getId());
        fieldWords.accept(doc, FieldNames.ARTIST, album.getArtist());
        fieldWords.accept(doc, FieldNames.ALBUM, album.getName());
        fieldFolderId.accept(doc, album.getFolderId());
        return doc;
    }

    /**
     * Create a document.
     * 
     * @param artist target of document
     * @param musicFolder target folder exists
     * @return document
     * @since legacy
     */
    /*
     *  XXX 3.x -> 8.x :
     *  Only null check specification of createArtistId3Document is different from legacy.
     *  (The reason is only to simplify the function.)
     *  
     *  Since the field of domain object Album is nonnull,
     *  null check was not performed.
     *  
     *  In implementation ARTIST and ALBUM became nullable,
     *  but null is not input at this point in data flow.
     */
    public Document createArtistId3Document(Artist artist, MusicFolder musicFolder) {
        Document doc = new Document();
        fieldId.accept(doc, artist.getId());
        fieldWords.accept(doc, FieldNames.ARTIST, artist.getName());
        fieldFolderId.accept(doc, musicFolder.getId());
        return doc;
    }

    /**
     * Create a document.
     * 
     * @param mediaFile target of document
     * @return document
     * @since legacy
     */
    public Document createSongDocument(MediaFile mediaFile) {
        Document doc = new Document();
        fieldId.accept(doc, mediaFile.getId());
        fieldMediatype.accept(doc, mediaFile.getMediaType().name());
        fieldWords.accept(doc, FieldNames.TITLE, mediaFile.getTitle());
        fieldWords.accept(doc, FieldNames.ARTIST, mediaFile.getArtist());
        fieldGenre.accept(doc, mediaFile.getGenre());
        fieldYear.accept(doc, FieldNames.YEAR, mediaFile.getYear());
        fieldFolderPath.accept(doc, mediaFile.getFolder());
        return doc;
    }

}
