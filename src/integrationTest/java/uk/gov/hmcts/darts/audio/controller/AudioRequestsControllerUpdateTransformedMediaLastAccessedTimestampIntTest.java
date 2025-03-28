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
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.util.Set;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
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

    @MockitoBean
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
        Integer transformedMediaId = transformedMediaEntity.getId();
        doNothing().when(mockAuthorisation).authoriseByTransformedMediaId(
            transformedMediaId,
            Set.of(JUDICIARY, REQUESTER, APPROVER, TRANSCRIBER, TRANSLATION_QA)
        );
        doNothing().when(mockAuthorisation).authoriseTransformedMediaAgainstUser(transformedMediaId);
        MockHttpServletRequestBuilder requestBuilder = patch(ENDPOINT_URL, transformedMediaId);

        mockMvc.perform(requestBuilder)
            .andExpect(status().isNoContent())
            .andReturn();

        verify(mockAuthorisation).authoriseByTransformedMediaId(
            transformedMediaEntity.getId(),
            Set.of(JUDICIARY, REQUESTER, APPROVER, TRANSCRIBER, TRANSLATION_QA)
        );
        verify(mockAuthorisation).authoriseTransformedMediaAgainstUser(transformedMediaId);
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

    @Test
    void updateTransformedMediaLastAccessedTimestampShouldReturnForbiddenErrorWhenRequestorDifferentUser() throws Exception {
        Integer transformedMediaId = transformedMediaEntity.getId();
        doNothing().when(mockAuthorisation).authoriseByTransformedMediaId(
            transformedMediaEntity.getId(),
            Set.of(JUDICIARY, REQUESTER, APPROVER, TRANSCRIBER, TRANSLATION_QA)
        );

        doThrow(new DartsApiException(MEDIA_REQUEST_NOT_VALID_FOR_USER))
            .when(mockAuthorisation).authoriseTransformedMediaAgainstUser(transformedMediaId);

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

        verify(mockAuthorisation).authoriseByTransformedMediaId(
            transformedMediaEntity.getId(),
            Set.of(JUDICIARY, REQUESTER, APPROVER, TRANSCRIBER, TRANSLATION_QA)
        );
        verify(mockAuthorisation).authoriseTransformedMediaAgainstUser(transformedMediaId);
    }

}
