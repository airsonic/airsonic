package org.airsonic.player.api.jukebox;

import org.airsonic.player.TestCaseUtils;
import org.airsonic.player.dao.*;
import org.airsonic.player.domain.*;
import org.airsonic.player.service.MediaScannerService;
import org.airsonic.player.service.PlayerService;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.util.HomeRule;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public abstract class AbstractAirsonicRestApiJukeboxIntTest {

    @ClassRule
    public static final HomeRule classRule = new HomeRule(); // sets airsonic.home to a temporary dir

    @TestConfiguration
    static class Config {
        private static class SpiedPlayerDaoPlayQueueFactory extends PlayerDaoPlayQueueFactory {
            @Override
            public PlayQueue createPlayQueue() {
                return spy(super.createPlayQueue());
            }
        }

        @Bean
        public PlayerDaoPlayQueueFactory playerDaoPlayQueueFactory() {
            return new SpiedPlayerDaoPlayQueueFactory();
        }
    }

    protected static final String CLIENT_NAME = "airsonic";
    protected static final String JUKEBOX_PLAYER_NAME = CLIENT_NAME + "-jukebox";
    private static final String EXPECTED_FORMAT = "json";
    private static String AIRSONIC_API_VERSION;

    private static boolean dataBasePopulated;
    private static DaoHelper staticDaoHelper;

    @Autowired
    protected PlayerService playerService;
    @Autowired
    private MockMvc mvc;
    @Autowired
    private MusicFolderDao musicFolderDao;
    @Autowired
    private SettingsService settingsService;
    @Autowired
    private MediaScannerService mediaScannerService;
    @Autowired
    private PlayerDao playerDao;
    @Autowired
    private MediaFileDao mediaFileDao;
    @Autowired
    private DaoHelper daoHelper;
    @Autowired
    private AlbumDao albumDao;
    @Autowired
    private ArtistDao artistDao;

    private Player testJukeboxPlayer;

    @BeforeClass
    public static void setupClass() {
        AIRSONIC_API_VERSION = TestCaseUtils.restApiVersion();
        dataBasePopulated = false;
    }

    @AfterClass
    public static void cleanDataBase() {
        staticDaoHelper.getJdbcTemplate().execute("DROP SCHEMA PUBLIC CASCADE");
        staticDaoHelper = null;
        dataBasePopulated = false;
    }

    /**
     * Populate test datas in the database only once.
     *
     * <ul>
     *     <li>Creates 2 music folder</li>
     *     <li>Scans the music folders</li>
     *     <li>Creates a test jukebox player</li>
     * </ul>
     */
    private void populateDatabase() {
        if (!dataBasePopulated) {
            staticDaoHelper = daoHelper;

            assertThat(musicFolderDao.getAllMusicFolders().size()).isEqualTo(1);
            MusicFolderTestData.getTestMusicFolders().forEach(musicFolderDao::createMusicFolder);
            settingsService.clearMusicFolderCache();

            TestCaseUtils.execScan(mediaScannerService);

            assertThat(playerDao.getAllPlayers().size()).isEqualTo(0);
            createTestPlayer();
            assertThat(playerDao.getAllPlayers().size()).isEqualTo(1);

            dataBasePopulated = true;
        }
    }

    @Before
    public void setup() throws Exception {
        populateDatabase();

        testJukeboxPlayer = findTestJukeboxPlayer();
        assertThat(testJukeboxPlayer).isNotNull();
        reset(testJukeboxPlayer.getPlayQueue());
        testJukeboxPlayer.getPlayQueue().clear();
        assertThat(testJukeboxPlayer.getPlayQueue().size()).isEqualTo(0);
        testJukeboxPlayer.getPlayQueue().addFiles(true,
                mediaFileDao.getSongsForAlbum("_DIR_ Ravel", "Complete Piano Works"));
        assertThat(testJukeboxPlayer.getPlayQueue().size()).isEqualTo(2);
    }

    protected abstract void createTestPlayer();

    private Player findTestJukeboxPlayer() {
        return playerDao.getAllPlayers().stream().filter(player -> player.getName().equals(JUKEBOX_PLAYER_NAME))
                .findFirst().orElseThrow(() -> new RuntimeException("No player found in database"));
    }

    private String convertDateToString(Date date) {
       SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.000'Z'");
       formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
       return formatter.format(date);
    }

    private ResultMatcher playListItem1isCorrect() {
        MediaFile mediaFile = testJukeboxPlayer.getPlayQueue().getFile(0);
        MediaFile parent = mediaFileDao.getMediaFile(mediaFile.getParentPath());
        Album album = albumDao.getAlbum(mediaFile.getArtist(), mediaFile.getAlbumName());
        Artist artist = artistDao.getArtist(mediaFile.getArtist());
        assertThat(album).isNotNull();
        return result -> {
            jsonPath("$.subsonic-response.jukeboxPlaylist.entry[0].id").value(mediaFile.getId()).match(result);
            jsonPath("$.subsonic-response.jukeboxPlaylist.entry[0].parent").value(parent.getId()).match(result);
            jsonPath("$.subsonic-response.jukeboxPlaylist.entry[0].isDir").value(false).match(result);
            jsonPath("$.subsonic-response.jukeboxPlaylist.entry[0].title").value("01 - Gaspard de la Nuit - i. Ondine").match(result);
            jsonPath("$.subsonic-response.jukeboxPlaylist.entry[0].album").value("Complete Piano Works").match(result);
            jsonPath("$.subsonic-response.jukeboxPlaylist.entry[0].artist").value("_DIR_ Ravel").match(result);
            jsonPath("$.subsonic-response.jukeboxPlaylist.entry[0].coverArt").value(parent.getId()).match(result);
            jsonPath("$.subsonic-response.jukeboxPlaylist.entry[0].size").value(45138).match(result);
            jsonPath("$.subsonic-response.jukeboxPlaylist.entry[0].contentType").value("audio/mpeg").match(result);
            jsonPath("$.subsonic-response.jukeboxPlaylist.entry[0].suffix").value("mp3").match(result);
            jsonPath("$.subsonic-response.jukeboxPlaylist.entry[0].duration").value(2).match(result);
            jsonPath("$.subsonic-response.jukeboxPlaylist.entry[0].bitRate").value(128).match(result);
            jsonPath("$.subsonic-response.jukeboxPlaylist.entry[0].path").value("_DIR_ Ravel/_DIR_ Ravel - Complete Piano Works/01 - Gaspard de la Nuit - i. Ondine.mp3").match(result);
            jsonPath("$.subsonic-response.jukeboxPlaylist.entry[0].isVideo").value(false).match(result);
            jsonPath("$.subsonic-response.jukeboxPlaylist.entry[0].playCount").isNumber().match(result);
            jsonPath("$.subsonic-response.jukeboxPlaylist.entry[0].created").value(convertDateToString(mediaFile.getCreated())).match(result);
            jsonPath("$.subsonic-response.jukeboxPlaylist.entry[0].albumId").value(album.getId()).match(result);
            jsonPath("$.subsonic-response.jukeboxPlaylist.entry[0].artistId").value(artist.getId()).match(result);
            jsonPath("$.subsonic-response.jukeboxPlaylist.entry[0].type").value("music").match(result);
        };
    }

    @Test
    @WithMockUser(username = "admin")
    public void jukeboxStartActionTest() throws Exception {
        // Given

        // When and Then
        performStartAction();
        performStatusAction("true");
        performGetAction()
                .andExpect(jsonPath("$.subsonic-response.jukeboxPlaylist.currentIndex").value("0"))
                .andExpect(jsonPath("$.subsonic-response.jukeboxPlaylist.playing").value("true"))
                .andExpect(jsonPath("$.subsonic-response.jukeboxPlaylist.gain").value("0.75"))
                .andExpect(jsonPath("$.subsonic-response.jukeboxPlaylist.position").value("0"))
                .andExpect(jsonPath("$.subsonic-response.jukeboxPlaylist.entry").isArray())
                .andExpect(jsonPath("$.subsonic-response.jukeboxPlaylist.entry.length()").value(2))
                .andExpect(playListItem1isCorrect())
                .andDo(print());

        verify(testJukeboxPlayer.getPlayQueue(), times(2)).setStatus(PlayQueue.Status.PLAYING);
        assertThat(testJukeboxPlayer.getPlayQueue().getIndex()).isEqualTo(0);
        assertThat(testJukeboxPlayer.getPlayQueue().getStatus()).isEqualTo(PlayQueue.Status.PLAYING);
    }

    @Test
    @WithMockUser(username = "admin")
    public void jukeboxStopActionTest() throws Exception {
        // Given

        // When and Then
        performStartAction();
        performStatusAction("true");
        performStopAction();
        performStatusAction("false");

        verify(testJukeboxPlayer.getPlayQueue(), times(2)).setStatus(PlayQueue.Status.PLAYING);
        verify(testJukeboxPlayer.getPlayQueue(), times(1)).setStatus(PlayQueue.Status.STOPPED);
        assertThat(testJukeboxPlayer.getPlayQueue().getIndex()).isEqualTo(0);
        assertThat(testJukeboxPlayer.getPlayQueue().getStatus()).isEqualTo(PlayQueue.Status.STOPPED);
    }

    private void performStatusAction(String expectedPlayingValue) throws Exception {
        mvc.perform(get("/rest/jukeboxControl.view")
                .param("v", AIRSONIC_API_VERSION)
                .param("c", CLIENT_NAME)
                .param("f", EXPECTED_FORMAT)
                .param("action", "status")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subsonic-response.status").value("ok"))
                .andExpect(jsonPath("$.subsonic-response.jukeboxStatus.currentIndex").value("0"))
                .andExpect(jsonPath("$.subsonic-response.jukeboxStatus.playing").value(expectedPlayingValue))
                .andExpect(jsonPath("$.subsonic-response.jukeboxStatus.position").value("0"));
    }

    private ResultActions performGetAction() throws Exception {
        return mvc.perform(get("/rest/jukeboxControl.view")
                .param("v", AIRSONIC_API_VERSION)
                .param("c", CLIENT_NAME)
                .param("f", EXPECTED_FORMAT)
                .param("action", "get")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subsonic-response.status").value("ok"));
    }

    private void performStopAction() throws Exception {
        mvc.perform(get("/rest/jukeboxControl.view")
                .param("v", AIRSONIC_API_VERSION)
                .param("c", CLIENT_NAME)
                .param("f", EXPECTED_FORMAT)
                .param("action", "stop")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subsonic-response.status").value("ok"))
                .andExpect(jsonPath("$.subsonic-response.jukeboxStatus.currentIndex").value("0"))
                .andExpect(jsonPath("$.subsonic-response.jukeboxStatus.playing").value("false"))
                .andExpect(jsonPath("$.subsonic-response.jukeboxStatus.position").value("0"));
    }

    private void performStartAction() throws Exception {
        mvc.perform(get("/rest/jukeboxControl.view")
                .param("v", AIRSONIC_API_VERSION)
                .param("c", CLIENT_NAME)
                .param("f", EXPECTED_FORMAT)
                .param("action", "start")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subsonic-response.status").value("ok"))
                .andExpect(jsonPath("$.subsonic-response.jukeboxStatus.currentIndex").value("0"))
                .andExpect(jsonPath("$.subsonic-response.jukeboxStatus.playing").value("true"))
                .andExpect(jsonPath("$.subsonic-response.jukeboxStatus.position").value("0"));
    }
}
