package uk.gov.hmcts.darts.audio.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.audio.service.AudioTransformationServiceGivenBuilder;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles({"intTest", "h2db"})
@AutoConfigureMockMvc
class AudioControllerPreviewIntTest extends IntegrationBase {
    private static final String ENDPOINT = "/audio/preview";

    @Autowired
    private AudioTransformationServiceGivenBuilder given;

    private MediaEntity mediaEntity;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setupData() {
        given.setupTest();
        mediaEntity = given.getMediaEntity1();
        given.externalObjectDirForMedia(mediaEntity);
    }

    @Test
    void previewShouldReturnSuccess() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT)
            .queryParam("media_id", String.valueOf(mediaEntity.getId()));

        mockMvc.perform(requestBuilder).andExpect(status().isOk());

    }

    @Test
    void previewShouldReturnErrorWhenNoMediaIdExistsInDatabase() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT)
            .queryParam("media_id", "1234567");

        mockMvc.perform(requestBuilder)
            .andExpect(header().string("Content-Type", "application/problem+json"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.type").value("AUDIO_101"));
    }
}
