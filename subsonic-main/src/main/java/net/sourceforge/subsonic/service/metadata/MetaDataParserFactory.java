/*
 This file is part of Subsonic.

 Subsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Subsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Subsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2009 (C) Sindre Mehus
 */
package net.sourceforge.subsonic.service.metadata;

import java.io.File;
import java.util.List;

/**
 * Factory for creating meta-data parsers.
 *
 * @author Sindre Mehus
 */
public class MetaDataParserFactory {

    private List<MetaDataParser> parsers;

    public void setParsers(List<MetaDataParser> parsers) {
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
