/*
 This file is part of Libresonic.

 Libresonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Libresonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Libresonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2016 (C) Libresonic Authors
 Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
 */
package org.libresonic.player;

import org.slf4j.LoggerFactory;

/**
 * Proxy to Slf4j Logger.
 */
@Deprecated
public class Logger {

    private final org.slf4j.Logger internalLogger;

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


    private Logger(String name) {
        internalLogger = LoggerFactory.getLogger(name);
    }

    /**
     * Logs a debug message.
     * @param message The log message.
     */
    public void debug(String message) {
        internalLogger.debug(message);
    }

    /**
     * Logs a debug message.
     * @param message The message.
     * @param error The optional exception.
     */
    public void debug(String message, Throwable error) {
        internalLogger.debug(message, error);
    }

    /**
     * Logs an info message.
     * @param message The message.
     */
    public void info(String message) {
        internalLogger.info(message);
    }

    /**
     * Logs an info message.
     * @param message The message.
     * @param error The optional exception.
     */
    public void info(String message, Throwable error) {
        internalLogger.info(message, error);
    }

    /**
     * Logs a warning message.
     * @param message The message.
     */
    public void warn(String message) {
        internalLogger.warn(message);
    }

    /**
     * Logs a warning message.
     * @param message The message.
     * @param error The optional exception.
     */
    public void warn(String message, Throwable error) {
        internalLogger.warn(message, error);
    }

    /**
     * Logs an error message.
     * @param message The message.
     */
    public void error(String message) {
        internalLogger.error(message);
    }

    /**
     * Logs an error message.
     * @param message The message.
     * @param error The optional exception.
     */
    public void error(String message, Throwable error) {
        internalLogger.error(message, error);
    }

}
