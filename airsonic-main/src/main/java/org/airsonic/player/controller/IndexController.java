package org.airsonic.player.controller;

import org.airsonic.player.domain.UserSettings;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.SettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/index")
public class IndexController {

    @Autowired
    private SecurityService securityService;
    @Autowired
    private SettingsService settingsService;

    @GetMapping
    public ModelAndView index(HttpServletRequest request) {
        UserSettings userSettings = settingsService.getUserSettings(securityService.getCurrentUsername(request));

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("showRight", userSettings.isShowNowPlayingEnabled());
        map.put("autoHidePlayQueue", userSettings.isAutoHidePlayQueue());
        map.put("listReloadDelay", userSettings.getListReloadDelay());
        map.put("keyboardShortcutsEnabled", userSettings.isKeyboardShortcutsEnabled());
        map.put("showSideBar", userSettings.isShowSideBar());
        map.put("brand", settingsService.getBrand());
        return new ModelAndView("index", "model", map);
    }
}
