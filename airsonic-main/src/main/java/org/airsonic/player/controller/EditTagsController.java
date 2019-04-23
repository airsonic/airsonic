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
package org.airsonic.player.controller;

import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.service.MediaFileService;
import org.airsonic.player.service.metadata.JaudiotaggerParser;
import org.airsonic.player.service.metadata.MetaDataParser;
import org.airsonic.player.service.metadata.MetaDataParserFactory;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for the page used to edit MP3 tags.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/editTags")
public class EditTagsController  {

    @Autowired
    private MetaDataParserFactory metaDataParserFactory;
    @Autowired
    private MediaFileService mediaFileService;

    @GetMapping
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {

        int id = ServletRequestUtils.getRequiredIntParameter(request, "id");
        MediaFile dir = mediaFileService.getMediaFile(id);
        List<MediaFile> files = mediaFileService.getChildrenOf(dir, true, false, true, false);

        Map<String, Object> map = new HashMap<String, Object>();
        if (!files.isEmpty()) {
            map.put("defaultArtist", files.get(0).getArtist());
            map.put("defaultAlbum", files.get(0).getAlbumName());
            map.put("defaultYear", files.get(0).getYear());
            map.put("defaultGenre", files.get(0).getGenre());
        }
        map.put("allGenres", JaudiotaggerParser.getID3V1Genres());

        List<Song> songs = new ArrayList<Song>();
        for (int i = 0; i < files.size(); i++) {
            songs.add(createSong(files.get(i), i));
        }
        map.put("id", id);
        map.put("songs", songs);

        return new ModelAndView("editTags","model",map);
    }

    private Song createSong(MediaFile file, int index) {
        MetaDataParser parser = metaDataParserFactory.getParser(file.getFile());

        Song song = new Song();
        song.setId(file.getId());
        song.setFileName(FilenameUtils.getBaseName(file.getPath()));
        song.setTrack(file.getTrackNumber());
        song.setSuggestedTrack(index + 1);
        song.setTitle(file.getTitle());
        song.setSuggestedTitle(parser.guessTitle(file.getFile()));
        song.setArtist(file.getArtist());
        song.setAlbum(file.getAlbumName());
        song.setYear(file.getYear());
        song.setGenre(file.getGenre());
        return song;
    }


    /**
     * Contains information about a single song.
     */
    public static class Song {
        private int id;
        private String fileName;
        private Integer suggestedTrack;
        private Integer track;
        private String suggestedTitle;
        private String title;
        private String artist;
        private String album;
        private Integer year;
        private String genre;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public Integer getSuggestedTrack() {
            return suggestedTrack;
        }

        public void setSuggestedTrack(Integer suggestedTrack) {
            this.suggestedTrack = suggestedTrack;
        }

        public Integer getTrack() {
            return track;
        }

        public void setTrack(Integer track) {
            this.track = track;
        }

        public String getSuggestedTitle() {
            return suggestedTitle;
        }

        public void setSuggestedTitle(String suggestedTitle) {
            this.suggestedTitle = suggestedTitle;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getArtist() {
            return artist;
        }

        public void setArtist(String artist) {
            this.artist = artist;
        }

        public String getAlbum() {
            return album;
        }

        public void setAlbum(String album) {
            this.album = album;
        }

        public Integer getYear() {
            return year;
        }

        public void setYear(Integer year) {
            this.year = year;
        }

        public String getGenre() {
            return genre;
        }

        public void setGenre(String genre) {
            this.genre = genre;
        }
    }
}
