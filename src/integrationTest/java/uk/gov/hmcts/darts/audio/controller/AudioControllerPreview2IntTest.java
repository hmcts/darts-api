package uk.gov.hmcts.darts.audio.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
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
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.LANGUAGE_SHOP_USER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.RCJ_APPEALS;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.REQUESTER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSCRIBER;

@SpringBootTest
@ActiveProfiles({"intTest", "h2db"})
@AutoConfigureMockMvc
class AudioControllerPreview2IntTest extends IntegrationBase {

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
            Set.of(JUDGE, REQUESTER, APPROVER, TRANSCRIBER, LANGUAGE_SHOP_USER, RCJ_APPEALS)
        );
    }

    @Test
    void preview2ShouldReturnSuccess() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = get(URI.create(
            String.format("/audio/preview2/%d", mediaEntity.getId())));

        mockMvc.perform(requestBuilder).andExpect(status().isOk());

        verify(authorisation).authoriseByMediaId(
            mediaEntity.getId(),
            Set.of(JUDGE, REQUESTER, APPROVER, TRANSCRIBER, LANGUAGE_SHOP_USER, RCJ_APPEALS)
        );
    }

    @Test
    void preview2WithRangeFromStartShouldReturnSuccess() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = get(URI.create(
            String.format("/audio/preview2/%d", mediaEntity.getId()))).header("Range", "bytes=0-1023");

        mockMvc.perform(requestBuilder).andExpect(status().isOk());

        verify(authorisation).authoriseByMediaId(
            mediaEntity.getId(),
            Set.of(JUDGE, REQUESTER, APPROVER, TRANSCRIBER, LANGUAGE_SHOP_USER, RCJ_APPEALS)
        );
    }

    @Test
    void preview2WithRangeShouldReturnSuccess() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = get(URI.create(
            String.format("/audio/preview2/%d", mediaEntity.getId()))).header("Range", "bytes=1024-2047");

        mockMvc.perform(requestBuilder).andExpect(status().isOk());

        verify(authorisation).authoriseByMediaId(
            mediaEntity.getId(),
            Set.of(JUDGE, REQUESTER, APPROVER, TRANSCRIBER, LANGUAGE_SHOP_USER, RCJ_APPEALS)
        );
    }

    @Test
    void preview2ShouldReturnErrorWhenNoMediaIdExistsInDatabase() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = get(
            String.format("/audio/preview2/%s", "1234567"));

        mockMvc.perform(requestBuilder)
            .andExpect(header().string("Content-Type", "application/problem+json"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.type").value("AUDIO_101"));
    }
}
