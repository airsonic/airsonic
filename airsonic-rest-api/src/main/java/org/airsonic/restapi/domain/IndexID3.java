package org.airsonic.restapi.domain;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "IndexID3", propOrder = {
        "artist"
})
public class IndexID3 {

    protected List<ArtistID3> artist;
    @XmlAttribute(name = "name", required = true)
    protected String name;

    public List<ArtistID3> getArtist() {
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
