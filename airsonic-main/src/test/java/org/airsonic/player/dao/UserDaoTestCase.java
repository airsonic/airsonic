package org.airsonic.player.dao;

import org.airsonic.player.domain.AvatarScheme;
import org.airsonic.player.domain.User;
import org.airsonic.player.domain.UserSettings;
import org.junit.Before;
import org.junit.Test;
import org.airsonic.player.domain.TranscodeScheme;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Date;
import java.util.Locale;

import static org.junit.Assert.*;

/**
 * Unit test of {@link UserDao}.
 *
 * @author Sindre Mehus
 */
public class UserDaoTestCase extends DaoTestCaseBean2 {

    @Autowired
    UserDao userDao;

    @Before
    public void setUp() throws Exception {
        getJdbcTemplate().execute("delete from user_role");
        getJdbcTemplate().execute("delete from user");
    }

    @Test
    public void testCreateUser() {
        User user = new User("sindre", "secret", "sindre@activeobjects.no", false, 1000L, 2000L, 3000L);
        user.setAdminRole(true);
        user.setCommentRole(true);
        user.setCoverArtRole(true);
        user.setDownloadRole(false);
        user.setPlaylistRole(true);
        user.setUploadRole(false);
        user.setPodcastRole(true);
        user.setStreamRole(true);
        user.setJukeboxRole(true);
        user.setSettingsRole(true);
        userDao.createUser(user);

        User newUser = userDao.getAllUsers().get(0);
        assertUserEquals(user, newUser);
    }

    @Test
    public void testCreateUserTransactionalError() {
        User user = new User ("muff1nman", "secret", "noemail") {
            @Override
            public boolean isPlaylistRole() {
                throw new RuntimeException();
            }
        };

        user.setAdminRole(true);
        int beforeSize = userDao.getAllUsers().size();
        boolean caughtException = false;
        try {
            userDao.createUser(user);
        } catch (RuntimeException e) {
            caughtException = true;
        }
        assertTrue("It was expected for createUser to throw an exception", caughtException);
        assertEquals(beforeSize, userDao.getAllUsers().size());
    }

    @Test
    public void testUpdateUser() {
        User user = new User("sindre", "secret", null);
        user.setAdminRole(true);
        user.setCommentRole(true);
        user.setCoverArtRole(true);
        user.setDownloadRole(false);
        user.setPlaylistRole(true);
        user.setUploadRole(false);
        user.setPodcastRole(true);
        user.setStreamRole(true);
        user.setJukeboxRole(true);
        user.setSettingsRole(true);
        userDao.createUser(user);

        user.setPassword("foo");
        user.setEmail("sindre@foo.bar");
        user.setLdapAuthenticated(true);
        user.setBytesStreamed(1);
        user.setBytesDownloaded(2);
        user.setBytesUploaded(3);
        user.setAdminRole(false);
        user.setCommentRole(false);
        user.setCoverArtRole(false);
        user.setDownloadRole(true);
        user.setPlaylistRole(false);
        user.setUploadRole(true);
        user.setPodcastRole(false);
        user.setStreamRole(false);
        user.setJukeboxRole(false);
        user.setSettingsRole(false);
        userDao.updateUser(user);

        User newUser = userDao.getAllUsers().get(0);
        assertUserEquals(user, newUser);
        assertEquals("Wrong bytes streamed.", 1, newUser.getBytesStreamed());
        assertEquals("Wrong bytes downloaded.", 2, newUser.getBytesDownloaded());
        assertEquals("Wrong bytes uploaded.", 3, newUser.getBytesUploaded());
    }

    @Test
    public void testGetUserByName() {
        User user = new User("sindre", "secret", null);
        userDao.createUser(user);

        User newUser = userDao.getUserByName("sindre", true);
        assertNotNull("Error in getUserByName().", newUser);
        assertUserEquals(user, newUser);

        assertNull("Error in getUserByName().", userDao.getUserByName("sindre2", true));
        assertNull("Error in getUserByName().", userDao.getUserByName("sindre ", true));
        assertNull("Error in getUserByName().", userDao.getUserByName("bente", true));
        assertNull("Error in getUserByName().", userDao.getUserByName("", true));
        assertNull("Error in getUserByName().", userDao.getUserByName(null, true));
    }

    @Test
    public void testDeleteUser() {
        assertEquals("Wrong number of users.", 0, userDao.getAllUsers().size());

        userDao.createUser(new User("sindre", "secret", null));
        assertEquals("Wrong number of users.", 1, userDao.getAllUsers().size());

        userDao.createUser(new User("bente", "secret", null));
        assertEquals("Wrong number of users.", 2, userDao.getAllUsers().size());

        userDao.deleteUser("sindre");
        assertEquals("Wrong number of users.", 1, userDao.getAllUsers().size());

        userDao.deleteUser("bente");
        assertEquals("Wrong number of users.", 0, userDao.getAllUsers().size());
    }

    @Test
    public void testGetRolesForUser() {
        User user = new User("sindre", "secret", null);
        user.setAdminRole(true);
        user.setCommentRole(true);
        user.setPodcastRole(true);
        user.setStreamRole(true);
        user.setSettingsRole(true);
        userDao.createUser(user);

        String[] roles = userDao.getRolesForUser("sindre");
        assertEquals("Wrong number of roles.", 5, roles.length);
        assertEquals("Wrong role.", "admin", roles[0]);
        assertEquals("Wrong role.", "comment", roles[1]);
        assertEquals("Wrong role.", "podcast", roles[2]);
        assertEquals("Wrong role.", "stream", roles[3]);
        assertEquals("Wrong role.", "settings", roles[4]);
    }

    @Test
    public void testUserSettings() {
        assertNull("Error in getUserSettings.", userDao.getUserSettings("sindre"));

        try {
            userDao.updateUserSettings(new UserSettings("sindre"));
            fail("Expected DataIntegrityViolationException.");
        } catch (DataIntegrityViolationException x) {
        }

        userDao.createUser(new User("sindre", "secret", null));
        assertNull("Error in getUserSettings.", userDao.getUserSettings("sindre"));

        userDao.updateUserSettings(new UserSettings("sindre"));
        UserSettings userSettings = userDao.getUserSettings("sindre");
        assertNotNull("Error in getUserSettings().", userSettings);
        assertNull("Error in getUserSettings().", userSettings.getLocale());
        assertNull("Error in getUserSettings().", userSettings.getThemeId());
        assertFalse("Error in getUserSettings().", userSettings.isFinalVersionNotificationEnabled());
        assertFalse("Error in getUserSettings().", userSettings.isBetaVersionNotificationEnabled());
        assertFalse("Error in getUserSettings().", userSettings.isSongNotificationEnabled());
        assertFalse("Error in getUserSettings().", userSettings.isShowSideBar());
        assertFalse("Error in getUserSettings().", userSettings.isLastFmEnabled());
        assertNull("Error in getUserSettings().", userSettings.getLastFmUsername());
        assertNull("Error in getUserSettings().", userSettings.getLastFmPassword());
        assertSame("Error in getUserSettings().", TranscodeScheme.OFF, userSettings.getTranscodeScheme());
        assertFalse("Error in getUserSettings().", userSettings.isShowNowPlayingEnabled());
        assertEquals("Error in getUserSettings().", -1, userSettings.getSelectedMusicFolderId());
        assertFalse("Error in getUserSettings().", userSettings.isPartyModeEnabled());
        assertFalse("Error in getUserSettings().", userSettings.isNowPlayingAllowed());
        assertSame("Error in getUserSettings().", AvatarScheme.NONE, userSettings.getAvatarScheme());
        assertNull("Error in getUserSettings().", userSettings.getSystemAvatarId());
        assertEquals("Error in getUserSettings().", 0, userSettings.getListReloadDelay());
        assertFalse("Error in getUserSettings().", userSettings.isKeyboardShortcutsEnabled());
        assertEquals("Error in getUserSettings().", 0, userSettings.getPaginationSize());

        UserSettings settings = new UserSettings("sindre");
        settings.setLocale(Locale.SIMPLIFIED_CHINESE);
        settings.setThemeId("midnight");
        settings.setBetaVersionNotificationEnabled(true);
        settings.setSongNotificationEnabled(false);
        settings.setShowSideBar(true);
        settings.getMainVisibility().setBitRateVisible(true);
        settings.getPlaylistVisibility().setYearVisible(true);
        settings.setLastFmEnabled(true);
        settings.setLastFmUsername("last_user");
        settings.setLastFmPassword("last_pass");
        settings.setTranscodeScheme(TranscodeScheme.MAX_192);
        settings.setShowNowPlayingEnabled(false);
        settings.setSelectedMusicFolderId(3);
        settings.setPartyModeEnabled(true);
        settings.setNowPlayingAllowed(true);
        settings.setAvatarScheme(AvatarScheme.SYSTEM);
        settings.setSystemAvatarId(1);
        settings.setChanged(new Date(9412L));
        settings.setListReloadDelay(60);
        settings.setKeyboardShortcutsEnabled(true);
        settings.setPaginationSize(120);

        userDao.updateUserSettings(settings);
        userSettings = userDao.getUserSettings("sindre");
        assertNotNull("Error in getUserSettings().", userSettings);
        assertEquals("Error in getUserSettings().", Locale.SIMPLIFIED_CHINESE, userSettings.getLocale());
        assertEquals("Error in getUserSettings().", false, userSettings.isFinalVersionNotificationEnabled());
        assertEquals("Error in getUserSettings().", true, userSettings.isBetaVersionNotificationEnabled());
        assertEquals("Error in getUserSettings().", false, userSettings.isSongNotificationEnabled());
        assertEquals("Error in getUserSettings().", true, userSettings.isShowSideBar());
        assertEquals("Error in getUserSettings().", "midnight", userSettings.getThemeId());
        assertEquals("Error in getUserSettings().", true, userSettings.getMainVisibility().isBitRateVisible());
        assertEquals("Error in getUserSettings().", true, userSettings.getPlaylistVisibility().isYearVisible());
        assertEquals("Error in getUserSettings().", true, userSettings.isLastFmEnabled());
        assertEquals("Error in getUserSettings().", "last_user", userSettings.getLastFmUsername());
        assertEquals("Error in getUserSettings().", "last_pass", userSettings.getLastFmPassword());
        assertSame("Error in getUserSettings().", TranscodeScheme.MAX_192, userSettings.getTranscodeScheme());
        assertFalse("Error in getUserSettings().", userSettings.isShowNowPlayingEnabled());
        assertEquals("Error in getUserSettings().", 3, userSettings.getSelectedMusicFolderId());
        assertTrue("Error in getUserSettings().", userSettings.isPartyModeEnabled());
        assertTrue("Error in getUserSettings().", userSettings.isNowPlayingAllowed());
        assertSame("Error in getUserSettings().", AvatarScheme.SYSTEM, userSettings.getAvatarScheme());
        assertEquals("Error in getUserSettings().", 1, userSettings.getSystemAvatarId().intValue());
        assertEquals("Error in getUserSettings().", new Date(9412L), userSettings.getChanged());
        assertEquals("Error in getUserSettings().", 60, userSettings.getListReloadDelay());
        assertTrue("Error in getUserSettings().", userSettings.isKeyboardShortcutsEnabled());
        assertEquals("Error in getUserSettings().", 120, userSettings.getPaginationSize());

        userDao.deleteUser("sindre");
        assertNull("Error in cascading delete.", userDao.getUserSettings("sindre"));
    }

    private void assertUserEquals(User expected, User actual) {
        assertEquals("Wrong name.", expected.getUsername(), actual.getUsername());
        assertEquals("Wrong password.", expected.getPassword(), actual.getPassword());
        assertEquals("Wrong email.", expected.getEmail(), actual.getEmail());
        assertEquals("Wrong LDAP auth.", expected.isLdapAuthenticated(), actual.isLdapAuthenticated());
        assertEquals("Wrong bytes streamed.", expected.getBytesStreamed(), actual.getBytesStreamed());
        assertEquals("Wrong bytes downloaded.", expected.getBytesDownloaded(), actual.getBytesDownloaded());
        assertEquals("Wrong bytes uploaded.", expected.getBytesUploaded(), actual.getBytesUploaded());
        assertEquals("Wrong admin role.", expected.isAdminRole(), actual.isAdminRole());
        assertEquals("Wrong comment role.", expected.isCommentRole(), actual.isCommentRole());
        assertEquals("Wrong cover art role.", expected.isCoverArtRole(), actual.isCoverArtRole());
        assertEquals("Wrong download role.", expected.isDownloadRole(), actual.isDownloadRole());
        assertEquals("Wrong playlist role.", expected.isPlaylistRole(), actual.isPlaylistRole());
        assertEquals("Wrong upload role.", expected.isUploadRole(), actual.isUploadRole());
        assertEquals("Wrong upload role.", expected.isUploadRole(), actual.isUploadRole());
        assertEquals("Wrong stream role.", expected.isStreamRole(), actual.isStreamRole());
        assertEquals("Wrong jukebox role.", expected.isJukeboxRole(), actual.isJukeboxRole());
        assertEquals("Wrong settings role.", expected.isSettingsRole(), actual.isSettingsRole());
    }
}