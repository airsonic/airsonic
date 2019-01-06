package org.airsonic.player.controller;

import com.google.common.base.Strings;
import org.airsonic.player.service.SecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.websocket.server.PathParam;

import java.util.HashMap;
import java.util.Map;

/**
 * For sonos link procedure.
 * <p>
 * The first call is : /sonoslink/xxxxxxxxx
 */
@Controller
public class SonosLinkController {

    @Autowired
    private SecurityService securityService;

    @GetMapping(value = "/sonoslink")
    public ModelAndView sonoslink() throws Exception {
        return new ModelAndView("sonoslink");
    }


    @PostMapping(value = "/sonoslink")
    public ModelAndView sonoslink(@RequestParam(value = "j_username", required = false) String username,
                                  @RequestParam(value = "j_password", required = false) String password,
                                  @RequestParam("householdid") String householdId,
                                  @RequestParam("linkCode") String linkCode) throws Exception {
        Map<String, Object> map = new HashMap<>();

        if (Strings.isNullOrEmpty(username) || Strings.isNullOrEmpty(password)) {
            map.put("errorCode", "sonos.user.notpresent");

        } else {
            try {
                Authentication auth = securityService.authenticate(username, password);

                if (auth != null && auth.isAuthenticated()) {
                    if (securityService.authoriseSonos(username, householdId, linkCode)) {
                        map.put("messageKey", "sonos.link.ok");
                        return new ModelAndView("sonoslinkClose", "model", map);

                    } else {
                        map.put("messageKey", "sonos.linkcode.allreadyused");
                        return new ModelAndView("sonoslinkClose", "model", map);
                    }
                } else {
                    map.put("messageKey", "sonos.cannot.authorise");
                }
            } catch (BadCredentialsException e) {
                map.put("householdid", securityService.getHousehold(linkCode));
                map.put("linkCode", linkCode);
                map.put("errorCode", "login.error");
            }
        }

        return new ModelAndView("sonoslink", "model", map);
    }


    @GetMapping(value = "/sonoslink", params = "linkCode")
    public ModelAndView linkCode(@PathParam("linkCode") String linkCode) throws Exception {
        Map<String, Object> map = new HashMap<>();

        String householdid = securityService.getHousehold(linkCode);

        if (householdid == null) {
            map.put("messageKey", "sonos.householdid.notfound");

            return new ModelAndView("sonoslinkClose", "model", map);

        } else {
            map.put("householdid", householdid);
            map.put("linkCode", linkCode);

            return new ModelAndView("sonoslink", "model", map);
        }

    }


}
