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
package net.sourceforge.subsonic;

import net.sourceforge.subsonic.domain.Version;
import net.sourceforge.subsonic.service.*;
import net.sourceforge.subsonic.util.*;
import org.apache.commons.lang.exception.*;

import java.io.*;
import java.text.*;
import java.util.*;

/**
 * Logger implementation which logs to SUBSONIC_HOME/subsonic.log.
 * <br/>
 * Note: Third party logging libraries (such as log4j and Commons logging) are intentionally not
 * used. These libraries causes a lot of headache when deploying to some application servers
 * (for instance Jetty and JBoss).
 *
 * @author Sindre Mehus
 * @version $Revision: 1.1 $ $Date: 2005/05/09 19:58:26 $
 */
public class Logger {

    private String category;

    private static List<Entry> entries = Collections.synchronizedList(new BoundedList<Entry>(50));
    private static PrintWriter writer;
    private static Boolean debugEnabled;

    /**
     * Creates a logger for the given class.
     * @param clazz The class.
     * @return A logger for the class.
     */
    public static Logger getLogger(Class clazz) {
        return new Logger(clazz.getName());
    }

    /**
     * Creates a logger for the given namee.
     * @param name The name.
     * @return A logger for the name.
     */
    public static Logger getLogger(String name) {
        return new Logger(name);
    }

    /**
     * Returns the last few log entries.
     * @return The last few log entries.
     */
    public static Entry[] getLatestLogEntries() {
        return entries.toArray(new Entry[entries.size()]);
    }

    private Logger(String name) {
        int lastDot = name.lastIndexOf('.');
        if (lastDot == -1) {
            category = name;
        } else {
            category = name.substring(lastDot + 1);
        }
    }

    /**
     * Logs a debug message.
     * @param message The log message.
     */
    public void debug(Object message) {
        debug(message, null);
    }

    /**
     * Logs a debug message.
     * @param message The message.
     * @param error The optional exception.
     */
    public void debug(Object message, Throwable error) {
        if (isDebugEnabled()) {
            add(Level.DEBUG, message, error);
        }
    }

    private static boolean isDebugEnabled() {
        if (debugEnabled == null) {
            VersionService versionService = ServiceLocator.getVersionService();
            if (versionService == null) {
                return true;  // versionService not yet available.
            }
            Version localVersion = versionService.getLocalVersion();
            debugEnabled = localVersion == null || localVersion.getBeta() != 0;
        }
        return debugEnabled;
    }

    /**
     * Logs an info message.
     * @param message The message.
     */
    public void info(Object message) {
        info(message, null);
    }

    /**
     * Logs an info message.
     * @param message The message.
     * @param error The optional exception.
     */
    public void info(Object message, Throwable error) {
        add(Level.INFO, message, error);
    }

    /**
     * Logs a warning message.
     * @param message The message.
     */
    public void warn(Object message) {
        warn(message, null);
    }

    /**
     * Logs a warning message.
     * @param message The message.
     * @param error The optional exception.
     */
    public void warn(Object message, Throwable error) {
        add(Level.WARN, message, error);
    }

    /**
     * Logs an error message.
     * @param message The message.
     */
    public void error(Object message) {
        error(message, null);
    }

    /**
     * Logs an error message.
     * @param message The message.
     * @param error The optional exception.
     */
    public void error(Object message, Throwable error) {
        add(Level.ERROR, message, error);
    }

    private void add(Level level, Object message, Throwable error) {
        Entry entry = new Entry(category, level, message, error);
        try {
            getPrintWriter().println(entry);
        } catch (IOException x) {
            System.err.println("Failed to write to subsonic.log. " + x);
        }
        entries.add(entry);
    }

    private static synchronized PrintWriter getPrintWriter() throws IOException {
        if (writer == null) {
            writer = new PrintWriter(new FileWriter(getLogFile(), false), true);
        }
        return writer;
    }

    public static File getLogFile() {
        File subsonicHome = SettingsService.getSubsonicHome();
        return new File(subsonicHome, "subsonic.log");
    }

    /**
    * Log level.
    */
    public enum Level {
        DEBUG, INFO, WARN, ERROR
    }

    /**
     * Log entry.
     */
    public static class Entry {
        private String category;
        private Date date;
        private Level level;
        private Object message;
        private Throwable error;
        private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");

        public Entry(String category, Level level, Object message, Throwable error) {
            this.date = new Date();
            this.category = category;
            this.level = level;
            this.message = message;
            this.error = error;
        }

        public String getCategory() {
            return category;
        }

        public Date getDate() {
            return date;
        }

        public Level getLevel() {
            return level;
        }

        public Object getMessage() {
            return message;
        }

        public Throwable getError() {
            return error;
        }

        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append('[').append(DATE_FORMAT.format(date)).append("] ");
            builder.append(level).append(' ');
            builder.append(category).append(" - ");
            builder.append(message);

            if (error != null) {
                builder.append('\n').append(ExceptionUtils.getFullStackTrace(error));
            }
            return builder.toString();
        }
    }
}
