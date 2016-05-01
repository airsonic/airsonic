/*
 This file is part of Subsonic.

 Subsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Subsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Subsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2009 (C) Sindre Mehus
 */
package net.sourceforge.subsonic.i18n;

import net.sourceforge.subsonic.service.*;
import net.sourceforge.subsonic.domain.*;
import org.springframework.web.servlet.*;

import javax.servlet.http.*;
import java.util.*;

/**
 * Locale resolver implementation which returns the locale selected in the settings.
 *
 * @author Sindre Mehus
 */
public class SubsonicLocaleResolver implements LocaleResolver {

    private SecurityService securityService;
    private SettingsService settingsService;
    private Set<Locale> locales;

    /**
    * Resolve the current locale via the given request.
    *
    * @param request Request to be used for resolution.
    * @return The current locale.
    */
    public Locale resolveLocale(HttpServletRequest request) {
        Locale locale = (Locale) request.getAttribute("subsonic.locale");
        if (locale != null) {
            return locale;
        }

        // Optimization: Cache locale in the request.
        locale = doResolveLocale(request);
        request.setAttribute("subsonic.locale", locale);

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
