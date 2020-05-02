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
 *  Copyright 2015 (C) Sindre Mehus
 */

package org.airsonic.player.service.sonos;

import org.airsonic.player.util.StringUtil;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.NameValuePair;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
public class SonosServiceRegistration {

    private static final Logger LOG = LoggerFactory.getLogger(SonosServiceRegistration.class);

    public void setEnabled(String airsonicBaseUrl, String sonosControllerIp, boolean enabled, String sonosServiceName, int sonosServiceId) throws IOException {
        String localUrl = airsonicBaseUrl + "ws/Sonos";
        String controllerUrl = String.format("http://%s:1400/customsd", sonosControllerIp);

        LOG.info((enabled ? "Enabling" : "Disabling") + " Sonos music service, using Sonos controller IP " + sonosControllerIp +
                 ", SID " + sonosServiceId + ", and Airsonic URL " + localUrl);

        List<Pair<String, String>> params = new ArrayList<Pair<String, String>>();
        params.add(Pair.of("sid", String.valueOf(sonosServiceId)));
        if (enabled) {
            params.add(Pair.of("name", sonosServiceName));
            params.add(Pair.of("uri", localUrl));
            params.add(Pair.of("secureUri", localUrl));
            params.add(Pair.of("pollInterval", "1200"));
            params.add(Pair.of("authType", "UserId"));
            params.add(Pair.of("containerType", "MService"));
            params.add(Pair.of("caps", "search"));
            params.add(Pair.of("caps", "trFavorites"));
            params.add(Pair.of("caps", "alFavorites"));
            params.add(Pair.of("caps", "ucPlaylists"));
            params.add(Pair.of("caps", "extendedMD"));
            params.add(Pair.of("presentationMapVersion", "1"));
            params.add(Pair.of("presentationMapUri", airsonicBaseUrl + "sonos/presentationMap.xml"));
            params.add(Pair.of("stringsVersion", "5"));
            params.add(Pair.of("stringsUri", airsonicBaseUrl + "sonos/strings.xml"));
        }

        String result = execute(controllerUrl, params);
        LOG.info("Sonos controller returned: " + result);
    }

    private String execute(String url, List<Pair<String, String>> parameters) throws IOException {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        for (Pair<String, String> parameter : parameters) {
            params.add(new BasicNameValuePair(parameter.getLeft(), parameter.getRight()));
        }
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(20 * 1000) // 20 seconds
                .setSocketTimeout(20 * 1000) // 20 seconds
                .build();
        HttpPost request = new HttpPost(url);
        request.setConfig(requestConfig);
        request.setEntity(new UrlEncodedFormEntity(params, StringUtil.ENCODING_UTF8));

        return executeRequest(request);
    }

    private String executeRequest(HttpUriRequest request) throws IOException {


        try (CloseableHttpClient client = HttpClients.createDefault()) {
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            return client.execute(request, responseHandler);

        }
    }
}
