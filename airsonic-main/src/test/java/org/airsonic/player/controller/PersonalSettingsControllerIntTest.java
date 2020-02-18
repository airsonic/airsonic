package org.airsonic.player.controller;

import junit.framework.TestCase;
import org.airsonic.player.dao.UserDao;
import org.airsonic.player.domain.User;
import org.airsonic.player.util.HomeRule;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class PersonalSettingsControllerIntTest extends TestCase {

    private static final String AIRSONIC_USER = "user";
    private static final String AIRSONIC_PASSWORD = "password";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private UserDao userDao;

    @ClassRule
    public static final HomeRule classRule = new HomeRule(); // sets airsonic.home to a temporary dir

    @Before
    public void setup() throws Exception {
        userDao.createUser(new User(AIRSONIC_USER, AIRSONIC_PASSWORD, "user@example.com"));
    }

    @After
    public void destroy() throws Exception {
        userDao.deleteUser(AIRSONIC_USER);
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER", "SETTINGS"})
    public void testRegenerateRestToken() throws Exception {
        User user = userDao.getUserByName(AIRSONIC_USER, true);
        user.setRestToken(null);
        userDao.updateUser(user);

        mvc.perform(post("/personalSettings.view")
                .with(csrf())
                .param("regenerateRestToken", "true")
                .contentType(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection());

        user = userDao.getUserByName(AIRSONIC_USER, true);
        assertNotNull(user.getRestToken());
        assertTrue(user.getRestToken().length() == 20);
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER", "SETTINGS"})
    public void testClearRestToken() throws Exception {
        User user = userDao.getUserByName(AIRSONIC_USER, true);
        userDao.updateRestTokenForUser(user);
        assertNotNull(user.getRestToken());

        mvc.perform(post("/personalSettings.view")
                .with(csrf())
                .param("clearRestToken", "true")
                .contentType(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection());

        user = userDao.getUserByName(AIRSONIC_USER, true);
        assertNull(user.getRestToken());
    }
}
