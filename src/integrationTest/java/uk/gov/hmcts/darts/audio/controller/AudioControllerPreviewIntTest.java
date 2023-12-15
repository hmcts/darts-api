package uk.gov.hmcts.darts.audio.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.audio.service.AudioTransformationServiceGivenBuilder;
import uk.gov.hmcts.darts.authorisation.component.Authorisation;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.net.URI;
import java.util.Set;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.APPROVER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.JUDGE;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.RCJ_APPEALS;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.REQUESTER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSCRIBER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSLATION_QA;

@AutoConfigureMockMvc
class AudioControllerPreviewIntTest extends IntegrationBase {

    @Autowired
    private AudioTransformationServiceGivenBuilder given;

    @MockBean
    private Authorisation authorisation;

    private MediaEntity mediaEntity;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setupData() {
        given.setupTest();
        mediaEntity = given.getMediaEntity1();
        given.externalObjectDirForMedia(mediaEntity);
        doNothing().when(authorisation).authoriseByMediaId(
            mediaEntity.getId(),
            Set.of(JUDGE, REQUESTER, APPROVER, TRANSCRIBER, TRANSLATION_QA, RCJ_APPEALS)
        );
    }

    @Test
    void previewShouldReturnSuccess() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = get(URI.create(
            String.format("/audio/preview/%d", mediaEntity.getId())));

        mockMvc.perform(requestBuilder).andExpect(status().isOk());

        verify(authorisation).authoriseByMediaId(
            mediaEntity.getId(),
            Set.of(JUDGE, REQUESTER, APPROVER, TRANSCRIBER, TRANSLATION_QA, RCJ_APPEALS)
        );
    }

    @Test
    void previewShouldReturnErrorWhenNoMediaIdExistsInDatabase() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = get(
            String.format("/audio/preview/%s", "1234567"));

        mockMvc.perform(requestBuilder)
            .andExpect(header().string("Content-Type", "application/problem+json"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.type").value("AUDIO_101"));
    }

    @Test
    void previewWithRangeFromStartShouldReturnSuccess() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = get(URI.create(
            String.format("/audio/preview/%d", mediaEntity.getId()))).header("Range", "bytes=0-1023");

        mockMvc.perform(requestBuilder).andExpect(status().isPartialContent());

        verify(authorisation).authoriseByMediaId(
            mediaEntity.getId(),
            Set.of(JUDGE, REQUESTER, APPROVER, TRANSCRIBER, TRANSLATION_QA, RCJ_APPEALS)
        );
    }

    @Test
    void previewWithRangeFromStartWithNoEndShouldReturnSuccess() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = get(URI.create(
            String.format("/audio/preview/%d", mediaEntity.getId()))).header("Range", "bytes=0-");

        mockMvc.perform(requestBuilder).andExpect(status().isPartialContent());

        verify(authorisation).authoriseByMediaId(
            mediaEntity.getId(),
            Set.of(JUDGE, REQUESTER, APPROVER, TRANSCRIBER, TRANSLATION_QA, RCJ_APPEALS)
        );
    }

    @Test
    void previewWithRangeShouldReturnSuccess() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = get(URI.create(
            String.format("/audio/preview/%d", mediaEntity.getId()))).header("Range", "bytes=1024-2047");

        mockMvc.perform(requestBuilder).andExpect(status().isPartialContent());

        verify(authorisation).authoriseByMediaId(
            mediaEntity.getId(),
            Set.of(JUDGE, REQUESTER, APPROVER, TRANSCRIBER, TRANSLATION_QA, RCJ_APPEALS)
        );
    }
}
