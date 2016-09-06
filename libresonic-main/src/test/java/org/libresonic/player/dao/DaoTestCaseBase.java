package org.libresonic.player.dao;

import junit.framework.TestCase;
import org.libresonic.player.TestCaseUtils;
import org.libresonic.player.util.FileUtil;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.File;

/**
 * Superclass for all DAO test cases.
 * Creates and configures the DAO's, and resets the test database.
 *
 * @author Sindre Mehus
 */
public abstract class DaoTestCaseBase extends TestCase {

    /** Do not re-create database if it is less than one hour old. */
    private static final long MAX_DB_AGE_MILLIS = 60L * 60 * 1000;

    static {
        deleteDatabase();
    }

    private DaoHelper daoHelper;
    protected PlayerDao playerDao;
    protected InternetRadioDao internetRadioDao;
    protected RatingDao ratingDao;
    protected MusicFolderDao musicFolderDao;
    protected UserDao userDao;
    protected TranscodingDao transcodingDao;
    protected PodcastDao podcastDao;

    protected DaoTestCaseBase() {
        daoHelper = new HsqlDaoHelper();

        playerDao = new PlayerDao();
        internetRadioDao = new InternetRadioDao();
        ratingDao = new RatingDao();
        musicFolderDao = new MusicFolderDao();
        userDao = new UserDao();
        transcodingDao = new TranscodingDao();
        podcastDao = new PodcastDao();

        playerDao.setDaoHelper(daoHelper);
        internetRadioDao.setDaoHelper(daoHelper);
        ratingDao.setDaoHelper(daoHelper);
        musicFolderDao.setDaoHelper(daoHelper);
        userDao.setDaoHelper(daoHelper);
        transcodingDao.setDaoHelper(daoHelper);
        podcastDao.setDaoHelper(daoHelper);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        getJdbcTemplate().execute("shutdown");
    }

    protected JdbcTemplate getJdbcTemplate() {
        return daoHelper.getJdbcTemplate();
    }

    private static void deleteDatabase() {
        File libresonicHome = new File(TestCaseUtils.libresonicHomePathForTest());
        File dbHome = new File(libresonicHome, "db");
        System.setProperty("libresonic.home", libresonicHome.getPath());

        long now = System.currentTimeMillis();
        if (now - dbHome.lastModified() > MAX_DB_AGE_MILLIS) {
            System.out.println("Resetting test database: " + dbHome);
            delete(dbHome);
        }
    }

    private static void delete(File file) {
        if (file.isDirectory()) {
            for (File child : FileUtil.listFiles(file)) {
                delete(child);
            }
        }
        file.delete();
    }
}
