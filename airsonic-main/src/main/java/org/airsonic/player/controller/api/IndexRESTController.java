package org.airsonic.player.controller.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping(value = "/api")
public class IndexRESTController {

    private static final Logger LOG = LoggerFactory.getLogger(IndexRESTController.class);

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<Void> index() {
        return ResponseEntity.noContent().build();
    }

}
