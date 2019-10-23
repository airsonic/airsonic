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
package org.airsonic.player.service.metadata;

import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.service.SettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * Parses meta data by guessing artist, album and song title based on the path of the file.
 *
 * @author Sindre Mehus
 */
@Service
@Order(200)
public class DefaultMetaDataParser extends MetaDataParser {

    @Autowired
    private final SettingsService settingsService;

    public DefaultMetaDataParser(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    /**
     * Parses meta data for the given file.
     *
     * @param file The file to parse.
     * @return Meta data for the file.
     */
    public MetaData getRawMetaData(File file) {
        MetaData metaData = new MetaData();
        String artist = guessArtist(file);
        metaData.setArtist(artist);
        metaData.setAlbumArtist(artist);
        metaData.setAlbumName(guessAlbum(file, artist));
        metaData.setTitle(guessTitle(file));
        return metaData;
    }

    /**
     * Updates the given file with the given meta data.
     * This method has no effect.
     *
     * @param file     The file to update.
     * @param metaData The new meta data.
     */
    public void setMetaData(MediaFile file, MetaData metaData) {
    }

    /**
     * Returns whether this parser supports tag editing (using the {@link #setMetaData} method).
     *
     * @return Always false.
     */
    public boolean isEditingSupported() {
        return false;
    }

    @Override
    SettingsService getSettingsService() {
        return settingsService;
    }

    /**
     * Returns whether this parser is applicable to the given file.
     *
     * @param file The file in question.
     * @return Whether this parser is applicable to the given file.
     */
    public boolean isApplicable(File file) {
        return file.isFile();
    }
}