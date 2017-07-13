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

import java.util.Date;

/**
 * Represents an internet radio station.
 *
 * @author Sindre Mehus
 * @version $Revision: 1.2 $ $Date: 2005/12/25 13:48:46 $
 */
public class InternetRadio {

    private Integer id;
    private String name;
    private String streamUrl;
    private String homepageUrl;
    private boolean isEnabled;
    private Date changed;

    /**
     * Creates a new internet radio station.
     *
     * @param id          The system-generated ID.
     * @param name        The user-defined name.
     * @param streamUrl   The stream URL for the station.
     * @param homepageUrl The home page URL for the station.
     * @param isEnabled   Whether the station is enabled.
     * @param changed     When the corresponding database entry was last changed.
     */
    public InternetRadio(Integer id, String name, String streamUrl, String homepageUrl, boolean isEnabled, Date changed) {
        this.id = id;
        this.name = name;
        this.streamUrl = streamUrl;
        this.homepageUrl = homepageUrl;
        this.isEnabled = isEnabled;
        this.changed = changed;
    }

    /**
     * Creates a new internet radio station.
     *
     * @param name        The user-defined name.
     * @param streamUrl   The URL for the station.
     * @param homepageUrl The home page URL for the station.
     * @param isEnabled   Whether the station is enabled.
     * @param changed     When the corresponding database entry was last changed.
     */
    public InternetRadio(String name, String streamUrl, String homepageUrl, boolean isEnabled, Date changed) {
        this(null, name, streamUrl, homepageUrl, isEnabled, changed);
    }

    /**
     * Returns the system-generated ID.
     *
     * @return The system-generated ID.
     */
    public Integer getId() {
        return id;
    }

    /**
     * Returns the user-defined name.
     *
     * @return The user-defined name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the user-defined name.
     *
     * @param name The user-defined name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the stream URL of the radio station.
     *
     * @return The stream URL of the radio station.
     */
    public String getStreamUrl() {
        return streamUrl;
    }

    /**
     * Sets the stream URL of the radio station.
     *
     * @param streamUrl The stream URL of the radio station.
     */
    public void setStreamUrl(String streamUrl) {
        this.streamUrl = streamUrl;
    }

    /**
     * Returns the homepage URL of the radio station.
     *
     * @return The homepage URL of the radio station.
     */
    public String getHomepageUrl() {
        return homepageUrl;
    }

    /**
     * Sets the home page URL of the radio station.
     *
     * @param homepageUrl The home page URL of the radio station.
     */
    public void setHomepageUrl(String homepageUrl) {
        this.homepageUrl = homepageUrl;
    }

    /**
     * Returns whether the radio station is enabled.
     *
     * @return Whether the radio station is enabled.
     */
    public boolean isEnabled() {
        return isEnabled;
    }

    /**
     * Sets whether the radio station is enabled.
     *
     * @param enabled Whether the radio station is enabled.
     */
    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    /**
     * Returns when the corresponding database entry was last changed.
     *
     * @return When the corresponding database entry was last changed.
     */
    public Date getChanged() {
        return changed;
    }

    /**
     * Sets when the corresponding database entry was last changed.
     *
     * @param changed When the corresponding database entry was last changed.
     */
    public void setChanged(Date changed) {
        this.changed = changed;
    }
}