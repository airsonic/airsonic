package org.airsonic.player.domain;

import com.google.common.base.Function;

import java.io.File;
import java.util.Date;
import java.util.List;

public class MediaFileWithUrlInfo {

    private final MediaFile file;
    private final String coverArtUrl;
    private final String streamUrl;

    public MediaFileWithUrlInfo(MediaFile file, String coverArtUrl, String streamUrl) {
        this.file = file;
        this.coverArtUrl = coverArtUrl;
        this.streamUrl = streamUrl;
    }

    public String getStreamUrl() {
        return streamUrl;
    }

    public String getCoverArtUrl() {
        return coverArtUrl;
    }

    public int getId() {
        return file.getId();
    }

    public void setId(int id) {
        file.setId(id);
    }

    public String getPath() {
        return file.getPath();
    }

    public void setPath(String path) {
        file.setPath(path);
    }

    public String getFolder() {
        return file.getFolder();
    }

    public void setFolder(String folder) {
        file.setFolder(folder);
    }

    public File getFile() {
        return file.getFile();
    }

    public boolean exists() {
        return file.exists();
    }

    public MediaFile.MediaType getMediaType() {
        return file.getMediaType();
    }

    public void setMediaType(MediaFile.MediaType mediaType) {
        file.setMediaType(mediaType);
    }

    public boolean isVideo() {
        return file.isVideo();
    }

    public boolean isAudio() {
        return file.isAudio();
    }

    public String getFormat() {
        return file.getFormat();
    }

    public void setFormat(String format) {
        file.setFormat(format);
    }

    public boolean isDirectory() {
        return file.isDirectory();
    }

    public boolean isFile() {
        return file.isFile();
    }

    public boolean isAlbum() {
        return file.isAlbum();
    }

    public String getTitle() {
        return file.getTitle();
    }

    public void setTitle(String title) {
        file.setTitle(title);
    }

    public String getAlbumName() {
        return file.getAlbumName();
    }

    public void setAlbumName(String album) {
        file.setAlbumName(album);
    }

    public String getArtist() {
        return file.getArtist();
    }

    public void setArtist(String artist) {
        file.setArtist(artist);
    }

    public String getAlbumArtist() {
        return file.getAlbumArtist();
    }

    public void setAlbumArtist(String albumArtist) {
        file.setAlbumArtist(albumArtist);
    }

    public String getName() {
        return file.getName();
    }

    public Integer getDiscNumber() {
        return file.getDiscNumber();
    }

    public void setDiscNumber(Integer discNumber) {
        file.setDiscNumber(discNumber);
    }

    public Integer getTrackNumber() {
        return file.getTrackNumber();
    }

    public void setTrackNumber(Integer trackNumber) {
        file.setTrackNumber(trackNumber);
    }

    public Integer getYear() {
        return file.getYear();
    }

    public void setYear(Integer year) {
        file.setYear(year);
    }

    public String getGenre() {
        return file.getGenre();
    }

    public void setGenre(String genre) {
        file.setGenre(genre);
    }

    public Integer getBitRate() {
        return file.getBitRate();
    }

    public void setBitRate(Integer bitRate) {
        file.setBitRate(bitRate);
    }

    public boolean isVariableBitRate() {
        return file.isVariableBitRate();
    }

    public void setVariableBitRate(boolean variableBitRate) {
        file.setVariableBitRate(variableBitRate);
    }

    public Integer getDurationSeconds() {
        return file.getDurationSeconds();
    }

    public void setDurationSeconds(Integer durationSeconds) {
        file.setDurationSeconds(durationSeconds);
    }

    public String getDurationString() {
        return file.getDurationString();
    }

    public Long getFileSize() {
        return file.getFileSize();
    }

    public void setFileSize(Long fileSize) {
        file.setFileSize(fileSize);
    }

    public Integer getWidth() {
        return file.getWidth();
    }

    public void setWidth(Integer width) {
        file.setWidth(width);
    }

    public Integer getHeight() {
        return file.getHeight();
    }

    public void setHeight(Integer height) {
        file.setHeight(height);
    }

    public String getCoverArtPath() {
        return file.getCoverArtPath();
    }

    public void setCoverArtPath(String coverArtPath) {
        file.setCoverArtPath(coverArtPath);
    }

    public String getParentPath() {
        return file.getParentPath();
    }

    public void setParentPath(String parentPath) {
        file.setParentPath(parentPath);
    }

    public File getParentFile() {
        return file.getParentFile();
    }

    public int getPlayCount() {
        return file.getPlayCount();
    }

    public void setPlayCount(int playCount) {
        file.setPlayCount(playCount);
    }

    public Date getLastPlayed() {
        return file.getLastPlayed();
    }

    public void setLastPlayed(Date lastPlayed) {
        file.setLastPlayed(lastPlayed);
    }

    public String getComment() {
        return file.getComment();
    }

    public void setComment(String comment) {
        file.setComment(comment);
    }

    public Date getCreated() {
        return file.getCreated();
    }

    public void setCreated(Date created) {
        file.setCreated(created);
    }

    public Date getChanged() {
        return file.getChanged();
    }

    public void setChanged(Date changed) {
        file.setChanged(changed);
    }

    public Date getLastScanned() {
        return file.getLastScanned();
    }

    public void setLastScanned(Date lastScanned) {
        file.setLastScanned(lastScanned);
    }

    public Date getStarredDate() {
        return file.getStarredDate();
    }

    public void setStarredDate(Date starredDate) {
        file.setStarredDate(starredDate);
    }

    public Date getChildrenLastUpdated() {
        return file.getChildrenLastUpdated();
    }

    public void setChildrenLastUpdated(Date childrenLastUpdated) {
        file.setChildrenLastUpdated(childrenLastUpdated);
    }

    public boolean isPresent() {
        return file.isPresent();
    }

    public void setPresent(boolean present) {
        file.setPresent(present);
    }

    public int getVersion() {
        return file.getVersion();
    }

    public File getCoverArtFile() {
        return file.getCoverArtFile();
    }

    public static List<Integer> toIdList(List<MediaFile> from) {
        return MediaFile.toIdList(from);
    }

    public static Function<MediaFile, Integer> toId() {
        return MediaFile.toId();
    }
}
