package org.airsonic.player.spring;

import org.airsonic.player.dao.MediaFileDao;
import org.airsonic.player.service.*;
import org.airsonic.player.service.sonos.SonosHelper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@Configuration
@ImportResource({"classpath:META-INF/cxf/cxf.xml", "classpath:META-INF/cxf/cxf-servlet.xml", "classpath:applicationContext-sonos.xml"})
public class SonosConfiguration {
    @Bean
    public SonosHelper sonosHelper(MediaFileService mediaFileService, SettingsService settingsService, PlaylistService playlistService, PlayerService playerService, TranscodingService transcodingService, MusicIndexService musicIndexService, SearchService searchService, RatingService ratingService, LastFmService lastFmService, PodcastService podcastService, MediaFileDao mediaFileDao) {
        SonosHelper sonosHelper = new SonosHelper();
        sonosHelper.setMediaFileService(mediaFileService);
        sonosHelper.setSettingsService(settingsService);
        sonosHelper.setPlaylistService(playlistService);
        sonosHelper.setPlayerService(playerService);
        sonosHelper.setTranscodingService(transcodingService);
        sonosHelper.setMusicIndexService(musicIndexService);
        sonosHelper.setSearchService(searchService);
        sonosHelper.setRatingService(ratingService);
        sonosHelper.setLastFmService(lastFmService);
        sonosHelper.setPodcastService(podcastService);
        sonosHelper.setMediaFileDao(mediaFileDao);
        return sonosHelper;
    }
}
