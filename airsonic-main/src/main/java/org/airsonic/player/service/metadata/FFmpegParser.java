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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.service.TranscodingService;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Parses meta data from video files using FFmpeg (http://ffmpeg.org/).
 * <p/>
 * Currently duration, bitrate and dimension are supported.
 *
 * @author Sindre Mehus
 */
@Service("ffmpegParser")
public class FFmpegParser extends MetaDataParser {

    private static final Logger LOG = LoggerFactory.getLogger(FFmpegParser.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String[] FFPROBE_OPTIONS = {
        "-v", "quiet", "-print_format", "json", "-show_format", "-show_streams"
    };

    @Autowired
    private TranscodingService transcodingService;
    @Autowired
    private SettingsService settingsService;

    /**
     * Parses meta data for the given music file. No guessing or reformatting is done.
     *
     *
     * @param file The music file to parse.
     * @return Meta data for the file.
     */
    @Override
    public MetaData getRawMetaData(File file) {

        MetaData metaData = new MetaData();

        try {
            // Use `ffprobe` in the transcode directory if it exists, otherwise let the system sort it out.
            String ffprobe;
            File inTranscodeDirectory = new File(transcodingService.getTranscodeDirectory(), "ffprobe");
            if (inTranscodeDirectory.exists()) {
                ffprobe = inTranscodeDirectory.getAbsolutePath();
            } else {
                ffprobe = "ffprobe";
            }

            List<String> command = new ArrayList<>();
            command.add(ffprobe);
            command.addAll(Arrays.asList(FFPROBE_OPTIONS));
            command.add(file.getAbsolutePath());

            Process process = Runtime.getRuntime().exec(command.toArray(new String[0]));
            final JsonNode result = objectMapper.readTree(process.getInputStream());

            metaData.setDurationSeconds(result.at("/format/duration").asInt());
            // Bitrate is in Kb/s
            metaData.setBitRate(result.at("/format/bit_rate").asInt() / 1000);

            // Find the first (if any) stream that has dimensions and use those.
            // 'width' and 'height' are display dimensions; compare to 'coded_width', 'coded_height'.
            for (JsonNode stream : result.at("/streams")) {
                if (stream.has("width") && stream.has("height")) {
                    metaData.setWidth(stream.get("width").asInt());
                    metaData.setHeight(stream.get("height").asInt());
                    break;
                }
            }
        } catch (Throwable x) {
            LOG.warn("Error when parsing metadata in " + file, x);
        }

        return metaData;
    }

    /**
     * Not supported.
     */
    @Override
    public void setMetaData(MediaFile file, MetaData metaData) {
        throw new RuntimeException("setMetaData() not supported in " + getClass().getSimpleName());
    }

    /**
     * Returns whether this parser supports tag editing (using the {@link #setMetaData} method).
     *
     * @return Always false.
     */
    @Override
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
    @Override
    public boolean isApplicable(File file) {
        String format = FilenameUtils.getExtension(file.getName()).toLowerCase();

        for (String s : settingsService.getVideoFileTypesAsArray()) {
            if (format.equals(s)) {
                return true;
            }
        }
        return false;
    }

    public void setTranscodingService(TranscodingService transcodingService) {
        this.transcodingService = transcodingService;
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }
}
