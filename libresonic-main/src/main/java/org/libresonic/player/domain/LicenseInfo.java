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
package org.libresonic.player.domain;

import java.util.Date;

import org.apache.commons.lang.StringUtils;

import org.libresonic.player.service.SettingsService;

/**
 * Controller for the "Podcast receiver" page.
 *
 * @author Sindre Mehus
 */
public class LicenseInfo {

    private String licenseEmail;
    private boolean licenseValid;
    private final Date trialExpires;
    private long trialDaysLeft;
    private final Date licenseExpires;

    public LicenseInfo(String licenseEmail, boolean licenseValid, Date trialExpires,
            long trialDaysLeft, Date licenseExpires) {
        this.licenseEmail = licenseEmail;
        this.licenseValid = licenseValid;
        this.trialExpires = trialExpires;
        this.trialDaysLeft = trialDaysLeft;
        this.licenseExpires = licenseExpires;
    }

    public String getLicenseEmail() {
        return licenseEmail;
    }

    public void setLicenseEmail(String licenseEmail) {
        this.licenseEmail = StringUtils.trimToNull(licenseEmail);
    }

    public boolean isLicenseValid() {
        return licenseValid;
    }

    public void setLicenseValid(boolean licenseValid) {
        this.licenseValid = licenseValid;
    }

    public boolean isTrial() {
        return trialExpires != null && !licenseValid;
    }

    public boolean isTrialExpired() {
        return trialExpires != null && (trialExpires.before(new Date()) || trialDaysLeft > SettingsService.TRIAL_DAYS + 1);
    }

    public boolean isLicenseOrTrialValid() {
        return isLicenseValid() || !isTrialExpired();
    }

    public Date getTrialExpires() {
        return trialExpires;
    }

    public long getTrialDaysLeft() {
        return trialDaysLeft;
    }

    public Date getLicenseExpires() {
        return licenseExpires;
    }
}
