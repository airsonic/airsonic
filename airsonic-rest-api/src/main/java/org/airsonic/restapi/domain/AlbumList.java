package org.airsonic.restapi.domain;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AlbumList", propOrder = {
        "album"
})
public class AlbumList {

    protected List<AlbumID3> album;

    public List<AlbumID3> getAlbum() {
        if (album == null) {
            album = new ArrayList<>();
        }
        return this.album;
    }

}
