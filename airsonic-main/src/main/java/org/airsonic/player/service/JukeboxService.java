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

import org.airsonic.player.domain.Player;
import org.airsonic.player.domain.PlayerTechnology;
import org.springframework.stereotype.Service;

/**
 * @author Rémi Cocula
 */
@Service
public class JukeboxService {

    private JukeboxLegacySubsonicService jukeboxLegacySubsonicService;
    private JukeboxJavaService jukeboxJavaService;

    public JukeboxService(JukeboxLegacySubsonicService jukeboxLegacySubsonicService,
                          JukeboxJavaService jukeboxJavaService) {
        this.jukeboxLegacySubsonicService = jukeboxLegacySubsonicService;
        this.jukeboxJavaService = jukeboxJavaService;
    }

    public void setGain(Player airsonicPlayer, float gain) {
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
     * Plays the playQueue of a jukebox player starting at the first item of the queue.
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

    /**
     * This method is only here due to legacy considerations and should be removed
     * if the jukeboxLegacySubsonicService is removed.
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
}
