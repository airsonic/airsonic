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
package net.sourceforge.subsonic.validator;

import org.springframework.validation.*;
import net.sourceforge.subsonic.command.*;
import net.sourceforge.subsonic.controller.*;

/**
 * Validator for {@link PasswordSettingsController}.
 *
 * @author Sindre Mehus
 */
public class PasswordSettingsValidator implements Validator {

    public boolean supports(Class clazz) {
        return clazz.equals(PasswordSettingsCommand.class);
    }

    public void validate(Object obj, Errors errors) {
        PasswordSettingsCommand command = (PasswordSettingsCommand) obj;

        if (command.getPassword() == null || command.getPassword().length() == 0) {
            errors.rejectValue("password", "usersettings.nopassword");
        } else if (!command.getPassword().equals(command.getConfirmPassword())) {
            errors.rejectValue("password", "usersettings.wrongpassword");
        }
    }
}
