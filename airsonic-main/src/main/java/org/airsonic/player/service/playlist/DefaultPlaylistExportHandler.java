package org.airsonic.player.service.playlist;

import chameleon.content.Content;
import chameleon.playlist.Media;
import chameleon.playlist.Playlist;
import chameleon.playlist.SpecificPlaylist;
import chameleon.playlist.SpecificPlaylistProvider;
import org.airsonic.player.dao.MediaFileDao;
import org.airsonic.player.domain.MediaFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DefaultPlaylistExportHandler implements PlaylistExportHandler {

    @Autowired
    MediaFileDao mediaFileDao;

    @Override
    public boolean canHandle(Class<? extends SpecificPlaylistProvider> providerClass) {
        return true;
    }

    @Override
    public SpecificPlaylist handle(int id, SpecificPlaylistProvider provider) throws Exception {
        chameleon.playlist.Playlist playlist = createChameleonGenericPlaylistFromDBId(id);
        return provider.toSpecificPlaylist(playlist);
    }

    Playlist createChameleonGenericPlaylistFromDBId(int id) {
        Playlist newPlaylist = new Playlist();
        List<MediaFile> files = mediaFileDao.getFilesInPlaylist(id);
        files.forEach(file -> {
            Media component = new Media();
            Content content = new Content(file.getPath());
            component.setSource(content);
            newPlaylist.getRootSequence().addComponent(component);
        });
        return newPlaylist;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
