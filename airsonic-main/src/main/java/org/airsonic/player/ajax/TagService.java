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
package org.airsonic.player.ajax;

import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.service.MediaFileService;
import org.airsonic.player.service.metadata.MetaData;
import org.airsonic.player.service.metadata.MetaDataParser;
import org.airsonic.player.service.metadata.MetaDataParserFactory;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Provides AJAX-enabled services for editing tags in music files.
 * This class is used by the DWR framework (http://getahead.ltd.uk/dwr/).
 *
 * @author Sindre Mehus
 */
@Service("ajaxTagService")
public class TagService {

    private static final Logger LOG = LoggerFactory.getLogger(TagService.class);

    @Autowired
    private MetaDataParserFactory metaDataParserFactory;
    @Autowired
    private MediaFileService mediaFileService;

    /**
     * Updated tags for a given music file.
     *
     * @param id     The ID of the music file.
     * @param track  The track number.
     * @param artist The artist name.
     * @param album  The album name.
     * @param title  The song title.
     * @param year   The release year.
     * @param genre  The musical genre.
     * @return "UPDATED" if the new tags were updated, "SKIPPED" if no update was necessary.
     *         Otherwise the error message is returned.
     */
    public String setTags(int id, String track, String artist, String album, String title, String year, String genre) {

        track = StringUtils.trimToNull(track);
        artist = StringUtils.trimToNull(artist);
        album = StringUtils.trimToNull(album);
        title = StringUtils.trimToNull(title);
        year = StringUtils.trimToNull(year);
        genre = StringUtils.trimToNull(genre);

        Integer trackNumber = null;
        if (track != null) {
            try {
                trackNumber = Integer.valueOf(track);
            } catch (NumberFormatException x) {
                LOG.warn("Illegal track number: " + track, x);
            }
        }

        Integer yearNumber = null;
        if (year != null) {
            try {
                yearNumber = Integer.valueOf(year);
            } catch (NumberFormatException x) {
                LOG.warn("Illegal year: " + year, x);
            }
        }

        try {

            MediaFile file = mediaFileService.getMediaFile(id);
            MetaDataParser parser = metaDataParserFactory.getParser(file.getFile());

            if (!parser.isEditingSupported()) {
                return "Tag editing of " + FilenameUtils.getExtension(file.getPath()) + " files is not supported.";
            }

            if (StringUtils.equals(artist, file.getArtist()) &&
                    StringUtils.equals(album, file.getAlbumName()) &&
                    StringUtils.equals(title, file.getTitle()) &&
                    ObjectUtils.equals(yearNumber, file.getYear()) &&
                    StringUtils.equals(genre, file.getGenre()) &&
                    ObjectUtils.equals(trackNumber, file.getTrackNumber())) {
                return "SKIPPED";
            }

            MetaData newMetaData = parser.getMetaData(file.getFile());

            // Note: album artist is intentionally set, as it is not user-changeable.
            newMetaData.setArtist(artist);
            newMetaData.setAlbumName(album);
            newMetaData.setTitle(title);
            newMetaData.setYear(yearNumber);
            newMetaData.setGenre(genre);
            newMetaData.setTrackNumber(trackNumber);
            parser.setMetaData(file, newMetaData);
            mediaFileService.refreshMediaFile(file);
            mediaFileService.refreshMediaFile(mediaFileService.getParentOf(file));
            return "UPDATED";

        } catch (Exception x) {
            LOG.warn("Failed to update tags for " + id, x);
            return x.getMessage();
        }
    }

    public void setMediaFileService(MediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
    }

    public void setMetaDataParserFactory(MetaDataParserFactory metaDataParserFactory) {
        this.metaDataParserFactory = metaDataParserFactory;
    }
}
