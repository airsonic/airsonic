package org.airsonic.player.service.playlist;

import chameleon.playlist.SpecificPlaylist;
import chameleon.playlist.SpecificPlaylistProvider;
import org.springframework.core.Ordered;

public interface PlaylistExportHandler extends Ordered {
    boolean canHandle(Class<? extends SpecificPlaylistProvider> providerClass);

    SpecificPlaylist handle(int id, SpecificPlaylistProvider provider) throws Exception;
}
