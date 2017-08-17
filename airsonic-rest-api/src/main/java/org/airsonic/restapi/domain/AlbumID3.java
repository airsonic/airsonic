package org.airsonic.restapi.domain;

import javax.xml.bind.annotation.*;

import java.util.Date;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AlbumID3")
@XmlSeeAlso({
        AlbumWithSongsID3.class
})
public class AlbumID3 {

    @XmlAttribute(name = "id", required = true)
    protected String id;
    @XmlAttribute(name = "name", required = true)
    protected String name;
    @XmlAttribute(name = "artist")
    protected String artist;
    @XmlAttribute(name = "artistId")
    protected String artistId;
    @XmlAttribute(name = "coverArt")
    protected String coverArt;
    @XmlAttribute(name = "songCount", required = true)
    protected int songCount;
    @XmlAttribute(name = "duration", required = true)
    protected int duration;
    @XmlAttribute(name = "playCount")
    protected Long playCount;
    @XmlAttribute(name = "created", required = true)
    @XmlSchemaType(name = "dateTime")
    protected Date created;
    @XmlAttribute(name = "starred")
    @XmlSchemaType(name = "dateTime")
    protected Date starred;
    @XmlAttribute(name = "year")
    protected Integer year;
    @XmlAttribute(name = "genre")
    protected String genre;

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

    public String getArtist() {
        return artist;
    }

    public void setArtist(String value) {
        this.artist = value;
    }

    public String getArtistId() {
        return artistId;
    }

    public void setArtistId(String value) {
        this.artistId = value;
    }

    public String getCoverArt() {
        return coverArt;
    }

    public void setCoverArt(String value) {
        this.coverArt = value;
    }

    public int getSongCount() {
        return songCount;
    }

    public void setSongCount(int value) {
        this.songCount = value;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int value) {
        this.duration = value;
    }

    public Long getPlayCount() {
        return playCount;
    }

    public void setPlayCount(Long value) {
        this.playCount = value;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date value) {
        this.created = value;
    }

    public Date getStarred() {
        return starred;
    }

    public void setStarred(Date value) {
        this.starred = value;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer value) {
        this.year = value;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String value) {
        this.genre = value;
    }

}
