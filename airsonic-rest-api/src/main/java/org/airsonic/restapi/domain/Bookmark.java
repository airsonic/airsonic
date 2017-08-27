package org.airsonic.restapi.domain;

import javax.xml.bind.annotation.*;

import java.util.Date;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Bookmark", propOrder = {
        "entry"
})
public class Bookmark {

    @XmlElement(required = true)
    protected Child entry;
    @XmlAttribute(name = "position", required = true)
    protected long position;
    @XmlAttribute(name = "username", required = true)
    protected String username;
    @XmlAttribute(name = "comment")
    protected String comment;
    @XmlAttribute(name = "created", required = true)
    @XmlSchemaType(name = "dateTime")
    protected Date created;
    @XmlAttribute(name = "changed", required = true)
    @XmlSchemaType(name = "dateTime")
    protected Date changed;

    public Child getEntry() {
        return entry;
    }

    public void setEntry(Child value) {
        this.entry = value;
    }

    public long getPosition() {
        return position;
    }

    public void setPosition(long value) {
        this.position = value;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String value) {
        this.username = value;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String value) {
        this.comment = value;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date value) {
        this.created = value;
    }

    public Date getChanged() {
        return changed;
    }

    public void setChanged(Date value) {
        this.changed = value;
    }

}
