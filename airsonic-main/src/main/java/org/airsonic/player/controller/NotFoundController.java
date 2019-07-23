package org.airsonic.player.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Spring MVC Controller that serves the login page.
 */
@Controller
@RequestMapping("/notFound")
public class NotFoundController {

    @GetMapping
    public ModelAndView notFound(HttpServletRequest request, HttpServletResponse response) {
        return new ModelAndView("notFound");
    }
}
