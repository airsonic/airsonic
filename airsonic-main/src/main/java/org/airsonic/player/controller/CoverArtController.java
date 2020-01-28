/*
 This file is part of Airsonic.

 Airsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Airsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2016 (C) Airsonic Authors
 Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
 */
package org.airsonic.player.controller;

import org.airsonic.player.dao.AlbumDao;
import org.airsonic.player.dao.ArtistDao;
import org.airsonic.player.domain.*;
import org.airsonic.player.service.*;
import org.airsonic.player.service.metadata.JaudiotaggerParser;
import org.airsonic.player.util.FileUtil;
import org.airsonic.player.util.StringUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jaudiotagger.tag.images.Artwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.LastModified;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;

/**
 * Controller which produces cover art images.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping({"/coverArt", "/ext/coverArt"})
public class CoverArtController implements LastModified {

    public static final String ALBUM_COVERART_PREFIX = "al-";
    public static final String ARTIST_COVERART_PREFIX = "ar-";
    public static final String PLAYLIST_COVERART_PREFIX = "pl-";
    public static final String PODCAST_COVERART_PREFIX = "pod-";

    private static final Logger LOG = LoggerFactory.getLogger(CoverArtController.class);

    @Autowired
    private MediaFileService mediaFileService;
    @Autowired
    private TranscodingService transcodingService;
    @Autowired
    private SettingsService settingsService;
    @Autowired
    private PlaylistService playlistService;
    @Autowired
    private PodcastService podcastService;
    @Autowired
    private ArtistDao artistDao;
    @Autowired
    private AlbumDao albumDao;
    @Autowired
    private JaudiotaggerParser jaudiotaggerParser;
    private Semaphore semaphore;

    @PostConstruct
    public void init() {
        semaphore = new Semaphore(settingsService.getCoverArtConcurrency());
    }

    public long getLastModified(HttpServletRequest request) {
        CoverArtRequest coverArtRequest = createCoverArtRequest(request);
        if (null == coverArtRequest) {
            return -1L;
        }
        return coverArtRequest.lastModified();
    }

    @GetMapping
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {

        CoverArtRequest coverArtRequest = createCoverArtRequest(request);
        LOG.trace("handleRequest - " + coverArtRequest);
        Integer size = ServletRequestUtils.getIntParameter(request, "size");

        // Send fallback image if no ID is given. (No need to cache it, since it will be cached in browser.)
        if (coverArtRequest == null) {
            sendFallback(size, response);
            return;
        }

        try {
            // Optimize if no scaling is required.
            if (size == null && coverArtRequest.getCoverArt() != null) {
                LOG.trace("sendUnscaled - " + coverArtRequest);
                sendUnscaled(coverArtRequest, response);
                return;
            }

            // Send cached image, creating it if necessary.
            if (size == null) {
                size = CoverArtScheme.LARGE.getSize() * 2;
            }
            File cachedImage = getCachedImage(coverArtRequest, size);
            sendImage(cachedImage, response);
        } catch (Exception e) {
            LOG.debug("Sending fallback as an exception was encountered during normal cover art processing", e);
            sendFallback(size, response);
        }

    }

    private CoverArtRequest createCoverArtRequest(HttpServletRequest request) {
        String id = request.getParameter("id");
        if (id == null) {
            return null;
        }

        if (id.startsWith(ALBUM_COVERART_PREFIX)) {
            return createAlbumCoverArtRequest(Integer.valueOf(id.replace(ALBUM_COVERART_PREFIX, "")));
        }
        if (id.startsWith(ARTIST_COVERART_PREFIX)) {
            return createArtistCoverArtRequest(Integer.valueOf(id.replace(ARTIST_COVERART_PREFIX, "")));
        }
        if (id.startsWith(PLAYLIST_COVERART_PREFIX)) {
            return createPlaylistCoverArtRequest(Integer.valueOf(id.replace(PLAYLIST_COVERART_PREFIX, "")));
        }
        if (id.startsWith(PODCAST_COVERART_PREFIX)) {
            return createPodcastCoverArtRequest(Integer.valueOf(id.replace(PODCAST_COVERART_PREFIX, "")), request);
        }
        return createMediaFileCoverArtRequest(Integer.valueOf(id), request);
    }

    private CoverArtRequest createAlbumCoverArtRequest(int id) {
        Album album = albumDao.getAlbum(id);
        return album == null ? null : new AlbumCoverArtRequest(album);
    }

    private CoverArtRequest createArtistCoverArtRequest(int id) {
        Artist artist = artistDao.getArtist(id);
        return artist == null ? null : new ArtistCoverArtRequest(artist);
    }

    private PlaylistCoverArtRequest createPlaylistCoverArtRequest(int id) {
        Playlist playlist = playlistService.getPlaylist(id);
        return playlist == null ? null : new PlaylistCoverArtRequest(playlist);
    }

    private CoverArtRequest createPodcastCoverArtRequest(int id, HttpServletRequest request) {
        PodcastChannel channel = podcastService.getChannel(id);
        if (channel == null) {
            return null;
        }
        if (channel.getMediaFileId() == null) {
            return new PodcastCoverArtRequest(channel);
        }
        return createMediaFileCoverArtRequest(channel.getMediaFileId(), request);
    }

    private CoverArtRequest createMediaFileCoverArtRequest(int id, HttpServletRequest request) {
        MediaFile mediaFile = mediaFileService.getMediaFile(id);
        if (mediaFile == null) {
            return null;
        }
        if (mediaFile.isVideo()) {
            int offset = ServletRequestUtils.getIntParameter(request, "offset", 60);
            return new VideoCoverArtRequest(mediaFile, offset);
        }
        return new MediaFileCoverArtRequest(mediaFile);
    }

    private void sendImage(File file, HttpServletResponse response) throws IOException {
        response.setContentType(StringUtil.getMimeType(FilenameUtils.getExtension(file.getName())));
        InputStream in = new FileInputStream(file);
        try {
            IOUtils.copy(in, response.getOutputStream());
        } finally {
            FileUtil.closeQuietly(in);
        }
    }

    private void sendFallback(Integer size, HttpServletResponse response) throws IOException {
        if (response.getContentType() == null) {
            response.setContentType(StringUtil.getMimeType("jpeg"));
        }
        InputStream in = null;
        try {
            in = getClass().getResourceAsStream("default_cover.jpg");
            BufferedImage image = ImageIO.read(in);
            if (size != null) {
                image = scale(image, size, size);
            }
            ImageIO.write(image, "jpeg", response.getOutputStream());
        } finally {
            FileUtil.closeQuietly(in);
        }
    }

    private void sendUnscaled(CoverArtRequest coverArtRequest, HttpServletResponse response) throws IOException {
        File file = coverArtRequest.getCoverArt();
        InputStream in = null;
        try {
            Pair<InputStream, String> imageInputStreamWithType = getImageInputStreamWithType(file);
            in = imageInputStreamWithType.getLeft();
            response.setContentType(imageInputStreamWithType.getRight());
            IOUtils.copy(in, response.getOutputStream());
        } finally {
            FileUtil.closeQuietly(in);
        }
    }

    private File getCachedImage(CoverArtRequest request, int size) throws IOException {
        String hash = DigestUtils.md5Hex(request.getKey());
        String encoding = request.getCoverArt() != null ? "jpeg" : "png";
        File cachedImage = new File(getImageCacheDirectory(size), hash + "." + encoding);

        // Synchronize to avoid concurrent writing to the same file.
        synchronized (hash.intern()) {

            // Is cache missing or obsolete?
            if (!cachedImage.exists() || request.lastModified() > cachedImage.lastModified()) {
//                LOG.info("Cache MISS - " + request + " (" + size + ")");
                OutputStream out = null;
                try {
                    semaphore.acquire();
                    BufferedImage image = request.createImage(size);
                    if (image == null) {
                        throw new Exception("Unable to decode image.");
                    }
                    out = new FileOutputStream(cachedImage);
                    ImageIO.write(image, encoding, out);

                } catch (Throwable x) {
                    // Delete corrupt (probably empty) thumbnail cache.
                    LOG.warn("Failed to create thumbnail for " + request, x);
                    FileUtil.closeQuietly(out);
                    cachedImage.delete();
                    throw new IOException("Failed to create thumbnail for " + request + ". " + x.getMessage());

                } finally {
                    semaphore.release();
                    FileUtil.closeQuietly(out);
                }
            } else {
//                LOG.info("Cache HIT - " + request + " (" + size + ")");
            }
            return cachedImage;
        }
    }

    /**
     * Returns an input stream to the image in the given file.  If the file is an audio file,
     * the embedded album art is returned.
     */
    private InputStream getImageInputStream(File file) throws IOException {
        return getImageInputStreamWithType(file).getLeft();
    }

    /**
     * Returns an input stream to the image in the given file.  If the file is an audio file,
     * the embedded album art is returned. In addition returns the mime type
     */
    private Pair<InputStream, String> getImageInputStreamWithType(File file) throws IOException {
        InputStream is;
        String mimeType;
        if (jaudiotaggerParser.isApplicable(file)) {
            LOG.trace("Using Jaudio Tagger for reading artwork from {}", file);
            MediaFile mediaFile = mediaFileService.getMediaFile(file);
            Artwork artwork;
            try {
                LOG.trace("Reading artwork from file {}", mediaFile);
                artwork = jaudiotaggerParser.getArtwork(mediaFile);
                is = new ByteArrayInputStream(artwork.getBinaryData());
                mimeType = artwork.getMimeType();
            } catch (Exception e) {
                LOG.debug("Could not read artwork from file {}", mediaFile);
                throw new RuntimeException(e);
            }
        } else {
            is = new FileInputStream(file);
            mimeType = StringUtil.getMimeType(FilenameUtils.getExtension(file.getName()));
        }
        return Pair.of(is, mimeType);
    }

    private InputStream getImageInputStreamForVideo(MediaFile mediaFile, int width, int height, int offset) throws Exception {
        VideoTranscodingSettings videoSettings = new VideoTranscodingSettings(width, height, offset, 0, false);
        TranscodingService.Parameters parameters = new TranscodingService.Parameters(mediaFile, videoSettings);
        String command = settingsService.getVideoImageCommand();
        parameters.setTranscoding(new Transcoding(null, null, null, null, command, null, null, false));
        return transcodingService.getTranscodedInputStream(parameters);
    }

    private synchronized File getImageCacheDirectory(int size) {
        File dir = new File(SettingsService.getAirsonicHome(), "thumbs");
        dir = new File(dir, String.valueOf(size));
        if (!dir.exists()) {
            if (dir.mkdirs()) {
                LOG.info("Created thumbnail cache " + dir);
            } else {
                LOG.error("Failed to create thumbnail cache " + dir);
            }
        }

        return dir;
    }

    public static BufferedImage scale(BufferedImage image, int width, int height) {
        int w = image.getWidth();
        int h = image.getHeight();
        BufferedImage thumb = image;

        // For optimal results, use step by step bilinear resampling - halfing the size at each step.
        do {
            w /= 2;
            h /= 2;
            if (w < width) {
                w = width;
            }
            if (h < height) {
                h = height;
            }

            BufferedImage temp = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = temp.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(thumb, 0, 0, temp.getWidth(), temp.getHeight(), null);
            g2.dispose();

            thumb = temp;
        } while (w != width);

        return thumb;
    }


    private abstract class CoverArtRequest {

        protected File coverArt;

        private CoverArtRequest() {
        }

        private CoverArtRequest(String coverArtPath) {
            this.coverArt = coverArtPath == null ? null : new File(coverArtPath);
        }

        private File getCoverArt() {
            return coverArt;
        }

        public abstract String getKey();

        public abstract long lastModified();

        public BufferedImage createImage(int size) {
            if (coverArt != null) {
                InputStream in = null;
                String reason = null;
                try {
                    in = getImageInputStream(coverArt);
                    if (in == null) {
                        reason = "getImageInputStream";
                    } else {
                        BufferedImage bimg = ImageIO.read(in);
                        if (bimg == null) {
                            reason = "ImageIO.read";
                        } else {
                            return scale(bimg, size, size);
                        }
                    }
                    LOG.warn("Failed to process cover art " + coverArt + ": " + reason + " failed");
                } catch (Throwable x) {
                    LOG.warn("Failed to process cover art " + coverArt + ": " + x, x);
                } finally {
                    FileUtil.closeQuietly(in);
                }
            }
            return createAutoCover(size, size);
        }

        protected BufferedImage createAutoCover(int width, int height) {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            AutoCover autoCover = new AutoCover(graphics, getKey(), getArtist(), getAlbum(), width, height);
            autoCover.paintCover();
            graphics.dispose();
            return image;
        }

        public abstract String getAlbum();

        public abstract String getArtist();
    }

    private class ArtistCoverArtRequest extends CoverArtRequest {

        private final Artist artist;

        private ArtistCoverArtRequest(Artist artist) {
            super(artist.getCoverArtPath());
            this.artist = artist;
        }

        @Override
        public String getKey() {
            return artist.getCoverArtPath() != null ? artist.getCoverArtPath() : (ARTIST_COVERART_PREFIX + artist.getId());
        }

        @Override
        public long lastModified() {
            return coverArt != null ? coverArt.lastModified() : artist.getLastScanned().getTime();
        }

        @Override
        public String getAlbum() {
            return null;
        }

        @Override
        public String getArtist() {
            return artist.getName();
        }

        @Override
        public String toString() {
            return "Artist " + artist.getId() + " - " + artist.getName();
        }
    }

    private class AlbumCoverArtRequest extends CoverArtRequest {

        private final Album album;

        private AlbumCoverArtRequest(Album album) {
            super(album.getCoverArtPath());
            this.album = album;
        }

        @Override
        public String getKey() {
            return album.getCoverArtPath() != null ? album.getCoverArtPath() : (ALBUM_COVERART_PREFIX + album.getId());
        }

        @Override
        public long lastModified() {
            return coverArt != null ? coverArt.lastModified() : album.getLastScanned().getTime();
        }

        @Override
        public String getAlbum() {
            return album.getName();
        }

        @Override
        public String getArtist() {
            return album.getArtist();
        }

        @Override
        public String toString() {
            return "Album " + album.getId() + " - " + album.getName();
        }
    }

    private class PlaylistCoverArtRequest extends CoverArtRequest {

        private final Playlist playlist;

        private PlaylistCoverArtRequest(Playlist playlist) {
            super(null);
            this.playlist = playlist;
        }

        @Override
        public String getKey() {
            return PLAYLIST_COVERART_PREFIX + playlist.getId();
        }

        @Override
        public long lastModified() {
            return playlist.getChanged().getTime();
        }

        @Override
        public String getAlbum() {
            return null;
        }

        @Override
        public String getArtist() {
            return playlist.getName();
        }

        @Override
        public String toString() {
            return "Playlist " + playlist.getId() + " - " + playlist.getName();
        }

        @Override
        public BufferedImage createImage(int size) {
            List<MediaFile> albums = getRepresentativeAlbums();
            if (albums.isEmpty()) {
                return createAutoCover(size, size);
            }
            if (albums.size() < 4) {
                return new MediaFileCoverArtRequest(albums.get(0)).createImage(size);
            }

            BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();

            int half = size / 2;
            graphics.drawImage(new MediaFileCoverArtRequest(albums.get(0)).createImage(half), null, 0, 0);
            graphics.drawImage(new MediaFileCoverArtRequest(albums.get(1)).createImage(half), null, half, 0);
            graphics.drawImage(new MediaFileCoverArtRequest(albums.get(2)).createImage(half), null, 0, half);
            graphics.drawImage(new MediaFileCoverArtRequest(albums.get(3)).createImage(half), null, half, half);
            graphics.dispose();
            return image;
        }

        private List<MediaFile> getRepresentativeAlbums() {
            Set<MediaFile> albums = new LinkedHashSet<MediaFile>();
            for (MediaFile song : playlistService.getFilesInPlaylist(playlist.getId())) {
                MediaFile album = mediaFileService.getParentOf(song);
                if (album != null && !mediaFileService.isRoot(album)) {
                    albums.add(album);
                }
            }
            return new ArrayList<>(albums);
        }
    }

    private class PodcastCoverArtRequest extends CoverArtRequest {

        private final PodcastChannel channel;

        PodcastCoverArtRequest(PodcastChannel channel) {
            this.channel = channel;
        }

        @Override
        public String getKey() {
            return PODCAST_COVERART_PREFIX + channel.getId();
        }

        @Override
        public long lastModified() {
            return -1;
        }

        @Override
        public String getAlbum() {
            return null;
        }

        @Override
        public String getArtist() {
            return channel.getTitle() != null ? channel.getTitle() : channel.getUrl();
        }
    }

    private class MediaFileCoverArtRequest extends CoverArtRequest {

        private final MediaFile mediaFile;
        private final MediaFile dir;

        private MediaFileCoverArtRequest(MediaFile mediaFile) {
            this.mediaFile = mediaFile;
            dir = mediaFile.isDirectory() ? mediaFile : mediaFileService.getParentOf(mediaFile);
            coverArt = mediaFileService.getCoverArt(mediaFile);
        }

        @Override
        public String getKey() {
            return coverArt != null ? coverArt.getPath() : dir.getPath();
        }

        @Override
        public long lastModified() {
            return coverArt != null ? coverArt.lastModified() : dir.getChanged().getTime();
        }

        @Override
        public String getAlbum() {
            return dir.getName();
        }

        @Override
        public String getArtist() {
            return dir.getAlbumArtist() != null ? dir.getAlbumArtist() : dir.getArtist();
        }

        @Override
        public String toString() {
            return "Media file " + mediaFile.getId() + " - " + mediaFile;
        }
    }

    private class VideoCoverArtRequest extends CoverArtRequest {

        private final MediaFile mediaFile;
        private final int offset;

        private VideoCoverArtRequest(MediaFile mediaFile, int offset) {
            this.mediaFile = mediaFile;
            this.offset = offset;
        }

        @Override
        public BufferedImage createImage(int size) {
            int height;
            height = size;
            int width = height * 16 / 9;
            InputStream in = null;
            try {
                in = getImageInputStreamForVideo(mediaFile, width, height, offset);
                BufferedImage result = ImageIO.read(in);
                if (result != null) {
                    return result;
                }
                LOG.warn("Failed to process cover art for " + mediaFile + ": {}", result);
            } catch (Throwable x) {
                LOG.warn("Failed to process cover art for " + mediaFile + ": " + x, x);
            } finally {
                FileUtil.closeQuietly(in);
            }
            return createAutoCover(width, height);
        }

        @Override
        public String getKey() {
            return mediaFile.getPath() + "/" + offset;
        }

        @Override
        public long lastModified() {
            return mediaFile.getChanged().getTime();
        }

        @Override
        public String getAlbum() {
            return null;
        }

        @Override
        public String getArtist() {
            return mediaFile.getName();
        }

        @Override
        public String toString() {
            return "Video file " + mediaFile.getId() + " - " + mediaFile;
        }
    }

    static class AutoCover {

        private final static int[] COLORS = {0x33B5E5, 0xAA66CC, 0x99CC00, 0xFFBB33, 0xFF4444};
        private final Graphics2D graphics;
        private final String artist;
        private final String album;
        private final int width;
        private final int height;
        private final Color color;

        AutoCover(Graphics2D graphics, String key, String artist, String album, int width, int height) {
            this.graphics = graphics;
            this.artist = artist;
            this.album = album;
            this.width = width;
            this.height = height;

            int hash = key.hashCode();
            int rgb = COLORS[Math.abs(hash) % COLORS.length];
            this.color = new Color(rgb);
        }

        public void paintCover() {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            graphics.setPaint(color);
            graphics.fillRect(0, 0, width, height);

            int y = height * 2 / 3;
            graphics.setPaint(new GradientPaint(0, y, new Color(82, 82, 82), 0, height, Color.BLACK));
            graphics.fillRect(0, y, width, height / 3);

            graphics.setPaint(Color.WHITE);
            float fontSize = 3.0f + height * 0.07f;
            Font font = new Font(Font.SANS_SERIF, Font.BOLD, (int) fontSize);
            graphics.setFont(font);

            if (album != null) {
                graphics.drawString(album, width * 0.05f, height * 0.6f);
            }
            if (artist != null) {
                graphics.drawString(artist, width * 0.05f, height * 0.8f);
            }

            int borderWidth = height / 50;
            graphics.fillRect(0, 0, borderWidth, height);
            graphics.fillRect(width - borderWidth, 0, height - borderWidth, height);
            graphics.fillRect(0, 0, width, borderWidth);
            graphics.fillRect(0, height - borderWidth, width, height);
        }
    }
}
