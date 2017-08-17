package org.airsonic.restapi.domain;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ArtistInfo", propOrder = {
        "biography",
        "musicBrainzId",
        "lastFmUrl",
        "smallImageUrl",
        "mediumImageUrl",
        "largeImageUrl",
        "similarArtist"
})
public class ArtistInfo {

    protected String biography;
    protected String musicBrainzId;
    protected String lastFmUrl;
    protected String smallImageUrl;
    protected String mediumImageUrl;
    protected String largeImageUrl;
    protected List<ArtistID3> similarArtist;

    public List<ArtistID3> getSimilarArtist() {
        if (similarArtist == null) {
            similarArtist = new ArrayList<>();
        }
        return this.similarArtist;
    }

    public String getBiography() {
        return biography;
    }

    public void setBiography(String value) {
        this.biography = value;
    }

    public String getMusicBrainzId() {
        return musicBrainzId;
    }

    public void setMusicBrainzId(String value) {
        this.musicBrainzId = value;
    }

    public String getLastFmUrl() {
        return lastFmUrl;
    }

    public void setLastFmUrl(String value) {
        this.lastFmUrl = value;
    }

    public String getSmallImageUrl() {
        return smallImageUrl;
    }

    public void setSmallImageUrl(String value) {
        this.smallImageUrl = value;
    }

    public String getMediumImageUrl() {
        return mediumImageUrl;
    }

    public void setMediumImageUrl(String value) {
        this.mediumImageUrl = value;
    }

    public String getLargeImageUrl() {
        return largeImageUrl;
    }

    public void setLargeImageUrl(String value) {
        this.largeImageUrl = value;
    }
}
