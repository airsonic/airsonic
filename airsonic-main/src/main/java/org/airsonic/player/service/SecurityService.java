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

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.airsonic.player.dao.SonosLinkDao;
import org.airsonic.player.dao.UserDao;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.SonosLink;
import org.airsonic.player.domain.User;
import org.airsonic.player.service.sonos.SonosSoapFault;
import org.airsonic.player.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.airsonic.player.domain.User.USERNAME_SONOS;

/**
 * Provides security-related services for authentication and authorization.
 *
 * @author Sindre Mehus
 */
@Service
public class SecurityService implements UserDetailsService {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityService.class);

    @Autowired
    private UserDao userDao;

    @Autowired
    private SonosLinkDao sonosLinkDao;

    @Autowired
    private SettingsService settingsService;

    @Autowired
    private Ehcache userCache;

    @Autowired
    private Ehcache sonosLinkcodeCache;

    @Autowired
    private JWTSecurityService jwtSecurityService;

    // This is initialize in GlobalSecurityConfig, is autowired.
    // Something wrong here, some circular ref, maybe rebuild responsibilities...
    private AuthenticationManager authenticationManager;


    /**
     * Locates the user based on the username.
     *
     * @param username The username
     * @return A fully populated user record (never <code>null</code>)
     * @throws UsernameNotFoundException if the user could not be found or the user has no GrantedAuthority.
     * @throws DataAccessException       If user could not be found for a repository-specific reason.
     */
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException, DataAccessException {
        return loadUserByUsername(username, true);
    }

    public UserDetails loadUserByUsername(String username, boolean caseSensitive)
            throws UsernameNotFoundException, DataAccessException {
        User user = getUserByName(username, caseSensitive);
        if (user == null) {
            throw new UsernameNotFoundException("User \"" + username + "\" was not found.");
        }

        List<GrantedAuthority> authorities = getGrantedAuthorities(username);

        return new org.springframework.security.core.userdetails.User(
                username,
                user.getPassword(),
                !user.isLdapAuthenticated(),
                true,
                true,
                true,
                authorities);
    }

    public List<GrantedAuthority> getGrantedAuthorities(String username) {
        String[] roles = userDao.getRolesForUser(username);
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("IS_AUTHENTICATED_ANONYMOUSLY"));
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        for (int i = 0; i < roles.length; i++) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + roles[i].toUpperCase()));
        }
        return authorities;
    }

    /**
     * Returns the currently logged-in user for the given HTTP request.
     *
     * @param request The HTTP request.
     * @return The logged-in user, or <code>null</code>.
     */
    public User getCurrentUser(HttpServletRequest request) {
        String username = getCurrentUsername(request);
        return username == null ? null : getUserByName(username);
    }

    public String getLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.User){
            return ((org.springframework.security.core.userdetails.User)authentication.getPrincipal()).getUsername();
        }

        return null;
    }


    /**
     * Returns the name of the currently logged-in user.
     *
     * @param request The HTTP request.
     * @return The name of the logged-in user, or <code>null</code>.
     */
    public String getCurrentUsername(HttpServletRequest request) {
        return new SecurityContextHolderAwareRequestWrapper(request, null).getRemoteUser();
    }

    /**
     * Returns the user with the given username.
     *
     * @param username The username used when logging in.
     * @return The user, or <code>null</code> if not found.
     */
    public User getUserByName(String username) {
        return getUserByName(username, true);
    }

    /**
     * Returns the user with the given username
     * @param username
     * @param caseSensitive If false, will do a case insensitive search
     * @return
     */
    public User getUserByName(String username, boolean caseSensitive) {
        return userDao.getUserByName(username, caseSensitive);
    }

    /**
     * Returns the user with the given email address.
     *
     * @param email The email address.
     * @return The user, or <code>null</code> if not found.
     */
    public User getUserByEmail(String email) {
        return userDao.getUserByEmail(email);
    }

    /**
     * Returns all users.
     *
     * @return Possibly empty array of all users.
     */
    public List<User> getAllUsers() {
        return userDao.getAllUsers();
    }

    /**
     * Returns whether the given user has administrative rights.
     */
    public boolean isAdmin(String username) {
        if (User.USERNAME_ADMIN.equals(username)) {
            return true;
        }
        User user = getUserByName(username);
        return user != null && user.isAdminRole();
    }

    /**
     * Creates a new user.
     *
     * @param user The user to create.
     */
    public void createUser(User user) {
        userDao.createUser(user);
        settingsService.setMusicFoldersForUser(user.getUsername(), MusicFolder.toIdList(settingsService.getAllMusicFolders()));
        LOG.info("Created user " + user.getUsername());
    }

    /**
     * Deletes the user with the given username.
     *
     * @param username The username.
     */
    public void deleteUser(String username) {
        userDao.deleteUser(username);
        LOG.info("Deleted user " + username);
        userCache.remove(username);
    }

    /**
     * Updates the given user.
     *
     * @param user The user to update.
     */
    public void updateUser(User user) {
        userDao.updateUser(user);
        userCache.remove(user.getUsername());
    }

    /**
     * Updates the byte counts for given user.
     *
     * @param user                 The user to update, may be <code>null</code>.
     * @param bytesStreamedDelta   Increment bytes streamed count with this value.
     * @param bytesDownloadedDelta Increment bytes downloaded count with this value.
     * @param bytesUploadedDelta   Increment bytes uploaded count with this value.
     */
    public void updateUserByteCounts(User user, long bytesStreamedDelta, long bytesDownloadedDelta, long bytesUploadedDelta) {
        if (user == null) {
            return;
        }

        user.setBytesStreamed(user.getBytesStreamed() + bytesStreamedDelta);
        user.setBytesDownloaded(user.getBytesDownloaded() + bytesDownloadedDelta);
        user.setBytesUploaded(user.getBytesUploaded() + bytesUploadedDelta);

        userDao.updateUser(user);
    }

    /**
     * Returns whether the given file may be read.
     *
     * @return Whether the given file may be read.
     */
    public boolean isReadAllowed(File file) {
        // Allowed to read from both music folder and podcast folder.
        return isInMusicFolder(file) || isInPodcastFolder(file);
    }

    /**
     * Returns whether the given file may be written, created or deleted.
     *
     * @return Whether the given file may be written, created or deleted.
     */
    public boolean isWriteAllowed(File file) {
        // Only allowed to write podcasts or cover art.
        boolean isPodcast = isInPodcastFolder(file);
        boolean isCoverArt = isInMusicFolder(file) && file.getName().startsWith("cover.");

        return isPodcast || isCoverArt;
    }

    /**
     * Returns whether the given file may be uploaded.
     *
     * @return Whether the given file may be uploaded.
     */
    public boolean isUploadAllowed(File file) {
        return isInMusicFolder(file) && !FileUtil.exists(file);
    }

    /**
     * Returns whether the given file is located in one of the music folders (or any of their sub-folders).
     *
     * @param file The file in question.
     * @return Whether the given file is located in one of the music folders.
     */
    private boolean isInMusicFolder(File file) {
        return getMusicFolderForFile(file) != null;
    }

    private MusicFolder getMusicFolderForFile(File file) {
        List<MusicFolder> folders = settingsService.getAllMusicFolders(false, true);
        String path = file.getPath();
        for (MusicFolder folder : folders) {
            if (isFileInFolder(path, folder.getPath().getPath())) {
                return folder;
            }
        }
        return null;
    }

    /**
     * Returns whether the given file is located in the Podcast folder (or any of its sub-folders).
     *
     * @param file The file in question.
     * @return Whether the given file is located in the Podcast folder.
     */
    private boolean isInPodcastFolder(File file) {
        String podcastFolder = settingsService.getPodcastFolder();
        return isFileInFolder(file.getPath(), podcastFolder);
    }

    public String getRootFolderForFile(File file) {
        MusicFolder folder = getMusicFolderForFile(file);
        if (folder != null) {
            return folder.getPath().getPath();
        }

        if (isInPodcastFolder(file)) {
            return settingsService.getPodcastFolder();
        }
        return null;
    }

    public boolean isFolderAccessAllowed(MediaFile file, String username) {
        if (isInPodcastFolder(file.getFile())) {
            return true;
        }

        for (MusicFolder musicFolder : settingsService.getMusicFoldersForUser(username)) {
            if (musicFolder.getPath().getPath().equals(file.getFolder())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns whether the given file is located in the given folder (or any of its sub-folders).
     * If the given file contains the expression ".." (indicating a reference to the parent directory),
     * this method will return <code>false</code>.
     *
     * @param file   The file in question.
     * @param folder The folder in question.
     * @return Whether the given file is located in the given folder.
     */
    protected boolean isFileInFolder(String file, String folder) {
        // Deny access if file contains ".." surrounded by slashes (or end of line).
        if (file.matches(".*(/|\\\\)\\.\\.(/|\\\\|$).*")) {
            return false;
        }

        // Convert slashes.
        file = file.replace('\\', '/');
        folder = folder.replace('\\', '/');

        return file.toUpperCase().startsWith(folder.toUpperCase());
    }


    // =======================================================================================================
    // Utilities for Sonos link.

    public Authentication authenticate(String username, String password){
        UsernamePasswordAuthenticationToken authReq = new UsernamePasswordAuthenticationToken(username, password);
        return authenticationManager.authenticate(authReq);
    }

    /**
     * Generate a link code, and put in the cache for future use.
     *
     * @param householdId from sonos, represent a user on sonos

     * @return The link code.
     */
    public String generateLinkCode(String householdId){
        String linkCode = createLinkCode();

        sonosLinkcodeCache.put(new Element(linkCode, householdId));

        return linkCode;
    }

    public String getHousehold(String linkCode){
        Element element = sonosLinkcodeCache.get(linkCode);

        if(element == null){
            return  null;
        } else {
            return (String) element.getValue();
        }
    }

    /**
     * Insert the authorisation in sonoslink. Verify is the linkcode exist, and cannot be get again.
     *
     * @param username The username authorisation for sonos link
     * @param householdId The id of entry in sonos controller
     * @param linkcode The link code used between sonos and airsonic.
     * @return true if the insert is ok, false if some entry exist with the linkcode
     */
    public boolean authoriseSonos(String username, String householdId, String linkcode) {
        if(sonosLinkDao.findByLinkcode(linkcode) != null){
            return false;
        }

        sonosLinkDao.create(new SonosLink(username, householdId, linkcode));
        return true;
    }

    /**
     * Find the user they have a linkCode for the householdId set, build the authToken and return it.
     *
     * @param householdId The householdId from Sonos
     * @param linkCode The linkCode return it before
     * @return The build authToken or null if didn't find any user with householdId and linkCode
     */
    public String getSonosAuthToken(String householdId, String linkCode){

        SonosLink sonosLink = sonosLinkDao.findByLinkcode(linkCode);
        if(sonosLink != null && householdId.equals(sonosLink.getHouseholdid())) {
            return buildToken(sonosLink);
        } else {
            return null;
        }
    }

    private String buildToken(SonosLink link) {
        return jwtSecurityService.createSonosToken(link.getUsername(), link.getHouseholdid(), link.getLinkcode());
    }

    public SonosLink getSonosLink(String linkCode) {
        return sonosLinkDao.findByLinkcode(linkCode);
    }

    public void authenticate(String sonosLinkToken) throws SonosSoapFault.LoginUnauthorized {
        SonosLink sonosLink =jwtSecurityService.verifySonosLink(sonosLinkToken);

        SonosLink saved = sonosLinkDao.findByLinkcode(sonosLink.getLinkcode());
        if(saved != null && saved.identical(sonosLink)){
            setUser(sonosLink.getUsername());
        } else {
            throw new SonosSoapFault.LoginUnauthorized();
        }
    }

    public void authenticate() throws SonosSoapFault.LoginUnauthorized {
        setUser(USERNAME_SONOS);
    }

    private void setUser(String username) throws SonosSoapFault.LoginUnauthorized {
        User user = getUserByName(USERNAME_SONOS, true);
        Authentication authentication = authenticate(user.getUsername(), user.getPassword());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

        // must be strict 32 character limit on length
    private String createLinkCode() {
        return UUID.randomUUID().toString().replace("-", "");
    }


    //--------------------------------------------------------------------------------------------------------


    public void setAuthenticationManager(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

}