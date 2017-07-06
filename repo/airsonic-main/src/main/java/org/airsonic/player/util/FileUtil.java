/*
 This file is part of Airsonic.

 Airsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Airsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2016 (C) Airsonic Authors
 Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
 */
package org.airsonic.player.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

/**
 * Miscellaneous file utility methods.
 *
 * @author Sindre Mehus
 */
public final class FileUtil {

    private static final Logger LOG = LoggerFactory.getLogger(FileUtil.class);

    /**
     * Disallow external instantiation.
     */
    private FileUtil() {
    }

    public static boolean isFile(final File file) {
        return timed(new FileTask<Boolean>("isFile", file) {
            @Override
            public Boolean execute() {
                return file.isFile();
            }
        });
    }

    public static boolean isDirectory(final File file) {
        return timed(new FileTask<Boolean>("isDirectory", file) {
            @Override
            public Boolean execute() {
                return file.isDirectory();
            }
        });
    }

    public static boolean exists(final File file) {
        return timed(new FileTask<Boolean>("exists", file) {
            @Override
            public Boolean execute() {
                return file.exists();
            }
        });
    }

    public static boolean exists(String path) {
        return exists(new File(path));
    }

    public static long lastModified(final File file) {
        return timed(new FileTask<Long>("lastModified", file) {
            @Override
            public Long execute() {
                return file.lastModified();
            }
        });
    }

    public static long length(final File file) {
        return timed(new FileTask<Long>("length", file) {
            @Override
            public Long execute() {
                return file.length();
            }
        });
    }

    /**
     * Similar to {@link File#listFiles()}, but never returns null.
     * Instead a warning is logged, and an empty array is returned.
     */
    public static File[] listFiles(final File dir) {
        File[] files = timed(new FileTask<File[]>("listFiles", dir) {
            @Override
            public File[] execute() {
                return dir.listFiles();
            }
        });

        if (files == null) {
            LOG.warn("Failed to list children for " + dir.getPath());
            return new File[0];
        }
        return files;
    }

    /**
     * Returns a short path for the given file.  The path consists of the name
     * of the parent directory and the given file.
     */
    public static String getShortPath(File file) {
        if (file == null) {
            return null;
        }
        File parent = file.getParentFile();
        if (parent == null) {
            return file.getName();
        }
        return parent.getName() + File.separator + file.getName();
    }

    /**
     * Closes the "closable", ignoring any excepetions.
     *
     * @param closeable The Closable to close, may be {@code null}.
     */
    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                // Ignored
            }
        }
    }

    private static <T> T timed(FileTask<T> task) {
//        long t0 = System.nanoTime();
//        try {
            return task.execute();
//        } finally {
//            long t1 = System.nanoTime();
//            LOG.debug((t1 - t0) / 1000L + " microsec, " + task);
//        }
    }

    private abstract static class FileTask<T> {

        private final String name;
        private final File file;

        public FileTask(String name, File file) {
            this.name = name;
            this.file = file;
        }

        public abstract T execute();

        @Override
        public String toString() {
            return name + ", " + file;
        }
    }
}