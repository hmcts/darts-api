package uk.gov.hmcts.darts.audio.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestType;
import uk.gov.hmcts.darts.authorisation.component.Authorisation;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.TransientObjectDirectoryStub;

import java.net.URI;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.APPROVER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.JUDICIARY;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.REQUESTER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSCRIBER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSLATION_QA;

@AutoConfigureMockMvc
class AudioRequestsControllerDeleteAudioRequestIntTest extends IntegrationBase {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private Authorisation authorisation;

    @Autowired
    protected TransientObjectDirectoryStub transientObjectDirectoryStub;

    @Test
    void audioRequestDeleteShouldReturnSuccess() throws Exception {
        var blobId = UUID.randomUUID().toString();

        var requestor = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        var mediaRequestEntity = dartsDatabase.createAndLoadOpenMediaRequestEntity(requestor, AudioRequestType.DOWNLOAD);
        var objectRecordStatusEntity = dartsDataRetrieval.getObjectRecordStatusEntity(STORED);

        var transientObjectDirectoryEntity = transientObjectDirectoryStub.createTransientObjectDirectoryEntity(
            mediaRequestEntity,
            objectRecordStatusEntity,
            blobId
        );
        dartsDatabase.getTransientObjectDirectoryRepository().saveAndFlush(transientObjectDirectoryEntity);

        doNothing().when(authorisation).authoriseByMediaRequestId(
            mediaRequestEntity.getId(),
            Set.of(JUDICIARY, REQUESTER, APPROVER, TRANSCRIBER, TRANSLATION_QA)
        );

        MockHttpServletRequestBuilder requestBuilder = delete(URI.create(
            String.format("/audio-requests/%d", mediaRequestEntity.getId())));

        mockMvc.perform(requestBuilder)
            .andExpect(status().is2xxSuccessful());

        verify(authorisation).authoriseByMediaRequestId(
            mediaRequestEntity.getId(),
            Set.of(JUDICIARY, REQUESTER, APPROVER, TRANSCRIBER, TRANSLATION_QA)
        );
    }

    @Test
    void audioRequestDeleteShouldReturnBadRequestWhenNoRequestIdProvided() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = delete(URI.create("/audio-requests/id"));

        mockMvc.perform(requestBuilder)
            .andExpect(status().isBadRequest())
            .andReturn();

        verifyNoInteractions(authorisation);
    }

}