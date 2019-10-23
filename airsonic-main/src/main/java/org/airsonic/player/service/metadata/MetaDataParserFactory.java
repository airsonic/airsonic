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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

/**
 * Factory for creating meta-data parsers.
 *
 * @author Sindre Mehus
 */
@Component
public class MetaDataParserFactory {

    private List<MetaDataParser> parsers;

    @Autowired
    public MetaDataParserFactory(List<MetaDataParser> parsers) {
        this.parsers = parsers;
    }

    /**
     * Returns a meta-data parser for the given file.
     *
     * @param file The file in question.
     * @return An applicable parser, or <code>null</code> if no parser is found.
     */
    public MetaDataParser getParser(File file) {
        for (MetaDataParser parser : parsers) {
            if (parser.isApplicable(file)) {
                return parser;
            }
        }
        return null;
    }
}
