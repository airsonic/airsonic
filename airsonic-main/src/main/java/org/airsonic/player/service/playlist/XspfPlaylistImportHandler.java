package org.airsonic.player.service.playlist;

import chameleon.playlist.SpecificPlaylist;
import chameleon.playlist.xspf.Location;
import chameleon.playlist.xspf.Playlist;
import chameleon.playlist.xspf.StringContainer;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.service.MediaFileService;
import org.airsonic.player.util.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class XspfPlaylistImportHandler implements PlaylistImportHandler {

    @Autowired
    MediaFileService mediaFileService;

    @Override
    public boolean canHandle(Class<? extends SpecificPlaylist> playlistClass) {
        return Playlist.class.equals(playlistClass);
    }

    @Override
    public Pair<List<MediaFile>, List<String>> handle(SpecificPlaylist inputSpecificPlaylist) {
        List<MediaFile> mediaFiles = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        Playlist xspfPlaylist = (Playlist) inputSpecificPlaylist;
        xspfPlaylist.getTracks().forEach(track -> {
            MediaFile mediaFile = null;
            for (StringContainer sc : track.getStringContainers()) {
                if (sc instanceof Location) {
                    Location location = (Location) sc;
                    try {
                        File file = new File(new URI(location.getText()));
                        mediaFile = mediaFileService.getMediaFile(file);
                    } catch (Exception ignored) {}

                    if (mediaFile == null) {
                        try {
                            File file = new File(sc.getText());
                            mediaFile = mediaFileService.getMediaFile(file);
                        } catch (Exception ignored) {}
                    }
                }
            }
            if (mediaFile != null) {
                mediaFiles.add(mediaFile);
            } else {
                String errorMsg = "Could not find media file matching ";
                try {
                    errorMsg += track.getStringContainers().stream().map(StringContainer::getText).collect(Collectors.joining(","));
                } catch (Exception ignored) {}
                errors.add(errorMsg);
            }
        });
        return Pair.create(mediaFiles, errors);
    }

    @Override
    public int getOrder() {
        return 40;
    }
}
