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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 *
 * @author R?mi Cocula
 */
@Service
public class JukeboxService {

    private static final Logger log = LoggerFactory.getLogger(JukeboxService.class);

    @Autowired
    private JukeboxLegacySubsonicService jukeboxLegacySubsonicService;
    @Autowired
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

    /**
     * This method should be removed when the jukebox is controlled only through rest api.
     *
     * @param airsonicPlayer
     * @param offset
     * @throws Exception
     */
    @Deprecated
    public void updateJukebox(Player airsonicPlayer, int offset) throws Exception {
        if (airsonicPlayer.getTechnology().equals(PlayerTechnology.JUKEBOX)) {
            jukeboxLegacySubsonicService.updateJukebox(airsonicPlayer,offset);
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
                if (jukeboxLegacySubsonicService.getPlayer() == null) {
                    return false;
                } else {
                    return jukeboxLegacySubsonicService.getPlayer().getId().equals(airsonicPlayer.getId());
                }
            case JAVA_JUKEBOX:
                return true;
        }
        return false;
    }

    /**
     * Plays the playQueue of a jukebox player starting at the first item of the queue.
     *
     * @param airsonicPlayer
     * @throws Exception
     */
    public void play(Player airsonicPlayer) throws Exception {
        switch (airsonicPlayer.getTechnology()) {
            case JUKEBOX:
                jukeboxLegacySubsonicService.updateJukebox(airsonicPlayer,0);
                break;
            case JAVA_JUKEBOX:
                jukeboxJavaService.play(airsonicPlayer);
                break;
        }
    }


    public void start(Player airsonicPlayer) throws Exception {
        switch (airsonicPlayer.getTechnology()) {
            case JUKEBOX:
                jukeboxLegacySubsonicService.updateJukebox(airsonicPlayer,0);
                break;
            case JAVA_JUKEBOX:
                jukeboxJavaService.start(airsonicPlayer);
                break;
        }
    }

    public void stop(Player airsonicPlayer) throws Exception {
        switch (airsonicPlayer.getTechnology()) {
            case JUKEBOX:
                jukeboxLegacySubsonicService.updateJukebox(airsonicPlayer,0);
                break;
            case JAVA_JUKEBOX:
                jukeboxJavaService.stop(airsonicPlayer);
                break;
        }
    }

    public void skip(Player airsonicPlayer,int index,int offset) throws Exception {
        switch (airsonicPlayer.getTechnology()) {
            case JUKEBOX:
                jukeboxLegacySubsonicService.updateJukebox(airsonicPlayer,offset);
                break;
            case JAVA_JUKEBOX:
                jukeboxJavaService.skip(airsonicPlayer,index,offset);
                break;
        }
    }


    /* properties setters */

    public void setJukeboxLegacySubsonicService(JukeboxLegacySubsonicService jukeboxLegacySubsonicService) {
        this.jukeboxLegacySubsonicService = jukeboxLegacySubsonicService;
    }

    public void setJukeboxJavaService(JukeboxJavaService jukeboxJavaService) {
        this.jukeboxJavaService = jukeboxJavaService;
    }

}
