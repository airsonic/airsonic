package org.airsonic.restapi.domain;

import javax.xml.bind.annotation.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Share", propOrder = {
        "entry"
})
public class Share {

    protected List<Child> entry;
    @XmlAttribute(name = "id", required = true)
    protected String id;
    @XmlAttribute(name = "url", required = true)
    protected String url;
    @XmlAttribute(name = "description")
    protected String description;
    @XmlAttribute(name = "username", required = true)
    protected String username;
    @XmlAttribute(name = "created", required = true)
    @XmlSchemaType(name = "dateTime")
    protected Date created;
    @XmlAttribute(name = "expires")
    @XmlSchemaType(name = "dateTime")
    protected Date expires;
    @XmlAttribute(name = "lastVisited")
    @XmlSchemaType(name = "dateTime")
    protected Date lastVisited;
    @XmlAttribute(name = "visitCount", required = true)
    protected int visitCount;

    public List<Child> getEntry() {
        if (entry == null) {
            entry = new ArrayList<>();
        }
        return this.entry;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String value) {
        this.description = value;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String value) {
        this.username = value;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date value) {
        this.created = value;
    }

    public Date getExpires() {
        return expires;
    }

    public void setExpires(Date value) {
        this.expires = value;
    }

    public Date getLastVisited() {
        return lastVisited;
    }

    public void setLastVisited(Date value) {
        this.lastVisited = value;
    }

    public int getVisitCount() {
        return visitCount;
    }

    public void setVisitCount(int value) {
        this.visitCount = value;
    }

}
