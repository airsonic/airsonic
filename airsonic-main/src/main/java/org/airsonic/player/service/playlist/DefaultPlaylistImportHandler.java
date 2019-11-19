package org.airsonic.player.service.playlist;

import chameleon.playlist.*;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.service.MediaFileService;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Component
public class DefaultPlaylistImportHandler implements PlaylistImportHandler {

    @Autowired
    MediaFileService mediaFileService;

    @Override
    public boolean canHandle(Class<? extends SpecificPlaylist> playlistClass) {
        return true;
    }

    @Override
    public Pair<List<MediaFile>, List<String>> handle(
            SpecificPlaylist inputSpecificPlaylist
    ) {
        List<MediaFile> mediaFiles = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        try {
            inputSpecificPlaylist.toPlaylist().acceptDown(new PlaylistVisitor() {
                @Override
                public void beginVisitPlaylist(Playlist playlist) {

                }

                @Override
                public void endVisitPlaylist(Playlist playlist) {

                }

                @Override
                public void beginVisitParallel(Parallel parallel) {

                }

                @Override
                public void endVisitParallel(Parallel parallel) {

                }

                @Override
                public void beginVisitSequence(Sequence sequence) {

                }

                @Override
                public void endVisitSequence(Sequence sequence) {

                }

                @Override
                public void beginVisitMedia(Media media) {
                    try {
                        URI uri = media.getSource().getURI();
                        File file = new File(uri);
                        MediaFile mediaFile = mediaFileService.getMediaFile(file);
                        if (mediaFile != null) {
                            mediaFiles.add(mediaFile);
                        } else {
                            errors.add("Cannot find media file " + file);
                        }
                    } catch (Exception e) {
                        errors.add(e.getMessage());
                    }
                }

                @Override
                public void endVisitMedia(Media media) {

                }
            });
        } catch (Exception e) {
            errors.add(e.getMessage());
        }

        return Pair.of(mediaFiles, errors);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
