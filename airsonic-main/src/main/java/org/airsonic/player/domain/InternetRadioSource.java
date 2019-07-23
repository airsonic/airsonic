package org.airsonic.player.domain;

public class InternetRadioSource {
    private String streamUrl;

    public InternetRadioSource(String streamUrl) {
        this.streamUrl = streamUrl;
    }

    public String getStreamUrl() { return streamUrl; }
}
