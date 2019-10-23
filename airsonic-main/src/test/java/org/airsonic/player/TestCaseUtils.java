package org.airsonic.player;

import org.airsonic.player.controller.JAXBWriter;
import org.airsonic.player.dao.DaoHelper;
import org.airsonic.player.service.MediaScannerService;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TestCaseUtils {

    private static final Logger LOG = LoggerFactory.getLogger(TestCaseUtils.class);

    private static File airsonicHomeDirForTest = null;

    /**
     * Returns the path of the AIRSONIC_HOME directory to use for tests.
     * This will create a temporary directory.
     *
     * @return AIRSONIC_HOME directory path.
     * @throws RuntimeException if it fails to create the temp directory.
     */
    public static String airsonicHomePathForTest() {

        if (airsonicHomeDirForTest == null) {
            try {
                airsonicHomeDirForTest = Files.createTempDirectory("airsonic_test_").toFile();
            } catch (IOException e) {
                throw new RuntimeException("Error while creating temporary AIRSONIC_HOME directory for tests");
            }
            LOG.info("AIRSONIC_HOME directory will be {}", airsonicHomeDirForTest.getAbsolutePath());
        }
        return airsonicHomeDirForTest.getAbsolutePath();
    }

    /**
     * @return current REST api version.
     */
    public static String restApiVersion() {
        return new JAXBWriter().getRestProtocolVersion();
    }

    /**
     * Cleans the AIRSONIC_HOME directory used for tests.
     */
    public static void cleanAirsonicHomeForTest() throws IOException {

        File airsonicHomeDir = new File(airsonicHomePathForTest());
        if (airsonicHomeDir.exists() && airsonicHomeDir.isDirectory()) {
            LOG.debug("Delete airsonic home (ie. {}).", airsonicHomeDir.getAbsolutePath());
            try {
                FileUtils.deleteDirectory(airsonicHomeDir);
            } catch (IOException e) {
                LOG.warn("Error while deleting airsonic home.");
                e.printStackTrace();
                throw e;
            }
        }
    }

    /**
     * Constructs a map of records count per table.
     *
     * @param daoHelper DaoHelper object
     * @return Map table name -> records count
     */
    public static Map<String, Integer> recordsInAllTables(DaoHelper daoHelper) {
        List<String> tableNames = daoHelper.getJdbcTemplate().queryForList("" +
                      "select table_name " +
                      "from information_schema.system_tables " +
                      "where table_name not like 'SYSTEM%'"
              , String.class);

        return tableNames.stream()
                .collect(Collectors.toMap(table -> table, table -> recordsInTable(table,daoHelper)));
    }

    /**
     * Counts records in a table.
     */
    public static Integer recordsInTable(String tableName, DaoHelper daoHelper) {
        return daoHelper.getJdbcTemplate().queryForObject("select count(1) from " + tableName,Integer.class);
    }

    public static ApplicationContext loadSpringApplicationContext(String baseResources) {
        String applicationContextService = baseResources + "applicationContext-service.xml";
        String applicationContextCache = baseResources + "applicationContext-cache.xml";

        String[] configLocations = new String[]{
                TestCaseUtils.class.getClass().getResource(applicationContextCache).toString(),
                TestCaseUtils.class.getClass().getResource(applicationContextService).toString()
        };
        return new ClassPathXmlApplicationContext(configLocations);
    }

    /**
     * Scans the music library   * @param mediaScannerService
     */
    public static void execScan(MediaScannerService mediaScannerService) {
        // TODO create a synchronous scan
        mediaScannerService.scanLibrary();

        while (mediaScannerService.isScanning()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
