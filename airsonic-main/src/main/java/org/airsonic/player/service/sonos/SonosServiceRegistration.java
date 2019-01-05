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

import org.airsonic.player.util.Pair;
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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>Registration with Sonos controller. They are 2 types of registration they are still supported by Sonos.
 * The ANONYMOUS and APPLICATION_LINK. The third USER_ID, must still work but will be not supported.</p>
 * <p></p>
 * @author Sindre Mehus
 * @author Nacrylic
 * @version $Id$
 */
@Component
public class SonosServiceRegistration {
    private static final Logger LOG = LoggerFactory.getLogger(SonosServiceRegistration.class);

    /**
     * The type of Authentication fo Sonos, the old want USER_ID, is will be not supported. We must use the
     * Anonymous or AppLink. The USER_ID
     */
    public enum AuthenticationType {
        //@Deprecated use ANONYMOUS or APPLICATION_LINK
        @Deprecated
        DEVICE_LINK("DeviceLink"),
        @Deprecated
        USER_ID("UserId"),

        ANONYMOUS("Anonymous"),
        APPLICATION_LINK("AppLink");

        private String fieldValue;

        AuthenticationType(String fieldValue){
            this.fieldValue = fieldValue;
        }

        public String getFieldValue() {
            return fieldValue;
        }
    }


    /**
     * Enable or disable Sonos registration
     *
     * @param airsonicBaseUrl must be the ip address, not the name
     * @param sonosControllerIp must be a ip too
     * @param enabled true for enable or false to disable
     * @param sonosServiceName the name of service you will see on Sonos service list
     * @param sonosServiceId the ID, the free id is : 240-253 or 255
     * @throws IOException if some io problem
     */
    public boolean setEnabled(String airsonicBaseUrl, String sonosControllerIp, boolean enabled, String sonosServiceName,
                           int sonosServiceId, AuthenticationType authenticationType) throws IOException {
        String localUrl = airsonicBaseUrl + "ws/Sonos";
        String controllerUrl = String.format("http://%s:1400/customsd", sonosControllerIp);

        LOG.info((enabled ? "Enabling" : "Disabling") + " Sonos music service, using Sonos controller IP " + sonosControllerIp +
                 ", SID " + sonosServiceId + ", and Airsonic URL " + localUrl);

        List<Pair<String, String>> params = new ArrayList<>();
        params.add(Pair.create("sid", String.valueOf(sonosServiceId)));

        // Need the csrf token on each request
        String csrfToken = retrieveCsrfToken(controllerUrl);
        if(csrfToken != null){
            params.add(Pair.create("csrfToken", csrfToken));
        }

        if (enabled) {
            params.add(Pair.create("name", sonosServiceName));
            params.add(Pair.create("uri", localUrl));
            params.add(Pair.create("secureUri", localUrl));
            params.add(Pair.create("pollInterval", "1200"));
            params.add(Pair.create("containerType", "MService"));
            params.add(Pair.create("caps", "search"));
            params.add(Pair.create("caps", "trFavorites"));
            params.add(Pair.create("caps", "alFavorites"));
            params.add(Pair.create("caps", "ucPlaylists"));
            params.add(Pair.create("caps", "extendedMD"));

            // If you change /home/michel/externalDev/airsonic/airsonic-main/src/main/webapp/sonos/presentationMap.xml
            // Change the presentationMapVersion
            params.add(Pair.create("presentationMapVersion", "1"));
            params.add(Pair.create("presentationMapUri", airsonicBaseUrl + "/sonos/presentationMap.xml"));

            // If you are change text in : /home/michel/externalDev/airsonic/airsonic-main/src/main/webapp/sonos/strings.xml
            // Change the stringsVersion
            params.add(Pair.create("stringsVersion", "11"));
            params.add(Pair.create("stringsUri", airsonicBaseUrl + "/sonos/strings.xml"));
            params.add(Pair.create("authType", authenticationType.getFieldValue()));

        } else {

            // For disable it need name with empty value, without that the service registration didn't disable.
            params.add(Pair.create("name", null));
        }

        return execute(controllerUrl, params);
    }


    private boolean execute(String url, List<Pair<String, String>> parameters) throws IOException {
        List<NameValuePair> params = new ArrayList<>();
        for (Pair<String, String> parameter : parameters) {
            params.add(new BasicNameValuePair(parameter.getFirst(), parameter.getSecond()));
        }

        HttpPost request = new HttpPost(url);
        request.setConfig(getDefaultRequestConfig());

        // Don't use the UTF8 encoding, is nice but the sonos controller didn't like (it's american!).
        request.setEntity(new UrlEncodedFormEntity(params));


        String result = executeRequest(request);
        LOG.info("Sonos controller returned: " + result);

        return result.contains("Success");
    }

    private String executeRequest(HttpUriRequest request) throws IOException {

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            return client.execute(request, responseHandler);

        }
    }

    private String retrieveCsrfToken(String controllerUrl) throws IOException {
        Document doc = Jsoup.connect(controllerUrl).get();
        Element element = doc.selectFirst("input[name='csrfToken']");

        if(element == null ){
            return null;
        } else {
            return element.attributes().get("value");
        }
    }

    private RequestConfig getDefaultRequestConfig(){
        return RequestConfig.custom()
                .setConnectTimeout(20 * 1000) // 20 seconds
                .setSocketTimeout(20 * 1000) // 20 seconds
                .build();

    }
}
