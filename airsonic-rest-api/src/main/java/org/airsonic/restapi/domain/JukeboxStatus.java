package org.airsonic.restapi.domain;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "JukeboxStatus")
@XmlSeeAlso({
        JukeboxPlaylist.class
})
public class JukeboxStatus {

    @XmlAttribute(name = "currentIndex", required = true)
    protected int currentIndex;
    @XmlAttribute(name = "playing", required = true)
    protected boolean playing;
    @XmlAttribute(name = "gain", required = true)
    protected float gain;
    @XmlAttribute(name = "position")
    protected Integer position;

    public int getCurrentIndex() {
        return currentIndex;
    }

    public void setCurrentIndex(int value) {
        this.currentIndex = value;
    }

    public boolean isPlaying() {
        return playing;
    }

    public void setPlaying(boolean value) {
        this.playing = value;
    }

    public float getGain() {
        return gain;
    }

    public void setGain(float value) {
        this.gain = value;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer value) {
        this.position = value;
    }

}
