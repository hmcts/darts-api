package uk.gov.hmcts.darts.transcriptions.controller;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.core.io.Resource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.audit.api.AuditApi;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.datamanagement.StorageConfiguration;
import uk.gov.hmcts.darts.common.datamanagement.api.DataManagementFacade;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.FileBasedDownloadResponseMetaData;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceScoreEnum;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.AuthorisationStub;
import uk.gov.hmcts.darts.testutils.stubs.TranscriptionStub;

import java.io.File;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.NoSuchFileException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.audit.api.AuditActivity.DOWNLOAD_TRANSCRIPTION;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.UNSTRUCTURED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.APPROVED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.COMPLETE;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.WITH_TRANSCRIBER;

@AutoConfigureMockMvc
@Transactional
class TranscriptionControllerDownloadTranscriptIntTest extends IntegrationBase {

    private static final String URL_TEMPLATE = "/transcriptions/{transcription_id}/document";
    private static final String TRANSCRIPTION_DOCUMENT_ID_HEADER = "transcription_document_id";

    @Autowired
    private AuthorisationStub authorisationStub;
    @Autowired
    private TranscriptionStub transcriptionStub;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserIdentity mockUserIdentity;
    @MockitoBean
    private AuditApi mockAuditApi;
    @MockitoBean
    private DataManagementFacade mockDataManagementFacade;

    @TempDir
    private File tempDirectory;

    private TranscriptionEntity transcriptionEntity;
    private UserAccountEntity testUser;
    private Integer transcriptionId;

    @BeforeEach
    void beforeEach() {
        authorisationStub.givenTestSchema();

        transcriptionEntity = authorisationStub.getTranscriptionEntity();

        TranscriptionStub transcriptionStub = dartsDatabase.getTranscriptionStub();

        TranscriptionWorkflowEntity approvedTranscriptionWorkflowEntity = transcriptionStub.createTranscriptionWorkflowEntity(
            transcriptionEntity,
            transcriptionEntity.getLastModifiedBy(),
            transcriptionEntity.getCreatedDateTime().plusHours(1),
            transcriptionStub.getTranscriptionStatusByEnum(APPROVED)
        );

        TranscriptionWorkflowEntity withTranscriberTranscriptionWorkflowEntity = transcriptionStub.createTranscriptionWorkflowEntity(
            transcriptionEntity,
            transcriptionEntity.getLastModifiedBy(),
            transcriptionEntity.getCreatedDateTime().plusHours(1).plusMinutes(15),
            transcriptionStub.getTranscriptionStatusByEnum(WITH_TRANSCRIBER)
        );

        TranscriptionWorkflowEntity completeTranscriptionWorkflowEntity = transcriptionStub.createTranscriptionWorkflowEntity(
            transcriptionEntity,
            transcriptionEntity.getLastModifiedBy(),
            transcriptionEntity.getCreatedDateTime().plusHours(1).plusMinutes(30),
            transcriptionStub.getTranscriptionStatusByEnum(COMPLETE)
        );

        assertEquals(0, dartsDatabase.getTranscriptionCommentRepository().findAll().size());
        transcriptionEntity.getTranscriptionWorkflowEntities()
            .addAll(List.of(
                approvedTranscriptionWorkflowEntity,
                withTranscriberTranscriptionWorkflowEntity,
                completeTranscriptionWorkflowEntity
            ));
        transcriptionEntity.setTranscriptionStatus(completeTranscriptionWorkflowEntity.getTranscriptionStatus());
        transcriptionEntity = dartsDatabase.getTranscriptionRepository().save(transcriptionEntity);

        assertEquals(COMPLETE.getId(), transcriptionEntity.getTranscriptionStatus().getId());
        assertEquals(5, transcriptionEntity.getTranscriptionWorkflowEntities().size());

        transcriptionId = transcriptionEntity.getId();

        testUser = authorisationStub.getTestUser();
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);

        doNothing().when(mockAuditApi)
            .record(DOWNLOAD_TRANSCRIPTION, testUser, transcriptionEntity.getCourtCase());
    }

    @Test
    void downloadTranscriptShouldReturnForbiddenError() throws Exception {
        when(mockUserIdentity.getUserAccount()).thenReturn(null);

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get(URL_TEMPLATE, transcriptionId)
            .header(
                "accept",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            );
        MvcResult mvcResult = mockMvc.perform(requestBuilder)
            .andExpect(status().isForbidden())
            .andReturn();

        String actualResponse = mvcResult.getResponse().getContentAsString();

        String expectedResponse = """
            {"type":"AUTHORISATION_106","title":"Could not obtain user details","status":403}
            """;
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);

        verifyNoInteractions(mockAuditApi);
    }

    @Test
    void downloadTranscriptShouldReturnNotFoundError() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get(URL_TEMPLATE, transcriptionId)
            .header(
                "accept",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            );
        MvcResult mvcResult = mockMvc.perform(requestBuilder)
            .andExpect(status().isNotFound())
            .andReturn();

        String actualResponse = mvcResult.getResponse().getContentAsString();

        String expectedResponse = """
            {"type":"TRANSCRIPTION_101","title":"The requested transcription cannot be found","status":404}
            """;
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);

        verifyNoInteractions(mockAuditApi);
    }

    @Test
    @SuppressWarnings("PMD.CloseResource")
    void downloadTranscriptShouldReturnOkWithMicrosoftWordNew() throws Exception {
        final String fileName = "Test Document.docx";
        final String fileType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        final int fileSize = 11_937;
        final ObjectRecordStatusEntity storedStatus = dartsDatabase.getObjectRecordStatusEntity(
            STORED);
        final ExternalLocationTypeEntity unstructuredLocation = dartsDatabase.getExternalLocationTypeEntity(
            UNSTRUCTURED);
        final String externalLocation = UUID.randomUUID().toString();
        final String checksum = "xi/XkzD2HuqTUzDafW8Cgw==";
        final String confidenceReason = "reason";
        final RetentionConfidenceScoreEnum confidenceScore = RetentionConfidenceScoreEnum.CASE_PERFECTLY_CLOSED;

        // setup a real file so we can assert against its processing
        StorageConfiguration configuration = new StorageConfiguration();
        configuration.setTempBlobWorkspace(tempDirectory.getAbsolutePath());
        var mockFileBasedDownloadResponseMetaData = new FileBasedDownloadResponseMetaData();
        try (OutputStream outputStream = mockFileBasedDownloadResponseMetaData.getOutputStream(configuration)) {
            outputStream.write("test-transcription".getBytes());
        }

        when(mockDataManagementFacade.retrieveFileFromStorage(any(TranscriptionDocumentEntity.class))).thenReturn(mockFileBasedDownloadResponseMetaData);

        transcriptionEntity = transcriptionStub.updateTranscriptionWithDocument(
            transcriptionEntity,
            fileName,
            fileType,
            fileSize,
            testUser,
            storedStatus,
            unstructuredLocation,
            externalLocation,
            checksum,
            confidenceScore,
            confidenceReason
        );

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get(URL_TEMPLATE, transcriptionId)
            .header(
                "accept",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            );
        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andExpect(header().string(
                CONTENT_DISPOSITION,
                "attachment; filename=\"" + fileName + "\""
            ))
            .andExpect(header().string(
                CONTENT_TYPE,
                fileType
            ))
            .andExpect(header().string(
                TRANSCRIPTION_DOCUMENT_ID_HEADER,
                String.valueOf(transcriptionEntity.getTranscriptionDocumentEntities().get(0).getId())
            ));

        // ensure that the input stream is closed
        try {
            mockFileBasedDownloadResponseMetaData.getResource().getInputStream().read();
            Assertions.fail();
        } catch (Exception closedConnectionException) {
            Assertions.assertInstanceOf(NoSuchFileException.class, closedConnectionException);
        }

        // ensure the source file is removed
        assertEquals(0, new File(configuration.getTempBlobWorkspace()).list().length);
        verify(mockAuditApi).record(DOWNLOAD_TRANSCRIPTION, testUser, transcriptionEntity.getCourtCase());

    }

    @Test
    @SuppressWarnings("PMD.CloseResource")
    void downloadTranscriptShouldReturnOkWithMicrosoftWordOld() throws Exception {
        final String fileName = "Test Document.doc";
        final String fileType = "application/msword";
        final int fileSize = 22_528;
        final ObjectRecordStatusEntity objectRecordStatusEntity = dartsDatabase.getObjectRecordStatusEntity(
            STORED);
        final ExternalLocationTypeEntity externalLocationTypeEntity = dartsDatabase.getExternalLocationTypeEntity(
            UNSTRUCTURED);
        final String externalLocation = UUID.randomUUID().toString();
        final String checksum = "KQ9vVogyRdsnEvxyNQz77g==";
        final RetentionConfidenceScoreEnum confidenceScore = RetentionConfidenceScoreEnum.CASE_PERFECTLY_CLOSED;
        final String confidenceReason = "reason";

        transcriptionEntity = transcriptionStub.updateTranscriptionWithDocument(
            transcriptionEntity,
            fileName,
            fileType,
            fileSize,
            testUser,
            objectRecordStatusEntity,
            externalLocationTypeEntity,
            externalLocation,
            checksum,
            confidenceScore,
            confidenceReason
        );

        var mockFileBasedDownloadResponseMetaData = mock(FileBasedDownloadResponseMetaData.class);
        when(mockDataManagementFacade.retrieveFileFromStorage(any(TranscriptionDocumentEntity.class))).thenReturn(mockFileBasedDownloadResponseMetaData);

        Resource resource = mock(Resource.class);
        when(mockFileBasedDownloadResponseMetaData.getResource()).thenReturn(resource);
        when(resource.getInputStream()).thenReturn(IOUtils.toInputStream("test-transcription", Charset.defaultCharset()));

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get(URL_TEMPLATE, transcriptionId)
            .header(
                "accept",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            );
        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andExpect(header().string(
                CONTENT_DISPOSITION,
                "attachment; filename=\"" + fileName + "\""
            ))
            .andExpect(header().string(
                CONTENT_TYPE,
                fileType
            ))
            .andExpect(header().string(
                TRANSCRIPTION_DOCUMENT_ID_HEADER,
                String.valueOf(transcriptionEntity.getTranscriptionDocumentEntities().get(0).getId())
            ));

        verify(mockAuditApi).record(DOWNLOAD_TRANSCRIPTION, testUser, transcriptionEntity.getCourtCase());
        verify(mockDataManagementFacade).retrieveFileFromStorage(any(TranscriptionDocumentEntity.class));
        verify(mockFileBasedDownloadResponseMetaData).getResource();
        verify(resource).getInputStream();
        verifyNoMoreInteractions(mockAuditApi, mockDataManagementFacade, mockFileBasedDownloadResponseMetaData);
    }

}