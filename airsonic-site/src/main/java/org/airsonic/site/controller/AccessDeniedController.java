package org.airsonic.site.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Spring MVC Controller that serves the login page.
 */
@Controller
@RequestMapping("/accessDenied")
public class AccessDeniedController {


    private static final Logger LOG = LoggerFactory.getLogger(AccessDeniedController.class);

    @RequestMapping(method = {RequestMethod.GET})
    public ModelAndView accessDenied(HttpServletRequest request, HttpServletResponse response) {
        return new ModelAndView("accessDenied");
    }


}
