package org.airsonic.restapi.domain;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import java.util.ArrayList;
import java.util.List;


@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ArtistsID3", propOrder = {
        "index"
})
public class ArtistsID3 {

    protected List<IndexID3> index;
    @XmlAttribute(name = "ignoredArticles", required = true)
    protected String ignoredArticles;

    public List<IndexID3> getIndex() {
        if (index == null) {
            index = new ArrayList<>();
        }
        return this.index;
    }

    public String getIgnoredArticles() {
        return ignoredArticles;
    }

    public void setIgnoredArticles(String value) {
        this.ignoredArticles = value;
    }

}
