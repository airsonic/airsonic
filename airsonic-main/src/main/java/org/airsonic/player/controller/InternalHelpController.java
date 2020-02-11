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
import org.airsonic.player.dao.MediaFileDao;
import org.airsonic.player.dao.MusicFolderDao;
import org.airsonic.player.domain.MediaLibraryStatistics;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.service.TranscodingService;
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
import org.springframework.core.env.Environment;
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
import java.sql.ResultSet;
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

    public class IndexStatistics {
        private String name;
        private int count;
        private int deletedCount;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public int getDeletedCount() {
            return deletedCount;
        }

        public void setDeletedCount(int deletedCount) {
            this.deletedCount = deletedCount;
        }
    }

    public class FileStatistics {
        private String name;
        private String path;
        private String freeFilesystemSizeBytes;
        private String totalFilesystemSizeBytes;
        private boolean readable;
        private boolean writable;
        private boolean executable;

        public FileStatistics() {}

        public FileStatistics(File path) {
        }

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

        public boolean isExecutable() {
            return executable;
        }

        public String getTotalFilesystemSizeBytes() {
            return totalFilesystemSizeBytes;
        }

        public String getPath() {
            return path;
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

        public void setExecutable(boolean executable) {
            this.executable = executable;
        }

        public void setTotalFilesystemSizeBytes(String totalFilesystemSizeBytes) {
            this.totalFilesystemSizeBytes = totalFilesystemSizeBytes;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public void setFromFile(File file) {
            this.setName(file.getName());
            this.setPath(file.getAbsolutePath());
            this.setFreeFilesystemSizeBytes(FileUtils.byteCountToDisplaySize(file.getUsableSpace()));
            this.setTotalFilesystemSizeBytes(FileUtils.byteCountToDisplaySize(file.getTotalSpace()));
            this.setReadable(Files.isReadable(file.toPath()));
            this.setWritable(Files.isWritable(file.toPath()));
            this.setExecutable(Files.isExecutable(file.toPath()));
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
    @Autowired
    private MediaFileDao mediaFileDao;
    @Autowired
    private TranscodingService transcodingService;
    @Autowired
    private Environment environment;

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

        // Gather internal information
        gatherScanInfo(map);
        gatherIndexInfo(map);
        gatherDatabaseInfo(map);
        gatherFilesystemInfo(map);
        gatherTranscodingInfo(map);
        gatherLocaleInfo(map);

        return new ModelAndView("internalhelp","model",map);
    }

    private void gatherScanInfo(Map<String, Object> map) {
        // Airsonic scan statistics
        MediaLibraryStatistics stats = indexManager.getStatistics();
        if (stats != null) {
            map.put("statAlbumCount", stats.getAlbumCount());
            map.put("statArtistCount", stats.getArtistCount());
            map.put("statSongCount", stats.getSongCount());
            map.put("statLastScanDate", stats.getScanDate());
            map.put("statTotalDurationSeconds", stats.getTotalDurationInSeconds());
            map.put("statTotalLengthBytes", FileUtils.byteCountToDisplaySize(stats.getTotalLengthInBytes()));
        }
    }

    private void gatherIndexInfo(Map<String, Object> map) {
        SortedMap<String, IndexStatistics> indexStats = new TreeMap<>();
        for (IndexType indexType : IndexType.values()) {
            try (IndexReader reader = indexManager.getSearcher(indexType).getIndexReader()) {
                IndexStatistics stat = new IndexStatistics();
                stat.setName(indexType.name());
                stat.setCount(reader.numDocs());
                stat.setDeletedCount(reader.numDeletedDocs());
                indexStats.put(indexType.name(), stat);
            } catch (IOException e) {
                LOG.debug("Unable to gather information about {} index", indexType.name(), e);
            }
        }
        map.put("indexStatistics", indexStats);

        try (Analyzer analyzer = analyzerFactory.getAnalyzer()) {
            map.put("indexLuceneVersion", analyzer.getVersion().toString());
        } catch (IOException e) {
            LOG.debug("Unable to gather information", e);
        }
    }

    private void gatherLocaleInfo(Map<String, Object> map) {
        map.put("localeDefault", Locale.getDefault());
        map.put("localeUserLanguage", System.getProperty("user.language"));
        map.put("localeUserCountry", System.getProperty("user.country"));
        map.put("localeFileEncoding", System.getProperty("file.encoding"));
        map.put("localeSunJnuEncoding", System.getProperty("sun.jnu.encoding"));
        map.put("localeSunIoUnicodeEncoding", System.getProperty("sun.io.unicode.encoding"));
        map.put("localeLang", System.getenv("LANG"));
        map.put("localeLcAll", System.getenv("LC_ALL"));
        map.put("localeDefaultCharset", Charset.defaultCharset());
    }

    private void gatherDatabaseInfo(Map<String, Object> map) {

        try (Connection conn = daoHelper.getDataSource().getConnection()) {

            // Driver name/version
            map.put("dbDriverName", conn.getMetaData().getDriverName());
            map.put("dbDriverVersion", conn.getMetaData().getDriverVersion());
            map.put("dbServerVersion", conn.getMetaData().getDatabaseProductVersion());

            // Gather information for existing database tables
            ResultSet resultSet = conn.getMetaData().getTables(null, null, "%", null);
            SortedMap<String, Long> dbTableCount = new TreeMap<>();
            while (resultSet.next()) {
                String tableSchema = resultSet.getString("TABLE_SCHEM");
                String tableName = resultSet.getString("TABLE_NAME");
                String tableType = resultSet.getString("TABLE_TYPE");
                LOG.debug("Got database table {}, schema {}, type {}", tableName, tableSchema, tableType);
                if (!"table".equalsIgnoreCase(tableType)) continue;   // Table type
                // MariaDB has "null" schemas, while other databases use "public".
                if (tableSchema != null && !"public".equalsIgnoreCase(tableSchema)) continue;  // Table schema
                try {
                    Long tableCount = daoHelper.getJdbcTemplate().queryForObject(String.format("SELECT count(*) FROM %s", tableName), Long.class);
                    dbTableCount.put(tableName, tableCount);
                } catch (Exception e) {
                    LOG.debug("Unable to gather information", e);
                }
            }
            map.put("dbTableCount", dbTableCount);

        } catch (SQLException e) {
            LOG.debug("Unable to gather information", e);
        }

        if (environment.acceptsProfiles("legacy")) {
            map.put("dbIsLegacy", true);
            File dbDirectory = new File(settingsService.getAirsonicHome(), "db");
            map.put("dbDirectorySizeBytes", dbDirectory.exists() ? FileUtils.sizeOfDirectory(dbDirectory) : 0);
            map.put("dbDirectorySize", FileUtils.byteCountToDisplaySize((long) map.get("dbDirectorySizeBytes")));
            File dbLogFile = new File(dbDirectory, "airsonic.log");
            map.put("dbLogSizeBytes", dbLogFile.exists() ? dbLogFile.length() : 0);
            map.put("dbLogSize", FileUtils.byteCountToDisplaySize((long) map.get("dbLogSizeBytes")));
        } else {
            map.put("dbIsLegacy", false);
        }

        map.put("dbMediaFileMusicNonPresentCount", daoHelper.getJdbcTemplate().queryForObject(String.format("SELECT count(*) FROM media_file WHERE NOT present AND type = 'MUSIC'"), Long.class));
        map.put("dbMediaFilePodcastNonPresentCount", daoHelper.getJdbcTemplate().queryForObject(String.format("SELECT count(*) FROM media_file WHERE NOT present AND type = 'PODCAST'"), Long.class));
        map.put("dbMediaFileDirectoryNonPresentCount", daoHelper.getJdbcTemplate().queryForObject(String.format("SELECT count(*) FROM media_file WHERE NOT present AND type = 'DIRECTORY'"), Long.class));
        map.put("dbMediaFileAlbumNonPresentCount", daoHelper.getJdbcTemplate().queryForObject(String.format("SELECT count(*) FROM media_file wheRE NOT present AND type = 'ALBUM'"), Long.class));

        map.put("dbMediaFileMusicPresentCount", daoHelper.getJdbcTemplate().queryForObject(String.format("SELECT count(*) FROM media_file WHERE present AND type = 'MUSIC'"), Long.class));
        map.put("dbMediaFilePodcastPresentCount", daoHelper.getJdbcTemplate().queryForObject(String.format("SELECT count(*) FROM media_file WHERE present AND type = 'PODCAST'"), Long.class));
        map.put("dbMediaFileDirectoryPresentCount", daoHelper.getJdbcTemplate().queryForObject(String.format("SELECT count(*) FROM media_file WHERE present AND type = 'DIRECTORY'"), Long.class));
        map.put("dbMediaFileAlbumPresentCount", daoHelper.getJdbcTemplate().queryForObject(String.format("SELECT count(*) FROM media_file WHERE present AND type = 'ALBUM'"), Long.class));

        map.put("dbMediaFileDistinctAlbumCount", daoHelper.getJdbcTemplate().queryForObject(String.format("SELECT count(DISTINCT album) FROM media_file WHERE present"), Long.class));
        map.put("dbMediaFileDistinctArtistCount", daoHelper.getJdbcTemplate().queryForObject(String.format("SELECT count(DISTINCT artist) FROM media_file WHERE present"), Long.class));
        map.put("dbMediaFileDistinctAlbumArtistCount", daoHelper.getJdbcTemplate().queryForObject(String.format("SELECT count(DISTINCT album_artist) FROM media_file WHERE present"), Long.class));

        map.put("dbMediaFilesInNonPresentMusicFoldersCount", mediaFileDao.getFilesInNonPresentMusicFoldersCount(Arrays.asList(settingsService.getPodcastFolder())));
        map.put("dbMediaFilesInNonPresentMusicFoldersSample", mediaFileDao.getFilesInNonPresentMusicFolders(10, Arrays.asList(settingsService.getPodcastFolder())));

        map.put("dbMediaFilesWithMusicFolderMismatchCount", mediaFileDao.getFilesWithMusicFolderMismatchCount());
        map.put("dbMediaFilesWithMusicFolderMismatchSample", mediaFileDao.getFilesWithMusicFolderMismatch(10));
    }

    private void gatherFilesystemInfo(Map<String, Object> map) {
        map.put("fsHomeDirectorySizeBytes", FileUtils.sizeOfDirectory(settingsService.getAirsonicHome()));
        map.put("fsHomeDirectorySize", FileUtils.byteCountToDisplaySize((long)map.get("fsHomeDirectorySizeBytes")));
        map.put("fsHomeTotalSpaceBytes", settingsService.getAirsonicHome().getTotalSpace());
        map.put("fsHomeTotalSpace", FileUtils.byteCountToDisplaySize((long)map.get("fsHomeTotalSpaceBytes")));
        map.put("fsHomeUsableSpaceBytes", settingsService.getAirsonicHome().getUsableSpace());
        map.put("fsHomeUsableSpace", FileUtils.byteCountToDisplaySize((long)map.get("fsHomeUsableSpaceBytes")));
        SortedMap<String, FileStatistics> fsMusicFolderStatistics = new TreeMap<>();
        for (MusicFolder folder: musicFolderDao.getAllMusicFolders()) {
            FileStatistics stat = new FileStatistics();
            stat.setFromFile(folder.getPath());
            stat.setName(folder.getName());
            fsMusicFolderStatistics.put(folder.getName(), stat);
        }
        map.put("fsMusicFolderStatistics", fsMusicFolderStatistics);
    }

    private void gatherTranscodingInfo(Map<String, Object> map) {
        map.put("fsFfprobeInfo", gatherStatisticsForTranscodingExecutable("ffprobe"));
        map.put("fsFfmpegInfo", gatherStatisticsForTranscodingExecutable("ffmpeg"));
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

    private File lookForExecutable(String executableName) {
        for (String path : System.getenv("PATH").split(File.pathSeparator)) {
            File file = new File(path, executableName);
            if (file.exists()) {
                LOG.debug("Found {} in {}", executableName, path);
                return file;
            } else {
                LOG.debug("Looking for {} in {} (not found)", executableName, path);
            }
        }
        return null;
    }

    private File lookForTranscodingExecutable(String executableName) {
        File executableLocation = null;
        for (String name: Arrays.asList(executableName, String.format("%s.exe", executableName))) {
            executableLocation = new File(transcodingService.getTranscodeDirectory(), name);
            if (executableLocation != null && executableLocation.exists()) return executableLocation;
            executableLocation = lookForExecutable(executableName);
            if (executableLocation != null && executableLocation.exists()) return executableLocation;
        }
        return null;
    }

    private FileStatistics gatherStatisticsForTranscodingExecutable(String executableName) {
        FileStatistics executableStatistics = null;
        File executableLocation = lookForTranscodingExecutable(executableName);
        if (executableLocation != null) {
            executableStatistics = new FileStatistics();
            executableStatistics.setFromFile(executableLocation);
        }
        return executableStatistics;
    }

}
