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
package org.airsonic.player.controller.api;

import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.service.SettingsService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.Date;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockUser(username = "admin", password = "admin")
public class MusicFolderRESTControllerTestCase extends BaseRESTControllerTestCase {

    @Autowired
    SettingsService settingsService;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void setUp() {
        super.setUp();

        MusicFolder testFolder = new MusicFolder(null, temporaryFolder.getRoot(), "testFolder", true, new Date());
        settingsService.createMusicFolder(testFolder);
    }

    @Test
    public void musicFoldersListExample() throws Exception {

        FieldDescriptor[] musicFolder = new FieldDescriptor[] {
                fieldWithPath("id").description("Music Folder id"),
                fieldWithPath("path").description("Filesystem path"),
                fieldWithPath("name").description("Short name for folder"),
                fieldWithPath("enabled").description("Is the folder enabled"),
                fieldWithPath("changed").description("When the corresponding database entry was last changed")
        };

        this.mockMvc.perform(
                get("/api/musicFolders")
                        .accept(APPLICATION_JSON)
                )
                .andExpect(status().is(200))
                .andDo(documentationHandler.document(
                        // TODO maybe do this in the future
//                        requestParameters(
//                                parameterWithName("includeNonExisting")
//                                        .description("Include folders that are not present. Defaults to false")
//                                        .optional(),
//                                parameterWithName("includeDisabled")
//                                        .description("Include folders that are disabled. Defaults to false")
//                                        .optional()),
                        responseFields(fieldWithPath("[]").description("Array of music folders. " +
                                "Does not include folders that are disabled or not present on the filesystem"))
                                .andWithPrefix("[].", musicFolder)
                ));
    }

}
