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
package org.airsonic.player.service;

import org.airsonic.player.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 *
 * @author Rémi Cocula
 */
public class JukeboxService {

    private static final Logger log = LoggerFactory.getLogger(JukeboxService.class);

    private JukeboxLegacySubsonicService jukeboxLegacySubsonicService;
    private JukeboxJavaService jukeboxJavaService;


    public void setGain(Player airsonicPlayer,float gain) {
        switch (airsonicPlayer.getTechnology()) {
            case JUKEBOX:
                jukeboxLegacySubsonicService.setGain(gain);
                break;
            case JAVA_JUKEBOX:
                jukeboxJavaService.setGain(airsonicPlayer,gain);
                break;
        }
    }

    public void setPosition(Player airsonicPlayer, int positionInSeconds) {
        switch (airsonicPlayer.getTechnology()) {
            case JUKEBOX:
                throw new UnsupportedOperationException();
            case JAVA_JUKEBOX:
                jukeboxJavaService.setPosition(airsonicPlayer,positionInSeconds);
                break;
        }
    }

    public float getGain(Player airsonicPlayer) {
        switch (airsonicPlayer.getTechnology()) {
            case JUKEBOX:
                return jukeboxLegacySubsonicService.getGain();
            case JAVA_JUKEBOX:
                return jukeboxJavaService.getGain(airsonicPlayer);
        }
        return 0;
    }

    @Deprecated
    public void updateJukebox(Player airsonicPlayer, int offset) throws Exception {
        switch (airsonicPlayer.getTechnology()) {
            case JUKEBOX:
                jukeboxLegacySubsonicService.updateJukebox(airsonicPlayer,offset);
            case JAVA_JUKEBOX:
                jukeboxJavaService.updateJukebox(airsonicPlayer,offset);
        }
    }

    public int getPosition(Player airsonicPlayer) {
        switch (airsonicPlayer.getTechnology()) {
            case JUKEBOX:
                return jukeboxLegacySubsonicService.getPosition();
            case JAVA_JUKEBOX:
                return jukeboxJavaService.getPosition(airsonicPlayer);
        }
        return 0;
    }

    /**
     * This method is only here due to legacy considerations and should be removed
     * if the jukeboxLegacySubsonicService is removed.
     * @param airsonicPlayer
     * @return
     */
    @Deprecated
    public boolean canControl(Player airsonicPlayer) {
        switch (airsonicPlayer.getTechnology()) {
            case JUKEBOX:
                return jukeboxLegacySubsonicService.getPlayer().getId().equals(airsonicPlayer.getId());
            case JAVA_JUKEBOX:
                return true;
        }
        return false;
    }


    /* properties setters */

    public void setJukeboxLegacySubsonicService(JukeboxLegacySubsonicService jukeboxLegacySubsonicService) {
        this.jukeboxLegacySubsonicService = jukeboxLegacySubsonicService;
    }

    public void setJukeboxJavaService(JukeboxJavaService jukeboxJavaService) {
        this.jukeboxJavaService = jukeboxJavaService;
    }

}
