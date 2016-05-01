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
package net.sourceforge.subsonic.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;

import net.sourceforge.subsonic.command.SearchCommand;
import net.sourceforge.subsonic.domain.MusicFolder;
import net.sourceforge.subsonic.domain.SearchCriteria;
import net.sourceforge.subsonic.domain.SearchResult;
import net.sourceforge.subsonic.domain.User;
import net.sourceforge.subsonic.domain.UserSettings;
import net.sourceforge.subsonic.service.PlayerService;
import net.sourceforge.subsonic.service.SearchService;
import net.sourceforge.subsonic.service.SecurityService;
import net.sourceforge.subsonic.service.SettingsService;

/**
 * Controller for the search page.
 *
 * @author Sindre Mehus
 */
public class SearchController extends SimpleFormController {

    private static final int MATCH_COUNT = 25;

    private SecurityService securityService;
    private SettingsService settingsService;
    private PlayerService playerService;
    private SearchService searchService;

    @Override
    protected Object formBackingObject(HttpServletRequest request) throws Exception {
        return new SearchCommand();
    }

    @Override
    protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object com, BindException errors)
            throws Exception {
        SearchCommand command = (SearchCommand) com;

        User user = securityService.getCurrentUser(request);
        UserSettings userSettings = settingsService.getUserSettings(user.getUsername());
        command.setUser(user);
        command.setPartyModeEnabled(userSettings.isPartyModeEnabled());

        List<MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(user.getUsername());
        String query = StringUtils.trimToNull(command.getQuery());

        if (query != null) {

            SearchCriteria criteria = new SearchCriteria();
            criteria.setCount(MATCH_COUNT);
            criteria.setQuery(query);

            SearchResult artists = searchService.search(criteria, musicFolders, SearchService.IndexType.ARTIST);
            command.setArtists(artists.getMediaFiles());

            SearchResult albums = searchService.search(criteria, musicFolders, SearchService.IndexType.ALBUM);
            command.setAlbums(albums.getMediaFiles());

            SearchResult songs = searchService.search(criteria, musicFolders, SearchService.IndexType.SONG);
            command.setSongs(songs.getMediaFiles());

            command.setPlayer(playerService.getPlayer(request, response));
        }

        return new ModelAndView(getSuccessView(), errors.getModel());
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setPlayerService(PlayerService playerService) {
        this.playerService = playerService;
    }

    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }
}
