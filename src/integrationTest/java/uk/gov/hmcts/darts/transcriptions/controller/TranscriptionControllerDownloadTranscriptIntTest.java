package uk.gov.hmcts.darts.transcriptions.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.audit.api.AuditApi;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ObjectDirectoryStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.AuthorisationStub;
import uk.gov.hmcts.darts.testutils.stubs.TranscriptionStub;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.audit.api.AuditActivity.DOWNLOAD_TRANSCRIPTION;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.UNSTRUCTURED;
import static uk.gov.hmcts.darts.common.enums.ObjectDirectoryStatusEnum.STORED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.APPROVED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.COMPLETE;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.WITH_TRANSCRIBER;

@AutoConfigureMockMvc
@Transactional
@SuppressWarnings({"PMD.ExcessiveImports"})
class TranscriptionControllerDownloadTranscriptIntTest extends IntegrationBase {

    private static final String URL_TEMPLATE = "/transcriptions/{transcription_id}/document";
    private static final String EXTERNAL_LOCATION_HEADER = "external_location";
    private static final String TRANSCRIPTION_DOCUMENT_ID_HEADER = "transcription_document_id";

    @Autowired
    private AuthorisationStub authorisationStub;
    @Autowired
    private TranscriptionStub transcriptionStub;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserIdentity mockUserIdentity;
    @MockBean
    private AuditApi mockAuditApi;

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
            .recordAudit(DOWNLOAD_TRANSCRIPTION, testUser, transcriptionEntity.getCourtCase());
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
    void downloadTranscriptShouldReturnBadRequestError() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get(URL_TEMPLATE, transcriptionId)
            .header(
                "accept",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            );
        MvcResult mvcResult = mockMvc.perform(requestBuilder)
            .andExpect(status().isBadRequest())
            .andReturn();

        String actualResponse = mvcResult.getResponse().getContentAsString();

        String expectedResponse = """
            {"type":"TRANSCRIPTION_109","title":"Failed to download transcript","status":400}
            """;
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);

        verifyNoInteractions(mockAuditApi);
    }

    @Test
    void downloadTranscriptShouldReturnOkWithMicrosoftWordNew() throws Exception {
        final String fileName = "Test Document.docx";
        final String fileType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        final int fileSize = 11_937;
        final ObjectDirectoryStatusEntity objectDirectoryStatusEntity = dartsDatabase.getObjectDirectoryStatusEntity(
            STORED);
        final ExternalLocationTypeEntity externalLocationTypeEntity = dartsDatabase.getExternalLocationTypeEntity(
            UNSTRUCTURED);
        final UUID externalLocation = UUID.randomUUID();
        final String checksum = "xi/XkzD2HuqTUzDafW8Cgw==";

        transcriptionEntity = transcriptionStub.updateTranscriptionWithDocument(
            transcriptionEntity,
            fileName,
            fileType,
            fileSize,
            testUser,
            objectDirectoryStatusEntity,
            externalLocationTypeEntity,
            externalLocation,
            checksum
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
                EXTERNAL_LOCATION_HEADER,
                externalLocation.toString()
            ))
            .andExpect(header().string(
                TRANSCRIPTION_DOCUMENT_ID_HEADER,
                String.valueOf(transcriptionEntity.getTranscriptionDocumentEntities().get(0).getId())
            ));

        verify(mockAuditApi).recordAudit(DOWNLOAD_TRANSCRIPTION, testUser, transcriptionEntity.getCourtCase());
    }

    @Test
    void downloadTranscriptShouldReturnOkWithMicrosoftWordOld() throws Exception {
        final String fileName = "Test Document.doc";
        final String fileType = "application/msword";
        final int fileSize = 22_528;
        final ObjectDirectoryStatusEntity objectDirectoryStatusEntity = dartsDatabase.getObjectDirectoryStatusEntity(
            STORED);
        final ExternalLocationTypeEntity externalLocationTypeEntity = dartsDatabase.getExternalLocationTypeEntity(
            UNSTRUCTURED);
        final UUID externalLocation = UUID.randomUUID();
        final String checksum = "KQ9vVogyRdsnEvxyNQz77g==";

        transcriptionEntity = transcriptionStub.updateTranscriptionWithDocument(
            transcriptionEntity,
            fileName,
            fileType,
            fileSize,
            testUser,
            objectDirectoryStatusEntity,
            externalLocationTypeEntity,
            externalLocation,
            checksum
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
                EXTERNAL_LOCATION_HEADER,
                externalLocation.toString()
            ))
            .andExpect(header().string(
                TRANSCRIPTION_DOCUMENT_ID_HEADER,
                String.valueOf(transcriptionEntity.getTranscriptionDocumentEntities().get(0).getId())
            ));

        verify(mockAuditApi).recordAudit(DOWNLOAD_TRANSCRIPTION, testUser, transcriptionEntity.getCourtCase());
    }
}
