package org.airsonic.restapi.domain;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Index", propOrder = {
        "artist"
})
public class Index {

    protected List<Artist> artist;
    @XmlAttribute(name = "name", required = true)
    protected String name;

    public List<Artist> getArtist() {
        if (artist == null) {
            artist = new ArrayList<>();
        }
        return this.artist;
    }

    public String getName() {
        return name;
    }

    public void setName(String value) {
        this.name = value;
    }

}
