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

import com.jayway.jsonpath.JsonPath;
import org.airsonic.player.domain.Version;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides version-related services, including functionality for determining whether a newer
 * version of Airsonic is available.
 *
 * @author Sindre Mehus
 */
@Service
public class VersionService {

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");
    private static final Logger LOG = LoggerFactory.getLogger(VersionService.class);

    private Version localVersion;
    private Version latestFinalVersion;
    private Version latestBetaVersion;
    private Date localBuildDate;
    private String localBuildNumber;

    /**
     * Time when latest version was fetched (in milliseconds).
     */
    private long lastVersionFetched;

    /**
     * Only fetch last version this often (in milliseconds.).
     */
    private static final long LAST_VERSION_FETCH_INTERVAL = 7L * 24L * 3600L * 1000L; // One week

    /**
     * Returns the version number for the locally installed Airsonic version.
     *
     * @return The version number for the locally installed Airsonic version.
     */
    public synchronized Version getLocalVersion() {
        if (localVersion == null) {
            try {
                localVersion = new Version(readLineFromResource("/version.txt"));
                LOG.info("Resolved local Airsonic version to: " + localVersion);
            } catch (Exception x) {
                LOG.warn("Failed to resolve local Airsonic version.", x);
            }
        }
        return localVersion;
    }

    /**
     * Returns the version number for the latest available Airsonic final version.
     *
     * @return The version number for the latest available Airsonic final version, or <code>null</code>
     *         if the version number can't be resolved.
     */
    public synchronized Version getLatestFinalVersion() {
        refreshLatestVersion();
        return latestFinalVersion;
    }

    /**
     * Returns the version number for the latest available Airsonic beta version.
     *
     * @return The version number for the latest available Airsonic beta version, or <code>null</code>
     *         if the version number can't be resolved.
     */
    public synchronized Version getLatestBetaVersion() {
        refreshLatestVersion();
        return latestBetaVersion;
    }

    /**
     * Returns the build date for the locally installed Airsonic version.
     *
     * @return The build date for the locally installed Airsonic version, or <code>null</code>
     *         if the build date can't be resolved.
     */
    public synchronized Date getLocalBuildDate() {
        if (localBuildDate == null) {
            try {
                String date = readLineFromResource("/build_date.txt");
                localBuildDate = DATE_FORMAT.parse(date);
            } catch (Exception x) {
                LOG.warn("Failed to resolve local Airsonic build date.", x);
            }
        }
        return localBuildDate;
    }

    /**
     * Returns the build number for the locally installed Airsonic version.
     *
     * @return The build number for the locally installed Airsonic version, or <code>null</code>
     *         if the build number can't be resolved.
     */
    public synchronized String getLocalBuildNumber() {
        if (localBuildNumber == null) {
            try {
                localBuildNumber = readLineFromResource("/build_number.txt");
            } catch (Exception x) {
                LOG.warn("Failed to resolve local Airsonic build number.", x);
            }
        }
        return localBuildNumber;
    }

    /**
     * Returns whether a new final version of Airsonic is available.
     *
     * @return Whether a new final version of Airsonic is available.
     */
    public boolean isNewFinalVersionAvailable() {
        Version latest = getLatestFinalVersion();
        Version local = getLocalVersion();

        if (latest == null || local == null) {
            return false;
        }

        return local.compareTo(latest) < 0;
    }

    /**
     * Returns whether a new beta version of Airsonic is available.
     *
     * @return Whether a new beta version of Airsonic is available.
     */
    public boolean isNewBetaVersionAvailable() {
        Version latest = getLatestBetaVersion();
        Version local = getLocalVersion();

        if (latest == null || local == null) {
            return false;
        }

        return local.compareTo(latest) < 0;
    }

    /**
     * Reads the first line from the resource with the given name.
     *
     * @param resourceName The resource name.
     * @return The first line of the resource.
     */
    private String readLineFromResource(String resourceName) {
        InputStream in = VersionService.class.getResourceAsStream(resourceName);
        if (in == null) {
            return null;
        }
        BufferedReader reader = null;
        try {

            reader = new BufferedReader(new InputStreamReader(in));
            return reader.readLine();

        } catch (IOException x) {
            return null;
        } finally {
            IOUtils.closeQuietly(reader);
            IOUtils.closeQuietly(in);
        }
    }

    /**
     * Refreshes the latest final and beta versions.
     */
    private void refreshLatestVersion() {
        long now = System.currentTimeMillis();
        boolean isOutdated = now - lastVersionFetched > LAST_VERSION_FETCH_INTERVAL;

        if (isOutdated) {
            try {
                lastVersionFetched = now;
                readLatestVersion();
            } catch (Exception x) {
                LOG.warn("Failed to resolve latest Airsonic version.", x);
            }
        }
    }

    private final String JSON_PATH = "$..tag_name";
    private final Pattern VERSION_REGEX = Pattern.compile("^v(.*)");
    private static final String VERSION_URL = "https://api.github.com/repos/airsonic/airsonic/releases";

    /**
     * Resolves the latest available Airsonic version by inspecting github.
     */
    private void readLatestVersion() throws IOException {

        LOG.debug("Starting to read latest version");
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(10000)
                .setSocketTimeout(10000)
                .build();
        HttpGet method = new HttpGet(VERSION_URL + "?v=" + getLocalVersion());
        method.setConfig(requestConfig);
        String content;
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            content = client.execute(method, responseHandler);
        }

        List<String> unsortedTags = JsonPath.read(content, JSON_PATH);

        Function<String, Version> convertToVersion = s -> {
            Matcher match = VERSION_REGEX.matcher(s);
            if(!match.matches()) {
                throw new RuntimeException("Unexpected tag format " + s);
            }
            return new Version(match.group(1));
        };

        Predicate<Version> finalVersionPredicate = version -> !version.isPreview();

        Optional<Version> betaV = unsortedTags.stream().map(convertToVersion).sorted(Comparator.reverseOrder()).findFirst();
        Optional<Version> finalV = unsortedTags.stream().map(convertToVersion).sorted(Comparator.reverseOrder()).filter(finalVersionPredicate).findFirst();

        LOG.debug("Got {} for beta version", betaV);
        LOG.debug("Got {} for final version", finalV);

        latestBetaVersion = betaV.get();
        latestFinalVersion = finalV.get();
    }
}
