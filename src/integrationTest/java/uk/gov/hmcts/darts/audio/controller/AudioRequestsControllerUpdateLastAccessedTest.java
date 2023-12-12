package uk.gov.hmcts.darts.audio.controller;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestType;
import uk.gov.hmcts.darts.authorisation.component.Authorisation;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.net.URI;
import java.util.Set;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.audio.exception.AudioRequestsApiError.MEDIA_REQUEST_NOT_VALID_FOR_USER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.APPROVER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.JUDGE;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.LANGUAGE_SHOP_USER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.RCJ_APPEALS;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.REQUESTER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSCRIBER;

@AutoConfigureMockMvc
@Slf4j
class AudioRequestsControllerUpdateLastAccessedTest extends IntegrationBase {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private Authorisation authorisation;

    private UserAccountEntity systemUser;
    private MediaRequestEntity mediaRequestEntity;


    @BeforeEach
    void beforeEach() {
        systemUser = dartsDatabase.getUserAccountStub().getSystemUserAccountEntity();
        UserAccountEntity requestor = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        mediaRequestEntity = dartsDatabase.createAndLoadOpenMediaRequestEntity(requestor, AudioRequestType.DOWNLOAD);
        dartsDatabase.getTransformedMediaStub().createTransformedMediaEntity(mediaRequestEntity);
    }

    @Test
    void updateAudioRequestLastAccessedTimestampReturnSuccess() throws Exception {
        doNothing().when(authorisation).authoriseByMediaRequestId(
            mediaRequestEntity.getId(),
            Set.of(JUDGE, REQUESTER, APPROVER, TRANSCRIBER, LANGUAGE_SHOP_USER, RCJ_APPEALS)
        );
        MockHttpServletRequestBuilder requestBuilder = patch(URI.create(
            String.format("/audio-requests/%d", mediaRequestEntity.getId())));

        mockMvc.perform(requestBuilder)
            .andExpect(status().isNoContent())
            .andReturn();

    }

    @Test
    void updateAudioRequestLastAccessedTimestampWhenRequestorDifferentUserThrowsUnauthorizedError() throws Exception {
        MediaRequestEntity mediaRequestEntityBySystemUser = dartsDatabase.createAndLoadOpenMediaRequestEntity(
            systemUser, AudioRequestType.DOWNLOAD);

        Mockito.doThrow(new DartsApiException(MEDIA_REQUEST_NOT_VALID_FOR_USER))
            .when(authorisation).authoriseByMediaRequestId(anyInt(), anySet());

        MockHttpServletRequestBuilder requestBuilder = patch(URI.create(
            String.format("/audio-requests/%d", mediaRequestEntityBySystemUser.getId())));

        MvcResult mvcResult = mockMvc.perform(requestBuilder)
            .andExpect(status().isUnauthorized())
            .andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();
        log.debug("Result: {}", actualJson);
        String expectedJson = """
            {
              "type":"AUDIO_REQUESTS_101",
              "title":"The audio request is not valid for this user",
              "status":401
            }""";

        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.NON_EXTENSIBLE);

    }
}
