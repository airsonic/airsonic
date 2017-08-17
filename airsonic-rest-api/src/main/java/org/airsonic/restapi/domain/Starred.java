package org.airsonic.restapi.domain;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Starred", propOrder = {
        "artist",
        "album",
        "song"
})
public class Starred {

    protected List<ArtistID3> artist;
    protected List<AlbumID3> album;
    protected List<Child> song;

    public List<ArtistID3> getArtist() {
        if (artist == null) {
            artist = new ArrayList<>();
        }
        return this.artist;
    }

    public List<AlbumID3> getAlbum() {
        if (album == null) {
            album = new ArrayList<>();
        }
        return this.album;
    }

    public List<Child> getSong() {
        if (song == null) {
            song = new ArrayList<>();
        }
        return this.song;
    }

}
