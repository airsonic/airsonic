package org.airsonic.restapi.domain;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Indexes", propOrder = {
        "shortcut",
        "index",
        "child"
})
public class Indexes {

    protected List<Artist> shortcut;
    protected List<Index> index;
    protected List<Child> child;
    @XmlAttribute(name = "lastModified", required = true)
    protected long lastModified;
    @XmlAttribute(name = "ignoredArticles", required = true)
    protected String ignoredArticles;

    public List<Artist> getShortcut() {
        if (shortcut == null) {
            shortcut = new ArrayList<>();
        }
        return this.shortcut;
    }

    public List<Index> getIndex() {
        if (index == null) {
            index = new ArrayList<>();
        }
        return this.index;
    }

    public List<Child> getChild() {
        if (child == null) {
            child = new ArrayList<>();
        }
        return this.child;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long value) {
        this.lastModified = value;
    }

    public String getIgnoredArticles() {
        return ignoredArticles;
    }

    public void setIgnoredArticles(String value) {
        this.ignoredArticles = value;
    }

}
