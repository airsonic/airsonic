package org.airsonic.player.controller.api;

import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.VersionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(value = "/api")
public class IndexRESTController {

    private static final Logger LOG = LoggerFactory.getLogger(IndexRESTController.class);

    @Autowired
    private SecurityService securityService;
    @Autowired
    private VersionService versionService;

    @RequestMapping(method = RequestMethod.GET)
    public Map<String,String> index(HttpServletRequest request) {
        String username = securityService.getCurrentUsername(request);
        Map<String,String> responseMap = new HashMap<>();
        responseMap.put("greeting","Welcome to the Airsonic REST API");
        responseMap.put("version",versionService.getLocalVersion().toString());
        responseMap.put("username",username);
        return responseMap;
    }

}
