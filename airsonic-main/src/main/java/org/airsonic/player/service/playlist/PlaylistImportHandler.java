package org.airsonic.player.service.playlist;

import chameleon.playlist.SpecificPlaylist;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.util.Pair;
import org.springframework.core.Ordered;

import java.util.List;

public interface PlaylistImportHandler extends Ordered {
    boolean canHandle(Class<? extends SpecificPlaylist> playlistClass);

    Pair<List<MediaFile>,List<String>> handle(SpecificPlaylist inputSpecificPlaylist);
}
