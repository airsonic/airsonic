package org.airsonic.player.api;

import org.airsonic.player.TestCaseUtils;
import org.airsonic.player.dao.UserDao;
import org.airsonic.player.domain.User;
import org.airsonic.player.util.HomeRule;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class AirsonicRestApiIntTest {

    private static final String CLIENT_NAME = "airsonic";
    private static final String AIRSONIC_USER = "admin";
    private static final String AIRSONIC_PASSWORD = "admin";
    private static final String EXPECTED_FORMAT = "json";
    private static final String CLIENT_SALT = "testsalt";

    private static String AIRSONIC_API_VERSION;

    @Autowired
    private MockMvc mvc;
    @Autowired
    private UserDao userDao;

    @ClassRule
    public static final HomeRule classRule = new HomeRule(); // sets airsonic.home to a temporary dir

    @BeforeClass
    public static void setupClass() {
        AIRSONIC_API_VERSION = TestCaseUtils.restApiVersion();
    }

    @Test
    public void pingTest() throws Exception {
        mvc.perform(get("/rest/ping")
                .param("v", AIRSONIC_API_VERSION)
                .param("c", CLIENT_NAME)
                .param("u", AIRSONIC_USER)
                .param("p", AIRSONIC_PASSWORD)
                .param("f", EXPECTED_FORMAT)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subsonic-response.status").value("ok"))
                .andExpect(jsonPath("$.subsonic-response.version").value(AIRSONIC_API_VERSION))
                .andDo(print());
    }

    @Test
    public void pingTestWithRestToken() throws Exception {

        User user = userDao.getUserByName(AIRSONIC_USER, true);
        userDao.updateRestTokenForUser(user);

        mvc.perform(get("/rest/ping")
                .param("v", AIRSONIC_API_VERSION)
                .param("c", CLIENT_NAME)
                .param("u", AIRSONIC_USER)
                .param("p", user.getRestToken())
                .param("f", EXPECTED_FORMAT)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subsonic-response.status").value("ok"))
                .andExpect(jsonPath("$.subsonic-response.version").value(AIRSONIC_API_VERSION))
                .andDo(print());
    }

    @Test
    public void pingTestWithRestTokenSalt() throws Exception {

        User user = userDao.getUserByName(AIRSONIC_USER, true);
        userDao.updateRestTokenForUser(user);

        String expectedToken = DigestUtils.md5Hex(user.getRestToken() + CLIENT_SALT);

        mvc.perform(get("/rest/ping")
                .param("v", AIRSONIC_API_VERSION)
                .param("c", CLIENT_NAME)
                .param("u", AIRSONIC_USER)
                .param("t", expectedToken)
                .param("s", CLIENT_SALT)
                .param("f", EXPECTED_FORMAT)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subsonic-response.status").value("ok"))
                .andExpect(jsonPath("$.subsonic-response.version").value(AIRSONIC_API_VERSION))
                .andDo(print());
    }

    @Test
    public void pingTestWithSalt() throws Exception {

        String expectedToken = DigestUtils.md5Hex(AIRSONIC_PASSWORD + CLIENT_SALT);

        mvc.perform(get("/rest/ping")
                .param("v", AIRSONIC_API_VERSION)
                .param("c", CLIENT_NAME)
                .param("u", AIRSONIC_USER)
                .param("t", expectedToken)
                .param("s", CLIENT_SALT)
                .param("f", EXPECTED_FORMAT)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subsonic-response.status").value("ok"))
                .andExpect(jsonPath("$.subsonic-response.version").value(AIRSONIC_API_VERSION))
                // FIXME: Uncomment this (and remove the "ok" lines above) when the t+s auth method is removed
                //.andExpect(jsonPath("$.subsonic-response.status").value("failed"))
                //.andExpect(jsonPath("$.subsonic-response.version").value(AIRSONIC_API_VERSION))
                //.andExpect(jsonPath("$.subsonic-response.error.code").value(40))
                .andDo(print());
    }
}
