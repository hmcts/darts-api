package uk.gov.hmcts.darts.audio.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestType;
import uk.gov.hmcts.darts.authorisation.component.Authorisation;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.TransientObjectDirectoryStub;

import java.net.URI;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.APPROVER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.JUDGE;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.RCJ_APPEALS;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.REQUESTER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSCRIBER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSLATION_QA;

@AutoConfigureMockMvc
@SuppressWarnings({"PMD.VariableDeclarationUsageDistance"})
class AudioRequestsControllerDeleteTransformedMediaIntTest extends IntegrationBase {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private Authorisation authorisation;

    @Autowired
    protected TransientObjectDirectoryStub transientObjectDirectoryStub;

    @Test
    void transformedMediaDeleteShouldReturnSuccess() throws Exception {
        var blobId = UUID.randomUUID();

        var requestor = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        var mediaRequestEntity = dartsDatabase.createAndLoadOpenMediaRequestEntity(requestor, AudioRequestType.DOWNLOAD);
        var objectDirectoryStatusEntity = dartsDatabase.getObjectDirectoryStatusEntity(STORED);
        dartsDatabase.getTransientObjectDirectoryRepository()
            .saveAndFlush(transientObjectDirectoryStub.createTransientObjectDirectoryEntity(
                mediaRequestEntity,
                objectDirectoryStatusEntity,
                blobId
            ));

        doNothing().when(authorisation).authoriseByMediaRequestId(
            mediaRequestEntity.getId(),
            Set.of(JUDGE, REQUESTER, APPROVER, TRANSCRIBER, TRANSLATION_QA, RCJ_APPEALS)
        );

        MockHttpServletRequestBuilder requestBuilder = delete(URI.create(
            String.format("/audio-requests/transformed_media/%d", mediaRequestEntity.getId())));

        assertFalse(dartsDatabase.getTransformedMediaRepository().findAll().isEmpty());
        assertFalse(dartsDatabase.getTransientObjectDirectoryRepository().findAll().isEmpty());

        mockMvc.perform(requestBuilder)
            .andExpect(status().is2xxSuccessful());

        assertTrue(dartsDatabase.getTransformedMediaRepository().findAll().isEmpty());
        assertTrue(dartsDatabase.getTransientObjectDirectoryRepository().findAll().isEmpty());
    }

    @Test
    void transformedMediaDeleteShouldReturnSuccessOthersExist() throws Exception {
        //only deletes the one requested.

        var blobId = UUID.randomUUID();

        var requestor = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        var mediaRequestEntity = dartsDatabase.createAndLoadOpenMediaRequestEntity(requestor, AudioRequestType.DOWNLOAD);
        var objectDirectoryStatusEntity = dartsDatabase.getObjectDirectoryStatusEntity(STORED);
        dartsDatabase.getTransientObjectDirectoryRepository()
            .saveAndFlush(transientObjectDirectoryStub.createTransientObjectDirectoryEntity(
                mediaRequestEntity,
                objectDirectoryStatusEntity,
                blobId
            ));
        //create extra one
        TransientObjectDirectoryEntity extraTransientObjectDirectoryEntity = dartsDatabase.getTransientObjectDirectoryRepository()
            .saveAndFlush(transientObjectDirectoryStub.createTransientObjectDirectoryEntity(
                mediaRequestEntity,
                objectDirectoryStatusEntity,
                blobId
            ));

        doNothing().when(authorisation).authoriseByMediaRequestId(
            mediaRequestEntity.getId(),
            Set.of(JUDGE, REQUESTER, APPROVER, TRANSCRIBER, TRANSLATION_QA, RCJ_APPEALS)
        );

        MockHttpServletRequestBuilder requestBuilder = delete(URI.create(
            String.format("/audio-requests/transformed_media/%d", mediaRequestEntity.getId())));

        assertFalse(dartsDatabase.getTransformedMediaRepository().findAll().isEmpty());
        assertFalse(dartsDatabase.getTransientObjectDirectoryRepository().findAll().isEmpty());

        mockMvc.perform(requestBuilder)
            .andExpect(status().is2xxSuccessful());

        assertTrue(dartsDatabase.getTransformedMediaRepository().findById(extraTransientObjectDirectoryEntity.getId()).isPresent());
    }

    @Test
    void transformedMediaDeleteShouldReturnBadRequestWhenNoRequestIdProvided() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = delete(URI.create("/audio-requests/transformed_media/id"));

        mockMvc.perform(requestBuilder)
            .andExpect(status().isBadRequest())
            .andReturn();

        verifyNoInteractions(authorisation);
    }

}
