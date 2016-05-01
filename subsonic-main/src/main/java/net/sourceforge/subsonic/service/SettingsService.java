/*
 This file is part of Subsonic.

 Subsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Subsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Subsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2009 (C) Sindre Mehus
 */
package net.sourceforge.subsonic.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;

import net.sourceforge.subsonic.Logger;
import net.sourceforge.subsonic.dao.AvatarDao;
import net.sourceforge.subsonic.dao.InternetRadioDao;
import net.sourceforge.subsonic.dao.MusicFolderDao;
import net.sourceforge.subsonic.dao.UserDao;
import net.sourceforge.subsonic.domain.AlbumListType;
import net.sourceforge.subsonic.domain.Avatar;
import net.sourceforge.subsonic.domain.InternetRadio;
import net.sourceforge.subsonic.domain.LicenseInfo;
import net.sourceforge.subsonic.domain.MediaLibraryStatistics;
import net.sourceforge.subsonic.domain.MusicFolder;
import net.sourceforge.subsonic.domain.Theme;
import net.sourceforge.subsonic.domain.UrlRedirectType;
import net.sourceforge.subsonic.domain.UserSettings;
import net.sourceforge.subsonic.util.FileUtil;
import net.sourceforge.subsonic.util.StringUtil;
import net.sourceforge.subsonic.util.Util;

/**
 * Provides persistent storage of application settings and preferences.
 *
 * @author Sindre Mehus
 */
public class SettingsService {

    // Subsonic home directory.
    private static final File SUBSONIC_HOME_WINDOWS = new File("c:/subsonic");
    private static final File SUBSONIC_HOME_OTHER = new File("/var/subsonic");

    // Number of free trial days.
    public static final long TRIAL_DAYS = 30L;

    // Global settings.
    private static final String KEY_INDEX_STRING = "IndexString";
    private static final String KEY_IGNORED_ARTICLES = "IgnoredArticles";
    private static final String KEY_SHORTCUTS = "Shortcuts";
    private static final String KEY_PLAYLIST_FOLDER = "PlaylistFolder";
    private static final String KEY_MUSIC_FILE_TYPES = "MusicFileTypes";
    private static final String KEY_VIDEO_FILE_TYPES = "VideoFileTypes";
    private static final String KEY_COVER_ART_FILE_TYPES = "CoverArtFileTypes2";
    private static final String KEY_COVER_ART_CONCURRENCY = "CoverArtConcurrency";
    private static final String KEY_WELCOME_TITLE = "WelcomeTitle";
    private static final String KEY_WELCOME_SUBTITLE = "WelcomeSubtitle";
    private static final String KEY_WELCOME_MESSAGE = "WelcomeMessage2";
    private static final String KEY_LOGIN_MESSAGE = "LoginMessage";
    private static final String KEY_LOCALE_LANGUAGE = "LocaleLanguage";
    private static final String KEY_LOCALE_COUNTRY = "LocaleCountry";
    private static final String KEY_LOCALE_VARIANT = "LocaleVariant";
    private static final String KEY_THEME_ID = "Theme";
    private static final String KEY_INDEX_CREATION_INTERVAL = "IndexCreationInterval";
    private static final String KEY_INDEX_CREATION_HOUR = "IndexCreationHour";
    private static final String KEY_FAST_CACHE_ENABLED = "FastCacheEnabled";
    private static final String KEY_PODCAST_UPDATE_INTERVAL = "PodcastUpdateInterval";
    private static final String KEY_PODCAST_FOLDER = "PodcastFolder";
    private static final String KEY_PODCAST_EPISODE_RETENTION_COUNT = "PodcastEpisodeRetentionCount";
    private static final String KEY_PODCAST_EPISODE_DOWNLOAD_COUNT = "PodcastEpisodeDownloadCount";
    private static final String KEY_DOWNLOAD_BITRATE_LIMIT = "DownloadBitrateLimit";
    private static final String KEY_UPLOAD_BITRATE_LIMIT = "UploadBitrateLimit";
    private static final String KEY_LICENSE_EMAIL = "LicenseEmail";
    private static final String KEY_LICENSE_CODE = "LicenseCode";
    private static final String KEY_LICENSE_DATE = "LicenseDate";
    private static final String KEY_DOWNSAMPLING_COMMAND = "DownsamplingCommand4";
    private static final String KEY_HLS_COMMAND = "HlsCommand3";
    private static final String KEY_JUKEBOX_COMMAND = "JukeboxCommand2";
    private static final String KEY_VIDEO_IMAGE_COMMAND = "VideoImageCommand";
    private static final String KEY_REWRITE_URL = "RewriteUrl";
    private static final String KEY_LDAP_ENABLED = "LdapEnabled";
    private static final String KEY_LDAP_URL = "LdapUrl";
    private static final String KEY_LDAP_MANAGER_DN = "LdapManagerDn";
    private static final String KEY_LDAP_MANAGER_PASSWORD = "LdapManagerPassword";
    private static final String KEY_LDAP_SEARCH_FILTER = "LdapSearchFilter";
    private static final String KEY_LDAP_AUTO_SHADOWING = "LdapAutoShadowing";
    private static final String KEY_GETTING_STARTED_ENABLED = "GettingStartedEnabled";
    private static final String KEY_PORT_FORWARDING_ENABLED = "PortForwardingEnabled";
    private static final String KEY_PORT = "Port";
    private static final String KEY_HTTPS_PORT = "HttpsPort";
    private static final String KEY_URL_REDIRECTION_ENABLED = "UrlRedirectionEnabled";
    private static final String KEY_URL_REDIRECT_TYPE = "UrlRedirectType";
    private static final String KEY_URL_REDIRECT_FROM = "UrlRedirectFrom";
    private static final String KEY_URL_REDIRECT_CONTEXT_PATH = "UrlRedirectContextPath";
    private static final String KEY_URL_REDIRECT_CUSTOM_URL = "UrlRedirectCustomUrl";
    private static final String KEY_SERVER_ID = "ServerId";
    private static final String KEY_SETTINGS_CHANGED = "SettingsChanged";
    private static final String KEY_LAST_SCANNED = "LastScanned";
    private static final String KEY_ORGANIZE_BY_FOLDER_STRUCTURE = "OrganizeByFolderStructure";
    private static final String KEY_SORT_ALBUMS_BY_YEAR = "SortAlbumsByYear";
    private static final String KEY_MEDIA_LIBRARY_STATISTICS = "MediaLibraryStatistics";
    private static final String KEY_TRIAL_EXPIRES = "TrialExpires";
    private static final String KEY_DLNA_ENABLED = "DlnaEnabled";
    private static final String KEY_DLNA_SERVER_NAME = "DlnaServerName";
    private static final String KEY_SONOS_ENABLED = "SonosEnabled";
    private static final String KEY_SONOS_SERVICE_NAME = "SonosServiceName";
    private static final String KEY_SONOS_SERVICE_ID = "SonosServiceId";

    // Default values.
    private static final String DEFAULT_INDEX_STRING = "A B C D E F G H I J K L M N O P Q R S T U V W X-Z(XYZ)";
    private static final String DEFAULT_IGNORED_ARTICLES = "The El La Los Las Le Les";
    private static final String DEFAULT_SHORTCUTS = "New Incoming Podcast";
    private static final String DEFAULT_PLAYLIST_FOLDER = Util.getDefaultPlaylistFolder();
    private static final String DEFAULT_MUSIC_FILE_TYPES = "mp3 ogg oga aac m4a flac wav wma aif aiff ape mpc shn";
    private static final String DEFAULT_VIDEO_FILE_TYPES = "flv avi mpg mpeg mp4 m4v mkv mov wmv ogv divx m2ts";
    private static final String DEFAULT_COVER_ART_FILE_TYPES = "cover.jpg cover.png cover.gif folder.jpg jpg jpeg gif png";
    private static final int DEFAULT_COVER_ART_CONCURRENCY = 4;
    private static final String DEFAULT_WELCOME_TITLE = "Welcome to Subsonic!";
    private static final String DEFAULT_WELCOME_SUBTITLE = null;
    private static final String DEFAULT_WELCOME_MESSAGE = "__Welcome to Subsonic!__\n" +
            "\\\\ \\\\\n" +
            "Subsonic is a free, web-based media streamer, providing ubiquitous access to your music. \n" +
            "\\\\ \\\\\n" +
            "Use it to share your music with friends, or to listen to your own music while at work. You can stream to multiple " +
            "players simultaneously, for instance to one player in your kitchen and another in your living room.\n" +
            "\\\\ \\\\\n" +
            "To change or remove this message, log in with administrator rights and go to {link:Settings > General|generalSettings.view}.";
    private static final String DEFAULT_LOGIN_MESSAGE = null;
    private static final String DEFAULT_LOCALE_LANGUAGE = "en";
    private static final String DEFAULT_LOCALE_COUNTRY = "";
    private static final String DEFAULT_LOCALE_VARIANT = "";
    private static final String DEFAULT_THEME_ID = "default";
    private static final int DEFAULT_INDEX_CREATION_INTERVAL = 1;
    private static final int DEFAULT_INDEX_CREATION_HOUR = 3;
    private static final boolean DEFAULT_FAST_CACHE_ENABLED = false;
    private static final int DEFAULT_PODCAST_UPDATE_INTERVAL = 24;
    private static final String DEFAULT_PODCAST_FOLDER = Util.getDefaultPodcastFolder();
    private static final int DEFAULT_PODCAST_EPISODE_RETENTION_COUNT = 10;
    private static final int DEFAULT_PODCAST_EPISODE_DOWNLOAD_COUNT = 1;
    private static final long DEFAULT_DOWNLOAD_BITRATE_LIMIT = 0;
    private static final long DEFAULT_UPLOAD_BITRATE_LIMIT = 0;
    private static final String DEFAULT_LICENSE_EMAIL = null;
    private static final String DEFAULT_LICENSE_CODE = null;
    private static final String DEFAULT_LICENSE_DATE = null;
    private static final String DEFAULT_DOWNSAMPLING_COMMAND = "ffmpeg -i %s -map 0:0 -b:a %bk -v 0 -f mp3 -";
    private static final String DEFAULT_HLS_COMMAND = "ffmpeg -ss %o -t %d -i %s -async 1 -b:v %bk -s %wx%h -ar 44100 -ac 2 -v 0 -f mpegts -c:v libx264 -preset superfast -c:a libmp3lame -threads 0 -";
    private static final String DEFAULT_JUKEBOX_COMMAND = "ffmpeg -ss %o -i %s -map 0:0 -v 0 -ar 44100 -ac 2 -f s16be -";
    private static final String DEFAULT_VIDEO_IMAGE_COMMAND = "ffmpeg -r 1 -ss %o -t 1 -i %s -s %wx%h -v 0 -f mjpeg -";
    private static final boolean DEFAULT_REWRITE_URL = true;
    private static final boolean DEFAULT_LDAP_ENABLED = false;
    private static final String DEFAULT_LDAP_URL = "ldap://host.domain.com:389/cn=Users,dc=domain,dc=com";
    private static final String DEFAULT_LDAP_MANAGER_DN = null;
    private static final String DEFAULT_LDAP_MANAGER_PASSWORD = null;
    private static final String DEFAULT_LDAP_SEARCH_FILTER = "(sAMAccountName={0})";
    private static final boolean DEFAULT_LDAP_AUTO_SHADOWING = false;
    private static final boolean DEFAULT_PORT_FORWARDING_ENABLED = false;
    private static final boolean DEFAULT_GETTING_STARTED_ENABLED = true;
    private static final int DEFAULT_PORT = 80;
    private static final int DEFAULT_HTTPS_PORT = 0;
    private static final boolean DEFAULT_URL_REDIRECTION_ENABLED = false;
    private static final UrlRedirectType DEFAULT_URL_REDIRECT_TYPE = UrlRedirectType.NORMAL;
    private static final String DEFAULT_URL_REDIRECT_FROM = "yourname";
    private static final String DEFAULT_URL_REDIRECT_CONTEXT_PATH = System.getProperty("subsonic.contextPath", "").replaceAll("/", "");
    private static final String DEFAULT_URL_REDIRECT_CUSTOM_URL = "http://";
    private static final String DEFAULT_SERVER_ID = null;
    private static final long DEFAULT_SETTINGS_CHANGED = 0L;
    private static final boolean DEFAULT_ORGANIZE_BY_FOLDER_STRUCTURE = true;
    private static final boolean DEFAULT_SORT_ALBUMS_BY_YEAR = true;
    private static final String DEFAULT_MEDIA_LIBRARY_STATISTICS = "0 0 0 0 0";
    private static final String DEFAULT_TRIAL_EXPIRES = null;
    private static final boolean DEFAULT_DLNA_ENABLED = false;
    private static final String DEFAULT_DLNA_SERVER_NAME = "Subsonic";
    private static final boolean DEFAULT_SONOS_ENABLED = false;
    private static final String DEFAULT_SONOS_SERVICE_NAME = "Subsonic";
    private static final int DEFAULT_SONOS_SERVICE_ID = 242;

    // Array of obsolete keys.  Used to clean property file.
    private static final List<String> OBSOLETE_KEYS = Arrays.asList("PortForwardingPublicPort", "PortForwardingLocalPort",
            "DownsamplingCommand", "DownsamplingCommand2", "DownsamplingCommand3", "AutoCoverBatch", "MusicMask",
            "VideoMask", "CoverArtMask, HlsCommand", "HlsCommand2", "JukeboxCommand", "UrlRedirectTrialExpires", "VideoTrialExpires",
            "CoverArtFileTypes", "UrlRedirectCustomHost", "CoverArtLimit", "StreamPort");

    private static final String LOCALES_FILE = "/net/sourceforge/subsonic/i18n/locales.txt";
    private static final String THEMES_FILE = "/net/sourceforge/subsonic/theme/themes.txt";

    private static final Logger LOG = Logger.getLogger(SettingsService.class);

    private Properties properties = new Properties();
    private List<Theme> themes;
    private List<Locale> locales;
    private InternetRadioDao internetRadioDao;
    private MusicFolderDao musicFolderDao;
    private UserDao userDao;
    private AvatarDao avatarDao;
    private VersionService versionService;

    private String[] cachedCoverArtFileTypesArray;
    private String[] cachedMusicFileTypesArray;
    private String[] cachedVideoFileTypesArray;
    private List<MusicFolder> cachedMusicFolders;
    private final ConcurrentMap<String, List<MusicFolder>> cachedMusicFoldersPerUser = new ConcurrentHashMap<String, List<MusicFolder>>();

    private static File subsonicHome;

    private boolean licenseValidated = true;
    private Date licenseExpires;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> licenseValidationFuture;

    private static final long LICENSE_VALIDATION_DELAY_HOURS = 12;
    private static final long LOCAL_IP_LOOKUP_DELAY_SECONDS = 60;
    private String localIpAddress;

    public SettingsService() {
        File propertyFile = getPropertyFile();

        if (propertyFile.exists()) {
            FileInputStream in = null;
            try {
                in = new FileInputStream(propertyFile);
                properties.load(in);
            } catch (Exception x) {
                LOG.error("Unable to read from property file.", x);
            } finally {
                IOUtils.closeQuietly(in);
            }

            // Remove obsolete properties.
            for (Iterator<Object> iterator = properties.keySet().iterator(); iterator.hasNext();) {
                String key = (String) iterator.next();
                if (OBSOLETE_KEYS.contains(key)) {
                    LOG.info("Removing obsolete property [" + key + ']');
                    iterator.remove();
                }
            }
        }

        // Start trial.
        if (getTrialExpires() == null) {
            Date expiryDate = new Date(System.currentTimeMillis() + TRIAL_DAYS * 24L * 3600L * 1000L);
            setTrialExpires(expiryDate);
        }

        save(false);
    }

    /**
     * Register in service locator so that non-Spring objects can access me.
     * This method is invoked automatically by Spring.
     */
    public void init() {
        logServerInfo();
        ServiceLocator.setSettingsService(this);
        scheduleLocalIpAddressLookup();
        scheduleLicenseValidation();
    }

    private void logServerInfo() {
        LOG.info("Java: " + System.getProperty("java.version") +
                 ", OS: " + System.getProperty("os.name"));
    }

    public void save() {
        save(true);
    }

    public void save(boolean updateChangedDate) {
        if (updateChangedDate) {
            setProperty(KEY_SETTINGS_CHANGED, String.valueOf(System.currentTimeMillis()));
        }

        OutputStream out = null;
        try {
            out = new FileOutputStream(getPropertyFile());
            properties.store(out, "Subsonic preferences.  NOTE: This file is automatically generated.");
        } catch (Exception x) {
            LOG.error("Unable to write to property file.", x);
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

    private File getPropertyFile() {
        return new File(getSubsonicHome(), "subsonic.properties");
    }

    /**
     * Returns the Subsonic home directory.
     *
     * @return The Subsonic home directory, if it exists.
     * @throws RuntimeException If directory doesn't exist.
     */
    public static synchronized File getSubsonicHome() {

        if (subsonicHome != null) {
            return subsonicHome;
        }

        File home;

        String overrideHome = System.getProperty("subsonic.home");
        if (overrideHome != null) {
            home = new File(overrideHome);
        } else {
            boolean isWindows = System.getProperty("os.name", "Windows").toLowerCase().startsWith("windows");
            home = isWindows ? SUBSONIC_HOME_WINDOWS : SUBSONIC_HOME_OTHER;
        }

        // Attempt to create home directory if it doesn't exist.
        if (!home.exists() || !home.isDirectory()) {
            boolean success = home.mkdirs();
            if (success) {
                subsonicHome = home;
            } else {
                String message = "The directory " + home + " does not exist. Please create it and make it writable. " +
                        "(You can override the directory location by specifying -Dsubsonic.home=... when " +
                        "starting the servlet container.)";
                System.err.println("ERROR: " + message);
            }
        } else {
            subsonicHome = home;
        }

        return home;
    }

    private int getInt(String key, int defaultValue) {
        return Integer.valueOf(properties.getProperty(key, String.valueOf(defaultValue)));
    }

    private void setInt(String key, int value) {
        setProperty(key, String.valueOf(value));
    }

    private long getLong(String key, long defaultValue) {
        return Long.valueOf(properties.getProperty(key, String.valueOf(defaultValue)));
    }

    private void setLong(String key, long value) {
        setProperty(key, String.valueOf(value));
    }

    private boolean getBoolean(String key, boolean defaultValue) {
        return Boolean.valueOf(properties.getProperty(key, String.valueOf(defaultValue)));
    }

    private void setBoolean(String key, boolean value) {
        setProperty(key, String.valueOf(value));
    }

    private String getString(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    private void setString(String key, String value) {
        setProperty(key, value);
    }

    public String getIndexString() {
        return properties.getProperty(KEY_INDEX_STRING, DEFAULT_INDEX_STRING);
    }

    public void setIndexString(String indexString) {
        setProperty(KEY_INDEX_STRING, indexString);
    }

    public String getIgnoredArticles() {
        return properties.getProperty(KEY_IGNORED_ARTICLES, DEFAULT_IGNORED_ARTICLES);
    }

    public String[] getIgnoredArticlesAsArray() {
        return getIgnoredArticles().split("\\s+");
    }

    public void setIgnoredArticles(String ignoredArticles) {
        setProperty(KEY_IGNORED_ARTICLES, ignoredArticles);
    }

    public String getShortcuts() {
        return properties.getProperty(KEY_SHORTCUTS, DEFAULT_SHORTCUTS);
    }

    public String[] getShortcutsAsArray() {
        return StringUtil.split(getShortcuts());
    }

    public void setShortcuts(String shortcuts) {
        setProperty(KEY_SHORTCUTS, shortcuts);
    }

    public String getPlaylistFolder() {
        return properties.getProperty(KEY_PLAYLIST_FOLDER, DEFAULT_PLAYLIST_FOLDER);
    }

    public void setPlaylistFolder(String playlistFolder) {
        setProperty(KEY_PLAYLIST_FOLDER, playlistFolder);
    }

    public String getMusicFileTypes() {
        return properties.getProperty(KEY_MUSIC_FILE_TYPES, DEFAULT_MUSIC_FILE_TYPES);
    }

    public synchronized void setMusicFileTypes(String fileTypes) {
        setProperty(KEY_MUSIC_FILE_TYPES, fileTypes);
        cachedMusicFileTypesArray = null;
    }

    public synchronized String[] getMusicFileTypesAsArray() {
        if (cachedMusicFileTypesArray == null) {
            cachedMusicFileTypesArray = toStringArray(getMusicFileTypes());
        }
        return cachedMusicFileTypesArray;
    }

    public String getVideoFileTypes() {
        return properties.getProperty(KEY_VIDEO_FILE_TYPES, DEFAULT_VIDEO_FILE_TYPES);
    }

    public synchronized void setVideoFileTypes(String fileTypes) {
        setProperty(KEY_VIDEO_FILE_TYPES, fileTypes);
        cachedVideoFileTypesArray = null;
    }

    public synchronized String[] getVideoFileTypesAsArray() {
        if (cachedVideoFileTypesArray == null) {
            cachedVideoFileTypesArray = toStringArray(getVideoFileTypes());
        }
        return cachedVideoFileTypesArray;
    }

    public String getCoverArtFileTypes() {
        return properties.getProperty(KEY_COVER_ART_FILE_TYPES, DEFAULT_COVER_ART_FILE_TYPES);
    }

    public synchronized void setCoverArtFileTypes(String fileTypes) {
        setProperty(KEY_COVER_ART_FILE_TYPES, fileTypes);
        cachedCoverArtFileTypesArray = null;
    }

    public synchronized String[] getCoverArtFileTypesAsArray() {
        if (cachedCoverArtFileTypesArray == null) {
            cachedCoverArtFileTypesArray = toStringArray(getCoverArtFileTypes());
        }
        return cachedCoverArtFileTypesArray;
    }

    public int getCoverArtConcurrency() {
        return getInt(KEY_COVER_ART_CONCURRENCY, DEFAULT_COVER_ART_CONCURRENCY);
    }

    public String getWelcomeTitle() {
        return StringUtils.trimToNull(properties.getProperty(KEY_WELCOME_TITLE, DEFAULT_WELCOME_TITLE));
    }

    public void setWelcomeTitle(String title) {
        setProperty(KEY_WELCOME_TITLE, title);
    }

    public String getWelcomeSubtitle() {
        return StringUtils.trimToNull(properties.getProperty(KEY_WELCOME_SUBTITLE, DEFAULT_WELCOME_SUBTITLE));
    }

    public void setWelcomeSubtitle(String subtitle) {
        setProperty(KEY_WELCOME_SUBTITLE, subtitle);
    }

    public String getWelcomeMessage() {
        return StringUtils.trimToNull(properties.getProperty(KEY_WELCOME_MESSAGE, DEFAULT_WELCOME_MESSAGE));
    }

    public void setWelcomeMessage(String message) {
        setProperty(KEY_WELCOME_MESSAGE, message);
    }

    public String getLoginMessage() {
        return StringUtils.trimToNull(properties.getProperty(KEY_LOGIN_MESSAGE, DEFAULT_LOGIN_MESSAGE));
    }

    public void setLoginMessage(String message) {
        setProperty(KEY_LOGIN_MESSAGE, message);
    }

    /**
     * Returns the number of days between automatic index creation, of -1 if automatic index
     * creation is disabled.
     */
    public int getIndexCreationInterval() {
        return getInt(KEY_INDEX_CREATION_INTERVAL, DEFAULT_INDEX_CREATION_INTERVAL);
    }

    /**
     * Sets the number of days between automatic index creation, of -1 if automatic index
     * creation is disabled.
     */
    public void setIndexCreationInterval(int days) {
        setInt(KEY_INDEX_CREATION_INTERVAL, days);
    }

    /**
     * Returns the hour of day (0 - 23) when automatic index creation should run.
     */
    public int getIndexCreationHour() {
        return getInt(KEY_INDEX_CREATION_HOUR, DEFAULT_INDEX_CREATION_HOUR);
    }

    /**
     * Sets the hour of day (0 - 23) when automatic index creation should run.
     */
    public void setIndexCreationHour(int hour) {
        setInt(KEY_INDEX_CREATION_HOUR, hour);
    }

    public boolean isFastCacheEnabled() {
        return getBoolean(KEY_FAST_CACHE_ENABLED, DEFAULT_FAST_CACHE_ENABLED);
    }

    public void setFastCacheEnabled(boolean enabled) {
        setBoolean(KEY_FAST_CACHE_ENABLED, enabled);
    }

    /**
     * Returns the number of hours between Podcast updates, of -1 if automatic updates
     * are disabled.
     */
    public int getPodcastUpdateInterval() {
        return getInt(KEY_PODCAST_UPDATE_INTERVAL, DEFAULT_PODCAST_UPDATE_INTERVAL);
    }

    /**
     * Sets the number of hours between Podcast updates, of -1 if automatic updates
     * are disabled.
     */
    public void setPodcastUpdateInterval(int hours) {
        setInt(KEY_PODCAST_UPDATE_INTERVAL, hours);
    }

    /**
     * Returns the number of Podcast episodes to keep (-1 to keep all).
     */
    public int getPodcastEpisodeRetentionCount() {
        return getInt(KEY_PODCAST_EPISODE_RETENTION_COUNT, DEFAULT_PODCAST_EPISODE_RETENTION_COUNT);
    }

    /**
     * Sets the number of Podcast episodes to keep (-1 to keep all).
     */
    public void setPodcastEpisodeRetentionCount(int count) {
        setInt(KEY_PODCAST_EPISODE_RETENTION_COUNT, count);
    }

    /**
     * Returns the number of Podcast episodes to download (-1 to download all).
     */
    public int getPodcastEpisodeDownloadCount() {
        return getInt(KEY_PODCAST_EPISODE_DOWNLOAD_COUNT, DEFAULT_PODCAST_EPISODE_DOWNLOAD_COUNT);
    }

    /**
     * Sets the number of Podcast episodes to download (-1 to download all).
     */
    public void setPodcastEpisodeDownloadCount(int count) {
        setInt(KEY_PODCAST_EPISODE_DOWNLOAD_COUNT, count);
    }

    /**
     * Returns the Podcast download folder.
     */
    public String getPodcastFolder() {
        return properties.getProperty(KEY_PODCAST_FOLDER, DEFAULT_PODCAST_FOLDER);
    }

    /**
     * Sets the Podcast download folder.
     */
    public void setPodcastFolder(String folder) {
        setProperty(KEY_PODCAST_FOLDER, folder);
    }

    /**
     * @return The download bitrate limit in Kbit/s. Zero if unlimited.
     */
    public long getDownloadBitrateLimit() {
        return Long.parseLong(properties.getProperty(KEY_DOWNLOAD_BITRATE_LIMIT, "" + DEFAULT_DOWNLOAD_BITRATE_LIMIT));
    }

    /**
     * @param limit The download bitrate limit in Kbit/s. Zero if unlimited.
     */
    public void setDownloadBitrateLimit(long limit) {
        setProperty(KEY_DOWNLOAD_BITRATE_LIMIT, "" + limit);
    }

    /**
     * @return The upload bitrate limit in Kbit/s. Zero if unlimited.
     */
    public long getUploadBitrateLimit() {
        return getLong(KEY_UPLOAD_BITRATE_LIMIT, DEFAULT_UPLOAD_BITRATE_LIMIT);
    }

    /**
     * @param limit The upload bitrate limit in Kbit/s. Zero if unlimited.
     */
    public void setUploadBitrateLimit(long limit) {
        setLong(KEY_UPLOAD_BITRATE_LIMIT, limit);
    }

    public String getLicenseEmail() {
        return properties.getProperty(KEY_LICENSE_EMAIL, DEFAULT_LICENSE_EMAIL);
    }

    public void setLicenseEmail(String email) {
        setProperty(KEY_LICENSE_EMAIL, email);
    }

    public String getLicenseCode() {
        return properties.getProperty(KEY_LICENSE_CODE, DEFAULT_LICENSE_CODE);
    }

    public void setLicenseCode(String code) {
        setProperty(KEY_LICENSE_CODE, code);
    }

    public Date getLicenseDate() {
        String value = properties.getProperty(KEY_LICENSE_DATE, DEFAULT_LICENSE_DATE);
        return value == null ? null : new Date(Long.parseLong(value));
    }

    public void setLicenseDate(Date date) {
        String value = (date == null ? null : String.valueOf(date.getTime()));
        setProperty(KEY_LICENSE_DATE, value);
    }

    public boolean isLicenseValid() {
        return isLicenseValid(getLicenseEmail(), getLicenseCode()) && licenseValidated;
    }

    public boolean isLicenseValid(String email, String license) {
        if (email == null || license == null) {
            return false;
        }
        return license.equalsIgnoreCase(StringUtil.md5Hex(email.toLowerCase()));
    }

    public LicenseInfo getLicenseInfo() {
        Date trialExpires = getTrialExpires();
        Date now = new Date();
        boolean trialValid = trialExpires.after(now);
        long trialDaysLeft = trialValid ? (trialExpires.getTime() - now.getTime()) / (24L * 3600L * 1000L) : 0L;

        return new LicenseInfo(getLicenseEmail(), isLicenseValid(), trialExpires, trialDaysLeft, licenseExpires);
    }

    public String getDownsamplingCommand() {
        return properties.getProperty(KEY_DOWNSAMPLING_COMMAND, DEFAULT_DOWNSAMPLING_COMMAND);
    }

    public void setDownsamplingCommand(String command) {
        setProperty(KEY_DOWNSAMPLING_COMMAND, command);
    }

    public String getHlsCommand() {
        return properties.getProperty(KEY_HLS_COMMAND, DEFAULT_HLS_COMMAND);
    }

    public void setHlsCommand(String command) {
        setProperty(KEY_HLS_COMMAND, command);
    }

    public String getJukeboxCommand() {
        return properties.getProperty(KEY_JUKEBOX_COMMAND, DEFAULT_JUKEBOX_COMMAND);
    }
    public String getVideoImageCommand() {
        return properties.getProperty(KEY_VIDEO_IMAGE_COMMAND, DEFAULT_VIDEO_IMAGE_COMMAND);
    }

    public boolean isRewriteUrlEnabled() {
        return getBoolean(KEY_REWRITE_URL, DEFAULT_REWRITE_URL);
    }

    public void setRewriteUrlEnabled(boolean rewriteUrl) {
        setBoolean(KEY_REWRITE_URL, rewriteUrl);
    }

    public boolean isLdapEnabled() {
        return getBoolean(KEY_LDAP_ENABLED, DEFAULT_LDAP_ENABLED);
    }

    public void setLdapEnabled(boolean ldapEnabled) {
        setBoolean(KEY_LDAP_ENABLED, ldapEnabled);
    }

    public String getLdapUrl() {
        return properties.getProperty(KEY_LDAP_URL, DEFAULT_LDAP_URL);
    }

    public void setLdapUrl(String ldapUrl) {
        properties.setProperty(KEY_LDAP_URL, ldapUrl);
    }

    public String getLdapSearchFilter() {
        return properties.getProperty(KEY_LDAP_SEARCH_FILTER, DEFAULT_LDAP_SEARCH_FILTER);
    }

    public void setLdapSearchFilter(String ldapSearchFilter) {
        properties.setProperty(KEY_LDAP_SEARCH_FILTER, ldapSearchFilter);
    }

    public String getLdapManagerDn() {
        return properties.getProperty(KEY_LDAP_MANAGER_DN, DEFAULT_LDAP_MANAGER_DN);
    }

    public void setLdapManagerDn(String ldapManagerDn) {
        properties.setProperty(KEY_LDAP_MANAGER_DN, ldapManagerDn);
    }

    public String getLdapManagerPassword() {
        String s = properties.getProperty(KEY_LDAP_MANAGER_PASSWORD, DEFAULT_LDAP_MANAGER_PASSWORD);
        try {
            return StringUtil.utf8HexDecode(s);
        } catch (Exception x) {
            LOG.warn("Failed to decode LDAP manager password.", x);
            return s;
        }
    }

    public void setLdapManagerPassword(String ldapManagerPassword) {
        try {
            ldapManagerPassword = StringUtil.utf8HexEncode(ldapManagerPassword);
        } catch (Exception x) {
            LOG.warn("Failed to encode LDAP manager password.", x);
        }
        properties.setProperty(KEY_LDAP_MANAGER_PASSWORD, ldapManagerPassword);
    }

    public boolean isLdapAutoShadowing() {
        return getBoolean(KEY_LDAP_AUTO_SHADOWING, DEFAULT_LDAP_AUTO_SHADOWING);
    }

    public void setLdapAutoShadowing(boolean ldapAutoShadowing) {
        setBoolean(KEY_LDAP_AUTO_SHADOWING, ldapAutoShadowing);
    }

    public boolean isGettingStartedEnabled() {
        return getBoolean(KEY_GETTING_STARTED_ENABLED, DEFAULT_GETTING_STARTED_ENABLED);
    }

    public void setGettingStartedEnabled(boolean isGettingStartedEnabled) {
        setBoolean(KEY_GETTING_STARTED_ENABLED, isGettingStartedEnabled);
    }

    public boolean isPortForwardingEnabled() {
        return getBoolean(KEY_PORT_FORWARDING_ENABLED, DEFAULT_PORT_FORWARDING_ENABLED);
    }

    public void setPortForwardingEnabled(boolean isPortForwardingEnabled) {
        setBoolean(KEY_PORT_FORWARDING_ENABLED, isPortForwardingEnabled);
    }

    public int getPort() {
        return getInt(KEY_PORT, DEFAULT_PORT);
    }

    public void setPort(int port) {
        setInt(KEY_PORT, port);
    }

    public int getHttpsPort() {
        return getInt(KEY_HTTPS_PORT, DEFAULT_HTTPS_PORT);
    }

    public void setHttpsPort(int httpsPort) {
        setInt(KEY_HTTPS_PORT, httpsPort);
    }

    public boolean isUrlRedirectionEnabled() {
        return getBoolean(KEY_URL_REDIRECTION_ENABLED, DEFAULT_URL_REDIRECTION_ENABLED);
    }

    public void setUrlRedirectionEnabled(boolean isUrlRedirectionEnabled) {
        setBoolean(KEY_URL_REDIRECTION_ENABLED, isUrlRedirectionEnabled);
    }

    public String getUrlRedirectUrl() {
        if (getUrlRedirectType() == UrlRedirectType.NORMAL) {
            return "http://" + getUrlRedirectFrom() + ".subsonic.org";
        }
        return StringUtils.removeEnd(getUrlRedirectCustomUrl(), "/");
    }

    public String getUrlRedirectFrom() {
        return properties.getProperty(KEY_URL_REDIRECT_FROM, DEFAULT_URL_REDIRECT_FROM);
    }

    public void setUrlRedirectFrom(String urlRedirectFrom) {
        properties.setProperty(KEY_URL_REDIRECT_FROM, urlRedirectFrom);
    }

    public UrlRedirectType getUrlRedirectType() {
        return UrlRedirectType.valueOf(properties.getProperty(KEY_URL_REDIRECT_TYPE, DEFAULT_URL_REDIRECT_TYPE.name()));
    }

    public void setUrlRedirectType(UrlRedirectType urlRedirectType) {
        properties.setProperty(KEY_URL_REDIRECT_TYPE, urlRedirectType.name());
    }

    public Date getTrialExpires() {
        String value = properties.getProperty(KEY_TRIAL_EXPIRES, DEFAULT_TRIAL_EXPIRES);
        return value == null ? null : new Date(Long.parseLong(value));
    }

    private void setTrialExpires(Date date) {
        String value = (date == null ? null : String.valueOf(date.getTime()));
        setProperty(KEY_TRIAL_EXPIRES, value);
    }

    public String getUrlRedirectContextPath() {
        return properties.getProperty(KEY_URL_REDIRECT_CONTEXT_PATH, DEFAULT_URL_REDIRECT_CONTEXT_PATH);
    }

    public void setUrlRedirectContextPath(String contextPath) {
        properties.setProperty(KEY_URL_REDIRECT_CONTEXT_PATH, contextPath);
    }

    public String getUrlRedirectCustomUrl() {
        return StringUtils.trimToNull(properties.getProperty(KEY_URL_REDIRECT_CUSTOM_URL, DEFAULT_URL_REDIRECT_CUSTOM_URL));
    }

    public void setUrlRedirectCustomUrl(String customUrl) {
        properties.setProperty(KEY_URL_REDIRECT_CUSTOM_URL, customUrl);
    }

    public String getServerId() {
        return properties.getProperty(KEY_SERVER_ID, DEFAULT_SERVER_ID);
    }

    public void setServerId(String serverId) {
        properties.setProperty(KEY_SERVER_ID, serverId);
    }

    public long getSettingsChanged() {
        return getLong(KEY_SETTINGS_CHANGED, DEFAULT_SETTINGS_CHANGED);
    }

    public Date getLastScanned() {
        String lastScanned = properties.getProperty(KEY_LAST_SCANNED);
        return lastScanned == null ? null : new Date(Long.parseLong(lastScanned));
    }

    public void setLastScanned(Date date) {
        if (date == null) {
            properties.remove(KEY_LAST_SCANNED);
        } else {
            setLong(KEY_LAST_SCANNED, date.getTime());
        }
    }

    public boolean isOrganizeByFolderStructure() {
        return getBoolean(KEY_ORGANIZE_BY_FOLDER_STRUCTURE, DEFAULT_ORGANIZE_BY_FOLDER_STRUCTURE);
    }

    public void setOrganizeByFolderStructure(boolean b) {
        setBoolean(KEY_ORGANIZE_BY_FOLDER_STRUCTURE, b);
    }

    public boolean isSortAlbumsByYear() {
        return getBoolean(KEY_SORT_ALBUMS_BY_YEAR, DEFAULT_SORT_ALBUMS_BY_YEAR);
    }

    public void setSortAlbumsByYear(boolean b) {
        setBoolean(KEY_SORT_ALBUMS_BY_YEAR, b);
    }

    public MediaLibraryStatistics getMediaLibraryStatistics() {
        return MediaLibraryStatistics.parse(getString(KEY_MEDIA_LIBRARY_STATISTICS, DEFAULT_MEDIA_LIBRARY_STATISTICS));
    }

    public void setMediaLibraryStatistics(MediaLibraryStatistics statistics) {
        setString(KEY_MEDIA_LIBRARY_STATISTICS, statistics.format());
    }

    /**
     * Returns the locale (for language, date format etc).
     *
     * @return The locale.
     */
    public Locale getLocale() {
        String language = properties.getProperty(KEY_LOCALE_LANGUAGE, DEFAULT_LOCALE_LANGUAGE);
        String country = properties.getProperty(KEY_LOCALE_COUNTRY, DEFAULT_LOCALE_COUNTRY);
        String variant = properties.getProperty(KEY_LOCALE_VARIANT, DEFAULT_LOCALE_VARIANT);

        return new Locale(language, country, variant);
    }

    /**
     * Sets the locale (for language, date format etc.)
     *
     * @param locale The locale.
     */
    public void setLocale(Locale locale) {
        setProperty(KEY_LOCALE_LANGUAGE, locale.getLanguage());
        setProperty(KEY_LOCALE_COUNTRY, locale.getCountry());
        setProperty(KEY_LOCALE_VARIANT, locale.getVariant());
    }

    /**
     * Returns the ID of the theme to use.
     *
     * @return The theme ID.
     */
    public String getThemeId() {
        return properties.getProperty(KEY_THEME_ID, DEFAULT_THEME_ID);
    }

    /**
     * Sets the ID of the theme to use.
     *
     * @param themeId The theme ID
     */
    public void setThemeId(String themeId) {
        setProperty(KEY_THEME_ID, themeId);
    }

    /**
     * Returns a list of available themes.
     *
     * @return A list of available themes.
     */
    public synchronized Theme[] getAvailableThemes() {
        if (themes == null) {
            themes = new ArrayList<Theme>();
            try {
                InputStream in = SettingsService.class.getResourceAsStream(THEMES_FILE);
                String[] lines = StringUtil.readLines(in);
                for (String line : lines) {
                    String[] elements = StringUtil.split(line);
                    if (elements.length == 2) {
                        themes.add(new Theme(elements[0], elements[1]));
                    } else if (elements.length == 3) {
                        themes.add(new Theme(elements[0], elements[1], elements[2]));
                    } else {
                        LOG.warn("Failed to parse theme from line: [" + line + "].");
                    }
                }
            } catch (IOException x) {
                LOG.error("Failed to resolve list of themes.", x);
                themes.add(new Theme("default", "Subsonic default"));
            }
        }
        return themes.toArray(new Theme[themes.size()]);
    }

    /**
     * Returns a list of available locales.
     *
     * @return A list of available locales.
     */
    public synchronized Locale[] getAvailableLocales() {
        if (locales == null) {
            locales = new ArrayList<Locale>();
            try {
                InputStream in = SettingsService.class.getResourceAsStream(LOCALES_FILE);
                String[] lines = StringUtil.readLines(in);

                for (String line : lines) {
                    locales.add(parseLocale(line));
                }

            } catch (IOException x) {
                LOG.error("Failed to resolve list of locales.", x);
                locales.add(Locale.ENGLISH);
            }
        }
        return locales.toArray(new Locale[locales.size()]);
    }

    private Locale parseLocale(String line) {
        String[] s = line.split("_");
        String language = s[0];
        String country = "";
        String variant = "";

        if (s.length > 1) {
            country = s[1];
        }
        if (s.length > 2) {
            variant = s[2];
        }
        return new Locale(language, country, variant);
    }

    /**
     * Returns the "brand" name. Normally, this is just "Subsonic".
     *
     * @return The brand name.
     */
    public String getBrand() {
        return "Subsonic";
    }

    /**
     * Returns all music folders. Non-existing and disabled folders are not included.
     *
     * @return Possibly empty list of all music folders.
     */
    public List<MusicFolder> getAllMusicFolders() {
        return getAllMusicFolders(false, false);
    }

    /**
     * Returns all music folders.
     *
     * @param includeDisabled Whether to include disabled folders.
     * @param includeNonExisting Whether to include non-existing folders.
     * @return Possibly empty list of all music folders.
     */
    public List<MusicFolder> getAllMusicFolders(boolean includeDisabled, boolean includeNonExisting) {
        if (cachedMusicFolders == null) {
            cachedMusicFolders = musicFolderDao.getAllMusicFolders();
        }
        
        List<MusicFolder> result = new ArrayList<MusicFolder>(cachedMusicFolders.size());
        for (MusicFolder folder : cachedMusicFolders) {
            if ((includeDisabled || folder.isEnabled()) && (includeNonExisting || FileUtil.exists(folder.getPath()))) {
                result.add(folder);
            }
        }
        return result;
    }

    /**
     * Returns all music folders a user have access to. Non-existing and disabled folders are not included.
     *
     * @return Possibly empty list of music folders.
     */
    public List<MusicFolder> getMusicFoldersForUser(String username) {
        List<MusicFolder> result = cachedMusicFoldersPerUser.get(username);
        if (result == null) {
            result = musicFolderDao.getMusicFoldersForUser(username);
            result.retainAll(getAllMusicFolders(false, false));
            cachedMusicFoldersPerUser.put(username, result);
        }
        return result;
    }

    /**
     * Returns all music folders a user have access to. Non-existing and disabled folders are not included.
     *
     * @param selectedMusicFolderId If non-null and included in the list of allowed music folders, this methods returns
     *                              a list of only this music folder.
     * @return Possibly empty list of music folders.
     */
    public List<MusicFolder> getMusicFoldersForUser(String username, Integer selectedMusicFolderId) {
        List<MusicFolder> allowed = getMusicFoldersForUser(username);
        if (selectedMusicFolderId == null) {
            return allowed;
        }
        MusicFolder selected = getMusicFolderById(selectedMusicFolderId);
        return allowed.contains(selected) ? Arrays.asList(selected) : Collections.<MusicFolder>emptyList();
    }

    /**
     * Returns the selected music folder for a given user, or {@code null} if all music folders should be displayed.
     */
    public MusicFolder getSelectedMusicFolder(String username) {
        UserSettings settings = getUserSettings(username);
        int musicFolderId = settings.getSelectedMusicFolderId();

        MusicFolder musicFolder = getMusicFolderById(musicFolderId);
        List<MusicFolder> allowedMusicFolders = getMusicFoldersForUser(username);
        return allowedMusicFolders.contains(musicFolder) ? musicFolder : null;
    }

    public void setMusicFoldersForUser(String username, List<Integer> musicFolderIds) {
        musicFolderDao.setMusicFoldersForUser(username, musicFolderIds);
        cachedMusicFoldersPerUser.remove(username);
    }

    /**
     * Returns the music folder with the given ID.
     *
     * @param id The ID.
     * @return The music folder with the given ID, or <code>null</code> if not found.
     */
    public MusicFolder getMusicFolderById(Integer id) {
        List<MusicFolder> all = getAllMusicFolders();
        for (MusicFolder folder : all) {
            if (id.equals(folder.getId())) {
                return folder;
            }
        }
        return null;
    }

    /**
     * Creates a new music folder.
     *
     * @param musicFolder The music folder to create.
     */
    public void createMusicFolder(MusicFolder musicFolder) {
        musicFolderDao.createMusicFolder(musicFolder);
        clearMusicFolderCache();
    }

    /**
     * Deletes the music folder with the given ID.
     *
     * @param id The ID of the music folder to delete.
     */
    public void deleteMusicFolder(Integer id) {
        musicFolderDao.deleteMusicFolder(id);
        clearMusicFolderCache();
    }

    /**
     * Updates the given music folder.
     *
     * @param musicFolder The music folder to update.
     */
    public void updateMusicFolder(MusicFolder musicFolder) {
        musicFolderDao.updateMusicFolder(musicFolder);
        clearMusicFolderCache();
    }

    public void clearMusicFolderCache() {
        cachedMusicFolders = null;
        cachedMusicFoldersPerUser.clear();
    }

    /**
     * Returns all internet radio stations. Disabled stations are not returned.
     *
     * @return Possibly empty list of all internet radio stations.
     */
    public List<InternetRadio> getAllInternetRadios() {
        return getAllInternetRadios(false);
    }

    /**
     * Returns the internet radio station with the given ID.
     *
     * @param id The ID.
     * @return The internet radio station with the given ID, or <code>null</code> if not found.
     */
    public InternetRadio getInternetRadioById(Integer id) {
        for (InternetRadio radio : getAllInternetRadios()) {
            if (id.equals(radio.getId())) {
                return radio;
            }
        }
        return null;
    }

    /**
     * Returns all internet radio stations.
     *
     * @param includeAll Whether disabled stations should be included.
     * @return Possibly empty list of all internet radio stations.
     */
    public List<InternetRadio> getAllInternetRadios(boolean includeAll) {
        List<InternetRadio> all = internetRadioDao.getAllInternetRadios();
        List<InternetRadio> result = new ArrayList<InternetRadio>(all.size());
        for (InternetRadio folder : all) {
            if (includeAll || folder.isEnabled()) {
                result.add(folder);
            }
        }
        return result;
    }

    /**
     * Creates a new internet radio station.
     *
     * @param radio The internet radio station to create.
     */
    public void createInternetRadio(InternetRadio radio) {
        internetRadioDao.createInternetRadio(radio);
    }

    /**
     * Deletes the internet radio station with the given ID.
     *
     * @param id The internet radio station ID.
     */
    public void deleteInternetRadio(Integer id) {
        internetRadioDao.deleteInternetRadio(id);
    }

    /**
     * Updates the given internet radio station.
     *
     * @param radio The internet radio station to update.
     */
    public void updateInternetRadio(InternetRadio radio) {
        internetRadioDao.updateInternetRadio(radio);
    }

    /**
     * Returns settings for the given user.
     *
     * @param username The username.
     * @return User-specific settings. Never <code>null</code>.
     */
    public UserSettings getUserSettings(String username) {
        UserSettings settings = userDao.getUserSettings(username);
        return settings == null ? createDefaultUserSettings(username) : settings;
    }

    private UserSettings createDefaultUserSettings(String username) {
        UserSettings settings = new UserSettings(username);
        settings.setFinalVersionNotificationEnabled(true);
        settings.setBetaVersionNotificationEnabled(false);
        settings.setSongNotificationEnabled(true);
        settings.setShowNowPlayingEnabled(true);
        settings.setShowChatEnabled(true);
        settings.setPartyModeEnabled(false);
        settings.setNowPlayingAllowed(true);
        settings.setAutoHidePlayQueue(true);
        settings.setShowSideBar(true);
        settings.setShowArtistInfoEnabled(true);
        settings.setViewAsList(false);
        settings.setQueueFollowingSongs(true);
        settings.setDefaultAlbumList(AlbumListType.RANDOM);
        settings.setLastFmEnabled(false);
        settings.setLastFmUsername(null);
        settings.setLastFmPassword(null);
        settings.setChanged(new Date());

        UserSettings.Visibility playlist = settings.getPlaylistVisibility();
        playlist.setArtistVisible(true);
        playlist.setAlbumVisible(true);
        playlist.setYearVisible(true);
        playlist.setDurationVisible(true);
        playlist.setBitRateVisible(true);
        playlist.setFormatVisible(true);
        playlist.setFileSizeVisible(true);

        UserSettings.Visibility main = settings.getMainVisibility();
        main.setTrackNumberVisible(true);
        main.setArtistVisible(true);
        main.setDurationVisible(true);

        return settings;
    }

    /**
     * Updates settings for the given username.
     *
     * @param settings The user-specific settings.
     */
    public void updateUserSettings(UserSettings settings) {
        userDao.updateUserSettings(settings);
    }

    /**
     * Returns all system avatars.
     *
     * @return All system avatars.
     */
    public List<Avatar> getAllSystemAvatars() {
        return avatarDao.getAllSystemAvatars();
    }

    /**
     * Returns the system avatar with the given ID.
     *
     * @param id The system avatar ID.
     * @return The avatar or <code>null</code> if not found.
     */
    public Avatar getSystemAvatar(int id) {
        return avatarDao.getSystemAvatar(id);
    }

    /**
     * Returns the custom avatar for the given user.
     *
     * @param username The username.
     * @return The avatar or <code>null</code> if not found.
     */
    public Avatar getCustomAvatar(String username) {
        return avatarDao.getCustomAvatar(username);
    }

    /**
     * Sets the custom avatar for the given user.
     *
     * @param avatar   The avatar, or <code>null</code> to remove the avatar.
     * @param username The username.
     */
    public void setCustomAvatar(Avatar avatar, String username) {
        avatarDao.setCustomAvatar(avatar, username);
    }

    public boolean isDlnaEnabled() {
        return getBoolean(KEY_DLNA_ENABLED, DEFAULT_DLNA_ENABLED);
    }

    public void setDlnaEnabled(boolean dlnaEnabled) {
        setBoolean(KEY_DLNA_ENABLED, dlnaEnabled);
    }

    public String getDlnaServerName() {
        return getString(KEY_DLNA_SERVER_NAME, DEFAULT_DLNA_SERVER_NAME);
    }

    public void setDlnaServerName(String dlnaServerName) {
        setString(KEY_DLNA_SERVER_NAME, dlnaServerName);
    }

    public boolean isSonosEnabled() {
        return getBoolean(KEY_SONOS_ENABLED, DEFAULT_SONOS_ENABLED);
    }

    public void setSonosEnabled(boolean sonosEnabled) {
        setBoolean(KEY_SONOS_ENABLED, sonosEnabled);
    }

    public String getSonosServiceName() {
        return getString(KEY_SONOS_SERVICE_NAME, DEFAULT_SONOS_SERVICE_NAME);
    }

    public void setSonosServiceName(String sonosServiceName) {
        setString(KEY_SONOS_SERVICE_NAME, sonosServiceName);
    }

    public int getSonosServiceId() {
        return getInt(KEY_SONOS_SERVICE_ID, DEFAULT_SONOS_SERVICE_ID);
    }

    public void setSonosServiceId(int sonosServiceid) {
        setInt(KEY_SONOS_SERVICE_ID, sonosServiceid);
    }

    public String getLocalIpAddress() {
        return localIpAddress;
    }

    /**
     * Rewrites an URL to make it accessible from remote clients.
     */
    public String rewriteRemoteUrl(String localUrl) {
        return StringUtil.rewriteRemoteUrl(localUrl, isUrlRedirectionEnabled(), getUrlRedirectType(), getUrlRedirectFrom(),
                                           getUrlRedirectCustomUrl(), getUrlRedirectContextPath(), getLocalIpAddress(),
                                           getPort());
    }

    private void setProperty(String key, String value) {
        if (value == null) {
            properties.remove(key);
        } else {
            properties.setProperty(key, value);
        }
    }

    private String[] toStringArray(String s) {
        List<String> result = new ArrayList<String>();
        StringTokenizer tokenizer = new StringTokenizer(s, " ");
        while (tokenizer.hasMoreTokens()) {
            result.add(tokenizer.nextToken());
        }

        return result.toArray(new String[result.size()]);
    }

    private void validateLicense() {
        String email = getLicenseEmail();
        Date date = getLicenseDate();

        if (email == null || date == null) {
            licenseValidated = false;
            return;
        }

        HttpClient client = new DefaultHttpClient();
        HttpConnectionParams.setConnectionTimeout(client.getParams(), 120000);
        HttpConnectionParams.setSoTimeout(client.getParams(), 120000);
        HttpGet method = new HttpGet("http://subsonic.org/backend/validateLicense.view" + "?email=" + StringUtil.urlEncode(email) +
                "&date=" + date.getTime() + "&version=" + versionService.getLocalVersion());
        try {
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            String content = client.execute(method, responseHandler);
            licenseValidated = content != null && !content.contains("false");
            if (!licenseValidated) {
                LOG.warn("License key is not valid.");
            }
            String[] lines = StringUtils.split(content);
            if (lines.length > 1) {
                licenseExpires = new Date(Long.parseLong(lines[1]));
            }

        } catch (Throwable x) {
            LOG.warn("Failed to validate license.", x);
        } finally {
            client.getConnectionManager().shutdown();
        }
    }

    public synchronized void scheduleLicenseValidation() {
        if (licenseValidationFuture != null) {
            licenseValidationFuture.cancel(true);
        }
        Runnable task = new Runnable() {
            public void run() {
                validateLicense();
            }
        };
        licenseValidated = true;
        licenseExpires = null;

        licenseValidationFuture = executor.scheduleWithFixedDelay(task, 0L, LICENSE_VALIDATION_DELAY_HOURS, TimeUnit.HOURS);
    }

    private void scheduleLocalIpAddressLookup() {
        Runnable task = new Runnable() {
            public void run() {
                localIpAddress = Util.getLocalIpAddress();
            }
        };
        executor.scheduleWithFixedDelay(task, 0L, LOCAL_IP_LOOKUP_DELAY_SECONDS, TimeUnit.SECONDS);
    }

    public void setInternetRadioDao(InternetRadioDao internetRadioDao) {
        this.internetRadioDao = internetRadioDao;
    }

    public void setMusicFolderDao(MusicFolderDao musicFolderDao) {
        this.musicFolderDao = musicFolderDao;
    }

    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }

    public void setAvatarDao(AvatarDao avatarDao) {
        this.avatarDao = avatarDao;
    }

    public void setVersionService(VersionService versionService) {
        this.versionService = versionService;
    }
}
