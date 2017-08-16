package org.airsonic.player.controller.api

import org.airsonic.player.dto.MusicFolder
import org.airsonic.player.dto.MusicFoldersResponse
import org.airsonic.player.service.SecurityService
import org.airsonic.player.service.SettingsService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping(value = "api/musicfolders")
class MusicFoldersController @Autowired constructor(val securityService: SecurityService,
                                                    val settingsService: SettingsService) {
    @GetMapping
    fun getMusicFolders(request: HttpServletRequest): ResponseEntity<MusicFoldersResponse> {
        val userName = securityService.getCurrentUsername(request)
        val musicFolders = settingsService.getMusicFoldersForUser(userName).map {
            MusicFolder(it.id, it.name)
        }
        return ResponseEntity.ok(MusicFoldersResponse(musicFolders))
    }
}