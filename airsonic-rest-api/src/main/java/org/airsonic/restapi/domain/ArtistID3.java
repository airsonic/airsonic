package org.airsonic.restapi.domain;

import javax.xml.bind.annotation.*;

import java.util.Date;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ArtistID3")
@XmlSeeAlso({
        ArtistWithAlbumsID3.class
})
public class ArtistID3 {

    @XmlAttribute(name = "id", required = true)
    protected String id;
    @XmlAttribute(name = "name", required = true)
    protected String name;
    @XmlAttribute(name = "coverArt")
    protected String coverArt;
    @XmlAttribute(name = "albumCount", required = true)
    protected int albumCount;
    @XmlAttribute(name = "starred")
    @XmlSchemaType(name = "dateTime")
    protected Date starred;

    public String getId() {
        return id;
    }

    public void setId(String value) {
        this.id = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String value) {
        this.name = value;
    }

    public String getCoverArt() {
        return coverArt;
    }

    public void setCoverArt(String value) {
        this.coverArt = value;
    }

    public int getAlbumCount() {
        return albumCount;
    }

    public void setAlbumCount(int value) {
        this.albumCount = value;
    }

    public Date getStarred() {
        return starred;
    }

    public void setStarred(Date value) {
        this.starred = value;
    }

}
