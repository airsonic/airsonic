package org.libresonic.player.controller;

import org.apache.commons.lang.ObjectUtils;
import org.libresonic.player.service.SettingsService;

import javax.servlet.http.HttpServletRequest;

/**
 * This class has been created to refactor code previously present
 * in the MultiController.
 */
public class ControllerUtils {

    public static void updatePortAndContextPath(HttpServletRequest request, SettingsService settingsService) {

        int port = Integer.parseInt(System.getProperty("libresonic.port", String.valueOf(request.getLocalPort())));
        int httpsPort = Integer.parseInt(System.getProperty("libresonic.httpsPort", "0"));

        String contextPath = request.getContextPath().replace("/", "");

        if (settingsService.getPort() != port) {
            settingsService.setPort(port);
            settingsService.save();
        }
        if (settingsService.getHttpsPort() != httpsPort) {
            settingsService.setHttpsPort(httpsPort);
            settingsService.save();
        }
        if (!ObjectUtils.equals(settingsService.getUrlRedirectContextPath(), contextPath)) {
            settingsService.setUrlRedirectContextPath(contextPath);
            settingsService.save();
        }
    }

}
