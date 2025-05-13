package uk.gov.hmcts.darts.audio.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestType;
import uk.gov.hmcts.darts.authorisation.component.Authorisation;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.testutils.GivenBuilder;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.skyscreamer.jsonassert.JSONAssert.assertNotEquals;
import static org.skyscreamer.jsonassert.JSONCompareMode.NON_EXTENSIBLE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.audio.exception.AudioRequestsApiError.MEDIA_REQUEST_NOT_VALID_FOR_USER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.APPROVER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.JUDICIARY;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.REQUESTER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSCRIBER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSLATION_QA;

@AutoConfigureMockMvc
class AudioRequestsControllerUpdateTransformedMediaLastAccessedTimestampIntTest extends IntegrationBase {

    private static final String ENDPOINT_URL = "/audio-requests/transformed_media/{transformed_media_id}";

    @Autowired
    private MockMvc mockMvc;

    private TransformedMediaEntity transformedMediaEntity;
    private MediaRequestEntity mediaRequestEntity;

    @BeforeEach
    void beforeEach() {
        UserAccountEntity requestor = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        mediaRequestEntity = dartsDatabase.createAndLoadOpenMediaRequestEntity(requestor, AudioRequestType.DOWNLOAD);
        transformedMediaEntity = dartsDatabase.getTransformedMediaStub().createTransformedMediaEntity(mediaRequestEntity);
    }

    @Test
    void updateTransformedMediaLastAccessedTimestampShouldReturnSuccess() throws Exception {
        authenticateValid();

        Integer transformedMediaId = transformedMediaEntity.getId();
        MockHttpServletRequestBuilder requestBuilder = patch(ENDPOINT_URL, transformedMediaId);

        mockMvc.perform(requestBuilder)
            .andExpect(status().isNoContent())
            .andReturn();
    }

    @Test
    void updateTransformedMediaLastAccessedTimestampShouldReturnNotFound() throws Exception {
        authenticateValid();
        MockHttpServletRequestBuilder requestBuilder = patch(
            ENDPOINT_URL,
            -999
        );

        mockMvc.perform(requestBuilder)
            .andExpect(header().string("Content-Type", "application/problem+json"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.type").value("AUDIO_REQUESTS_103"));
    }

    @Test
    void updateTransformedMediaLastAccessedTimestampShouldReturnForbiddenErrorWhenRequestorDifferentUser() throws Exception {
        UserAccountEntity userAccount = givenBuilder.anAuthenticatedUserWithRoles(mediaRequestEntity.getHearing().getCourtroom().getCourthouse(), TRANSCRIBER);
        assertFalse(userAccount.getId() == mediaRequestEntity.getCurrentOwner().getId());

        Integer transformedMediaId = transformedMediaEntity.getId();

        MockHttpServletRequestBuilder requestBuilder = patch(ENDPOINT_URL, transformedMediaId);

        MvcResult mvcResult = mockMvc.perform(requestBuilder)
            .andExpect(status().isForbidden())
            .andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();
        String expectedJson = """
            {
              "type":"AUDIO_REQUESTS_101",
              "title":"The audio request is not valid for this user",
              "status":403
            }""";

        assertEquals(expectedJson, actualJson, NON_EXTENSIBLE);
    }

    private void authenticateValid() {
        UserAccountEntity userAccount = givenBuilder.anAuthenticatedUserWithRoles(mediaRequestEntity.getHearing().getCourtroom().getCourthouse(), TRANSCRIBER);
        mediaRequestEntity.setCurrentOwner(userAccount);
        dartsDatabase.save(mediaRequestEntity);
        assertTrue(userAccount.getId() == mediaRequestEntity.getCurrentOwner().getId());
    }
}
