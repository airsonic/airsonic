package org.airsonic.restapi.domain;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Videos", propOrder = {
        "video"
})
public class Videos {

    protected List<Child> video;

    public List<Child> getVideo() {
        if (video == null) {
            video = new ArrayList<>();
        }
        return this.video;
    }

}
