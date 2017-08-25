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

import org.junit.Test;

import javax.servlet.RequestDispatcher;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class RESTControllerTestCase extends BaseRESTControllerTestCase {

    // This test is a bit of a hack for
    // https://github.com/spring-projects/spring-boot/issues/5574
    @Test
    public void errorExample() throws Exception {


        this.mockMvc.perform(
                get("/error")
                        .accept(APPLICATION_JSON)
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 400)
                        .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/api/nonexistant")
                        .requestAttr(RequestDispatcher.ERROR_MESSAGE, "No message available"))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("error", is("Bad Request")))
                .andExpect(jsonPath("status", is(400)))
                .andExpect(jsonPath("message", is("No message available")))
                .andExpect(jsonPath("timestamp", isA(Long.class)))
                .andExpect(jsonPath("path", is("/api/nonexistant")))
                .andDo(documentationHandler.document(
                        responseFields(
                                fieldWithPath("error").description("The HTTP error that occurred, e.g. `Bad Request`"),
                                fieldWithPath("message").description("A description of the cause of the error"),
                                fieldWithPath("path").description("The path to which the request was made"),
                                fieldWithPath("status").description("The HTTP status code, e.g. `400`"),
                                fieldWithPath("timestamp").description("The time, in milliseconds, at which the error occurred"))
                        ));
    }

    // TODO 204 seems to generate a "null" body with 4 bytes. Should be 0 bytes...
    @Test
    public void indexExample() throws Exception {
        this.mockMvc.perform(get("/api/").accept(APPLICATION_JSON))
                .andExpect(status().is(204))
                .andDo(documentationHandler.document());
    }

}
