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
package net.sourceforge.subsonic.upload;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;

import java.io.File;

/**
 * Extension of Commons FileUpload for monitoring the upload progress.
 *
 * @author Pierre-Alexandre Losson -- http://www.telio.be/blog -- plosson@users.sourceforge.net
 */
public class MonitoredDiskFileItemFactory extends DiskFileItemFactory {
    private UploadListener listener;

    public MonitoredDiskFileItemFactory(UploadListener listener) {
        super();
        this.listener = listener;
    }

    public MonitoredDiskFileItemFactory(int sizeThreshold, File repository, UploadListener listener) {
        super(sizeThreshold, repository);
        this.listener = listener;
    }

    public FileItem createItem(String fieldName, String contentType, boolean isFormField, String fileName) {
        return new MonitoredDiskFileItem(fieldName, contentType, isFormField, fileName, getSizeThreshold(), getRepository(), listener);
    }
}
