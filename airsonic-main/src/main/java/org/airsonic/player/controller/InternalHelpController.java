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

import org.airsonic.player.dao.DaoHelper;
import org.airsonic.player.dao.MusicFolderDao;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.service.VersionService;
import org.airsonic.player.service.search.AnalyzerFactory;
import org.airsonic.player.service.search.IndexManager;
import org.airsonic.player.service.search.IndexType;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

/**
 * Controller for the help page.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/internalhelp")
public class InternalHelpController {

    private static final Logger LOG = LoggerFactory.getLogger(InternalHelpController.class);

    private static final int LOG_LINES_TO_SHOW = 50;

    public class MusicFolderStatistics {
        private String name;
        private String freeFilesystemSizeBytes;
        private String totalFilesystemSizeBytes;
        private boolean readable;
        private boolean writable;

        public String getName() {
            return name;
        }

        public String getFreeFilesystemSizeBytes() {
            return freeFilesystemSizeBytes;
        }

        public boolean isReadable() {
            return readable;
        }

        public boolean isWritable() {
            return writable;
        }

        public String getTotalFilesystemSizeBytes() {
            return totalFilesystemSizeBytes;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setFreeFilesystemSizeBytes(String freeFilesystemSizeBytes) {
            this.freeFilesystemSizeBytes = freeFilesystemSizeBytes;
        }

        public void setReadable(boolean readable) {
            this.readable = readable;
        }

        public void setWritable(boolean writable) {
            this.writable = writable;
        }

        public void setTotalFilesystemSizeBytes(String totalFilesystemSizeBytes) {
            this.totalFilesystemSizeBytes = totalFilesystemSizeBytes;
        }
    }

    @Autowired
    private VersionService versionService;
    @Autowired
    private SettingsService settingsService;
    @Autowired
    private SecurityService securityService;
    @Autowired
    private IndexManager indexManager;
    @Autowired
    private DaoHelper daoHelper;
    @Autowired
    private AnalyzerFactory analyzerFactory;
    @Autowired
    private MusicFolderDao musicFolderDao;

    @GetMapping
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> map = new HashMap<>();

        if (versionService.isNewFinalVersionAvailable()) {
            map.put("newVersionAvailable", true);
            map.put("latestVersion", versionService.getLatestFinalVersion());
        } else if (versionService.isNewBetaVersionAvailable()) {
            map.put("newVersionAvailable", true);
            map.put("latestVersion", versionService.getLatestBetaVersion());
        }

        long totalMemory = Runtime.getRuntime().totalMemory();
        long freeMemory = Runtime.getRuntime().freeMemory();

        String serverInfo = request.getSession().getServletContext().getServerInfo() +
                            ", java " + System.getProperty("java.version") +
                            ", " + System.getProperty("os.name");

        // Airsonic scan statistics
        map.put("statAlbumCount", indexManager.getStatistics().getAlbumCount());
        map.put("statArtistCount", indexManager.getStatistics().getArtistCount());
        map.put("statSongCount", indexManager.getStatistics().getSongCount());
        map.put("statLastScanDate", indexManager.getStatistics().getScanDate());
        map.put("statTotalDurationSeconds", indexManager.getStatistics().getTotalDurationInSeconds());
        map.put("statTotalLengthBytes", FileUtils.byteCountToDisplaySize(indexManager.getStatistics().getTotalLengthInBytes()));

        // Lucene index statistics
        try (IndexReader reader = indexManager.getSearcher(IndexType.SONG).getIndexReader()) {
            map.put("indexSongCount", reader.numDocs());
            map.put("indexSongDeletedCount", reader.numDeletedDocs());
        } catch (IOException e) {
            LOG.debug("Unable to gather information", e);
        }
        try (IndexReader reader = indexManager.getSearcher(IndexType.ALBUM).getIndexReader()) {
            map.put("indexAlbumCount", reader.numDocs());
            map.put("indexAlbumDeletedCount", reader.numDeletedDocs());
        } catch (IOException e) {
            LOG.debug("Unable to gather information", e);
        }
        try (IndexReader reader = indexManager.getSearcher(IndexType.ARTIST).getIndexReader()) {
            map.put("indexArtistCount", reader.numDocs());
            map.put("indexArtistDeletedCount", reader.numDeletedDocs());
        } catch (IOException e) {
            LOG.debug("Unable to gather information", e);
        }
        try (IndexReader reader = indexManager.getSearcher(IndexType.ALBUM_ID3).getIndexReader()) {
            map.put("indexAlbumId3Count", reader.numDocs());
            map.put("indexAlbumId3DeletedCount", reader.numDeletedDocs());
        } catch (IOException e) {
            LOG.debug("Unable to gather information", e);
        }
        try (IndexReader reader = indexManager.getSearcher(IndexType.ARTIST_ID3).getIndexReader()) {
            map.put("indexArtistId3Count", reader.numDocs());
            map.put("indexArtistId3DeletedCount", reader.numDeletedDocs());
        } catch (IOException e) {
            LOG.debug("Unable to gather information", e);
        }
        try (Analyzer analyzer = analyzerFactory.getAnalyzer()) {
            map.put("indexLuceneVersion", analyzer.getVersion().toString());
        } catch (IOException e) {
            LOG.debug("Unable to gather information", e);
        }

        // Database statistics
        try (Connection conn = daoHelper.getDataSource().getConnection()) {
            map.put("dbDriverName", conn.getMetaData().getDriverName());
            map.put("dbDriverVersion", conn.getMetaData().getDriverVersion());
        } catch (SQLException e) {
            LOG.debug("Unable to gather information", e);
        }
        File dbDirectory = new File(settingsService.getAirsonicHome(), "db");
        map.put("dbDirectorySizeBytes", dbDirectory.exists() ? FileUtils.sizeOfDirectory(dbDirectory) : 0);
        map.put("dbDirectorySize", FileUtils.byteCountToDisplaySize((long) map.get("dbDirectorySizeBytes")));
        File dbLogFile = new File(dbDirectory, "airsonic.log");
        map.put("dbLogSizeBytes", dbLogFile.exists() ? dbLogFile.length() : 0);
        map.put("dbLogSize", FileUtils.byteCountToDisplaySize((long) map.get("dbLogSizeBytes")));
        SortedMap<String, Long> dbTableCount = new TreeMap<>();
        try {
            for (String tableName : daoHelper.getJdbcTemplate().queryForList("SELECT table_name FROM INFORMATION_SCHEMA.SYSTEM_TABLES WHERE table_schem = 'PUBLIC'", String.class)) {
                try {
                    Long tableCount = daoHelper.getJdbcTemplate().queryForObject(String.format("SELECT count(*) FROM %s", tableName), Long.class);
                    dbTableCount.put(tableName, tableCount);
                } catch (Exception e) {
                    LOG.debug("Unable to gather information", e);
                }
            }
        } catch (Exception e) {
            LOG.debug("Unable to gather information", e);
        }

        map.put("dbMediaFileMusicNonPresentCount", daoHelper.getJdbcTemplate().queryForObject(String.format("SELECT count(*) FROM MEDIA_FILE WHERE NOT present AND type = 'MUSIC'"), Long.class));
        map.put("dbMediaFilePodcastNonPresentCount", daoHelper.getJdbcTemplate().queryForObject(String.format("SELECT count(*) FROM MEDIA_FILE WHERE NOT present AND type = 'PODCAST'"), Long.class));
        map.put("dbMediaFileDirectoryNonPresentCount", daoHelper.getJdbcTemplate().queryForObject(String.format("SELECT count(*) FROM MEDIA_FILE WHERE NOT present AND type = 'DIRECTORY'"), Long.class));
        map.put("dbMediaFileAlbumNonPresentCount", daoHelper.getJdbcTemplate().queryForObject(String.format("SELECT count(*) FROM MEDIA_FILE WHERE NOT present AND type = 'ALBUM'"), Long.class));

        map.put("dbMediaFileMusicPresentCount", daoHelper.getJdbcTemplate().queryForObject(String.format("SELECT count(*) FROM MEDIA_FILE WHERE present AND type = 'MUSIC'"), Long.class));
        map.put("dbMediaFilePodcastPresentCount", daoHelper.getJdbcTemplate().queryForObject(String.format("SELECT count(*) FROM MEDIA_FILE WHERE present AND type = 'PODCAST'"), Long.class));
        map.put("dbMediaFileDirectoryPresentCount", daoHelper.getJdbcTemplate().queryForObject(String.format("SELECT count(*) FROM MEDIA_FILE WHERE present AND type = 'DIRECTORY'"), Long.class));
        map.put("dbMediaFileAlbumPresentCount", daoHelper.getJdbcTemplate().queryForObject(String.format("SELECT count(*) FROM MEDIA_FILE WHERE present AND type = 'ALBUM'"), Long.class));

        map.put("dbMediaFileDistinctAlbumCount", daoHelper.getJdbcTemplate().queryForObject(String.format("SELECT count(DISTINCT album) FROM MEDIA_FILE WHERE present"), Long.class));
        map.put("dbMediaFileDistinctArtistCount", daoHelper.getJdbcTemplate().queryForObject(String.format("SELECT count(DISTINCT artist) FROM MEDIA_FILE WHERE present"), Long.class));
        map.put("dbMediaFileDistinctAlbumArtistCount", daoHelper.getJdbcTemplate().queryForObject(String.format("SELECT count(DISTINCT album_artist) FROM MEDIA_FILE WHERE present"), Long.class));

        map.put("dbTableCount", dbTableCount);

        // Filesystem statistics
        map.put("fsHomeDirectorySizeBytes", FileUtils.sizeOfDirectory(settingsService.getAirsonicHome()));
        map.put("fsHomeDirectorySize", FileUtils.byteCountToDisplaySize((long)map.get("fsHomeDirectorySizeBytes")));
        map.put("fsHomeTotalSpaceBytes", settingsService.getAirsonicHome().getTotalSpace());
        map.put("fsHomeTotalSpace", FileUtils.byteCountToDisplaySize((long)map.get("fsHomeTotalSpaceBytes")));
        map.put("fsHomeUsableSpaceBytes", settingsService.getAirsonicHome().getUsableSpace());
        map.put("fsHomeUsableSpace", FileUtils.byteCountToDisplaySize((long)map.get("fsHomeUsableSpaceBytes")));
        SortedMap<String, MusicFolderStatistics> fsMusicFolderStatistics = new TreeMap<>();
        for (MusicFolder folder: musicFolderDao.getAllMusicFolders()) {
            MusicFolderStatistics stat = new MusicFolderStatistics();
            stat.setName(folder.getName());
            stat.setFreeFilesystemSizeBytes(FileUtils.byteCountToDisplaySize(folder.getPath().getUsableSpace()));
            stat.setTotalFilesystemSizeBytes(FileUtils.byteCountToDisplaySize(folder.getPath().getTotalSpace()));
            stat.setReadable(Files.isReadable(folder.getPath().toPath()));
            stat.setWritable(Files.isWritable(folder.getPath().toPath()));
            fsMusicFolderStatistics.put(folder.getName(), stat);
        }
        map.put("fsMusicFolderStatistics", fsMusicFolderStatistics);

        // OS information
        map.put("localeDefault", Locale.getDefault());
        map.put("localeUserLanguage", System.getProperty("user.language"));
        map.put("localeUserCountry", System.getProperty("user.country"));
        map.put("localeFileEncoding", System.getProperty("file.encoding"));
        map.put("localeSunJnuEncoding", System.getProperty("sun.jnu.encoding"));
        map.put("localeSunIoUnicodeEncoding", System.getProperty("sun.io.unicode.encoding"));
        map.put("localeLang", System.getenv("LANG"));
        map.put("localeLcAll", System.getenv("LC_ALL"));
        map.put("localeDefaultCharset", Charset.defaultCharset());

        map.put("user", securityService.getCurrentUser(request));
        map.put("brand", settingsService.getBrand());
        map.put("localVersion", versionService.getLocalVersion());
        map.put("buildDate", versionService.getLocalBuildDate());
        map.put("buildNumber", versionService.getLocalBuildNumber());
        map.put("serverInfo", serverInfo);
        map.put("usedMemory", totalMemory - freeMemory);
        map.put("totalMemory", totalMemory);
        File logFile = SettingsService.getLogFile();
        List<String> latestLogEntries = getLatestLogEntries(logFile);
        map.put("logEntries", latestLogEntries);
        map.put("logFile", logFile);

        return new ModelAndView("internalhelp","model",map);
    }

    private static List<String> getLatestLogEntries(File logFile) {
        List<String> lines = new ArrayList<>(LOG_LINES_TO_SHOW);
        try (ReversedLinesFileReader reader = new ReversedLinesFileReader(logFile, Charset.defaultCharset())) {
            String current;
            while ((current = reader.readLine()) != null) {
                if (lines.size() >= LOG_LINES_TO_SHOW) {
                    break;
                }
                lines.add(0, current);
            }
            return lines;
        } catch (IOException e) {
            LOG.warn("Could not open log file " + logFile, e);
            return null;
        }
    }


}
