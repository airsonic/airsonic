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
package net.sourceforge.subsonic.dao;

import java.io.File;
import java.util.Date;

import net.sourceforge.subsonic.domain.MusicFolder;

/**
 * Unit test of {@link MusicFolderDao}.
 *
 * @author Sindre Mehus
 */
public class MusicFolderDaoTestCase extends DaoTestCaseBase {

    @Override
    protected void setUp() throws Exception {
        getJdbcTemplate().execute("delete from music_folder");
    }

    public void testCreateMusicFolder() {
        MusicFolder musicFolder = new MusicFolder(new File("path"), "name", true, new Date());
        musicFolderDao.createMusicFolder(musicFolder);

        MusicFolder newMusicFolder = musicFolderDao.getAllMusicFolders().get(0);
        assertMusicFolderEquals(musicFolder, newMusicFolder);
    }

    public void testUpdateMusicFolder() {
        MusicFolder musicFolder = new MusicFolder(new File("path"), "name", true, new Date());
        musicFolderDao.createMusicFolder(musicFolder);
        musicFolder = musicFolderDao.getAllMusicFolders().get(0);

        musicFolder.setPath(new File("newPath"));
        musicFolder.setName("newName");
        musicFolder.setEnabled(false);
        musicFolder.setChanged(new Date(234234L));
        musicFolderDao.updateMusicFolder(musicFolder);

        MusicFolder newMusicFolder = musicFolderDao.getAllMusicFolders().get(0);
        assertMusicFolderEquals(musicFolder, newMusicFolder);
    }

    public void testDeleteMusicFolder() {
        assertEquals("Wrong number of music folders.", 0, musicFolderDao.getAllMusicFolders().size());

        musicFolderDao.createMusicFolder(new MusicFolder(new File("path"), "name", true, new Date()));
        assertEquals("Wrong number of music folders.", 1, musicFolderDao.getAllMusicFolders().size());

        musicFolderDao.createMusicFolder(new MusicFolder(new File("path"), "name", true, new Date()));
        assertEquals("Wrong number of music folders.", 2, musicFolderDao.getAllMusicFolders().size());

        musicFolderDao.deleteMusicFolder(musicFolderDao.getAllMusicFolders().get(0).getId());
        assertEquals("Wrong number of music folders.", 1, musicFolderDao.getAllMusicFolders().size());

        musicFolderDao.deleteMusicFolder(musicFolderDao.getAllMusicFolders().get(0).getId());
        assertEquals("Wrong number of music folders.", 0, musicFolderDao.getAllMusicFolders().size());
    }

    private void assertMusicFolderEquals(MusicFolder expected, MusicFolder actual) {
        assertEquals("Wrong name.", expected.getName(), actual.getName());
        assertEquals("Wrong path.", expected.getPath(), actual.getPath());
        assertEquals("Wrong enabled state.", expected.isEnabled(), actual.isEnabled());
        assertEquals("Wrong changed date.", expected.getChanged(), actual.getChanged());
    }


}