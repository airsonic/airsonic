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
import org.apache.lucene.document.Field;
import org.apache.lucene.document.NumericField;
import org.springframework.stereotype.Component;

/**
 * A factory that generates the documents to be stored in the index.
 */
@Component
public class DocumentFactory {

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
     * Create a document.
     * 
     * @param mediaFile target of document
     * @return document
     * @since legacy
     */
    public Document createAlbumDocument(MediaFile mediaFile) {
        Document doc = new Document();
        doc.add(new NumericField(FieldNames.ID, Field.Store.YES, false)
                .setIntValue(mediaFile.getId()));

        if (mediaFile.getArtist() != null) {
            doc.add(new Field(FieldNames.ARTIST, mediaFile.getArtist(), Field.Store.YES,
                    Field.Index.ANALYZED));
        }
        if (mediaFile.getAlbumName() != null) {
            doc.add(new Field(FieldNames.ALBUM, mediaFile.getAlbumName(), Field.Store.YES,
                    Field.Index.ANALYZED));
        }
        if (mediaFile.getFolder() != null) {
            doc.add(new Field(FieldNames.FOLDER, mediaFile.getFolder(), Field.Store.NO,
                    Field.Index.NOT_ANALYZED_NO_NORMS));
        }
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
        doc.add(new NumericField(FieldNames.ID, Field.Store.YES, false)
                .setIntValue(mediaFile.getId()));

        if (mediaFile.getArtist() != null) {
            doc.add(new Field(FieldNames.ARTIST, mediaFile.getArtist(), Field.Store.YES,
                    Field.Index.ANALYZED));
        }
        if (mediaFile.getFolder() != null) {
            doc.add(new Field(FieldNames.FOLDER, mediaFile.getFolder(), Field.Store.NO,
                    Field.Index.NOT_ANALYZED_NO_NORMS));
        }
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
        doc.add(new NumericField(FieldNames.ID, Field.Store.YES, false).setIntValue(album.getId()));

        if (album.getArtist() != null) {
            doc.add(new Field(FieldNames.ARTIST, album.getArtist(), Field.Store.YES,
                    Field.Index.ANALYZED));
        }
        if (album.getName() != null) {
            doc.add(new Field(FieldNames.ALBUM, album.getName(), Field.Store.YES,
                    Field.Index.ANALYZED));
        }
        if (album.getFolderId() != null) {
            doc.add(new NumericField(FieldNames.FOLDER_ID, Field.Store.NO, true)
                    .setIntValue(album.getFolderId()));
        }
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
    public Document createArtistId3Document(Artist artist, MusicFolder musicFolder) {
        Document doc = new Document();
        doc.add(new NumericField(FieldNames.ID, Field.Store.YES, false)
                .setIntValue(artist.getId()));
        doc.add(new Field(FieldNames.ARTIST, artist.getName(), Field.Store.YES,
                Field.Index.ANALYZED));
        doc.add(new NumericField(FieldNames.FOLDER_ID, Field.Store.NO, true)
                .setIntValue(musicFolder.getId()));
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
        doc.add(new NumericField(FieldNames.ID, Field.Store.YES, false)
                .setIntValue(mediaFile.getId()));
        doc.add(new Field(FieldNames.MEDIA_TYPE, mediaFile.getMediaType().name(), Field.Store.NO,
                Field.Index.ANALYZED_NO_NORMS));
        if (mediaFile.getTitle() != null) {
            doc.add(new Field(FieldNames.TITLE, mediaFile.getTitle(), Field.Store.YES,
                    Field.Index.ANALYZED));
        }
        if (mediaFile.getArtist() != null) {
            doc.add(new Field(FieldNames.ARTIST, mediaFile.getArtist(), Field.Store.YES,
                    Field.Index.ANALYZED));
        }
        if (mediaFile.getGenre() != null) {
            doc.add(new Field(FieldNames.GENRE, normalizeGenre(mediaFile.getGenre()),
                    Field.Store.NO, Field.Index.ANALYZED));
        }
        if (mediaFile.getYear() != null) {
            doc.add(new NumericField(FieldNames.YEAR, Field.Store.NO, true)
                    .setIntValue(mediaFile.getYear()));
        }
        if (mediaFile.getFolder() != null) {
            doc.add(new Field(FieldNames.FOLDER, mediaFile.getFolder(), Field.Store.NO,
                    Field.Index.NOT_ANALYZED_NO_NORMS));
        }
        return doc;
    }

}
