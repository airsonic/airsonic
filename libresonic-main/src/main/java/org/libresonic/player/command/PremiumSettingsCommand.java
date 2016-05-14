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
package org.libresonic.player.command;

import org.apache.commons.lang.StringUtils;

import org.libresonic.player.controller.PremiumSettingsController;
import org.libresonic.player.domain.LicenseInfo;
import org.libresonic.player.domain.User;

/**
 * Command used in {@link PremiumSettingsController}.
 *
 * @author Sindre Mehus
 */
public class PremiumSettingsCommand {

    private String path;
    private String brand;
    private LicenseInfo licenseInfo;
    private String licenseCode;
    private boolean forceChange;
    private boolean submissionError;
    private User user;
    private boolean toast;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public LicenseInfo getLicenseInfo() {
        return licenseInfo;
    }

    public String getLicenseCode() {
        return licenseCode;
    }

    public void setLicenseCode(String licenseCode) {
        this.licenseCode = StringUtils.trimToNull(licenseCode);
    }

    public void setLicenseInfo(LicenseInfo licenseInfo) {
        this.licenseInfo = licenseInfo;
    }

    public boolean isForceChange() {
        return forceChange;
    }

    public void setForceChange(boolean forceChange) {
        this.forceChange = forceChange;
    }

    public boolean isSubmissionError() {
        return submissionError;
    }

    public void setSubmissionError(boolean submissionError) {
        this.submissionError = submissionError;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    public void setToast(boolean toast) {
        this.toast = toast;
    }

    public boolean isToast() {
        return toast;
    }
}
