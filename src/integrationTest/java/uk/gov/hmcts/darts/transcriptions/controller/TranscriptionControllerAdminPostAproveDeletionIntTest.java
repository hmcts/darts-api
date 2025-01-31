package uk.gov.hmcts.darts.transcriptions.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.darts.audit.api.AuditActivity;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.AuditEntity;
import uk.gov.hmcts.darts.common.entity.AuditEntity_;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.ObjectAdminActionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.repository.ObjectAdminActionRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionDocumentRepository;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.ObjectAdminActionStub;
import uk.gov.hmcts.darts.testutils.stubs.ObjectHiddenReasonStub;
import uk.gov.hmcts.darts.testutils.stubs.SuperAdminUserStub;
import uk.gov.hmcts.darts.testutils.stubs.TransactionDocumentStub;
import uk.gov.hmcts.darts.testutils.stubs.TranscriptionStub;
import uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError;
import uk.gov.hmcts.darts.transcriptions.model.Problem;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionDocumentHideResponse;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class TranscriptionControllerAdminPostAproveDeletionIntTest extends IntegrationBase {

    private static final String ENDPOINT_URL = "/admin/transcription-documents/${TRANSACTION_DOCUMENT_ID}/approve-deletion";

    @Autowired
    private SuperAdminUserStub superAdminUserStub;

    @Autowired
    private TranscriptionStub transcriptionStub;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserIdentity userIdentity;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TransactionDocumentStub transactionDocumentStub;

    @Autowired
    private ObjectAdminActionStub objectAdminActionStub;

    @Autowired
    private ObjectHiddenReasonStub objectHiddenReasonStub;

    @Autowired
    private TranscriptionDocumentRepository transcriptionDocumentRepository;

    @Autowired
    private ObjectAdminActionRepository objectAdminActionRepository;


    @Test
    void testTranscriptionDocumentHideSuccess() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        CourtroomEntity courtroomAtNewcastleEntity = dartsDatabase.createCourtroomUnlessExists("Newcastle", "room_a");
        HearingEntity headerEntity = dartsDatabase.createHearing(
            courtroomAtNewcastleEntity.getCourthouse().getCourthouseName(),
            courtroomAtNewcastleEntity.getName(),
            "c1",
            LocalDateTime.of(2020, 6, 20, 10, 0, 0)
        );

        String fileName = "file";
        String fileType = "fileType";
        Integer fileBytes = 299;
        boolean hidden = true;

        TranscriptionEntity transcriptionEntity = transcriptionStub.createTranscription(headerEntity);
        TranscriptionDocumentEntity transcriptionDocumentEntity = transactionDocumentStub
            .createTranscriptionDocument(fileName, fileBytes, fileType, hidden, transcriptionEntity);

        ObjectAdminActionEntity adminActionEntity = objectAdminActionStub.createAndSave(ObjectAdminActionStub.ObjectAdminActionSpec.builder()
                                                .transcriptionDocument(transcriptionDocumentEntity)
                                                .objectHiddenReason(objectHiddenReasonStub.getAnyWithMarkedForDeletion(true))
                                                .markedForManualDeletion(false)
                                                .markedForManualDelBy(null)
                                                .markedForManualDelDateTime(null)
                                                .build());
        // run the test
        MvcResult mvcResult = mockMvc.perform(post(getUrl(transcriptionDocumentEntity.getId().toString()))
                                                  .header("Content-Type", "application/json"))
            .andExpect(status().is2xxSuccessful())
            .andReturn();


        assertAudit(adminActionEntity);

        TranscriptionDocumentHideResponse transcriptionResponse
            = objectMapper.readValue(mvcResult.getResponse().getContentAsByteArray(), TranscriptionDocumentHideResponse.class);

        TranscriptionDocumentEntity documentEntity = transcriptionDocumentRepository.findById(transcriptionDocumentEntity.getId()).get();
        List<ObjectAdminActionEntity> objectAdminActionEntity = objectAdminActionRepository.findByTranscriptionDocument_Id(transcriptionDocumentEntity.getId());

        // ensure that the database data is contained in the response
        assertEquals(documentEntity.getId(), transcriptionResponse.getId());
        assertEquals(documentEntity.isHidden(), transcriptionResponse.getIsHidden());
        assertEquals(objectAdminActionEntity.getFirst().getId(), transcriptionResponse.getAdminAction().getId());
        assertEquals(objectAdminActionEntity.getFirst().getComments(), transcriptionResponse.getAdminAction().getComments());
        assertEquals(objectAdminActionEntity.getFirst().getTicketReference(), transcriptionResponse.getAdminAction().getTicketReference());
        assertEquals(objectAdminActionEntity.getFirst().getObjectHiddenReason().getId(), transcriptionResponse.getAdminAction().getReasonId());
        assertTrue(objectAdminActionEntity.getFirst().isMarkedForManualDeletion());
        assertEquals(objectAdminActionEntity.getFirst().getHiddenBy().getId(), transcriptionResponse.getAdminAction().getHiddenById());
        assertEquals(objectAdminActionEntity.getFirst()
                         .getHiddenDateTime().truncatedTo(ChronoUnit.SECONDS),
                     transcriptionResponse.getAdminAction().getHiddenAt().truncatedTo(ChronoUnit.SECONDS));
        assertEquals(objectAdminActionEntity.getFirst().getMarkedForManualDelBy().getId(),
                     transcriptionResponse.getAdminAction().getMarkedForManualDeletionById());
        assertEquals(objectAdminActionEntity.getFirst().getMarkedForManualDelDateTime()
                         .truncatedTo(ChronoUnit.SECONDS), transcriptionResponse.getAdminAction()
                         .getMarkedForManualDeletionAt().truncatedTo(ChronoUnit.SECONDS));
    }

    @Test
    void testTranscriptionDocumentApproveDeletionForbidden() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity, SecurityRoleEnum.SUPER_USER);

        mockMvc.perform(post(getUrl("-12"))
                            .header("Content-Type", "application/json"))
            .andExpect(status().isForbidden())
            .andReturn();
    }

    @Test
    void testTranscriptionDocumentIdDoesntExist() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity, SecurityRoleEnum.SUPER_ADMIN);

        CourtroomEntity courtroomAtNewcastleEntity = dartsDatabase.createCourtroomUnlessExists("Newcastle", "room_a");
        dartsDatabase.createHearing(
            courtroomAtNewcastleEntity.getCourthouse().getCourthouseName(),
            courtroomAtNewcastleEntity.getName(),
            "c1",
            LocalDateTime.of(2020, 6, 20, 10, 0, 0)
        );

        MvcResult mvcResult = mockMvc.perform(post(getUrl("-12"))
                                                  .header("Content-Type", "application/json"))
            .andExpect(status().isNotFound())
            .andReturn();

        String content = mvcResult.getResponse().getContentAsString();
        Problem problemResponse = objectMapper.readValue(content, Problem.class);
        assertEquals(TranscriptionApiError.TRANSCRIPTION_DOCUMENT_ID_NOT_FOUND.getType(), problemResponse.getType());
    }

    private static String getUrl(String documentId) {
        return ENDPOINT_URL.replace("${TRANSACTION_DOCUMENT_ID}", Integer.valueOf(documentId).toString());
    }

    private void assertAudit(ObjectAdminActionEntity objectAdminActionEntity) {
        List<AuditEntity> caseExpiredAuditEntries = dartsDatabase.getAuditRepository()
            .findAll((Specification<AuditEntity>) (root, query, criteriaBuilder) -> criteriaBuilder.and(
                criteriaBuilder.equal(root.get(AuditEntity_.additionalData), String.valueOf(objectAdminActionEntity.getId())),
                criteriaBuilder.equal(root.get(AuditEntity_.auditActivity).get("id"), AuditActivity.MANUAL_DELETION.getId())
            ));

        // assert additional audit data
        assertFalse(caseExpiredAuditEntries.isEmpty());
        assertEquals(1, caseExpiredAuditEntries.size());
        assertNotNull(caseExpiredAuditEntries.get(0).getCreatedBy());
        assertNotNull(caseExpiredAuditEntries.get(0).getLastModifiedBy());
        assertNotNull(caseExpiredAuditEntries.get(0).getCreatedDateTime());
        assertNotNull(caseExpiredAuditEntries.get(0).getLastModifiedDateTime());
        assertEquals(userIdentity.getUserAccount().getId(), caseExpiredAuditEntries.get(0).getUser().getId());
        assertNull(caseExpiredAuditEntries.get(0).getCourtCase());
    }

}