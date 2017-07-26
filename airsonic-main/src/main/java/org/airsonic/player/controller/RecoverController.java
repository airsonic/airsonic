package org.airsonic.player.controller;

import net.tanesha.recaptcha.ReCaptcha;
import net.tanesha.recaptcha.ReCaptchaFactory;
import net.tanesha.recaptcha.ReCaptchaResponse;
import org.airsonic.player.domain.User;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.SettingsService;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Spring MVC Controller that serves the login page.
 */
@Controller
@RequestMapping("/recover")
public class RecoverController {


    private static final Logger LOG = LoggerFactory.getLogger(RecoverController.class);

    @Autowired
    private SettingsService settingsService;
    @Autowired
    private SecurityService securityService;

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.POST})
    public ModelAndView recover(HttpServletRequest request, HttpServletResponse response) throws Exception {

        Map<String, Object> map = new HashMap<String, Object>();
        String usernameOrEmail = StringUtils.trimToNull(request.getParameter("usernameOrEmail"));
        ReCaptcha captcha = ReCaptchaFactory.newSecureReCaptcha("6LcZ3OMSAAAAANkKMdFdaNopWu9iS03V-nLOuoiH",
                "6LcZ3OMSAAAAAPaFg89mEzs-Ft0fIu7wxfKtkwmQ", false);
        boolean showCaptcha = true;

        if (usernameOrEmail != null) {

            map.put("usernameOrEmail", usernameOrEmail);
            User user = getUserByUsernameOrEmail(usernameOrEmail);
            String challenge = request.getParameter("recaptcha_challenge_field");
            String uresponse = request.getParameter("recaptcha_response_field");
            ReCaptchaResponse captchaResponse = captcha.checkAnswer(request.getRemoteAddr(), challenge, uresponse);

            if (!captchaResponse.isValid()) {
                map.put("error", "recover.error.invalidcaptcha");
            } else if (user == null) {
                map.put("error", "recover.error.usernotfound");
            } else if (user.getEmail() == null) {
                map.put("error", "recover.error.noemail");
            } else {
                String password = RandomStringUtils.randomAlphanumeric(8);
                if (emailPassword(password, user.getUsername(), user.getEmail())) {
                    map.put("sentTo", user.getEmail());
                    user.setLdapAuthenticated(false);
                    user.setPassword(password);
                    securityService.updateUser(user);
                    showCaptcha = false;
                } else {
                    map.put("error", "recover.error.sendfailed");
                }
            }
        }

        if (showCaptcha) {
            map.put("captcha", captcha.createRecaptchaHtml(null, null));
        }

        return new ModelAndView("recover", "model", map);
    }

    private User getUserByUsernameOrEmail(String usernameOrEmail) {
        if (usernameOrEmail != null) {
            User user = securityService.getUserByName(usernameOrEmail);
            if (user != null) {
                return user;
            }
            return securityService.getUserByEmail(usernameOrEmail);
        }
        return null;
    }

    /*
    * e-mail user new password via configured Smtp server
    */
    private boolean emailPassword(String password, String username, String email) {
        /* Default to protocol smtp when SmtpEncryption is set to "None" */
        String prot = "smtp";

        if (settingsService.getSmtpServer() == null || settingsService.getSmtpServer().isEmpty()) {
            LOG.warn("Can not send email; no Smtp server configured.");
            return false;
        }

        Properties props = new Properties();
        if (settingsService.getSmtpEncryption().equals("SSL/TLS")) {
            prot = "smtps";
            props.put("mail." + prot + ".ssl.enable", "true");
        } else if (settingsService.getSmtpEncryption().equals("STARTTLS")) {
            prot = "smtp";
            props.put("mail." + prot + ".starttls.enable", "true");
        }
        props.put("mail." + prot + ".host", settingsService.getSmtpServer());
        props.put("mail." + prot + ".port", settingsService.getSmtpPort());
        /* use authentication when SmtpUser is configured */
        if (settingsService.getSmtpUser() != null && !settingsService.getSmtpUser().isEmpty()) {
            props.put("mail." + prot + ".auth", "true");
        }

        Session session = Session.getInstance(props, null);

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(settingsService.getSmtpFrom()));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
            message.setSubject("Airsonic Password");
            message.setText("Hi there!\n\n" +
                    "You have requested to reset your Airsonic password.  Please find your new login details below.\n\n" +
                    "Username: " + username + "\n" +
                    "Password: " + password + "\n\n" +
                    "--\n" +
                    "Your Airsonic server\n" +
                    "airsonic.github.io/");
            message.setSentDate(new Date());

            Transport trans = session.getTransport(prot);
            try {
                if (props.get("mail." + prot + ".auth") != null && props.get("mail." + prot + ".auth").equals("true")) {
                    trans.connect(settingsService.getSmtpServer(), settingsService.getSmtpUser(), settingsService.getSmtpPassword());
                } else {
                    trans.connect();
                }
                trans.sendMessage(message, message.getAllRecipients());
            } finally {
                trans.close();
            }
            return true;

        } catch (Exception x) {
            LOG.warn("Failed to send email.", x);
            return false;
        }
    }

}
