package org.airsonic.restapi.domain;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Genres", propOrder = {
        "genre"
})
public class Genres {

    protected List<Genre> genre;

    public List<Genre> getGenre() {
        if (genre == null) {
            genre = new ArrayList<>();
        }
        return this.genre;
    }

}
