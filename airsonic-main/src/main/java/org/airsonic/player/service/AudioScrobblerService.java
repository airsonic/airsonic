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
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.UserSettings;
import org.airsonic.player.service.scrobbler.LastFMScrobbler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * Provides services for "audioscrobbling", which is the process of
 * registering what songs are played at website.
 */
@Service
public class AudioScrobblerService {

    private LastFMScrobbler lastFMScrobbler;
    @Autowired
    private SettingsService settingsService;

    /**
     * Registers the given media file at audio scrobble service. This method returns immediately, the actual registration is done
     * by separate threads.
     *
     * @param mediaFile  The media file to register.
     * @param username   The user which played the music file.
     * @param submission Whether this is a submission or a now playing notification.
     * @param time       Event time, or {@code null} to use current time.
     */
    public synchronized void register(MediaFile mediaFile, String username, boolean submission, Date time) {
        if (mediaFile == null || mediaFile.isVideo()) {
            return;
        }

        UserSettings userSettings = settingsService.getUserSettings(username);
        if (userSettings.isLastFmEnabled() && userSettings.getLastFmUsername() != null && userSettings.getLastFmPassword() != null) {
            if (lastFMScrobbler == null) {
                lastFMScrobbler = new LastFMScrobbler();
            }
            lastFMScrobbler.register(mediaFile, userSettings.getLastFmUsername(), userSettings.getLastFmPassword(), submission, time);
        }
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }
}
