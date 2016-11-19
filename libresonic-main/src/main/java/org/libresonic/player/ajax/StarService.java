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
package org.libresonic.player.ajax;

import org.libresonic.player.Logger;
import org.libresonic.player.dao.MediaFileDao;
import org.libresonic.player.domain.User;
import org.libresonic.player.service.SecurityService;
import org.directwebremoting.WebContext;
import org.directwebremoting.WebContextFactory;

/**
 * Provides AJAX-enabled services for starring.
 * <p/>
 * This class is used by the DWR framework (http://getahead.ltd.uk/dwr/).
 *
 * @author Sindre Mehus
 */
public class StarService {

    private static final Logger LOG = Logger.getLogger(StarService.class);

    private SecurityService securityService;
    private MediaFileDao mediaFileDao;

    public void star(int id) {
        mediaFileDao.starMediaFile(id, getUser());
    }

    public void unstar(int id) {
        mediaFileDao.unstarMediaFile(id, getUser());
    }

    private String getUser() {
        WebContext webContext = WebContextFactory.get();
        User user = securityService.getCurrentUser(webContext.getHttpServletRequest());
        return user.getUsername();
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    public void setMediaFileDao(MediaFileDao mediaFileDao) {
        this.mediaFileDao = mediaFileDao;
    }
}