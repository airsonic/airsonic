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
import org.airsonic.player.util.StringUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Provides services for "audioscrobbling", which is the process of
 * registering what songs are played at www.last.fm.
 * <p/>
 * See http://www.last.fm/api/submissions
 *
 * @author Sindre Mehus
 */
@Service
public class AudioScrobblerService {

    private static final Logger LOG = LoggerFactory.getLogger(AudioScrobblerService.class);
    private static final int MAX_PENDING_REGISTRATION = 2000;

    private RegistrationThread thread;
    private final LinkedBlockingQueue<RegistrationData> queue = new LinkedBlockingQueue<RegistrationData>();

    @Autowired
    private SettingsService settingsService;
    private final RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(15000)
            .setSocketTimeout(15000)
            .build();

    /**
     * Registers the given media file at www.last.fm. This method returns immediately, the actual registration is done
     * by a separate thread.
     *
     * @param mediaFile  The media file to register.
     * @param username   The user which played the music file.
     * @param submission Whether this is a submission or a now playing notification.
     * @param time       Event time, or {@code null} to use current time.
     */
    public synchronized void register(MediaFile mediaFile, String username, boolean submission, Date time) {

        if (thread == null) {
            thread = new RegistrationThread();
            thread.start();
        }

        if (queue.size() >= MAX_PENDING_REGISTRATION) {
            LOG.warn("Last.fm scrobbler queue is full. Ignoring " + mediaFile);
            return;
        }

        RegistrationData registrationData = createRegistrationData(mediaFile, username, submission, time);
        if (registrationData == null) {
            return;
        }

        try {
            queue.put(registrationData);
        } catch (InterruptedException x) {
            LOG.warn("Interrupted while queuing Last.fm scrobble.", x);
        }
    }

    private RegistrationData createRegistrationData(MediaFile mediaFile, String username, boolean submission, Date time) {

        if (mediaFile == null || mediaFile.isVideo()) {
            return null;
        }

        UserSettings userSettings = settingsService.getUserSettings(username);
        if (!userSettings.isLastFmEnabled() || userSettings.getLastFmUsername() == null || userSettings.getLastFmPassword() == null) {
            return null;
        }

        RegistrationData reg = new RegistrationData();
        reg.username = userSettings.getLastFmUsername();
        reg.password = userSettings.getLastFmPassword();
        reg.artist = mediaFile.getArtist();
        reg.album = mediaFile.getAlbumName();
        reg.title = mediaFile.getTitle();
        reg.duration = mediaFile.getDurationSeconds() == null ? 0 : mediaFile.getDurationSeconds();
        reg.time = time == null ? new Date() : time;
        reg.submission = submission;

        return reg;
    }

    /**
     * Scrobbles the given song data at last.fm, using the protocol defined at http://www.last.fm/api/submissions.
     *
     * @param registrationData Registration data for the song.
     */
    private void scrobble(RegistrationData registrationData) throws Exception {
        if (registrationData == null) {
            return;
        }

        String[] lines = authenticate(registrationData);
        if (lines == null) {
            return;
        }

        String sessionId = lines[1];
        String nowPlayingUrl = lines[2];
        String submissionUrl = lines[3];

        if (registrationData.submission) {
            lines = registerSubmission(registrationData, sessionId, submissionUrl);
        } else {
            lines = registerNowPlaying(registrationData, sessionId, nowPlayingUrl);
        }

        if (lines[0].startsWith("FAILED")) {
            LOG.warn("Failed to scrobble song '" + registrationData.title + "' at Last.fm: " + lines[0]);
        } else if (lines[0].startsWith("BADSESSION")) {
            LOG.warn("Failed to scrobble song '" + registrationData.title + "' at Last.fm.  Invalid session.");
        } else if (lines[0].startsWith("OK")) {
            LOG.info("Successfully registered " + (registrationData.submission ? "submission" : "now playing") +
                      " for song '" + registrationData.title + "' for user " + registrationData.username + " at Last.fm: " + registrationData.time);
        }
    }

    /**
     * Returns the following lines if authentication succeeds:
     * <p/>
     * Line 0: Always "OK"
     * Line 1: Session ID, e.g., "17E61E13454CDD8B68E8D7DEEEDF6170"
     * Line 2: URL to use for now playing, e.g., "http://post.audioscrobbler.com:80/np_1.2"
     * Line 3: URL to use for submissions, e.g., "http://post2.audioscrobbler.com:80/protocol_1.2"
     * <p/>
     * If authentication fails, <code>null</code> is returned.
     */
    private String[] authenticate(RegistrationData registrationData) throws Exception {
        String clientId = "sub";
        String clientVersion = "0.1";
        long timestamp = System.currentTimeMillis() / 1000L;
        String authToken = calculateAuthenticationToken(registrationData.password, timestamp);
        URI uri = new URI("https",
                /* userInfo= */ null, "post.audioscrobbler.com", -1,
                "/",
                String.format("hs=true&p=1.2.1&c=%s&v=%s&u=%s&t=%s&a=%s",
                        clientId, clientVersion, registrationData.username,
                        timestamp, authToken),
                /* fragment= */ null);

        String[] lines = executeGetRequest(uri);

        if (lines[0].startsWith("BANNED")) {
            LOG.warn("Failed to scrobble song '" + registrationData.title + "' at Last.fm. Client version is banned.");
            return null;
        }

        if (lines[0].startsWith("BADAUTH")) {
            LOG.warn("Failed to scrobble song '" + registrationData.title + "' at Last.fm. Wrong username or password.");
            return null;
        }

        if (lines[0].startsWith("BADTIME")) {
            LOG.warn("Failed to scrobble song '" + registrationData.title + "' at Last.fm. Bad timestamp, please check local clock.");
            return null;
        }

        if (lines[0].startsWith("FAILED")) {
            LOG.warn("Failed to scrobble song '" + registrationData.title + "' at Last.fm: " + lines[0]);
            return null;
        }

        if (!lines[0].startsWith("OK")) {
            LOG.warn("Failed to scrobble song '" + registrationData.title + "' at Last.fm.  Unknown response: " + lines[0]);
            return null;
        }

        return lines;
    }

    private String[] registerSubmission(RegistrationData registrationData, String sessionId, String url) throws IOException {
        Map<String, String> params = new HashMap<String, String>();
        params.put("s", sessionId);
        params.put("a[0]", registrationData.artist);
        params.put("t[0]", registrationData.title);
        params.put("i[0]", String.valueOf(registrationData.time.getTime() / 1000L));
        params.put("o[0]", "P");
        params.put("r[0]", "");
        params.put("l[0]", String.valueOf(registrationData.duration));
        params.put("b[0]", registrationData.album);
        params.put("n[0]", "");
        params.put("m[0]", "");
        return executePostRequest(url, params);
    }

    private String[] registerNowPlaying(RegistrationData registrationData, String sessionId, String url) throws IOException {
        Map<String, String> params = new HashMap<String, String>();
        params.put("s", sessionId);
        params.put("a", registrationData.artist);
        params.put("t", registrationData.title);
        params.put("b", registrationData.album);
        params.put("l", String.valueOf(registrationData.duration));
        params.put("n", "");
        params.put("m", "");
        return executePostRequest(url, params);
    }

    private String calculateAuthenticationToken(String password, long timestamp) {
        return DigestUtils.md5Hex(DigestUtils.md5Hex(password) + timestamp);
    }

    private String[] executeGetRequest(URI url) throws IOException {
        HttpGet method = new HttpGet(url);
        method.setConfig(requestConfig);
        return executeRequest(method);
    }

    private String[] executePostRequest(String url, Map<String, String> parameters) throws IOException {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            params.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
        }

        HttpPost request = new HttpPost(url);
        request.setEntity(new UrlEncodedFormEntity(params, StringUtil.ENCODING_UTF8));
        request.setConfig(requestConfig);
        return executeRequest(request);
    }

    private String[] executeRequest(HttpUriRequest request) throws IOException {

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            String response = client.execute(request, responseHandler);
            return response.split("\\n");

        }
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    private class RegistrationThread extends Thread {
        private RegistrationThread() {
            super("AudioScrobbler Registration");
        }

        @Override
        public void run() {
            while (true) {
                RegistrationData registrationData = null;
                try {
                    registrationData = queue.take();
                    scrobble(registrationData);
                } catch (IOException x) {
                    handleNetworkError(registrationData, x);
                } catch (Exception x) {
                    LOG.warn("Error in Last.fm registration.", x);
                }
            }
        }

        private void handleNetworkError(RegistrationData registrationData, IOException x) {
            try {
                queue.put(registrationData);
                LOG.info("Last.fm registration for " + registrationData.title +
                         " encountered network error.  Will try again later. In queue: " + queue.size(), x);
            } catch (InterruptedException e) {
                LOG.error("Failed to reschedule Last.fm registration for " + registrationData.title, e);
            }
            try {
                sleep(60L * 1000L);  // Wait 60 seconds.
            } catch (InterruptedException e) {
                LOG.error("Failed to sleep after Last.fm registration failure for " + registrationData.title, e);
            }
        }
    }

    private static class RegistrationData {
        private String username;
        private String password;
        private String artist;
        private String album;
        private String title;
        private int duration;
        private Date time;
        public boolean submission;
    }

}
