package org.libresonic.player.controller;

import org.libresonic.player.Logger;
import org.libresonic.player.domain.User;
import org.libresonic.player.service.PlaylistService;
import org.libresonic.player.service.SecurityService;
import org.libresonic.player.service.SettingsService;
import org.libresonic.player.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * Spring MVC Controller that serves the login page.
 */
@Controller
@RequestMapping("/login")
public class LoginController {


    private static final Logger LOG = Logger.getLogger(LoginController.class);

    @Autowired
    private final SecurityService securityService = null;
    @Autowired
    private final SettingsService settingsService = null;

    @RequestMapping(method = { RequestMethod.GET, RequestMethod.POST })
    public ModelAndView login(HttpServletRequest request, HttpServletResponse response) throws Exception {

        // Auto-login if "user" and "password" parameters are given.
        String username = request.getParameter("user");
        String password = request.getParameter("password");
        if (username != null && password != null) {
            username = StringUtil.urlEncode(username);
            password = StringUtil.urlEncode(password);
            return new ModelAndView(new RedirectView("/j_spring_security_check?"+
                    UsernamePasswordAuthenticationFilter.SPRING_SECURITY_FORM_USERNAME_KEY+"=" + username +
                    "&"+UsernamePasswordAuthenticationFilter.SPRING_SECURITY_FORM_PASSWORD_KEY+"=" + password
            ));
        }

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("logout", request.getParameter("logout") != null);
        map.put("error", request.getParameter("error") != null);
        map.put("brand", settingsService.getBrand());
        map.put("loginMessage", settingsService.getLoginMessage());

        User admin = securityService.getUserByName(User.USERNAME_ADMIN);
        if (User.USERNAME_ADMIN.equals(admin.getPassword())) {
            map.put("insecure", true);
        }

        return new ModelAndView("login", "model", map);
    }

}
