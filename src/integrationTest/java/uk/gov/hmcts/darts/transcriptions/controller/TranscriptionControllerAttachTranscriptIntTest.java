package uk.gov.hmcts.darts.transcriptions.controller;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.audit.api.AuditApi;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.SecurityGroupRepository;
import uk.gov.hmcts.darts.notification.entity.NotificationEntity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.AuthorisationStub;
import uk.gov.hmcts.darts.testutils.stubs.TranscriptionStub;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.audit.api.AuditActivity.COMPLETE_TRANSCRIPTION;
import static uk.gov.hmcts.darts.audit.api.AuditActivity.IMPORT_TRANSCRIPTION;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.INBOUND;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.UNSTRUCTURED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;
import static uk.gov.hmcts.darts.notification.api.NotificationApi.NotificationTemplate.TRANSCRIPTION_AVAILABLE;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.APPROVED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.COMPLETE;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.WITH_TRANSCRIBER;

@AutoConfigureMockMvc
@Transactional
@SuppressWarnings({"PMD.ExcessiveImports"})
class TranscriptionControllerAttachTranscriptIntTest extends IntegrationBase {

    private static final String URL_TEMPLATE = "/transcriptions/{transcription_id}/document";

    @Autowired
    private AuthorisationStub authorisationStub;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserIdentity mockUserIdentity;
    @MockitoBean
    private MultipartProperties mockMultipartProperties;
    @MockitoBean
    private AuditApi mockAuditApi;

    private Integer transcriptionId;
    private Integer testUserId;

    private TranscriptionEntity transcriptionEntity;
    private SecurityGroupEntity transcriptionCompany;

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

        assertEquals(0, dartsDatabase.getTranscriptionCommentRepository().findAll().size());
        transcriptionEntity.getTranscriptionWorkflowEntities()
            .addAll(List.of(approvedTranscriptionWorkflowEntity, withTranscriberTranscriptionWorkflowEntity));
        transcriptionEntity.setTranscriptionStatus(withTranscriberTranscriptionWorkflowEntity.getTranscriptionStatus());
        dartsDatabase.getTranscriptionRepository().save(transcriptionEntity);

        assertEquals(WITH_TRANSCRIBER.getId(), transcriptionEntity.getTranscriptionStatus().getId());
        assertEquals(4, transcriptionEntity.getTranscriptionWorkflowEntities().size());

        transcriptionId = transcriptionEntity.getId();

        SecurityGroupRepository securityGroupRepository = dartsDatabase.getSecurityGroupRepository();
        transcriptionCompany = securityGroupRepository.findById(-4).orElseThrow();
        transcriptionCompany.setCourthouseEntities(Set.of(authorisationStub.getCourthouseEntity()));

        UserAccountEntity testUser = authorisationStub.getSeparateIntegrationUser();
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);
        testUserId = testUser.getId();

        doNothing().when(mockAuditApi)
            .record(IMPORT_TRANSCRIPTION, testUser, transcriptionEntity.getCourtCase());
    }

    @Test
    void attachTranscriptShouldReturnForbiddenError() throws Exception {

        when(mockUserIdentity.getUserAccount()).thenReturn(null);

        MockMultipartFile transcript = new MockMultipartFile(
            "transcript",
            "Test Document.doc",
            "application/msword",
            "Test Document (doc)".getBytes()
        );

        final MvcResult mvcResult = mockMvc.perform(
                multipart(
                    URL_TEMPLATE,
                    transcriptionId
                ).file(transcript))
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
    void attachTranscriptShouldReturnBadRequestErrorWithFileExtensionTypeBlocked() throws Exception {
        setPermissions(authorisationStub.getSeparateIntegrationUser());

        MockMultipartFile transcript = new MockMultipartFile(
            "transcript",
            "Test Document.txt",
            "text/plain",
            "Test Document (txt)".getBytes()
        );

        final MvcResult mvcResult = mockMvc.perform(
                multipart(
                    URL_TEMPLATE,
                    transcriptionId
                ).file(transcript))
            .andExpect(status().isBadRequest())
            .andReturn();

        String actualResponse = mvcResult.getResponse().getContentAsString();

        String expectedResponse = """
            {"type":"TRANSCRIPTION_108","title":"Failed to attach transcript","status":400}
            """;
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);

        verifyNoInteractions(mockAuditApi);
    }

    @Test
    void attachTranscriptShouldReturnBadRequestErrorWithFileSizeLimitExceeded() throws Exception {

        setPermissions(authorisationStub.getSeparateIntegrationUser());

        MockMultipartFile transcript = new MockMultipartFile(
            "transcript",
            "Test Document.doc",
            "application/msword",
            "Test Document exceeding max configured doc size(doc)".getBytes()
        );

        final MvcResult mvcResult = mockMvc.perform(
                multipart(
                    URL_TEMPLATE,
                    transcriptionId
                ).file(transcript))
            .andExpect(status().isBadRequest())
            .andReturn();

        String actualResponse = mvcResult.getResponse().getContentAsString();

        String expectedResponse = """
            {"type":"TRANSCRIPTION_108","title":"Failed to attach transcript","status":400}
            """;
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);

        verifyNoInteractions(mockAuditApi);
    }

    private void setPermissions(UserAccountEntity user) {
        user.getSecurityGroupEntities().clear();
        user.getSecurityGroupEntities().add(transcriptionCompany);
        dartsDatabase.getUserAccountRepository().save(user);
    }

    @Test
    void attachTranscriptShouldReturnOkWithMicrosoftWordNew() throws Exception {
        setPermissions(authorisationStub.getSeparateIntegrationUser());

        MockMultipartFile transcript = new MockMultipartFile(
            "transcript",
            "Test Document.docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "Test Document (docx)".getBytes()
        );

        final MvcResult mvcResult = mockMvc.perform(
                multipart(
                    URL_TEMPLATE,
                    transcriptionId
                ).file(transcript))
            .andExpect(status().isOk())
            .andReturn();

        String actualResponse = mvcResult.getResponse().getContentAsString();

        DocumentContext documentContext = JsonPath.parse(actualResponse);
        Integer transcriptionDocumentId = documentContext.read("$.transcription_document_id");
        assertNotNull(transcriptionDocumentId);
        Integer transcriptionWorkflowId = documentContext.read("$.transcription_workflow_id");
        assertNotNull(transcriptionWorkflowId);

        final TranscriptionEntity completeTranscriptionEntity = dartsDatabase.getTranscriptionRepository()
            .findById(transcriptionId).orElseThrow();
        assertEquals(COMPLETE.getId(), completeTranscriptionEntity.getTranscriptionStatus().getId());
        final List<TranscriptionWorkflowEntity> transcriptionWorkflowEntities = completeTranscriptionEntity.getTranscriptionWorkflowEntities();
        final TranscriptionWorkflowEntity transcriptionWorkflowEntity = transcriptionWorkflowEntities
            .get(transcriptionWorkflowEntities.size() - 1);
        assertEquals(transcriptionWorkflowId, transcriptionWorkflowEntity.getId());
        assertEquals(
            COMPLETE.getId(),
            transcriptionWorkflowEntity.getTranscriptionStatus().getId()
        );
        assertEquals(0, dartsDatabase.getTranscriptionCommentRepository().findAll().size());
        assertEquals(testUserId, transcriptionWorkflowEntity.getWorkflowActor().getId());

        final List<TranscriptionDocumentEntity> transcriptionDocumentEntities = completeTranscriptionEntity.getTranscriptionDocumentEntities();
        assertEquals(1, transcriptionDocumentEntities.size());
        TranscriptionDocumentEntity transcriptionDocumentEntity = transcriptionDocumentEntities.getFirst();
        assertEquals("Test Document.docx", transcriptionDocumentEntity.getFileName());
        assertEquals(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            transcriptionDocumentEntity.getFileType()
        );
        assertTrue(transcriptionDocumentEntity.getFileSize() > 0);

        assertEquals(authorisationStub.getSeparateIntegrationUser(), transcriptionDocumentEntities.getFirst().getLastModifiedBy());

        final List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntities = transcriptionDocumentEntity
            .getExternalObjectDirectoryEntities();
        assertEquals(2, externalObjectDirectoryEntities.size());
        ExternalObjectDirectoryEntity externalObjectDirectoryInboundEntity = externalObjectDirectoryEntities.getFirst();
        assertEquals(STORED.getId(), externalObjectDirectoryInboundEntity.getStatus().getId());
        assertEquals(INBOUND.getId(), externalObjectDirectoryInboundEntity.getExternalLocationType().getId());
        assertNotNull(externalObjectDirectoryInboundEntity.getExternalLocation());
        assertEquals(transcriptionDocumentEntity.getChecksum(), externalObjectDirectoryInboundEntity.getChecksum());

        ExternalObjectDirectoryEntity externalObjectDirectoryUnstructuredEntity = externalObjectDirectoryEntities.get(1);
        assertEquals(STORED.getId(), externalObjectDirectoryUnstructuredEntity.getStatus().getId());
        assertEquals(UNSTRUCTURED.getId(), externalObjectDirectoryUnstructuredEntity.getExternalLocationType().getId());
        assertNotNull(externalObjectDirectoryUnstructuredEntity.getExternalLocation());
        assertEquals(transcriptionDocumentEntity.getChecksum(), externalObjectDirectoryUnstructuredEntity.getChecksum());

        List<NotificationEntity> notificationEntities = dartsDatabase.getNotificationRepository().findAll();
        List<String> templateList = notificationEntities.stream().map(NotificationEntity::getEventId).toList();
        assertTrue(templateList.contains(TRANSCRIPTION_AVAILABLE.toString()));

        verify(mockAuditApi).record(COMPLETE_TRANSCRIPTION, authorisationStub.getSeparateIntegrationUser(), transcriptionEntity.getCourtCase());
        verify(mockAuditApi).record(IMPORT_TRANSCRIPTION, authorisationStub.getSeparateIntegrationUser(), transcriptionEntity.getCourtCase());
    }

    @Test
    void attachTranscriptShouldReturnOkWithMicrosoftWordOld() throws Exception {
        setPermissions(authorisationStub.getSeparateIntegrationUser());

        MockMultipartFile transcript = new MockMultipartFile(
            "transcript",
            "Test Document.doc",
            "application/msword",
            "Test Document (doc)".getBytes()
        );

        final MvcResult mvcResult = mockMvc.perform(
                multipart(
                    URL_TEMPLATE,
                    transcriptionId
                ).file(transcript))
            .andExpect(status().isOk())
            .andReturn();

        String actualResponse = mvcResult.getResponse().getContentAsString();

        DocumentContext documentContext = JsonPath.parse(actualResponse);
        Integer transcriptionDocumentId = documentContext.read("$.transcription_document_id");
        assertNotNull(transcriptionDocumentId);
        Integer transcriptionWorkflowId = documentContext.read("$.transcription_workflow_id");
        assertNotNull(transcriptionWorkflowId);

        final TranscriptionEntity completeTranscriptionEntity = dartsDatabase.getTranscriptionRepository()
            .findById(transcriptionId).orElseThrow();
        assertEquals(COMPLETE.getId(), completeTranscriptionEntity.getTranscriptionStatus().getId());
        final List<TranscriptionWorkflowEntity> transcriptionWorkflowEntities = completeTranscriptionEntity.getTranscriptionWorkflowEntities();
        final TranscriptionWorkflowEntity transcriptionWorkflowEntity = transcriptionWorkflowEntities
            .get(transcriptionWorkflowEntities.size() - 1);
        assertEquals(transcriptionWorkflowId, transcriptionWorkflowEntity.getId());
        assertEquals(
            COMPLETE.getId(),
            transcriptionWorkflowEntity.getTranscriptionStatus().getId()
        );
        assertEquals(0, dartsDatabase.getTranscriptionCommentRepository().findAll().size());
        assertEquals(testUserId, transcriptionWorkflowEntity.getWorkflowActor().getId());

        final List<TranscriptionDocumentEntity> transcriptionDocumentEntities = completeTranscriptionEntity.getTranscriptionDocumentEntities();
        assertEquals(1, transcriptionDocumentEntities.size());
        TranscriptionDocumentEntity transcriptionDocumentEntity = transcriptionDocumentEntities.getFirst();
        assertEquals("Test Document.doc", transcriptionDocumentEntity.getFileName());
        assertEquals("application/msword", transcriptionDocumentEntity.getFileType());
        assertTrue(transcriptionDocumentEntity.getFileSize() > 0);

        final List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntities = transcriptionDocumentEntity
            .getExternalObjectDirectoryEntities();
        assertEquals(2, externalObjectDirectoryEntities.size());
        ExternalObjectDirectoryEntity externalObjectDirectoryInboundEntity = externalObjectDirectoryEntities.getFirst();
        assertEquals(STORED.getId(), externalObjectDirectoryInboundEntity.getStatus().getId());
        assertEquals(INBOUND.getId(), externalObjectDirectoryInboundEntity.getExternalLocationType().getId());
        assertNotNull(externalObjectDirectoryInboundEntity.getExternalLocation());
        assertEquals(transcriptionDocumentEntity.getChecksum(), externalObjectDirectoryInboundEntity.getChecksum());

        ExternalObjectDirectoryEntity externalObjectDirectoryUnstructuredEntity = externalObjectDirectoryEntities.get(1);
        assertEquals(STORED.getId(), externalObjectDirectoryUnstructuredEntity.getStatus().getId());
        assertEquals(UNSTRUCTURED.getId(), externalObjectDirectoryUnstructuredEntity.getExternalLocationType().getId());
        assertNotNull(externalObjectDirectoryUnstructuredEntity.getExternalLocation());
        assertEquals(externalObjectDirectoryUnstructuredEntity.getChecksum(), externalObjectDirectoryUnstructuredEntity.getChecksum());

        List<NotificationEntity> notificationEntities = dartsDatabase.getNotificationRepository().findAll();
        List<String> templateList = notificationEntities.stream().map(NotificationEntity::getEventId).toList();
        assertTrue(templateList.contains(TRANSCRIPTION_AVAILABLE.toString()));

        verify(mockAuditApi).record(COMPLETE_TRANSCRIPTION, authorisationStub.getSeparateIntegrationUser(), transcriptionEntity.getCourtCase());
        verify(mockAuditApi).record(IMPORT_TRANSCRIPTION, authorisationStub.getSeparateIntegrationUser(), transcriptionEntity.getCourtCase());
    }

}
