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
package net.sourceforge.subsonic.domain;

/**
 * Parameters used when transcoding videos.
 *
 * @author Sindre Mehus
 * @version $Id$
 */
public class VideoTranscodingSettings {

    private final int width;
    private final int height;
    private final int timeOffset;
    private final int duration;
    private final boolean hls;

    public VideoTranscodingSettings(int width, int height, int timeOffset, int duration, boolean hls) {
        this.width = width;
        this.height = height;
        this.timeOffset = timeOffset;
        this.duration = duration;
        this.hls = hls;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getTimeOffset() {
        return timeOffset;
    }

    public int getDuration() {
        return duration;
    }

    public boolean isHls() {
        return hls;
    }
}
