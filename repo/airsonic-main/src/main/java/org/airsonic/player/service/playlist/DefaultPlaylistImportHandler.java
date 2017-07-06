package org.airsonic.player.service.playlist;

import chameleon.playlist.*;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.service.MediaFileService;
import org.airsonic.player.util.Pair;
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
                public void beginVisitPlaylist(Playlist playlist) throws Exception {

                }

                @Override
                public void endVisitPlaylist(Playlist playlist) throws Exception {

                }

                @Override
                public void beginVisitParallel(Parallel parallel) throws Exception {

                }

                @Override
                public void endVisitParallel(Parallel parallel) throws Exception {

                }

                @Override
                public void beginVisitSequence(Sequence sequence) throws Exception {

                }

                @Override
                public void endVisitSequence(Sequence sequence) throws Exception {

                }

                @Override
                public void beginVisitMedia(Media media) throws Exception {
                    try {
                        URI uri = media.getSource().getURI();
                        File file = new File(uri);
                        MediaFile mediaFile = mediaFileService.getMediaFile(file);
                        if(mediaFile != null) {
                            mediaFiles.add(mediaFile);
                        } else {
                            errors.add("Cannot find media file " + file);
                        }
                    } catch (Exception e) {
                        errors.add(e.getMessage());
                    }
                }

                @Override
                public void endVisitMedia(Media media) throws Exception {

                }
            });
        } catch (Exception e) {
            errors.add(e.getMessage());
        }

        return Pair.create(mediaFiles, errors);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
