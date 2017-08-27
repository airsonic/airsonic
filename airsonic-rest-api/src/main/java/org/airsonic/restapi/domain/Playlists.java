package org.airsonic.restapi.domain;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Playlists", propOrder = {
        "playlist"
})
public class Playlists {

    protected List<Playlist> playlist;

    public List<Playlist> getPlaylist() {
        if (playlist == null) {
            playlist = new ArrayList<>();
        }
        return this.playlist;
    }

}
