package org.airsonic.player.controller;

import junit.framework.TestCase;
import org.airsonic.player.dao.UserDao;
import org.airsonic.player.domain.User;
import org.airsonic.player.util.HomeRule;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class LoginControllerIntTest extends TestCase {

    private static final String AIRSONIC_USER = "admin";
    private static final String AIRSONIC_PASSWORD = "admin";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private UserDao userDao;

    @ClassRule
    public static final HomeRule classRule = new HomeRule(); // sets airsonic.home to a temporary dir

    @Test
    public void testPasswordLogin() throws Exception {
        mvc.perform(formLogin("/login")
                .user("j_username", AIRSONIC_USER)
                .password("j_password", AIRSONIC_PASSWORD))
                .andExpect(authenticated());
    }

    @Test
    public void testRestTokenLogin() throws Exception {
        User user = userDao.getUserByName(AIRSONIC_USER, true);
        userDao.updateRestTokenForUser(user);

        mvc.perform(formLogin("/login")
                .user("j_username", AIRSONIC_USER)
                .password("j_password", user.getRestToken()))
                .andExpect(unauthenticated());
    }
}
