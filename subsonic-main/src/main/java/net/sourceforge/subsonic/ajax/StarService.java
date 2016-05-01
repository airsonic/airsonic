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

import net.sourceforge.subsonic.Logger;
import net.sourceforge.subsonic.dao.MediaFileDao;
import net.sourceforge.subsonic.domain.User;
import net.sourceforge.subsonic.service.SecurityService;
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