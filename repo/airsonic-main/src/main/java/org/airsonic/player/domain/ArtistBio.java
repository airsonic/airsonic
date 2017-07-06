/*
 * This file is part of Airsonic.
 *
 *  Airsonic is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Airsonic is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Copyright 2014 (C) Sindre Mehus
 */

package org.airsonic.player.domain;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
public class ArtistBio {

    private final String biography;
    private final String musicBrainzId;
    private final String lastFmUrl;
    private final String smallImageUrl;
    private final String mediumImageUrl;
    private final String largeImageUrl;

    public ArtistBio(String biography, String musicBrainzId, String lastFmUrl, String smallImageUrl,
                     String mediumImageUrl, String largeImageUrl) {
        this.biography = biography;
        this.musicBrainzId = musicBrainzId;
        this.lastFmUrl = lastFmUrl;
        this.smallImageUrl = smallImageUrl;
        this.mediumImageUrl = mediumImageUrl;
        this.largeImageUrl = largeImageUrl;
    }

    public String getBiography() {
        return biography;
    }

    public String getMusicBrainzId() {
        return musicBrainzId;
    }

    public String getLastFmUrl() {
        return lastFmUrl;
    }

    public String getSmallImageUrl() {
        return smallImageUrl;
    }

    public String getMediumImageUrl() {
        return mediumImageUrl;
    }

    public String getLargeImageUrl() {
        return largeImageUrl;
    }
}
