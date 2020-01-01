package org.airsonic.player.service;

import org.airsonic.player.domain.InternetRadio;
import org.airsonic.player.domain.InternetRadioSource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.List;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class InternetRadioServiceTest {

    String TEST_RADIO_NAME = "Test Radio";
    String TEST_RADIO_HOMEPAGE = "http://example.com";
    String TEST_PLAYLIST_URL_MOVE = "http://example.com/stream_move.m3u";
    String TEST_PLAYLIST_URL_MOVE_LOOP = "http://example.com/stream_infinity_move.m3u";
    String TEST_PLAYLIST_URL_LARGE = "http://example.com/stream_infinity_repeat.m3u";
    String TEST_PLAYLIST_URL_LARGE_2 = "http://example.com/stream_infinity_big.m3u";
    String TEST_PLAYLIST_URL_1 = "http://example.com/stream1.m3u";
    String TEST_PLAYLIST_URL_2 = "http://example.com/stream2.m3u";
    String TEST_STREAM_URL_1 = "http://example.com/stream1";
    String TEST_STREAM_URL_2 = "http://example.com/stream2";
    String TEST_STREAM_URL_3 = "http://example.com/stream3";
    String TEST_STREAM_URL_4 = "http://example.com/stream4";
    String TEST_STREAM_PLAYLIST_CONTENTS_1 = (
        "http://example.com/stream1\n" +
        "http://example.com/stream2\n"
    );
    String TEST_STREAM_PLAYLIST_CONTENTS_2 = (
        "#EXTM3U\n" +
        "#EXTINF:123, Sample artist - Sample title\n" +
        "http://example.com/stream3\n" +
        "#EXTINF:321,Example Artist - Example title\n" +
        "http://example.com/stream4\n"
    );

    InternetRadio radio1;
    InternetRadio radio2;
    InternetRadio radioMove;
    InternetRadio radioMoveLoop;
    InternetRadio radioLarge;
    InternetRadio radioLarge2;

    @Spy
    InternetRadioService internetRadioService;

    @Before
    public void setup() throws Exception {

        // Prepare a mock InternetRadio object
        radio1 = new InternetRadio(1, TEST_RADIO_NAME, TEST_PLAYLIST_URL_1, TEST_RADIO_HOMEPAGE, true, new Date());
        radio2 = new InternetRadio(2, TEST_RADIO_NAME, TEST_PLAYLIST_URL_2, TEST_RADIO_HOMEPAGE, true, new Date());
        radioMove = new InternetRadio(3, TEST_RADIO_NAME, TEST_PLAYLIST_URL_MOVE, TEST_RADIO_HOMEPAGE, true, new Date());
        radioMoveLoop = new InternetRadio(3, TEST_RADIO_NAME, TEST_PLAYLIST_URL_MOVE_LOOP, TEST_RADIO_HOMEPAGE, true, new Date());
        radioLarge = new InternetRadio(4, TEST_RADIO_NAME, TEST_PLAYLIST_URL_LARGE, TEST_RADIO_HOMEPAGE, true, new Date());
        radioLarge2 = new InternetRadio(5, TEST_RADIO_NAME, TEST_PLAYLIST_URL_LARGE_2, TEST_RADIO_HOMEPAGE, true, new Date());

        // Prepare the mocked URL connection for the simple playlist
        HttpURLConnection mockURLConnection1 = Mockito.mock(HttpURLConnection.class);
        InputStream mockURLInputStream1 = new ByteArrayInputStream(TEST_STREAM_PLAYLIST_CONTENTS_1.getBytes());
        doReturn(mockURLInputStream1).when(mockURLConnection1).getInputStream();
        doReturn(HttpURLConnection.HTTP_OK).when(mockURLConnection1).getResponseCode();

        // Prepare the mocked URL connection for the second simple playlist
        HttpURLConnection mockURLConnection2 = Mockito.mock(HttpURLConnection.class);
        InputStream mockURLInputStream2 = new ByteArrayInputStream(TEST_STREAM_PLAYLIST_CONTENTS_2.getBytes());
        doReturn(mockURLInputStream2).when(mockURLConnection2).getInputStream();
        doReturn(HttpURLConnection.HTTP_OK).when(mockURLConnection2).getResponseCode();

        // Prepare the mocked URL connection for the redirection to simple playlist
        HttpURLConnection mockURLConnectionMove = Mockito.mock(HttpURLConnection.class);
        InputStream mockURLInputStreamMove = new ByteArrayInputStream("".getBytes());
        doReturn(mockURLInputStreamMove).when(mockURLConnectionMove).getInputStream();
        doReturn(HttpURLConnection.HTTP_MOVED_PERM).when(mockURLConnectionMove).getResponseCode();
        doReturn(TEST_PLAYLIST_URL_2).when(mockURLConnectionMove).getHeaderField(eq("Location"));

        // Prepare the mocked URL connection for the redirection loop
        HttpURLConnection mockURLConnectionMoveLoop = Mockito.mock(HttpURLConnection.class);
        InputStream mockURLInputStreamMoveLoop = new ByteArrayInputStream("".getBytes());
        doReturn(mockURLInputStreamMoveLoop).when(mockURLConnectionMoveLoop).getInputStream();
        doReturn(HttpURLConnection.HTTP_MOVED_PERM).when(mockURLConnectionMoveLoop).getResponseCode();
        doReturn(TEST_PLAYLIST_URL_MOVE_LOOP).when(mockURLConnectionMoveLoop).getHeaderField(eq("Location"));

        // Prepare the mocked URL connection for the 'content too large' test
        HttpURLConnection mockURLConnectionLarge = Mockito.mock(HttpURLConnection.class);
        InputStream mockURLInputStreamLarge = new InputStream() {
            private long pos = 0;
            @Override
            public int read() {
                return TEST_STREAM_PLAYLIST_CONTENTS_2.charAt((int)(pos++ % TEST_STREAM_PLAYLIST_CONTENTS_2.length()));
            }
        };
        doReturn(mockURLInputStreamLarge).when(mockURLConnectionLarge).getInputStream();
        doReturn(HttpURLConnection.HTTP_OK).when(mockURLConnectionLarge).getResponseCode();

        // Prepare the mocked URL connection for the 'content too large' test
        // (return a single entry with 'aaaa...' running infinitely long).
        HttpURLConnection mockURLConnectionLarge2 = Mockito.mock(HttpURLConnection.class);
        InputStream mockURLInputStreamLarge2 = new InputStream() {
            private long pos = 0;
            @Override
            public int read() {
                return 0x41;
            }
        };
        doReturn(mockURLInputStreamLarge2).when(mockURLConnectionLarge2).getInputStream();
        doReturn(HttpURLConnection.HTTP_OK).when(mockURLConnectionLarge2).getResponseCode();

        // Prepare the mock 'connectToURL' method
        doReturn(mockURLConnection1).when(internetRadioService).connectToURL(eq(new URL(TEST_PLAYLIST_URL_1)));
        doReturn(mockURLConnection2).when(internetRadioService).connectToURL(eq(new URL(TEST_PLAYLIST_URL_2)));
        doReturn(mockURLConnectionMove).when(internetRadioService).connectToURL(eq(new URL(TEST_PLAYLIST_URL_MOVE)));
        doReturn(mockURLConnectionMoveLoop).when(internetRadioService).connectToURL(eq(new URL(TEST_PLAYLIST_URL_MOVE_LOOP)));
        doReturn(mockURLConnectionLarge).when(internetRadioService).connectToURL(eq(new URL(TEST_PLAYLIST_URL_LARGE)));
        doReturn(mockURLConnectionLarge2).when(internetRadioService).connectToURL(eq(new URL(TEST_PLAYLIST_URL_LARGE_2)));
    }

    @Test
    public void testParseSimplePlaylist() {
        List<InternetRadioSource> radioSources = internetRadioService.getInternetRadioSources(radio1);

        Assert.assertEquals(2, radioSources.size());
        Assert.assertEquals(TEST_STREAM_URL_1, radioSources.get(0).getStreamUrl());
        Assert.assertEquals(TEST_STREAM_URL_2, radioSources.get(1).getStreamUrl());
    }

    @Test
    public void testRedirects() {
        List<InternetRadioSource> radioSources = internetRadioService.getInternetRadioSources(radioMove);

        Assert.assertEquals(2, radioSources.size());
        Assert.assertEquals(TEST_STREAM_URL_3, radioSources.get(0).getStreamUrl());
        Assert.assertEquals(TEST_STREAM_URL_4, radioSources.get(1).getStreamUrl());
    }

    @Test
    public void testLargeInput() {
        List<InternetRadioSource> radioSources = internetRadioService.getInternetRadioSources(radioLarge);

        // A PlaylistTooLarge exception is thrown internally, and the
        // `getInternetRadioSources` method logs it and returns a
        // limited number of sources.
        Assert.assertEquals(250, radioSources.size());
    }

    @Test
    public void testLargeInputURL() {
        List<InternetRadioSource> radioSources = internetRadioService.getInternetRadioSources(radioLarge2);

        // A PlaylistTooLarge exception is thrown internally, and the
        // `getInternetRadioSources` method logs it and returns a
        // limited number of bytes from the input.
        Assert.assertEquals(1, radioSources.size());
    }

    @Test
    public void testRedirectLoop() {
        List<InternetRadioSource> radioSources = internetRadioService.getInternetRadioSources(radioMoveLoop);

        // A PlaylistHasTooManyRedirects exception is thrown internally,
        // and the `getInternetRadioSources` method logs it and returns 0 sources.
        Assert.assertEquals(0, radioSources.size());
    }
}
