package org.airsonic.restapi.domain;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Genre", propOrder = {
        "content"
})
public class Genre {

    @XmlValue
    protected String content;
    @XmlAttribute(name = "songCount", required = true)
    protected int songCount;
    @XmlAttribute(name = "albumCount", required = true)
    protected int albumCount;

    public String getContent() {
        return content;
    }

    public void setContent(String value) {
        this.content = value;
    }

    public int getSongCount() {
        return songCount;
    }

    public void setSongCount(int value) {
        this.songCount = value;
    }

    public int getAlbumCount() {
        return albumCount;
    }

    public void setAlbumCount(int value) {
        this.albumCount = value;
    }

}
