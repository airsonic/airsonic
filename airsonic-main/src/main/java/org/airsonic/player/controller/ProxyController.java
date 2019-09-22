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
package org.airsonic.player.controller;

import org.airsonic.player.util.FileUtil;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.InputStream;

import static org.springframework.http.HttpStatus.OK;

/**
 * A proxy for external HTTP requests.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/proxy")
public class ProxyController  {

    @GetMapping
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String url = ServletRequestUtils.getRequiredStringParameter(request, "url");

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(15000)
                .setSocketTimeout(15000)
                .build();
        HttpGet method = new HttpGet(url);
        method.setConfig(requestConfig);

        InputStream in = null;
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            try (CloseableHttpResponse resp = client.execute(method)) {
                int statusCode = resp.getStatusLine().getStatusCode();
                if (statusCode != OK.value()) {
                    response.sendError(statusCode);
                } else {
                    in = resp.getEntity().getContent();
                    IOUtils.copy(in, response.getOutputStream());
                }
            }
        } finally {
            FileUtil.closeQuietly(in);
        }
        return null;
    }
}
