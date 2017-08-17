package org.airsonic.restapi.domain;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AlbumInfo", propOrder = {
        "notes",
        "musicBrainzId",
        "lastFmUrl",
        "smallImageUrl",
        "mediumImageUrl",
        "largeImageUrl"
})
public class AlbumInfo {

    protected String notes;
    protected String musicBrainzId;
    protected String lastFmUrl;
    protected String smallImageUrl;
    protected String mediumImageUrl;
    protected String largeImageUrl;

    public String getNotes() {
        return notes;
    }

    public void setNotes(String value) {
        this.notes = value;
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
