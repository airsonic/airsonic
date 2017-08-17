package org.airsonic.restapi.domain;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Bookmarks", propOrder = {
        "bookmark"
})
public class Bookmarks {

    protected List<Bookmark> bookmark;

    public List<Bookmark> getBookmark() {
        if (bookmark == null) {
            bookmark = new ArrayList<>();
        }
        return this.bookmark;
    }

}
