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

 Copyright 2009 (C) Sindre Mehus
 */
package org.libresonic.player.validator;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import org.libresonic.player.command.PremiumSettingsCommand;
import org.libresonic.player.controller.PremiumSettingsController;
import org.libresonic.player.service.SettingsService;

/**
 * Validator for {@link PremiumSettingsController}.
 *
 * @author Sindre Mehus
 */
public class PremiumSettingsValidator implements Validator {
    private SettingsService settingsService;

    public boolean supports(Class clazz) {
        return clazz.equals(PremiumSettingsCommand.class);
    }

    public void validate(Object obj, Errors errors) {
        PremiumSettingsCommand command = (PremiumSettingsCommand) obj;

        if (!settingsService.isLicenseValid(command.getLicenseInfo().getLicenseEmail(), command.getLicenseCode())) {
            command.setSubmissionError(true);
            errors.rejectValue("licenseCode", "premium.invalidlicense");
        }
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }
}
