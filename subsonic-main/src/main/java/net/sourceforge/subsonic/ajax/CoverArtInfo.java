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
package net.sourceforge.subsonic.ajax;

/**
 * Contains info about cover art images for an album.
 *
 * @author Sindre Mehus
 */
public class CoverArtInfo {

    private final String imagePreviewUrl;
    private final String imageDownloadUrl;

    public CoverArtInfo(String imagePreviewUrl, String imageDownloadUrl) {
        this.imagePreviewUrl = imagePreviewUrl;
        this.imageDownloadUrl = imageDownloadUrl;
    }

    public String getImagePreviewUrl() {
        return imagePreviewUrl;
    }

    public String getImageDownloadUrl() {
        return imageDownloadUrl;
    }
}