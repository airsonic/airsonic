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
package org.libresonic.player.controller;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.tanesha.recaptcha.ReCaptcha;
import net.tanesha.recaptcha.ReCaptchaFactory;
import net.tanesha.recaptcha.ReCaptchaResponse;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.libresonic.player.Logger;
import org.libresonic.player.domain.Playlist;
import org.libresonic.player.domain.User;
import org.libresonic.player.domain.UserSettings;
import org.libresonic.player.service.PlaylistService;
import org.libresonic.player.service.SecurityService;
import org.libresonic.player.service.SettingsService;
import org.libresonic.player.util.StringUtil;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.multiaction.MultiActionController;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Multi-controller used for simple pages.
 *
 * @author Sindre Mehus
 */
public class MultiController extends MultiActionController {

    private static final Logger LOG = Logger.getLogger(MultiController.class);

    private SecurityService securityService;
    private SettingsService settingsService;
    private PlaylistService playlistService;

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
            message.setSubject("Libresonic Password");
            message.setText("Hi there!\n\n" +
                    "You have requested to reset your Libresonic password.  Please find your new login details below.\n\n" +
                    "Username: " + username + "\n" +
                    "Password: " + password + "\n\n" +
                    "--\n" +
                    "Your Libresonic server\n" +
                    "libresonic.org");
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

    public ModelAndView accessDenied(HttpServletRequest request, HttpServletResponse response) {
        return new ModelAndView("accessDenied");
    }

    public ModelAndView notFound(HttpServletRequest request, HttpServletResponse response) {
        return new ModelAndView("notFound");
    }

    public ModelAndView gettingStarted(HttpServletRequest request, HttpServletResponse response) {
        updatePortAndContextPath(request);

        if (request.getParameter("hide") != null) {
            settingsService.setGettingStartedEnabled(false);
            settingsService.save();
            return new ModelAndView(new RedirectView("home.view"));
        }

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("runningAsRoot", "root".equals(System.getProperty("user.name")));
        return new ModelAndView("gettingStarted", "model", map);
    }

    public ModelAndView index(HttpServletRequest request, HttpServletResponse response) {
        updatePortAndContextPath(request);
        UserSettings userSettings = settingsService.getUserSettings(securityService.getCurrentUsername(request));

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("showRight", userSettings.isShowNowPlayingEnabled() || userSettings.isShowChatEnabled());
        map.put("autoHidePlayQueue", userSettings.isAutoHidePlayQueue());
        map.put("listReloadDelay", userSettings.getListReloadDelay());
        map.put("showSideBar", userSettings.isShowSideBar());
        map.put("brand", settingsService.getBrand());
        return new ModelAndView("index", "model", map);
    }

    public ModelAndView exportPlaylist(HttpServletRequest request, HttpServletResponse response) throws Exception {

        int id = ServletRequestUtils.getRequiredIntParameter(request, "id");
        Playlist playlist = playlistService.getPlaylist(id);
        if (!playlistService.isReadAllowed(playlist, securityService.getCurrentUsername(request))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return null;

        }
        response.setContentType("application/x-download");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + StringUtil.fileSystemSafe(playlist.getName()) + ".m3u8\"");

        playlistService.exportPlaylist(id, response.getOutputStream());
        return null;
    }

    private void updatePortAndContextPath(HttpServletRequest request) {

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

    public ModelAndView test(HttpServletRequest request, HttpServletResponse response) {
        return new ModelAndView("test");
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setPlaylistService(PlaylistService playlistService) {
        this.playlistService = playlistService;
    }
}
