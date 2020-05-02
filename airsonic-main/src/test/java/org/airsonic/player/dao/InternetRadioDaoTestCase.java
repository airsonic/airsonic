package org.airsonic.player.dao;

import org.airsonic.player.domain.InternetRadio;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

import static org.junit.Assert.assertEquals;

/**
 * Unit test of {@link InternetRadioDao}.
 *
 * @author Sindre Mehus
 */
public class InternetRadioDaoTestCase extends DaoTestCaseBean2 {

    @Autowired
    InternetRadioDao internetRadioDao;

    @Before
    public void setUp() {
        getJdbcTemplate().execute("delete from internet_radio");
    }

    @Test
    public void testCreateInternetRadio() {
        InternetRadio radio = new InternetRadio("name", "streamUrl", "homePageUrl", true, new Date());
        internetRadioDao.createInternetRadio(radio);

        InternetRadio newRadio = internetRadioDao.getAllInternetRadios().get(0);
        assertInternetRadioEquals(radio, newRadio);
    }

    @Test
    public void testUpdateInternetRadio() {
        InternetRadio radio = new InternetRadio("name", "streamUrl", "homePageUrl", true, new Date());
        internetRadioDao.createInternetRadio(radio);
        radio = internetRadioDao.getAllInternetRadios().get(0);

        radio.setName("newName");
        radio.setStreamUrl("newStreamUrl");
        radio.setHomepageUrl("newHomePageUrl");
        radio.setEnabled(false);
        radio.setChanged(new Date(234234L));
        internetRadioDao.updateInternetRadio(radio);

        InternetRadio newRadio = internetRadioDao.getAllInternetRadios().get(0);
        assertInternetRadioEquals(radio, newRadio);
    }

    @Test
    public void testDeleteInternetRadio() {
        assertEquals("Wrong number of radios.", 0, internetRadioDao.getAllInternetRadios().size());

        internetRadioDao.createInternetRadio(new InternetRadio("name", "streamUrl", "homePageUrl", true, new Date()));
        assertEquals("Wrong number of radios.", 1, internetRadioDao.getAllInternetRadios().size());

        internetRadioDao.createInternetRadio(new InternetRadio("name", "streamUrl", "homePageUrl", true, new Date()));
        assertEquals("Wrong number of radios.", 2, internetRadioDao.getAllInternetRadios().size());

        internetRadioDao.deleteInternetRadio(internetRadioDao.getAllInternetRadios().get(0).getId());
        assertEquals("Wrong number of radios.", 1, internetRadioDao.getAllInternetRadios().size());

        internetRadioDao.deleteInternetRadio(internetRadioDao.getAllInternetRadios().get(0).getId());
        assertEquals("Wrong number of radios.", 0, internetRadioDao.getAllInternetRadios().size());
    }

    private void assertInternetRadioEquals(InternetRadio expected, InternetRadio actual) {
        assertEquals("Wrong name.", expected.getName(), actual.getName());
        assertEquals("Wrong stream url.", expected.getStreamUrl(), actual.getStreamUrl());
        assertEquals("Wrong home page url.", expected.getHomepageUrl(), actual.getHomepageUrl());
        assertEquals("Wrong enabled state.", expected.isEnabled(), actual.isEnabled());
        assertEquals("Wrong changed date.", expected.getChanged(), actual.getChanged());
    }


}