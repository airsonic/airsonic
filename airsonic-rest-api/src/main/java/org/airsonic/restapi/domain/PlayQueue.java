package org.airsonic.restapi.domain;

import javax.xml.bind.annotation.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PlayQueue", propOrder = {
        "entry"
})
public class PlayQueue {

    protected List<Child> entry;
    @XmlAttribute(name = "current")
    protected Integer current;
    @XmlAttribute(name = "position")
    protected Long position;
    @XmlAttribute(name = "username", required = true)
    protected String username;
    @XmlAttribute(name = "changed", required = true)
    @XmlSchemaType(name = "dateTime")
    protected Date changed;
    @XmlAttribute(name = "changedBy", required = true)
    protected String changedBy;

    public List<Child> getEntry() {
        if (entry == null) {
            entry = new ArrayList<>();
        }
        return this.entry;
    }

    public Integer getCurrent() {
        return current;
    }

    public void setCurrent(Integer value) {
        this.current = value;
    }

    public Long getPosition() {
        return position;
    }

    public void setPosition(Long value) {
        this.position = value;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String value) {
        this.username = value;
    }

    public Date getChanged() {
        return changed;
    }

    public void setChanged(Date value) {
        this.changed = value;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String value) {
        this.changedBy = value;
    }

}
