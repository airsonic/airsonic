package org.airsonic.restapi.domain;

import javax.xml.bind.annotation.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Directory", propOrder = {
        "child"
})
public class Directory {

    protected List<Child> child;
    @XmlAttribute(name = "id", required = true)
    protected String id;
    @XmlAttribute(name = "parent")
    protected String parent;
    @XmlAttribute(name = "name", required = true)
    protected String name;
    @XmlAttribute(name = "starred")
    @XmlSchemaType(name = "dateTime")
    protected Date starred;
    @XmlAttribute(name = "userRating")
    protected Integer userRating;
    @XmlAttribute(name = "averageRating")
    protected Double averageRating;
    @XmlAttribute(name = "playCount")
    protected Long playCount;

    public List<Child> getChild() {
        if (child == null) {
            child = new ArrayList<>();
        }
        return this.child;
    }

    public String getId() {
        return id;
    }

    public void setId(String value) {
        this.id = value;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String value) {
        this.parent = value;
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

    public Long getPlayCount() {
        return playCount;
    }

    public void setPlayCount(Long value) {
        this.playCount = value;
    }

}
