package org.airsonic.player.util;

import org.airsonic.player.service.SettingsService;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

public class LegacyHsqlUtil {

    private static final Logger LOG = LoggerFactory.getLogger(LegacyHsqlUtil.class);

    /**
     * Return the current version of the HSQLDB database, as reported by the database properties file.
     */
    public static String getHsqldbDatabaseVersion() {
        Properties prop = new Properties();
        File configFile = new File(SettingsService.getDefaultJDBCPath() + ".properties");
        if (!configFile.exists()) {
            LOG.debug("HSQLDB database doesn't exist, cannot determine version");
            return null;
        }
        try (InputStream stream = new FileInputStream(configFile)) {
            prop.load(stream);
            return prop.getProperty("version");
        } catch (IOException e) {
            LOG.error("Failed to determine HSQLDB database version", e);
            return null;
        }
    }

    /**
     * Create a new connection to the HSQLDB database.
     */
    public static Connection getHsqldbDatabaseConnection() throws SQLException {
        String url = SettingsService.getDefaultJDBCUrl();
        Properties properties = new Properties();
        properties.put("user", SettingsService.getDefaultJDBCUsername());
        properties.put("password", SettingsService.getDefaultJDBCPassword());
        return DriverManager.getConnection(url, properties);
    }

    /**
     * Check if a HSQLDB database upgrade will occur and backups are needed.
     *
     * DB   Driver      Likely reason                                Decision
     * null -           new db or non-legacy                         false
     * -    null or !2  something went wrong, we better make copies  true
     * 1.x  2.x         this is the big upgrade                      true
     * 2.x  2.x         already up to date                           false
     *
     * @return true if a database backup/migration should be performed
     */
    public static boolean isHsqldbDatabaseUpgradeNeeded() {
        // Check the current database version
        String currentVersion = getHsqldbDatabaseVersion();
        if (currentVersion == null) {
            LOG.debug("HSQLDB database not found, skipping upgrade checks");
            return false;
        }

        // Check the database driver version
        String driverVersion = null;
        try {
            Driver driver = DriverManager.getDriver(SettingsService.getDefaultJDBCUrl());
            driverVersion = String.format("%d.%d", driver.getMajorVersion(), driver.getMinorVersion());
            if (driver.getMajorVersion() != 2) {
                LOG.warn("HSQLDB database driver version {} is untested ; trying to connect anyway, this may upgrade the database from version {}", driverVersion, currentVersion);
                return true;
            }
        } catch (SQLException e) {
            LOG.warn("HSQLDB database driver version cannot be determined ; trying to connect anyway, this may upgrade the database from version {}", currentVersion, e);
            return true;
        }

        // Log what we're about to do and determine if we should perform a controlled upgrade with backups.
        if (currentVersion.startsWith(driverVersion)) {
            // If we're already on the same version as the driver, nothing should happen.
            LOG.debug("HSQLDB database upgrade unneeded, already on version {}", driverVersion);
            return false;
        } else if (currentVersion.startsWith("2.")) {
            // If the database version is 2.x but older than the driver, the upgrade should be relatively painless.
            LOG.debug("HSQLDB database will be silently upgraded from version {} to {}", currentVersion, driverVersion);
            return false;
        } else if ("1.8.0".equals(currentVersion) || "1.8.1".equals(currentVersion)) {
            // If we're on a 1.8.0 or 1.8.1 database and upgrading to 2.x, we're going to handle this manually and check what we're doing.
            LOG.info("HSQLDB database upgrade needed, from version {} to {}", currentVersion, driverVersion);
            return true;
        } else {
            // If this happens we're on a completely untested version and we don't know what will happen.
            LOG.warn("HSQLDB database upgrade needed, from version {} to {}", currentVersion, driverVersion);
            return true;
        }
    }

    /**
     * Perform a backup of the HSQLDB database, to a timestamped directory.
     * @return the path to the backup directory
     */
    public static Path performHsqldbDatabaseBackup() throws IOException {

        String timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
        Path source = Paths.get(SettingsService.getDefaultJDBCPath()).getParent();
        Path destination = source.resolveSibling(String.format("%s.backup.%s", source.getFileName().toString(), timestamp));

        LOG.debug("Performing HSQLDB database backup...");
        FileUtils.copyDirectory(source.toFile(), destination.toFile());
        LOG.info("HSQLDB database backed up to {}", destination.toString());

        return destination;
    }

    /**
     * Perform an in-place database upgrade from HSQLDB 1.x to 2.x.
     */
    public static void performHsqldbDatabaseUpgrade() throws SQLException {

        LOG.debug("Performing HSQLDB database upgrade...");

        // This will upgrade HSQLDB on the first connection. This does not
        // use Spring's DataSource, as running SHUTDOWN against it will
        // prevent further connections to the database.
        try (Connection conn = getHsqldbDatabaseConnection()) {
            LOG.debug("Database connection established. Current version is: {}", conn.getMetaData().getDatabaseProductVersion());
            // On upgrade, the official documentation recommends that we
            // run 'SHUTDOWN SCRIPT' to compact all the database into a
            // single SQL file.
            //
            // In practice, if we don't do that, we did not observe issues
            // immediately but after the upgrade.
            LOG.debug("Shutting down database (SHUTDOWN SCRIPT)...");
            try (Statement st = conn.createStatement()) {
                st.execute("SHUTDOWN SCRIPT");
            }
        }

        LOG.info("HSQLDB database has been upgraded to version {}", getHsqldbDatabaseVersion());
    }

    /**
     * If needed, perform an in-place database upgrade from HSQLDB 1.x to 2.x after having created backups.
     */
    public static void upgradeHsqldbDatabaseSafely() {
        if (LegacyHsqlUtil.isHsqldbDatabaseUpgradeNeeded()) {
            try {
                performHsqldbDatabaseBackup();
            } catch (Exception e) {
                throw new RuntimeException("Failed to backup HSQLDB database before upgrade", e);
            }
            try {
                performHsqldbDatabaseUpgrade();
            } catch (Exception e) {
                throw new RuntimeException("Failed to upgrade HSQLDB database", e);
            }
        }
    }
}
