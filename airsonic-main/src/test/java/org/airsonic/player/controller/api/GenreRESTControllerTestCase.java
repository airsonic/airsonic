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

import org.airsonic.player.dao.MediaFileDao;
import org.airsonic.player.domain.Genre;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockUser(username = "admin", password = "admin")
public class GenreRESTControllerTestCase extends BaseRESTControllerTestCase {

    @Autowired
    MediaFileDao mediaFileDao;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void setUp() {
        super.setUp();

        List<Genre> genreList = new ArrayList<>();

        Genre genre1 = new Genre("Rock", 34, 10);
        Genre genre2 = new Genre("Alternative", 21, 2);

        genreList.add(genre1);
        genreList.add(genre2);

        mediaFileDao.updateGenres(genreList);
    }

    @Test
    public void genresListExample() throws Exception {

        FieldDescriptor[] genre = new FieldDescriptor[] {
                fieldWithPath("name").description("Genre Name"),
                fieldWithPath("songCount").description("Songs with the given Genre"),
                fieldWithPath("albumCount").description("Albums with the given Genre"),
        };

        this.mockMvc.perform(
                get("/api/genres").accept(APPLICATION_JSON))
                .andExpect(status().is(200))
                .andDo(documentationHandler.document(
                        responseFields(fieldWithPath("[]").description("Array of genres.")).andWithPrefix("[].", genre)
                ));
    }

}
