package org.airsonic.player.validator;

import junit.framework.TestCase;
import org.airsonic.player.command.PasswordSettingsCommand;
import org.junit.Before;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

public class PasswordSettingsValidatorTestCase extends TestCase {

    private PasswordSettingsCommand psc;

    @Before
    public void setUp() throws Exception {
        psc = new PasswordSettingsCommand();
        psc.setUsername("username");
        psc.setPassword("1234");
    }

    private Errors validatePassword(){
        PasswordSettingsValidator psv = new PasswordSettingsValidator();
        Errors errors = new BeanPropertyBindingResult(psc, "psv");
        psv.validate(psc, errors);
        return  errors;
    }

    public void testValidateEmptyPassword() {
        psc.setPassword("");
        Errors errors = this.validatePassword();
        assertTrue(errors.hasErrors());
    }

    public void testValidateDifferentPasswords() {
        psc.setConfirmPassword("5678");

        Errors errors = this.validatePassword();
        assertTrue(errors.hasErrors());
    }

    public void testValidateEverythingOK() {
        psc.setConfirmPassword("1234");

        Errors errors = this.validatePassword();
        assertFalse(errors.hasErrors());
    }
}
