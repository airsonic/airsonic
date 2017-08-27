package org.airsonic.restapi.domain;

import javax.xml.bind.annotation.*;

import java.util.Date;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Artist")
public class Artist {

    @XmlAttribute(name = "id", required = true)
    protected String id;
    @XmlAttribute(name = "name", required = true)
    protected String name;
    @XmlAttribute(name = "starred")
    @XmlSchemaType(name = "dateTime")
    protected Date starred;
    @XmlAttribute(name = "userRating")
    protected Integer userRating;
    @XmlAttribute(name = "averageRating")
    protected Double averageRating;

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

    public Date getStarred() {
        return starred;
    }

    public void setStarred(Date value) {
        this.starred = value;
    }

    public Integer getUserRating() {
        return userRating;
    }

    public void setUserRating(Integer value) {
        this.userRating = value;
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double value) {
        this.averageRating = value;
    }

}
