package org.airsonic.restapi.domain;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Lyrics", propOrder = {
        "content"
})
public class Lyrics {

    @XmlValue
    protected String content;
    @XmlAttribute(name = "artist")
    protected String artist;
    @XmlAttribute(name = "title")
    protected String title;

    public String getContent() {
        return content;
    }

    public void setContent(String value) {
        this.content = value;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String value) {
        this.artist = value;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String value) {
        this.title = value;
    }

}
