package uk.gov.hmcts.darts.audio.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestType;
import uk.gov.hmcts.darts.audit.api.AuditActivity;
import uk.gov.hmcts.darts.audit.service.AuditService;
import uk.gov.hmcts.darts.authorisation.component.Authorisation;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.datamanagement.enums.DatastoreContainerType;
import uk.gov.hmcts.darts.common.entity.AuditEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.AuditRepository;
import uk.gov.hmcts.darts.datamanagement.exception.FileNotDownloadedException;
import uk.gov.hmcts.darts.datamanagement.service.DataManagementService;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.AuthorisationStub;
import uk.gov.hmcts.darts.testutils.stubs.TransientObjectDirectoryStub;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.audiorequests.model.AudioRequestType.PLAYBACK;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSCRIBER;

@AutoConfigureMockMvc
@SuppressWarnings({"PMD.ExcessiveImports"})
class AudioRequestsControllerDownloadIntTest extends IntegrationBase {

    private static final URI ENDPOINT = URI.create("/audio-requests/download");

    private static final Integer DOWNLOAD_AUDIT_ACTIVITY_ID = AuditActivity.EXPORT_AUDIO.getId();
    @MockBean
    private Authorisation mockAuthorisation;

    @MockBean
    private UserIdentity mockUserIdentity;

    @Autowired
    protected TransientObjectDirectoryStub transientObjectDirectoryStub;

    @Autowired
    private AuthorisationStub authorisationStub;

    @Autowired
    private MockMvc mockMvc;

    @SpyBean
    private DataManagementService dataManagementService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private AuditRepository auditRepository;

    @BeforeEach
    void setUp() {
        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);
    }

    @Test
    @Disabled("Impacted by V1_362__constraint_transcription_part6.sql")
    void audioRequestDownloadShouldDownloadFromOutboundStorageAndReturnSuccess() throws Exception {
        var blobId = UUID.randomUUID();

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

        doNothing().when(mockAuthorisation)
            .authoriseByTransformedMediaId(transformedMediaId, Set.of(TRANSCRIBER));

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT)
            .queryParam("transformed_media_id", String.valueOf(transformedMediaId));

        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andExpect(header().exists("Content-Length"));

        verify(dataManagementService).downloadData(eq(DatastoreContainerType.OUTBOUND), eq("darts-outbound"), any());

        verify(mockAuthorisation).authoriseByTransformedMediaId(
            transformedMediaId,
            Set.of(TRANSCRIBER)
        );

        Integer courtCaseId = mediaRequestEntity.getHearing().getCourtCase().getId();
        OffsetDateTime fromDate = OffsetDateTime.now().minusDays(1);
        OffsetDateTime toDate = OffsetDateTime.now().plusDays(1);
        List<AuditEntity> auditEntities = auditRepository.getAuditEntitiesByCaseAndActivityForDateRange(courtCaseId,
                                                                                                        DOWNLOAD_AUDIT_ACTIVITY_ID,
                                                                                                        fromDate, toDate);

        assertEquals("2", auditEntities.get(0).getCourtCase().getCaseNumber());
        assertEquals(1, auditEntities.size());
    }

    @Test
    @Disabled("Impacted by V1_362__constraint_transcription_part6.sql")
    void audioRequestDownloadShouldReturnInternalServerErrorWhenExceptionDuringDownloadBlobData() throws Exception {
        var blobId = UUID.randomUUID();

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

        doNothing().when(mockAuthorisation)
            .authoriseByTransformedMediaId(transformedMediaId, Set.of(TRANSCRIBER));

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT)
            .queryParam("transformed_media_id", String.valueOf(transformedMediaId));

        when(dataManagementService.downloadData(any(), any(), any())).thenThrow(new FileNotDownloadedException("Bom!"));

        mockMvc.perform(requestBuilder)
            .andExpect(status().isInternalServerError());
    }

    @Test
    void audioRequestDownloadGetShouldReturnNotFound() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT)
            .queryParam("transformed_media_id", "-999");

        mockMvc.perform(requestBuilder)
            .andExpect(header().string("Content-Type", "application/problem+json"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.type").value("AUDIO_REQUESTS_103"));
    }

    @Test
    void audioRequestDownloadGetShouldReturnBadRequestWhenMediaRequestEntityIsPlayback() throws Exception {
        authorisationStub.givenTestSchema();

        var mediaRequestEntity = authorisationStub.getMediaRequestEntity();
        mediaRequestEntity.setRequestType(PLAYBACK);
        dartsDatabase.save(mediaRequestEntity);

        final Integer transformedMediaId = authorisationStub.getTransformedMediaEntity().getId();

        doNothing().when(mockAuthorisation)
            .authoriseByTransformedMediaId(transformedMediaId, Set.of(TRANSCRIBER));

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT)
            .queryParam("transformed_media_id", String.valueOf(transformedMediaId));

        mockMvc.perform(requestBuilder)
            .andExpect(header().string("Content-Type", "application/problem+json"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.type").value("AUDIO_REQUESTS_102"));

        verify(mockAuthorisation).authoriseByTransformedMediaId(
            transformedMediaId,
            Set.of(TRANSCRIBER)
        );
    }

    @Test
    void audioRequestDownloadGetShouldReturnErrorWhenNoRelatedTransientObjectExistsInDatabase() throws Exception {
        authorisationStub.givenTestSchema();

        final Integer transformedMediaId = authorisationStub.getTransformedMediaEntity().getId();

        doNothing().when(mockAuthorisation)
            .authoriseByTransformedMediaId(transformedMediaId, Set.of(TRANSCRIBER));

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT)
            .queryParam("transformed_media_id", String.valueOf(transformedMediaId));

        mockMvc.perform(requestBuilder)
            .andExpect(header().string("Content-Type", "application/problem+json"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.type").value("AUDIO_101"));

        verify(mockAuthorisation).authoriseByTransformedMediaId(
            transformedMediaId,
            Set.of(TRANSCRIBER)
        );
    }

    @Test
    void audioDownloadGetShouldReturnBadRequestWhenNoRequestBodyIsProvided() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT);

        mockMvc.perform(requestBuilder)
            .andExpect(header().string("Content-Type", "application/problem+json"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(mockAuthorisation);
    }

}