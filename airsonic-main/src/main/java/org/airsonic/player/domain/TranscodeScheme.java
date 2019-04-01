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
package org.airsonic.player.domain;

/**
 * Enumeration of transcoding schemes. Transcoding is the process of
 * converting an audio stream to a lower bit rate.
 *
 * @author Sindre Mehus
 */
public enum TranscodeScheme {

    OFF(0),
    MAX_32(32),
    MAX_40(40),
    MAX_48(48),
    MAX_56(56),
    MAX_64(64),
    MAX_80(80),
    MAX_96(96),
    MAX_112(112),
    MAX_128(128),
    MAX_160(160),
    MAX_192(192),
    MAX_224(224),
    MAX_256(256),
    MAX_320(320);

    private int maxBitRate;

    TranscodeScheme(int maxBitRate) {
        this.maxBitRate = maxBitRate;
    }

    /**
     * Returns the maximum bit rate for this transcoding scheme.
     *
     * @return The maximum bit rate for this transcoding scheme.
     */
    public int getMaxBitRate() {
        return maxBitRate;
    }

    /**
     * Returns the strictest transcode scheme (i.e., the scheme with the lowest max bitrate).
     *
     * @param other The other transcode scheme. May be <code>null</code>, in which case 'this' is returned.
     * @return The strictest scheme.
     */
    public TranscodeScheme strictest(TranscodeScheme other) {
        if (other == null || other == TranscodeScheme.OFF) {
            return this;
        }

        if (this == TranscodeScheme.OFF) {
            return other;
        }

        return maxBitRate < other.maxBitRate ? this : other;
    }

    /**
     * Returns a human-readable string representation of this object.
     *
     * @return A human-readable string representation of this object.
     */
    public String toString() {
        if (this == OFF) {
            return "No limit";
        }
        return getMaxBitRate() + " Kbps";
    }

    /**
     * Returns the enum constant which corresponds to the given max bit rate.
     *
     * @param maxBitRate The max bit rate.
     * @return The corresponding enum, or <code>null</code> if not found.
     */
    public static TranscodeScheme valueOf(int maxBitRate) {
        for (TranscodeScheme scheme : values()) {
            if (scheme.getMaxBitRate() == maxBitRate) {
                return scheme;
            }
        }
        return null;
    }

    public static TranscodeScheme fromMaxBitRate(int maxBitRate) {
        for (TranscodeScheme transcodeScheme : TranscodeScheme.values()) {
            if (maxBitRate == transcodeScheme.getMaxBitRate()) {
                return transcodeScheme;
            }
        }
        return null;
    }
}
