package org.airsonic.restapi.domain;

import javax.xml.bind.annotation.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Playlist", propOrder = {
        "allowedUser"
})
@XmlSeeAlso({
        PlaylistWithSongs.class
})
public class Playlist {

    protected List<String> allowedUser;
    @XmlAttribute(name = "id", required = true)
    protected String id;
    @XmlAttribute(name = "name", required = true)
    protected String name;
    @XmlAttribute(name = "comment")
    protected String comment;
    @XmlAttribute(name = "owner")
    protected String owner;
    @XmlAttribute(name = "public")
    protected Boolean _public;
    @XmlAttribute(name = "songCount", required = true)
    protected int songCount;
    @XmlAttribute(name = "duration", required = true)
    protected int duration;
    @XmlAttribute(name = "created", required = true)
    @XmlSchemaType(name = "dateTime")
    protected Date created;
    @XmlAttribute(name = "changed", required = true)
    @XmlSchemaType(name = "dateTime")
    protected Date changed;
    @XmlAttribute(name = "coverArt")
    protected String coverArt;

    public List<String> getAllowedUser() {
        if (allowedUser == null) {
            allowedUser = new ArrayList<>();
        }
        return this.allowedUser;
    }

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

    public String getComment() {
        return comment;
    }

    public void setComment(String value) {
        this.comment = value;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String value) {
        this.owner = value;
    }

    public Boolean isPublic() {
        return _public;
    }

    public void setPublic(Boolean value) {
        this._public = value;
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

    public String getCoverArt() {
        return coverArt;
    }

    public void setCoverArt(String value) {
        this.coverArt = value;
    }

}
