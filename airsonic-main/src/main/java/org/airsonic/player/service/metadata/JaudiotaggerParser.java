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
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.KeyNotFoundException;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;
import org.jaudiotagger.tag.reference.GenreTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.LogManager;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses meta data from audio files using the Jaudiotagger library
 * (http://www.jthink.net/jaudiotagger/)
 *
 * @author Sindre Mehus
 */
@Service
@Order(0)
public class JaudiotaggerParser extends MetaDataParser {

    private static final Logger LOG = LoggerFactory.getLogger(JaudiotaggerParser.class);
    private static final Pattern GENRE_PATTERN = Pattern.compile("\\((\\d+)\\).*");
    private static final Pattern TRACK_NUMBER_PATTERN = Pattern.compile("(\\d+)/\\d+");
    private static final Pattern YEAR_NUMBER_PATTERN = Pattern.compile("(\\d{4}).*");
    @Autowired
    private final SettingsService settingsService;

    public JaudiotaggerParser(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    static {
        try {
            LogManager.getLogManager().reset();
        } catch (SecurityException x) {
            LOG.warn("Failed to turn off logging from Jaudiotagger.", x);
        }
    }

    /**
     * Parses meta data for the given music file. No guessing or reformatting is done.
     *
     * @param file The music file to parse.
     * @return Meta data for the file.
     */
    @Override
    public MetaData getRawMetaData(File file) {

        MetaData metaData = new MetaData();
        AudioFile audioFile;
        try {
            audioFile = AudioFileIO.read(file);
        } catch (Throwable x) {
            LOG.warn("Error when parsing tags in " + file, x);
            return metaData;
        }

        Tag tag = audioFile.getTag();
        if (tag != null) {
            metaData.setAlbumArtist(getTagField(tag, FieldKey.ALBUM_ARTIST));
            metaData.setAlbumName(getTagField(tag, FieldKey.ALBUM));
            metaData.setArtist(getTagField(tag, FieldKey.ARTIST));
            metaData.setDiscNumber(parseInteger(getTagField(tag, FieldKey.DISC_NO)));
            metaData.setGenre(mapGenre(getTagField(tag, FieldKey.GENRE)));
            metaData.setMusicBrainzRecordingId(getTagField(tag, FieldKey.MUSICBRAINZ_TRACK_ID));
            metaData.setMusicBrainzReleaseId(getTagField(tag, FieldKey.MUSICBRAINZ_RELEASEID));
            metaData.setTitle(getTagField(tag, FieldKey.TITLE));
            metaData.setTrackNumber(parseIntegerPattern(getTagField(tag, FieldKey.TRACK), TRACK_NUMBER_PATTERN));
            metaData.setYear(parseIntegerPattern(getTagField(tag, FieldKey.YEAR), YEAR_NUMBER_PATTERN));

            if (StringUtils.isBlank(metaData.getArtist())) {
                metaData.setArtist(metaData.getAlbumArtist());
            }
            if (StringUtils.isBlank(metaData.getAlbumArtist())) {
                metaData.setAlbumArtist(metaData.getArtist());
            }

        }

        AudioHeader audioHeader = audioFile.getAudioHeader();
        if (audioHeader != null) {
            metaData.setVariableBitRate(audioHeader.isVariableBitRate());
            metaData.setBitRate((int) audioHeader.getBitRateAsNumber());
            metaData.setDurationSeconds(audioHeader.getTrackLength());
        }

        return metaData;
    }

    private static String getTagField(Tag tag, FieldKey fieldKey) {
        try {
            return StringUtils.trimToNull(tag.getFirst(fieldKey));
        } catch (KeyNotFoundException x) {
            // Ignored.
            return null;
        }
    }

    /**
     * Returns all tags supported by id3v1.
     */
    public static SortedSet<String> getID3V1Genres() {
        return new TreeSet<>(GenreTypes.getInstanceOf().getAlphabeticalValueList());
    }

    /**
     * Sometimes the genre is returned as "(17)" or "(17)Rock", instead of "Rock".  This method
     * maps the genre ID to the corresponding text.
     */
    private static String mapGenre(String genre) {
        if (genre == null) {
            return null;
        }
        Matcher matcher = GENRE_PATTERN.matcher(genre);
        if (matcher.matches()) {
            int genreId = Integer.parseInt(matcher.group(1));
            if (genreId >= 0 && genreId < GenreTypes.getInstanceOf().getSize()) {
                return GenreTypes.getInstanceOf().getValueForId(genreId);
            }
        }
        return genre;
    }

    private static Integer parseIntegerPattern(String str, Pattern pattern) {
        if (str == null) {
            return null;
        }

        Integer result = null;

        try {
            result = Integer.valueOf(str);
        } catch (NumberFormatException x) {
            if (pattern == null) {
                return null;
            }
            Matcher matcher = pattern.matcher(str);
            if (matcher.matches()) {
                try {
                    result = Integer.valueOf(matcher.group(1));
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }

        if (Integer.valueOf(0).equals(result)) {
            return null;
        }
        return result;
    }

    private static Integer parseInteger(String s) {
        return parseIntegerPattern(StringUtils.trimToNull(s), null);
    }

    /**
     * Updates the given file with the given meta data.
     *
     * @param file     The music file to update.
     * @param metaData The new meta data.
     */
    @Override
    public void setMetaData(MediaFile file, MetaData metaData) {

        try {
            AudioFile audioFile = AudioFileIO.read(file.getFile());
            Tag tag = audioFile.getTagOrCreateAndSetDefault();

            tag.setField(FieldKey.ARTIST, StringUtils.trimToEmpty(metaData.getArtist()));
            tag.setField(FieldKey.ALBUM, StringUtils.trimToEmpty(metaData.getAlbumName()));
            tag.setField(FieldKey.TITLE, StringUtils.trimToEmpty(metaData.getTitle()));
            tag.setField(FieldKey.GENRE, StringUtils.trimToEmpty(metaData.getGenre()));
            try {
                tag.setField(FieldKey.ALBUM_ARTIST, StringUtils.trimToEmpty(metaData.getAlbumArtist()));
            } catch (Exception x) {
                // Silently ignored. ID3v1 doesn't support album artist.
            }

            Integer track = metaData.getTrackNumber();
            if (track == null) {
                tag.deleteField(FieldKey.TRACK);
            } else {
                tag.setField(FieldKey.TRACK, String.valueOf(track));
            }

            Integer year = metaData.getYear();
            if (year == null) {
                tag.deleteField(FieldKey.YEAR);
            } else {
                tag.setField(FieldKey.YEAR, String.valueOf(year));
            }

            audioFile.commit();

        } catch (Throwable x) {
            LOG.warn("Failed to update tags for file " + file, x);
            throw new RuntimeException("Failed to update tags for file " + file + ". " + x.getMessage(), x);
        }
    }

    /**
     * Returns whether this parser supports tag editing (using the {@link #setMetaData} method).
     *
     * @return Always true.
     */
    @Override
    public boolean isEditingSupported() {
        return true;
    }

    @Override
    SettingsService getSettingsService() {
        return settingsService;
    }

    /**
     * Returns whether this parser is applicable to the given file.
     *
     * @param file The music file in question.
     * @return Whether this parser is applicable to the given file.
     */
    @Override
    public boolean isApplicable(File file) {
        if (!file.isFile()) {
            return false;
        }

        String format = FilenameUtils.getExtension(file.getName()).toLowerCase();

        return "mp3".equals(format) ||
               "m4a".equals(format) ||
               "m4b".equals(format) ||
               "aac".equals(format) ||
               "ogg".equals(format) ||
               "flac".equals(format) ||
               "wav".equals(format) ||
               "mpc".equals(format) ||
               "mp+".equals(format) ||
               "ape".equals(format) ||
               "wma".equals(format);
    }

    public static Artwork getArtwork(MediaFile file) {
        AudioFile audioFile;
        try {
            audioFile = AudioFileIO.read(file.getFile());
        } catch (Exception e) {
            LOG.info("Failed to find cover art tag in " + file, e);
            return null;
        }
        Tag tag = audioFile.getTag();
        return tag == null ? null : tag.getFirstArtwork();
    }
}