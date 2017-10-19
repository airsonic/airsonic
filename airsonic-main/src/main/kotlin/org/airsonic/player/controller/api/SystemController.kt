package org.airsonic.player.controller.api

import org.airsonic.player.dto.PingResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(value = "api/system")
class SystemController {
    @GetMapping("/ping")
    fun getPing(): ResponseEntity<PingResponse> {
        return ResponseEntity.ok(PingResponse("ok"))
    }
}