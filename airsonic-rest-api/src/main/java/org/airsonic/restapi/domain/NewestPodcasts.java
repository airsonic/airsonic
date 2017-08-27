package org.airsonic.restapi.domain;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "NewestPodcasts", propOrder = {
        "episode"
})
public class NewestPodcasts {

    protected List<PodcastEpisode> episode;

    public List<PodcastEpisode> getEpisode() {
        if (episode == null) {
            episode = new ArrayList<>();
        }
        return this.episode;
    }

}
