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

/**
 * Locates services for objects that are not part of the Spring context.
 *
 * @author Sindre Mehus
 */
@Deprecated
public class ServiceLocator {

    private static SettingsService settingsService;
    private static VersionService versionService;

    private ServiceLocator() {
    }

    public static SettingsService getSettingsService() {
        return settingsService;
    }

    public static void setSettingsService(SettingsService settingsService) {
        ServiceLocator.settingsService = settingsService;
    }

    public static VersionService getVersionService() {
        return versionService;
    }

    public static void setVersionService(VersionService versionService) {
        ServiceLocator.versionService = versionService;
    }
}

