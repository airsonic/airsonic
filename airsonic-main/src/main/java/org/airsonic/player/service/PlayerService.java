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
package org.airsonic.player.service;

import org.airsonic.player.dao.PlayerDao;
import org.airsonic.player.domain.Player;
import org.airsonic.player.domain.Transcoding;
import org.airsonic.player.domain.TransferStatus;
import org.airsonic.player.domain.User;
import org.airsonic.player.util.StringUtil;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.ServletRequestUtils;

import javax.annotation.PostConstruct;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Provides services for maintaining the set of players.
 *
 * @author Sindre Mehus
 * @see Player
 */
@Service
@DependsOn("liquibase")
public class PlayerService {

    private static final String COOKIE_NAME = "player";
    private static final int COOKIE_EXPIRY = 365 * 24 * 3600; // One year

    @Autowired
    private PlayerDao playerDao;
    @Autowired
    private StatusService statusService;
    @Autowired
    private SecurityService securityService;
    @Autowired
    private TranscodingService transcodingService;

    @PostConstruct
    public void init() {
        playerDao.deleteOldPlayers(60);
    }

    /**
     * Equivalent to <code>getPlayer(request, response, true)</code> .
     */
    public Player getPlayer(HttpServletRequest request, HttpServletResponse response) throws Exception {
        return getPlayer(request, response, true, false);
    }

    /**
     * Returns the player associated with the given HTTP request.  If no such player exists, a new
     * one is created.
     *
     * @param request              The HTTP request.
     * @param response             The HTTP response.
     * @param remoteControlEnabled Whether this method should return a remote-controlled player.
     * @param isStreamRequest      Whether the HTTP request is a request for streaming data.
     * @return The player associated with the given HTTP request.
     */
    public synchronized Player getPlayer(HttpServletRequest request, HttpServletResponse response,
                                         boolean remoteControlEnabled, boolean isStreamRequest) throws Exception {

        // Find by 'player' request parameter.
        Player player = getPlayerById(ServletRequestUtils.getIntParameter(request, "player"));

        // Find in session context.
        if (player == null && remoteControlEnabled) {
            Integer playerId = (Integer) request.getSession().getAttribute("player");
            if (playerId != null) {
                player = getPlayerById(playerId);
            }
        }

        // Find by cookie.
        String username = securityService.getCurrentUsername(request);
        if (player == null && remoteControlEnabled) {
            player = getPlayerById(getPlayerIdFromCookie(request, username));
        }

        // Make sure we're not hijacking the player of another user.
        if (player != null && player.getUsername() != null && username != null && !player.getUsername().equals(username)) {
            player = null;
        }

        // Look for player with same IP address and user name.
        if (player == null) {
            player = getNonRestPlayerByIpAddressAndUsername(request.getRemoteAddr(), username);
        }

        // If no player was found, create it.
        if (player == null) {
            player = new Player();
            createPlayer(player);
//            LOG.debug("Created player " + player.getId() + " (remoteControlEnabled: " + remoteControlEnabled +
//                      ", isStreamRequest: " + isStreamRequest + ", username: " + username +
//                      ", ip: " + request.getRemoteAddr() + ").");
        }

        // Update player data.
        boolean isUpdate = false;
        if (username != null && player.getUsername() == null) {
            player.setUsername(username);
            isUpdate = true;
        }
        if (player.getIpAddress() == null || isStreamRequest ||
            (!isPlayerConnected(player) && player.isDynamicIp() && !request.getRemoteAddr().equals(player.getIpAddress()))) {
            player.setIpAddress(request.getRemoteAddr());
            isUpdate = true;
        }
        String userAgent = request.getHeader("user-agent");
        if (isStreamRequest) {
            player.setType(userAgent);
            player.setLastSeen(new Date());
            isUpdate = true;
        }

        if (isUpdate) {
            updatePlayer(player);
        }

        // Set cookie in response.
        if (response != null) {
            String cookieName = COOKIE_NAME + "-" + StringUtil.utf8HexEncode(username);
            Cookie cookie = new Cookie(cookieName, String.valueOf(player.getId()));
            cookie.setMaxAge(COOKIE_EXPIRY);
            cookie.setHttpOnly(true);
            String path = request.getContextPath();
            if (StringUtils.isEmpty(path)) {
                path = "/";
            }
            cookie.setPath(path);
            response.addCookie(cookie);
        }

        // Save player in session context.
        if (remoteControlEnabled) {
            request.getSession().setAttribute("player", player.getId());
        }

        return player;
    }

    /**
     * Updates the given player.
     *
     * @param player The player to update.
     */
    public void updatePlayer(Player player) {
        playerDao.updatePlayer(player);
    }

    /**
     * Returns the player with the given ID.
     *
     * @param id The unique player ID.
     * @return The player with the given ID, or <code>null</code> if no such player exists.
     */
    public Player getPlayerById(Integer id) {
        if (id == null) {
            return null;
        } else {
            return playerDao.getPlayerById(id);
        }
    }

    /**
     * Returns whether the given player is connected.
     *
     * @param player The player in question.
     * @return Whether the player is connected.
     */
    private boolean isPlayerConnected(Player player) {
        for (TransferStatus status : statusService.getStreamStatusesForPlayer(player)) {
            if (status.isActive()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the (non-REST) player with the given IP address and username. If no username is given, only IP address is
     * used as search criteria.
     *
     * @param ipAddress The IP address.
     * @param username  The remote user.
     * @return The player with the given IP address, or <code>null</code> if no such player exists.
     */
    private Player getNonRestPlayerByIpAddressAndUsername(final String ipAddress, final String username) {
        if (ipAddress == null) {
            return null;
        }
        for (Player player : getAllPlayers()) {
            boolean isRest = player.getClientId() != null;
            boolean ipMatches = ipAddress.equals(player.getIpAddress());
            boolean userMatches = username == null || username.equals(player.getUsername());
            if (!isRest && ipMatches && userMatches) {
                return player;
            }
        }
        return null;
    }

    /**
     * Reads the player ID from the cookie in the HTTP request.
     *
     * @param request  The HTTP request.
     * @param username The name of the current user.
     * @return The player ID embedded in the cookie, or <code>null</code> if cookie is not present.
     */
    private Integer getPlayerIdFromCookie(HttpServletRequest request, String username) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        String cookieName = COOKIE_NAME + "-" + StringUtil.utf8HexEncode(username);
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                try {
                    return Integer.valueOf(cookie.getValue());
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * Returns all players owned by the given username and client ID.
     *
     * @param username The name of the user.
     * @param clientId The third-party client ID (used if this player is managed over the
     *                 Airsonic REST API). May be <code>null</code>.
     * @return All relevant players.
     */
    public List<Player> getPlayersForUserAndClientId(String username, String clientId) {
        return playerDao.getPlayersForUserAndClientId(username, clientId);
    }

    /**
     * Returns all currently registered players.
     *
     * @return All currently registered players.
     */
    public List<Player> getAllPlayers() {
        return playerDao.getAllPlayers();
    }

    /**
     * Removes the player with the given ID.
     *
     * @param id The unique player ID.
     */
    public synchronized void removePlayerById(int id) {
        playerDao.deletePlayer(id);
    }

    /**
     * Creates and returns a clone of the given player.
     *
     * @param playerId The ID of the player to clone.
     * @return The cloned player.
     */
    public Player clonePlayer(int playerId) {
        Player player = getPlayerById(playerId);
        if (player.getName() != null) {
            player.setName(player.getName() + " (copy)");
        }

        createPlayer(player);
        return player;
    }

    /**
     * Creates the given player, and activates all transcodings.
     *
     * @param player The player to create.
     */
    public void createPlayer(Player player) {
        playerDao.createPlayer(player);

        List<Transcoding> transcodings = transcodingService.getAllTranscodings();
        List<Transcoding> defaultActiveTranscodings = new ArrayList<Transcoding>();
        for (Transcoding transcoding : transcodings) {
            if (transcoding.isDefaultActive()) {
                defaultActiveTranscodings.add(transcoding);
            }
        }
        if (player != null) {
            transcodingService.setTranscodingsForPlayer(player, defaultActiveTranscodings);
        }
    }

    /**
     * Returns a player associated to the special "guest" user, creating it if necessary.
     */
    public Player getGuestPlayer(HttpServletRequest request) {

        // Create guest user if necessary.
        User user = securityService.getUserByName(User.USERNAME_GUEST);
        if (user == null) {
            user = new User(User.USERNAME_GUEST, RandomStringUtils.randomAlphanumeric(30), null);
            user.setStreamRole(true);
            securityService.createUser(user);
        }

        // Look for existing player.
        List<Player> players = getPlayersForUserAndClientId(User.USERNAME_GUEST, null);
        if (!players.isEmpty()) {
            return players.get(0);
        }

        // Create player if necessary.
        Player player = new Player();
        if (request != null ) {
            player.setIpAddress(request.getRemoteAddr());
        }
        player.setUsername(User.USERNAME_GUEST);
        createPlayer(player);

        return player;
    }

    public void setStatusService(StatusService statusService) {
        this.statusService = statusService;
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    public void setPlayerDao(PlayerDao playerDao) {
        this.playerDao = playerDao;
    }

    public void setTranscodingService(TranscodingService transcodingService) {
        this.transcodingService = transcodingService;
    }
}
