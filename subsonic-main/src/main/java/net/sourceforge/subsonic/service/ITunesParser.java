/*
 * This file is part of Subsonic.
 *
 *  Subsonic is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Subsonic is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Subsonic.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Copyright 2015 (C) Sindre Mehus
 */

package net.sourceforge.subsonic.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.io.IOUtils;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import net.sourceforge.subsonic.util.StringUtil;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
public class ITunesParser {

    private String iTunesXml;
    private final XMLInputFactory inputFactory;

    public ITunesParser(String iTunesXml) {
        this.iTunesXml = iTunesXml;
        inputFactory = XMLInputFactory.newFactory();
        inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    }

    public List<ITunesPlaylist> parse() throws Exception {
        List<ITunesPlaylist> playlists = parsePlaylists();
        Map<String, File> tracks = parseTracks(playlists);
        populatePlaylistTracks(playlists, tracks);

        for (ITunesPlaylist p : playlists) {
            System.out.println(p);
        }

        return playlists;
    }

    private List<ITunesPlaylist> parsePlaylists() throws Exception {
        List<ITunesPlaylist> playlists = new ArrayList<ITunesPlaylist>();

        InputStream in = new FileInputStream(iTunesXml);
        try {
            ITunesPlaylist playlist = null;

            XMLStreamReader streamReader = inputFactory.createXMLStreamReader(in);
            while (streamReader.hasNext()) {
                int code = streamReader.next();
                if (code == XMLStreamReader.START_ELEMENT) {
                    String key = readKey(streamReader);

                    if ("Playlist ID".equals(key)) {
                        playlist = new ITunesPlaylist(readNextTag(streamReader));
                        playlists.add(playlist);
                    }

                    if (playlist != null) {
                        if ("Name".equals(key)) {
                            playlist.name = readNextTag(streamReader);
                        } else if ("Smart Info".equals(key)) {
                            playlist.smart = true;
                        } else if ("Visible".equals(key)) {
                            playlist.visible = false;
                        } else if ("Distinguished Kind".equals(key)) {
                            playlist.distinguishedKind = readNextTag(streamReader);
                        } else if ("Track ID".equals(key)) {
                            playlist.trackIds.add(readNextTag(streamReader));
                        }
                    }
                }
            }
        } finally {
            IOUtils.closeQuietly(in);
        }

        return Lists.newArrayList(Iterables.filter(playlists, new Predicate<ITunesPlaylist>() {
            @Override
            public boolean apply(ITunesPlaylist input) {
                return input.isIncluded();
            }
        }));
    }

    private Map<String, File> parseTracks(List<ITunesPlaylist> playlists) throws Exception {
        Map<String, File> result = new HashMap<String, File>();
        SortedSet<String> trackIds = new TreeSet<String>();
        for (ITunesPlaylist playlist : playlists) {
            trackIds.addAll(playlist.trackIds);
        }

        InputStream in = new FileInputStream(iTunesXml);

        try {
            XMLStreamReader streamReader = inputFactory.createXMLStreamReader(in);
            String trackId = null;
            while (streamReader.hasNext()) {
                int code = streamReader.next();
                if (code == XMLStreamReader.START_ELEMENT) {
                    String key = readKey(streamReader);
                    if ("Track ID".equals(key)) {
                        trackId = readNextTag(streamReader);
                    } else if (trackId != null && trackIds.contains(trackId) && "Location".equals(key)) {
                        String location = readNextTag(streamReader);
                        File file = new File(StringUtil.urlDecode(new URL(location).getFile()));
                        result.put(trackId, file);
                    }
                }
            }
        } finally {
            IOUtils.closeQuietly(in);
        }
        return result;
    }

    private void populatePlaylistTracks(List<ITunesPlaylist> playlists, Map<String, File> tracks) {
        for (ITunesPlaylist playlist : playlists) {
            for (String trackId : playlist.trackIds) {
                File file = tracks.get(trackId);
                if (file != null) {
                    playlist.trackFiles.add(file);
                }
            }
        }
    }

    private String readNextTag(XMLStreamReader streamReader) throws XMLStreamException {
        while (streamReader.next() != XMLStreamConstants.START_ELEMENT) {
        }
        return streamReader.getElementText();
    }

    private String readKey(XMLStreamReader streamReader) {
        try {
            if (streamReader.getEventType() == XMLStreamConstants.START_ELEMENT &&
                "key".equals(streamReader.getName().getLocalPart())) {
                return streamReader.getElementText();
            }
        } catch (XMLStreamException e) {
            // TODO
            System.out.println(streamReader.getName().getLocalPart() + " " + e);
        }
        return null;
    }

    // TODO
    public static void main(String[] args) throws Exception {
        new ITunesParser("/Users/sindre/Music/iTunes/iTunes Music Library.xml").parse();
    }

    private static class ITunesPlaylist {

        private final String id;
        private String name;
        private boolean smart;
        private final List<String> trackIds = new ArrayList<String>();
        private final List<File> trackFiles = new ArrayList<File>();
        private boolean visible = true;
        private String distinguishedKind;

        public ITunesPlaylist(String id) {
            this.id = id;
        }

        public boolean isIncluded() {
            return !smart && visible && distinguishedKind == null;
        }

        @Override
        public String toString() {
            StringBuilder s = new StringBuilder(id + " - " + name );
            for (File trackFile : trackFiles) {
                s.append("\n  " + trackFile);
            }
            return s.toString();
        }
    }
}
