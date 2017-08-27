package org.airsonic.restapi.domain;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "NowPlayingEntry")
public class NowPlayingEntry
        extends Child {

    @XmlAttribute(name = "username", required = true)
    protected String username;
    @XmlAttribute(name = "minutesAgo", required = true)
    protected int minutesAgo;
    @XmlAttribute(name = "playerId", required = true)
    protected int playerId;
    @XmlAttribute(name = "playerName")
    protected String playerName;

    public String getUsername() {
        return username;
    }

    public void setUsername(String value) {
        this.username = value;
    }

    public int getMinutesAgo() {
        return minutesAgo;
    }

    public void setMinutesAgo(int value) {
        this.minutesAgo = value;
    }

    public int getPlayerId() {
        return playerId;
    }

    public void setPlayerId(int value) {
        this.playerId = value;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String value) {
        this.playerName = value;
    }

}
