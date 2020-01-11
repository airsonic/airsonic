package org.airsonic.player.service;

import com.google.common.collect.Lists;
import org.airsonic.player.dao.MediaFileDao;
import org.airsonic.player.dao.PlaylistDao;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.Playlist;
import org.airsonic.player.service.playlist.DefaultPlaylistImportHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PlaylistServiceTestImport {

    PlaylistService playlistService;

    @InjectMocks
    DefaultPlaylistImportHandler defaultPlaylistImportHandler;

    @Mock
    MediaFileDao mediaFileDao;

    @Mock
    PlaylistDao playlistDao;

    @Mock
    MediaFileService mediaFileService;

    @Mock
    SettingsService settingsService;

    @Mock
    SecurityService securityService;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Captor
    ArgumentCaptor<Playlist> actual;

    @Captor
    ArgumentCaptor<List<MediaFile>> medias;

    @Before
    public void setup() {
        playlistService = new PlaylistService(
                mediaFileDao,
                playlistDao,
                securityService,
                settingsService,
                Collections.emptyList(),
                Lists.newArrayList(defaultPlaylistImportHandler));

    }

    @Test
    public void testImportFromM3U() throws Exception {
        String username = "testUser";
        String playlistName = "test-playlist";
        StringBuilder builder = new StringBuilder();
        builder.append("#EXTM3U\n");
        File mf1 = folder.newFile();
        FileUtils.touch(mf1);
        File mf2 = folder.newFile();
        FileUtils.touch(mf2);
        File mf3 = folder.newFile();
        FileUtils.touch(mf3);
        builder.append(mf1.getAbsolutePath()).append("\n");
        builder.append(mf2.getAbsolutePath()).append("\n");
        builder.append(mf3.getAbsolutePath()).append("\n");
        doAnswer(new PersistPlayList(23)).when(playlistDao).createPlaylist(any());
        doAnswer(new MediaFileHasEverything()).when(mediaFileService).getMediaFile(any(File.class));
        InputStream inputStream = new ByteArrayInputStream(builder.toString().getBytes(StandardCharsets.UTF_8));
        String path = "/path/to/" + playlistName + ".m3u";
        playlistService.importPlaylist(username, playlistName, path, inputStream, null);
        verify(playlistDao).createPlaylist(actual.capture());
        verify(playlistDao).setFilesInPlaylist(eq(23), medias.capture());
        Playlist expected = new Playlist();
        expected.setUsername(username);
        expected.setName(playlistName);
        expected.setComment("Auto-imported from " + path);
        expected.setImportedFrom(path);
        expected.setShared(true);
        expected.setId(23);
        assertTrue("\n" + ToStringBuilder.reflectionToString(actual.getValue()) + "\n\n did not equal \n\n" + ToStringBuilder.reflectionToString(expected), EqualsBuilder.reflectionEquals(actual.getValue(), expected, "created", "changed"));
        List<MediaFile> mediaFiles = medias.getValue();
        assertEquals(3, mediaFiles.size());
    }

    @Test
    public void testImportFromPLS() throws Exception {
        String username = "testUser";
        String playlistName = "test-playlist";
        StringBuilder builder = new StringBuilder();
        builder.append("[playlist]\n");
        File mf1 = folder.newFile();
        FileUtils.touch(mf1);
        File mf2 = folder.newFile();
        FileUtils.touch(mf2);
        File mf3 = folder.newFile();
        FileUtils.touch(mf3);
        builder.append("File1=").append(mf1.getAbsolutePath()).append("\n");
        builder.append("File2=").append(mf2.getAbsolutePath()).append("\n");
        builder.append("File3=").append(mf3.getAbsolutePath()).append("\n");
        doAnswer(new PersistPlayList(23)).when(playlistDao).createPlaylist(any());
        doAnswer(new MediaFileHasEverything()).when(mediaFileService).getMediaFile(any(File.class));
        InputStream inputStream = new ByteArrayInputStream(builder.toString().getBytes(StandardCharsets.UTF_8));
        String path = "/path/to/" + playlistName + ".pls";
        playlistService.importPlaylist(username, playlistName, path, inputStream, null);
        verify(playlistDao).createPlaylist(actual.capture());
        verify(playlistDao).setFilesInPlaylist(eq(23), medias.capture());
        Playlist expected = new Playlist();
        expected.setUsername(username);
        expected.setName(playlistName);
        expected.setComment("Auto-imported from " + path);
        expected.setImportedFrom(path);
        expected.setShared(true);
        expected.setId(23);
        assertTrue("\n" + ToStringBuilder.reflectionToString(actual.getValue()) + "\n\n did not equal \n\n" + ToStringBuilder.reflectionToString(expected), EqualsBuilder.reflectionEquals(actual.getValue(), expected, "created", "changed"));
        List<MediaFile> mediaFiles = medias.getValue();
        assertEquals(3, mediaFiles.size());
    }

    @Test
    public void testImportFromXSPF() throws Exception {
        String username = "testUser";
        String playlistName = "test-playlist";
        StringBuilder builder = new StringBuilder();
        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                       + "<playlist version=\"1\" xmlns=\"http://xspf.org/ns/0/\">\n"
                       + "    <trackList>\n");
        File mf1 = folder.newFile();
        FileUtils.touch(mf1);
        File mf2 = folder.newFile();
        FileUtils.touch(mf2);
        File mf3 = folder.newFile();
        FileUtils.touch(mf3);
        builder.append("<track><location>file://").append(mf1.getAbsolutePath()).append("</location></track>\n");
        builder.append("<track><location>file://").append(mf2.getAbsolutePath()).append("</location></track>\n");
        builder.append("<track><location>file://").append(mf3.getAbsolutePath()).append("</location></track>\n");
        builder.append("    </trackList>\n" + "</playlist>\n");
        doAnswer(new PersistPlayList(23)).when(playlistDao).createPlaylist(any());
        doAnswer(new MediaFileHasEverything()).when(mediaFileService).getMediaFile(any(File.class));
        InputStream inputStream = new ByteArrayInputStream(builder.toString().getBytes(StandardCharsets.UTF_8));
        String path = "/path/to/" + playlistName + ".xspf";
        playlistService.importPlaylist(username, playlistName, path, inputStream, null);
        verify(playlistDao).createPlaylist(actual.capture());
        verify(playlistDao).setFilesInPlaylist(eq(23), medias.capture());
        Playlist expected = new Playlist();
        expected.setUsername(username);
        expected.setName(playlistName);
        expected.setComment("Auto-imported from " + path);
        expected.setImportedFrom(path);
        expected.setShared(true);
        expected.setId(23);
        assertTrue("\n" + ToStringBuilder.reflectionToString(actual.getValue()) + "\n\n did not equal \n\n" + ToStringBuilder.reflectionToString(expected), EqualsBuilder.reflectionEquals(actual.getValue(), expected, "created", "changed"));
        List<MediaFile> mediaFiles = medias.getValue();
        assertEquals(3, mediaFiles.size());
    }

    private static class PersistPlayList implements Answer {
        private final int id;
        public PersistPlayList(int id) {
            this.id = id;
        }

        @Override
        public Object answer(InvocationOnMock invocationOnMock) {
            Playlist playlist = invocationOnMock.getArgumentAt(0, Playlist.class);
            playlist.setId(id);
            return null;
        }
    }

    private static class MediaFileHasEverything implements Answer {

        @Override
        public Object answer(InvocationOnMock invocationOnMock) {
            File file = invocationOnMock.getArgumentAt(0, File.class);
            MediaFile mediaFile = new MediaFile();
            mediaFile.setPath(file.getPath());
            return mediaFile;
        }
    }
}
