package org.airsonic.restapi.domain;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Podcasts", propOrder = {
        "channel"
})
public class Podcasts {

    protected List<PodcastChannel> channel;

    public List<PodcastChannel> getChannel() {
        if (channel == null) {
            channel = new ArrayList<>();
        }
        return this.channel;
    }

}
