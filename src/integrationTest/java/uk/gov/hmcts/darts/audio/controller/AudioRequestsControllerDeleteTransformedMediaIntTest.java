package uk.gov.hmcts.darts.audio.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestType;
import uk.gov.hmcts.darts.authorisation.component.Authorisation;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.TransientObjectDirectoryStub;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
@SuppressWarnings({"PMD.VariableDeclarationUsageDistance"})
class AudioRequestsControllerDeleteTransformedMediaIntTest extends IntegrationBase {

    private static final String ENDPOINT_URL = "/audio-requests/transformed_media/{transformed_media_id}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private Authorisation mockAuthorisation;

    @Autowired
    protected TransientObjectDirectoryStub transientObjectDirectoryStub;

    @Test
    void transformedMediaDeleteShouldReturnSuccess() throws Exception {
        var blobId = UUID.randomUUID().toString();

        var requestor = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        var mediaRequestEntity = dartsDatabase.createAndLoadOpenMediaRequestEntity(requestor, AudioRequestType.DOWNLOAD);
        var objectRecordStatusEntity = dartsDatabase.getObjectRecordStatusEntity(STORED);
        var transientObjectDirectoryEntity = dartsDatabase.getTransientObjectDirectoryRepository()
            .saveAndFlush(transientObjectDirectoryStub.createTransientObjectDirectoryEntity(
                mediaRequestEntity,
                objectRecordStatusEntity,
                blobId
            ));

        final Integer transformedMediaId = transientObjectDirectoryEntity.getTransformedMedia().getId();

        doNothing().when(mockAuthorisation).authoriseByTransformedMediaId(
            transformedMediaId,
            Set.of(JUDICIARY, REQUESTER, APPROVER, TRANSCRIBER, TRANSLATION_QA)
        );

        MockHttpServletRequestBuilder requestBuilder = delete(
            ENDPOINT_URL,
            transformedMediaId
        );

        assertFalse(dartsDatabase.getTransformedMediaRepository().findAll().isEmpty());
        assertFalse(dartsDatabase.getTransientObjectDirectoryRepository().findAll().isEmpty());

        mockMvc.perform(requestBuilder)
            .andExpect(status().is2xxSuccessful());

        assertTrue(dartsDatabase.getTransformedMediaRepository().findAll().isEmpty());
        assertTrue(dartsDatabase.getTransientObjectDirectoryRepository().findAll().isEmpty());

        verify(mockAuthorisation).authoriseByTransformedMediaId(
            transformedMediaId,
            Set.of(JUDICIARY, REQUESTER, APPROVER, TRANSCRIBER, TRANSLATION_QA)
        );
    }

    @Test
    void transformedMediaDeleteShouldReturnSuccessOthersExist() throws Exception {
        //only deletes the one requested.

        var blobId = UUID.randomUUID().toString();

        var requestor = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        var mediaRequestEntity = dartsDatabase.createAndLoadOpenMediaRequestEntity(requestor, AudioRequestType.DOWNLOAD);
        var objectRecordStatusEntity = dartsDatabase.getObjectRecordStatusEntity(STORED);
        var transientObjectDirectoryEntity = dartsDatabase.getTransientObjectDirectoryRepository()
            .saveAndFlush(transientObjectDirectoryStub.createTransientObjectDirectoryEntity(
                mediaRequestEntity,
                objectRecordStatusEntity,
                blobId
            ));

        final Integer transformedMediaId = transientObjectDirectoryEntity.getTransformedMedia().getId();

        doNothing().when(mockAuthorisation).authoriseByTransformedMediaId(
            transformedMediaId,
            Set.of(JUDICIARY, REQUESTER, APPROVER, TRANSCRIBER, TRANSLATION_QA)
        );

        MockHttpServletRequestBuilder requestBuilder = delete(
            ENDPOINT_URL,
            transformedMediaId
        );

        assertFalse(dartsDatabase.getTransformedMediaRepository().findAll().isEmpty());
        assertFalse(dartsDatabase.getTransientObjectDirectoryRepository().findAll().isEmpty());

        mockMvc.perform(requestBuilder)
            .andExpect(status().is2xxSuccessful());

        //create extra one
        TransientObjectDirectoryEntity extraTransientObjectDirectoryEntity = dartsDatabase.getTransientObjectDirectoryRepository()
            .saveAndFlush(transientObjectDirectoryStub.createTransientObjectDirectoryEntity(
                mediaRequestEntity,
                objectRecordStatusEntity,
                blobId
            ));

        assertTrue(dartsDatabase.getTransformedMediaRepository().findById(extraTransientObjectDirectoryEntity.getId()).isPresent());

        verify(mockAuthorisation).authoriseByTransformedMediaId(
            transformedMediaId,
            Set.of(JUDICIARY, REQUESTER, APPROVER, TRANSCRIBER, TRANSLATION_QA)
        );
    }

    @Test
    void transformedMediaDeleteShouldReturnBadRequestWhenNoRequestIdProvided() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = delete(
            ENDPOINT_URL,
            "xyz"
        );

        mockMvc.perform(requestBuilder)
            .andExpect(status().isBadRequest())
            .andReturn();

        verifyNoInteractions(mockAuthorisation);
    }

}