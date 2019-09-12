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
import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.Artist;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.ParamSearchResult;
import org.airsonic.player.domain.SearchResult;
import org.airsonic.player.service.MediaFileService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.lucene.document.Document;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static org.springframework.util.ObjectUtils.isEmpty;

/**
 * Termination used by SearchService.
 * 
 * Since SearchService operates as a proxy for storage (DB) using lucene,
 * there are many redundant descriptions different from essential data processing.
 * This class is a transfer class for saving those redundant descriptions.
 * 
 * Exception handling is not termination,
 * so do not include exception handling in this class.
 */
@Component
public class SearchServiceUtilities {

    /* Search by id only. */
    @Autowired
    private ArtistDao artistDao;

    /* Search by id only. */
    @Autowired
    private AlbumDao albumDao;

    /*
     * Search by id only.
     * Although there is no influence at present,
     * mediaFileService has a caching mechanism.
     * Service is used instead of Dao until you are sure you need to use mediaFileDao.
     */
    @Autowired
    private MediaFileService mediaFileService;

    public final Function<Long, Integer> round = (i) -> {
        // return
        // NumericUtils.floatToSortableInt(i);
        return i.intValue();
    };

    public final Function<Document, Integer> getId = d -> {
        return Integer.valueOf(d.get(FieldNames.ID));
    };

    public final BiConsumer<List<MediaFile>, Integer> addMediaFileIfAnyMatch = (dist, id) -> {
        if (!dist.stream().anyMatch(m -> id == m.getId())) {
            MediaFile mediaFile = mediaFileService.getMediaFile(id);
            if (!isEmpty(mediaFile)) {
                dist.add(mediaFile);
            }
        }
    };

    public final BiConsumer<List<Artist>, Integer> addArtistId3IfAnyMatch = (dist, id) -> {
        if (!dist.stream().anyMatch(a -> id == a.getId())) {
            Artist artist = artistDao.getArtist(id);
            if (!isEmpty(artist)) {
                dist.add(artist);
            }
        }
    };

    public final Function<Class<?>, @Nullable IndexType> getIndexType = (assignableClass) -> {
        IndexType indexType = null;
        if (assignableClass.isAssignableFrom(Album.class)) {
            indexType = IndexType.ALBUM_ID3;
        } else if (assignableClass.isAssignableFrom(Artist.class)) {
            indexType = IndexType.ARTIST_ID3;
        } else if (assignableClass.isAssignableFrom(MediaFile.class)) {
            indexType = IndexType.SONG;
        }
        return indexType;
    };

    public final Function<Class<?>, @Nullable String> getFieldName = (assignableClass) -> {
        String fieldName = null;
        if (assignableClass.isAssignableFrom(Album.class)) {
            fieldName = FieldNames.ALBUM;
        } else if (assignableClass.isAssignableFrom(Artist.class)) {
            fieldName = FieldNames.ARTIST;
        } else if (assignableClass.isAssignableFrom(MediaFile.class)) {
            fieldName = FieldNames.TITLE;
        }
        return fieldName;
    };

    public final BiConsumer<List<Album>, Integer> addAlbumId3IfAnyMatch = (dist, subjectId) -> {
        if (!dist.stream().anyMatch(a -> subjectId == a.getId())) {
            Album album = albumDao.getAlbum(subjectId);
            if (!isEmpty(album)) {
                dist.add(album);
            }
        }
    };

    public final boolean addIgnoreNull(Collection<?> collection, Object object) {
        return CollectionUtils.addIgnoreNull(collection, object);
    }

    public final boolean addIgnoreNull(Collection<?> collection, IndexType indexType,
            int subjectId) {
        if (indexType == IndexType.ALBUM | indexType == IndexType.SONG) {
            return addIgnoreNull(collection, mediaFileService.getMediaFile(subjectId));
        } else if (indexType == IndexType.ALBUM_ID3) {
            return addIgnoreNull(collection, albumDao.getAlbum(subjectId));
        }
        return false;
    }

    public final <T> void addIgnoreNull(ParamSearchResult<T> dist, IndexType indexType,
            int subjectId, Class<T> subjectClass) {
        if (indexType == IndexType.SONG) {
            MediaFile mediaFile = mediaFileService.getMediaFile(subjectId);
            addIgnoreNull(dist.getItems(), subjectClass.cast(mediaFile));
        } else if (indexType == IndexType.ARTIST_ID3) {
            Artist artist = artistDao.getArtist(subjectId);
            addIgnoreNull(dist.getItems(), subjectClass.cast(artist));
        } else if (indexType == IndexType.ALBUM_ID3) {
            Album album = albumDao.getAlbum(subjectId);
            addIgnoreNull(dist.getItems(), subjectClass.cast(album));
        }
    }

    public final void addIfAnyMatch(SearchResult dist, IndexType subjectIndexType,
            Document subject) {
        int documentId = getId.apply(subject);
        if (subjectIndexType == IndexType.ARTIST | subjectIndexType == IndexType.ALBUM
                | subjectIndexType == IndexType.SONG) {
            addMediaFileIfAnyMatch.accept(dist.getMediaFiles(), documentId);
        } else if (subjectIndexType == IndexType.ARTIST_ID3) {
            addArtistId3IfAnyMatch.accept(dist.getArtists(), documentId);
        } else if (subjectIndexType == IndexType.ALBUM_ID3) {
            addAlbumId3IfAnyMatch.accept(dist.getAlbums(), documentId);
        }
    }

}
