/*
 This file is part of Airsonic.

 Airsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Airsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2016 (C) Airsonic Authors
 Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
 */
package org.airsonic.player.domain;

import org.apache.commons.lang.StringUtils;

import java.util.Date;

/**
 * Represens a remote player.  A player has a unique ID, a user-defined name, a logged-on user,
 * miscellaneous identifiers, and an associated playlist.
 *
 * @author Sindre Mehus
 */
public class Player {

    private Integer id;
    private String name;
    private PlayerTechnology technology = PlayerTechnology.WEB;
    private String clientId;
    private String type;
    private String username;
    private String ipAddress;
    private boolean isDynamicIp = true;
    private boolean isAutoControlEnabled = true;
    private boolean isM3uBomEnabled = true;
    private Date lastSeen;
    private TranscodeScheme transcodeScheme = TranscodeScheme.OFF;
    private PlayQueue playQueue;
    private String javaJukeboxMixer;

    /**
     * Returns the player ID.
     *
     * @return The player ID.
     */
    public Integer getId() {
        return id;
    }

    /**
     * Sets the player ID.
     *
     * @param id The player ID.
     */
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * Returns the user-defined player name.
     *
     * @return The user-defined player name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the user-defined player name.
     *
     * @param name The user-defined player name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the player "technology", e.g., web, external or jukebox.
     *
     * @return The player technology.
     */
    public PlayerTechnology getTechnology() {
        return technology;
    }

    /**
     * Returns the third-party client ID (used if this player is managed over the
     * Airsonic REST API).
     *
     * @return The client ID.
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * Sets the third-party client ID (used if this player is managed over the
     * Airsonic REST API).
     *
     * @param clientId The client ID.
     */
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    /**
     * Sets the player "technology", e.g., web, external or jukebox.
     *
     * @param technology The player technology.
     */
    public void setTechnology(PlayerTechnology technology) {
        this.technology = technology;
    }

    public boolean isJukebox() {
        return (technology == PlayerTechnology.JUKEBOX || technology == PlayerTechnology.JAVA_JUKEBOX);
    }

    public boolean isJavaJukebox() {
        return (technology == PlayerTechnology.JAVA_JUKEBOX);
    }

    public boolean isExternal() {
        return technology == PlayerTechnology.EXTERNAL;
    }

    public boolean isExternalWithPlaylist() {
        return technology == PlayerTechnology.EXTERNAL_WITH_PLAYLIST;
    }

    public boolean isWeb() {
        return technology == PlayerTechnology.WEB;
    }

    /**
     * Returns the player type, e.g., WinAmp, iTunes.
     *
     * @return The player type.
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the player type, e.g., WinAmp, iTunes.
     *
     * @param type The player type.
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Returns the logged-in user.
     *
     * @return The logged-in user.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the logged-in username.
     *
     * @param username The logged-in username.
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Returns whether the player is automatically started.
     *
     * @return Whether the player is automatically started.
     */
    public boolean isAutoControlEnabled() {
        return isAutoControlEnabled;
    }

    /**
     * Sets whether the player is automatically started.
     *
     * @param isAutoControlEnabled Whether the player is automatically started.
     */
    public void setAutoControlEnabled(boolean isAutoControlEnabled) {
        this.isAutoControlEnabled = isAutoControlEnabled;
    }

    /**
     * Returns whether apply BOM mark when generating a M3U file.
     *
     * @return Whether apply BOM mark when generating a M3U file.
     */
    public boolean isM3uBomEnabled() {
        return isM3uBomEnabled;
    }

    /**
     * Sets whether apply BOM mark when generating a M3U file.
     *
     * @param isM3uBomEnabled Whether apply BOM mark when generating a M3U file.
     */
    public void setM3uBomEnabled(boolean isM3uBomEnabled) {
        this.isM3uBomEnabled = isM3uBomEnabled;
    }

    /**
     * Returns the time when the player was last seen.
     *
     * @return The time when the player was last seen.
     */
    public Date getLastSeen() {
        return lastSeen;
    }

    /**
     * Sets the time when the player was last seen.
     *
     * @param lastSeen The time when the player was last seen.
     */
    public void setLastSeen(Date lastSeen) {
        this.lastSeen = lastSeen;
    }

    /**
     * Returns the transcode scheme.
     *
     * @return The transcode scheme.
     */
    public TranscodeScheme getTranscodeScheme() {
        return transcodeScheme;
    }

    /**
     * Sets the transcode scheme.
     *
     * @param transcodeScheme The transcode scheme.
     */
    public void setTranscodeScheme(TranscodeScheme transcodeScheme) {
        this.transcodeScheme = transcodeScheme;
    }

    /**
     * Returns the IP address of the player.
     *
     * @return The IP address of the player.
     */
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * Sets the IP address of the player.
     *
     * @param ipAddress The IP address of the player.
     */
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    /**
     * Returns whether this player has a dynamic IP address.
     *
     * @return Whether this player has a dynamic IP address.
     */
    public boolean isDynamicIp() {
        return isDynamicIp;
    }

    /**
     * Sets whether this player has a dynamic IP address.
     *
     * @param dynamicIp Whether this player has a dynamic IP address.
     */
    public void setDynamicIp(boolean dynamicIp) {
        isDynamicIp = dynamicIp;
    }

    /**
     * Returns the player's playlist.
     *
     * @return The player's playlist
     */
    public PlayQueue getPlayQueue() {
        return playQueue;
    }

    /**
     * Sets the player's playlist.
     *
     * @param playQueue The player's playlist.
     */
    public void setPlayQueue(PlayQueue playQueue) {
        this.playQueue = playQueue;
    }

    /**
     * Returns a long description of the player, e.g., <code>Player 3 [admin]</code>
     *
     * @return A long description of the player.
     */
    public String getDescription() {
        StringBuilder builder = new StringBuilder();
        if (name != null) {
            builder.append(name);
        } else {
            builder.append("Player ").append(id);
        }

        builder.append(" [").append(username).append(']');
        return builder.toString();
    }

    /**
     * Returns a short description of the player, e.g., <code>Player 3</code>
     *
     * @return A short description of the player.
     */
    public String getShortDescription() {
        if (StringUtils.isNotBlank(name)) {
            return name;
        }
        return "Player " + id;
    }


    public void setJavaJukeboxMixer(String javaJukeboxMixer) {
        this.javaJukeboxMixer = javaJukeboxMixer;
    }

    public String getJavaJukeboxMixer() {
        return javaJukeboxMixer;
    }


    /**
     * Returns a string representation of the player.
     *
     * @return A string representation of the player.
     * @see #getDescription()
     */
    @Override
    public String toString() {
        return getDescription();
    }

}
