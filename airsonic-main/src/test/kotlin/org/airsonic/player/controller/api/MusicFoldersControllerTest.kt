package org.airsonic.player.controller.api

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import org.airsonic.player.domain.MusicFolder
import org.airsonic.player.service.SecurityService
import org.airsonic.player.service.SettingsService
import org.junit.Assert.assertEquals
import org.junit.Test
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import java.io.File
import java.util.*

class MusicFoldersControllerTest {
    @Test
    fun getMusicFolders_ShouldReturnMusicFoldersForUser() {
        val securityServiceMock = mock<SecurityService> {
            on { getCurrentUsername(any())} doReturn "Ben"
        }
        val settingsServiceMock = mock<SettingsService> {
            on { getMusicFoldersForUser("Ben") } doReturn listOf(MusicFolder(1, File("/var/music"), "Music", true, Date()))
        }

        val tested = MusicFoldersController(securityServiceMock, settingsServiceMock)

        val result = tested.getMusicFolders(MockHttpServletRequest())

        assertEquals(result.statusCode, HttpStatus.OK)
        assertEquals(result.body.musicFolders.size, 1)
        result.body.musicFolders.first().let {
            assertEquals(it.name, "Music")
            assertEquals(it.id, 1)
        }
    }
}