package org.airsonic.restapi.domain;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PodcastChannel", propOrder = {
        "episode"
})
public class PodcastChannel {

    protected List<PodcastEpisode> episode;
    @XmlAttribute(name = "id", required = true)
    protected String id;
    @XmlAttribute(name = "url", required = true)
    protected String url;
    @XmlAttribute(name = "title")
    protected String title;
    @XmlAttribute(name = "description")
    protected String description;
    @XmlAttribute(name = "coverArt")
    protected String coverArt;
    @XmlAttribute(name = "originalImageUrl")
    protected String originalImageUrl;
    @XmlAttribute(name = "status", required = true)
    protected PodcastStatus status;
    @XmlAttribute(name = "errorMessage")
    protected String errorMessage;

    public List<PodcastEpisode> getEpisode() {
        if (episode == null) {
            episode = new ArrayList<>();
        }
        return this.episode;
    }

    public String getId() {
        return id;
    }

    public void setId(String value) {
        this.id = value;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String value) {
        this.url = value;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String value) {
        this.title = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String value) {
        this.description = value;
    }

    public String getCoverArt() {
        return coverArt;
    }

    public void setCoverArt(String value) {
        this.coverArt = value;
    }

    public String getOriginalImageUrl() {
        return originalImageUrl;
    }

    public void setOriginalImageUrl(String value) {
        this.originalImageUrl = value;
    }

    public PodcastStatus getStatus() {
        return status;
    }

    public void setStatus(PodcastStatus value) {
        this.status = value;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String value) {
        this.errorMessage = value;
    }

}
