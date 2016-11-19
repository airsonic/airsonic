/*
 This file is part of Libresonic.

 Libresonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Libresonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Libresonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2016 (C) Libresonic Authors
 Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
 */
package org.libresonic.player.service;

import org.libresonic.player.Logger;
import org.libresonic.player.domain.Version;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides version-related services, including functionality for determining whether a newer
 * version of Libresonic is available.
 *
 * @author Sindre Mehus
 */
public class VersionService {

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");
    private static final Logger LOG = Logger.getLogger(VersionService.class);

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
     * URL from which to fetch latest versions.
     */
    private static final String VERSION_URL = "http://libresonic.org/release/version.txt";

    public void init() {
        ServiceLocator.setVersionService(this);
    }

    /**
     * Returns the version number for the locally installed Libresonic version.
     *
     * @return The version number for the locally installed Libresonic version.
     */
    public synchronized Version getLocalVersion() {
        if (localVersion == null) {
            try {
                localVersion = new Version(readLineFromResource("/version.txt"));
                LOG.info("Resolved local Libresonic version to: " + localVersion);
            } catch (Exception x) {
                LOG.warn("Failed to resolve local Libresonic version.", x);
            }
        }
        return localVersion;
    }

    /**
     * Returns the version number for the latest available Libresonic final version.
     *
     * @return The version number for the latest available Libresonic final version, or <code>null</code>
     *         if the version number can't be resolved.
     */
    public synchronized Version getLatestFinalVersion() {
        refreshLatestVersion();
        return latestFinalVersion;
    }

    /**
     * Returns the version number for the latest available Libresonic beta version.
     *
     * @return The version number for the latest available Libresonic beta version, or <code>null</code>
     *         if the version number can't be resolved.
     */
    public synchronized Version getLatestBetaVersion() {
        refreshLatestVersion();
        return latestBetaVersion;
    }

    /**
     * Returns the build date for the locally installed Libresonic version.
     *
     * @return The build date for the locally installed Libresonic version, or <code>null</code>
     *         if the build date can't be resolved.
     */
    public synchronized Date getLocalBuildDate() {
        if (localBuildDate == null) {
            try {
                String date = readLineFromResource("/build_date.txt");
                localBuildDate = DATE_FORMAT.parse(date);
            } catch (Exception x) {
                LOG.warn("Failed to resolve local Libresonic build date.", x);
            }
        }
        return localBuildDate;
    }

    /**
     * Returns the build number for the locally installed Libresonic version.
     *
     * @return The build number for the locally installed Libresonic version, or <code>null</code>
     *         if the build number can't be resolved.
     */
    public synchronized String getLocalBuildNumber() {
        if (localBuildNumber == null) {
            try {
                localBuildNumber = readLineFromResource("/build_number.txt");
            } catch (Exception x) {
                LOG.warn("Failed to resolve local Libresonic build number.", x);
            }
        }
        return localBuildNumber;
    }

    /**
     * Returns whether a new final version of Libresonic is available.
     *
     * @return Whether a new final version of Libresonic is available.
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
     * Returns whether a new beta version of Libresonic is available.
     *
     * @return Whether a new beta version of Libresonic is available.
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
                LOG.warn("Failed to resolve latest Libresonic version.", x);
            }
        }
    }

    /**
     * Resolves the latest available Libresonic version by screen-scraping a web page.
     *
     * @throws IOException If an I/O error occurs.
     */
    private void readLatestVersion() throws IOException {

        HttpClient client = new DefaultHttpClient();
        HttpConnectionParams.setConnectionTimeout(client.getParams(), 10000);
        HttpConnectionParams.setSoTimeout(client.getParams(), 10000);
        HttpGet method = new HttpGet(VERSION_URL + "?v=" + getLocalVersion());
        String content;
        try {

            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            content = client.execute(method, responseHandler);

        } finally {
            client.getConnectionManager().shutdown();
        }

        BufferedReader reader = new BufferedReader(new StringReader(content));
        Pattern finalPattern = Pattern.compile("LIBRESONIC_FULL_VERSION_BEGIN(.*)LIBRESONIC_FULL_VERSION_END");
        Pattern betaPattern = Pattern.compile("LIBRESONIC_BETA_VERSION_BEGIN(.*)LIBRESONIC_BETA_VERSION_END");

        try {
            String line = reader.readLine();
            while (line != null) {
                Matcher finalMatcher = finalPattern.matcher(line);
                if (finalMatcher.find()) {
                    latestFinalVersion = new Version(finalMatcher.group(1));
                    LOG.info("Resolved latest Libresonic final version to: " + latestFinalVersion);
                }
                Matcher betaMatcher = betaPattern.matcher(line);
                if (betaMatcher.find()) {
                    latestBetaVersion = new Version(betaMatcher.group(1));
                    LOG.info("Resolved latest Libresonic beta version to: " + latestBetaVersion);
                }
                line = reader.readLine();
            }

        } finally {
            reader.close();
        }
    }
}
