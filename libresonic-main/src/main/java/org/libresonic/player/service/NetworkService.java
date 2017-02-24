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

import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.libresonic.player.Logger;
import org.libresonic.player.domain.UrlRedirectType;
import org.libresonic.player.util.StringUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Provides network-related services, including port forwarding on UPnP routers and
 * URL redirection from http://xxxx.libresonic.org.
 *
 * @author Sindre Mehus
 */
public class NetworkService {

    private static final Logger LOG = Logger.getLogger(NetworkService.class);
    private static final long URL_REDIRECTION_DELAY = 2 * 3600L;

    private static final String URL_REDIRECTION_REGISTER_URL = getBackendUrl() + "/backend/redirect/register.view";
    private static final String URL_REDIRECTION_UNREGISTER_URL = getBackendUrl() + "/backend/redirect/unregister.view";
    private static final String URL_REDIRECTION_TEST_URL = getBackendUrl() + "/backend/redirect/test.view";

    private SettingsService settingsService;
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(4);
    private final URLRedirectionTask urlRedirectionTask = new URLRedirectionTask();
    private Future<?> urlRedirectionFuture;

    private final static Status portForwardingStatus;
    static {
        portForwardingStatus = new Status();
        portForwardingStatus.setText("NOT SUPPORTED");
    }
    private final Status urlRedirectionStatus = new Status();
    private boolean testUrlRedirection;

    public void init() {
        initUrlRedirection(false);
    }

    /**
     * Configures URL redirection.
     *
     * @param test Whether to test that the redirection works.
     */
    public synchronized void initUrlRedirection(boolean test) {

        if (true) {
            // This feature isn't currently supported, since it's a public service of subsonic.org
            // Display a warning message for now
            boolean enabled = settingsService.isUrlRedirectionEnabled() && settingsService.getUrlRedirectType() == UrlRedirectType.NORMAL;
            if (enabled) {
                LOG.warn("The URL redirection service is currently not enabled!");
            }
            return;
        }

        urlRedirectionStatus.setText("Idle");
        if (urlRedirectionFuture != null) {
            urlRedirectionFuture.cancel(true);
        }
        testUrlRedirection = test;
        urlRedirectionFuture = executor.scheduleWithFixedDelay(urlRedirectionTask, 0L, URL_REDIRECTION_DELAY, TimeUnit.SECONDS);
    }

    public Status getPortForwardingStatus() {
        return portForwardingStatus;
    }

    public Status getURLRedirecionStatus() {
        return urlRedirectionStatus;
    }

    public static String getBackendUrl() {
        return "true".equals(System.getProperty("libresonic.test")) ? "http://localhost:8080" : "http://libresonic.org";
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    private class URLRedirectionTask extends Task {

        @Override
        protected void execute() {

            boolean enable = settingsService.isUrlRedirectionEnabled() && settingsService.getUrlRedirectType() == UrlRedirectType.NORMAL;
            HttpPost request = new HttpPost(enable ? URL_REDIRECTION_REGISTER_URL : URL_REDIRECTION_UNREGISTER_URL);

            int port = settingsService.getPort();

            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("serverId", settingsService.getServerId()));
            params.add(new BasicNameValuePair("redirectFrom", settingsService.getUrlRedirectFrom()));
            params.add(new BasicNameValuePair("port", String.valueOf(port)));
            params.add(new BasicNameValuePair("localIp", settingsService.getLocalIpAddress()));
            params.add(new BasicNameValuePair("localPort", String.valueOf(port)));
            params.add(new BasicNameValuePair("contextPath", settingsService.getUrlRedirectContextPath()));



            try (CloseableHttpClient client = HttpClients.createDefault()) {
                urlRedirectionStatus.setText(enable ? "Registering web address..." : "Unregistering web address...");
                request.setEntity(new UrlEncodedFormEntity(params, StringUtil.ENCODING_UTF8));

                try (CloseableHttpResponse response = client.execute(request)) {
                    StatusLine status = response.getStatusLine();

                    switch (status.getStatusCode()) {
                        case HttpStatus.SC_BAD_REQUEST:
                            urlRedirectionStatus.setText(EntityUtils.toString(response.getEntity()));
                            testUrlRedirection = false;
                            break;
                        case HttpStatus.SC_OK:
                            urlRedirectionStatus.setText(enable ? "Successfully registered web address." : "Web address disabled.");
                            break;
                        default:
                            testUrlRedirection = false;
                            throw new IOException(status.getStatusCode() + " " + status.getReasonPhrase());
                    }
                }
            } catch (Throwable x) {
                LOG.warn(enable ? "Failed to register web address." : "Failed to unregister web address.", x);
                urlRedirectionStatus.setText(enable ? ("Failed to register web address. " + x.getMessage() +
                        " (" + x.getClass().getSimpleName() + ")") : "Web address disabled.");
            }

            // Test redirection, but only once.
            if (testUrlRedirection) {
                testUrlRedirection = false;
                testUrlRedirection();
            }

            //  Don't do it again if disabled.
            if (!enable && urlRedirectionFuture != null) {
                urlRedirectionFuture.cancel(false);
            }
        }

        private void testUrlRedirection() {

            String urlToTest;
            String url = URL_REDIRECTION_TEST_URL;
            if (settingsService.getUrlRedirectType() == UrlRedirectType.NORMAL) {
                url += "?redirectFrom=" + settingsService.getUrlRedirectFrom();
                urlToTest = settingsService.getUrlRedirectFrom() + ".libresonic.org";
            } else {
                url += "?customUrl=" + settingsService.getUrlRedirectCustomUrl();
                urlToTest = settingsService.getUrlRedirectCustomUrl();
            }

            HttpGet request = new HttpGet(url);

            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(15000)
                    .setSocketTimeout(15000)
                    .build();
            request.setConfig(requestConfig);
            try (CloseableHttpClient client = HttpClients.createDefault()) {
                urlRedirectionStatus.setText("Testing web address " + urlToTest + ". Please wait...");
                String response = client.execute(request, new BasicResponseHandler());
                urlRedirectionStatus.setText(response);
            } catch (Throwable x) {
                LOG.warn("Failed to test web address.", x);
                urlRedirectionStatus.setText("Failed to test web address. " + x.getMessage() + " (" + x.getClass().getSimpleName() + ")");
            }
        }
    }

    private abstract class Task implements Runnable {
        public void run() {
            String name = getClass().getSimpleName();
            try {
                execute();
            } catch (Throwable x) {
                LOG.error("Error executing " + name + ": " + x.getMessage(), x);
            }
        }

        protected abstract void execute();
    }

    public static class Status {

        private String text;
        private Date date;

        public void setText(String text) {
            this.text = text;
            date = new Date();
        }

        public String getText() {
            return text;
        }

        public Date getDate() {
            return date;
        }
    }
}
