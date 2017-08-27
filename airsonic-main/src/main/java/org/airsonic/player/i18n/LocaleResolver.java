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
package org.airsonic.player.i18n;

import org.airsonic.player.domain.UserSettings;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.SettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
/**
 * Locale resolver implementation which returns the locale selected in the settings.
 *
 * @author Sindre Mehus
 */
@Service
public class LocaleResolver implements org.springframework.web.servlet.LocaleResolver {

    @Autowired
    private SecurityService securityService;
    @Autowired
    private SettingsService settingsService;
    private Set<Locale> locales;

    /**
    * Resolve the current locale via the given request.
    *
    * @param request Request to be used for resolution.
    * @return The current locale.
    */
    public Locale resolveLocale(HttpServletRequest request) {
        Locale locale = (Locale) request.getAttribute("airsonic.locale");
        if (locale != null) {
            return locale;
        }

        // Optimization: Cache locale in the request.
        locale = doResolveLocale(request);
        request.setAttribute("airsonic.locale", locale);

        return locale;
    }

    private Locale doResolveLocale(HttpServletRequest request) {
        Locale locale = null;

        // Look for user-specific locale.
        String username = securityService.getCurrentUsername(request);
        if (username != null) {
            UserSettings userSettings = settingsService.getUserSettings(username);
            if (userSettings != null) {
                locale = userSettings.getLocale();
            }
        }

        if (locale != null && localeExists(locale)) {
            return locale;
        }

        // Return system locale.
        locale = settingsService.getLocale();
        return localeExists(locale) ? locale : Locale.ENGLISH;
    }

    /**
     * Returns whether the given locale exists.
     * @param locale The locale.
     * @return Whether the locale exists.
     */
    private synchronized boolean localeExists(Locale locale) {
        // Lazily create set of locales.
        if (locales == null) {
            locales = new HashSet<Locale>(Arrays.asList(settingsService.getAvailableLocales()));
        }

        return locales.contains(locale);
    }

    public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
        throw new UnsupportedOperationException("Cannot change locale - use a different locale resolution strategy");
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }
}
