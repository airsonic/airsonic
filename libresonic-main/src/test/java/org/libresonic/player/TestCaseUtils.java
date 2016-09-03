package org.libresonic.player;

import org.libresonic.player.dao.DaoHelper;
import org.libresonic.player.service.MediaScannerService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TestCaseUtils {

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
    Map<String, Integer> nbRecords =
            tableNames.stream()
                    .collect(Collectors.toMap(table -> table, table -> recordsInTable(table,daoHelper)));

    return nbRecords;
  }

  /**
   * Counts records in a table.
   *
   * @param tableName
   * @param daoHelper
   * @return
     */
  public static Integer recordsInTable(String tableName, DaoHelper daoHelper) {
    return daoHelper.getJdbcTemplate().queryForInt("select count(1) from " + tableName);
  }


  /**
   * Constructs the path of a resource according to the path of the current class.
   *
   * @param baseResources
   * @return
     */
  private static String basePath(String baseResources) {
    String basePath = TestCaseUtils.class.getResource(baseResources).toString();
    if (basePath.startsWith("file:")) {
      return TestCaseUtils.class.getResource(baseResources).toString().replace("file:","");
    }
    return basePath;
  }


  public static void setLibresonicHome(String baseResources) {
    String subsoncicHome = basePath(baseResources);
    System.setProperty("libresonic.home",subsoncicHome);
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
