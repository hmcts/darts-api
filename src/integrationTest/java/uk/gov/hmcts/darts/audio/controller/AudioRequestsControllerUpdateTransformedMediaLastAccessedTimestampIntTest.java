package uk.gov.hmcts.darts.audio.controller;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestType;
import uk.gov.hmcts.darts.authorisation.component.Authorisation;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.net.URI;
import java.util.Set;

import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
@Slf4j
class AudioRequestsControllerUpdateTransformedMediaLastAccessedTimestampIntTest extends IntegrationBase {

    private static final String ENDPOINT_URL = "/audio-requests/transformed_media/{transformed_media_id}";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private Authorisation mockAuthorisation;

    private TransformedMediaEntity transformedMediaEntity;


    @BeforeEach
    void beforeEach() {
        UserAccountEntity requestor = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        MediaRequestEntity mediaRequestEntity = dartsDatabase.createAndLoadOpenMediaRequestEntity(requestor, AudioRequestType.DOWNLOAD);
        transformedMediaEntity = dartsDatabase.getTransformedMediaStub().createTransformedMediaEntity(mediaRequestEntity);
    }

    @Test
    void updateTransformedMediaLastAccessedTimestampShouldReturnSuccess() throws Exception {
        doNothing().when(mockAuthorisation).authoriseByTransformedMediaId(
            transformedMediaEntity.getId(),
            Set.of(JUDGE, REQUESTER, APPROVER, TRANSCRIBER, TRANSLATION_QA, RCJ_APPEALS)
        );
        MockHttpServletRequestBuilder requestBuilder = patch(URI.create(
            String.format("/audio-requests/transformed_media/%d", transformedMediaEntity.getId())));

        mockMvc.perform(requestBuilder)
            .andExpect(status().isNoContent())
            .andReturn();
    }

    @Test
    void updateTransformedMediaLastAccessedTimestampShouldReturnNotFound() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = patch(
            ENDPOINT_URL,
            -999
        );

        mockMvc.perform(requestBuilder)
            .andExpect(header().string("Content-Type", "application/problem+json"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.type").value("AUDIO_REQUESTS_103"));
    }

}
