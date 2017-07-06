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
package org.airsonic.player.upload;

import org.apache.commons.fileupload.disk.DiskFileItem;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Extension of Commons FileUpload for monitoring the upload progress.
 *
 * @author Pierre-Alexandre Losson -- http://www.telio.be/blog -- plosson@users.sourceforge.net
 */
public class MonitoredDiskFileItem extends DiskFileItem {
    private MonitoredOutputStream mos;
    private UploadListener listener;

    public MonitoredDiskFileItem(String fieldName, String contentType, boolean isFormField, String fileName, int sizeThreshold,
                                 File repository, UploadListener listener) {
        super(fieldName, contentType, isFormField, fileName, sizeThreshold, repository);
        this.listener = listener;
        if (fileName != null) {
            listener.start(fileName);
        }
    }

    public OutputStream getOutputStream() throws IOException {
        if (mos == null) {
            mos = new MonitoredOutputStream(super.getOutputStream(), listener);
        }
        return mos;
    }
}
