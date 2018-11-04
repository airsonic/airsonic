package org.airsonic.player.api;

import org.airsonic.player.TestCaseUtils;
import org.airsonic.player.util.HomeRule;
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

    public static final String CLIENT_NAME = "airsonic";
    public static final String AIRSONIC_USER = "admin";
    public static final String AIRSONIC_PASSWORD = "admin";
    public static final String EXPECTED_FORMAT = "json";

    private static String AIRSONIC_API_VERSION;

    @Autowired
    private MockMvc mvc;

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
}
