package org.airsonic.player.dto

data class PingResponse(val status: String)
data class MusicFoldersResponse(val musicFolders: List<MusicFolder>)
data class MusicFolder(val id: Int, val name: String)