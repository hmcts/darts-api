package uk.gov.hmcts.darts.transcriptions.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.ObjectAdminActionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.enums.HiddenReason;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.repository.ObjectAdminActionRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionDocumentRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionStatusRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.SuperAdminUserStub;
import uk.gov.hmcts.darts.testutils.stubs.TransactionDocumentStub;
import uk.gov.hmcts.darts.testutils.stubs.TranscriptionStub;
import uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError;
import uk.gov.hmcts.darts.transcriptions.model.AdminActionRequest;
import uk.gov.hmcts.darts.transcriptions.model.Problem;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionDocumentHideRequest;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionDocumentHideResponse;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class TranscriptionControllerAdminPostTranscriptionIntTest extends IntegrationBase {

    private static final String ENDPOINT_URL = "/admin/transcription-documents/${TRANSACTION_DOCUMENT_ID}/hide";

    @Autowired
    private SuperAdminUserStub superAdminUserStub;

    @Autowired
    private TranscriptionStub transcriptionStub;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private TranscriptionStatusRepository transcriptionStatusRepository;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserIdentity userIdentity;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TransactionDocumentStub transactionDocumentStub;

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

        TranscriptionDocumentHideRequest transcriptionDocumentHideRequest = new TranscriptionDocumentHideRequest();
        transcriptionDocumentHideRequest.setIsHidden(true);

        String comment = "comments";
        String ticketReference = "reference";

        AdminActionRequest adminActionRequest = new AdminActionRequest();
        adminActionRequest.setReasonId(HiddenReason.OTHER_HIDE.getId());
        adminActionRequest.setComments(comment);
        adminActionRequest.setTicketReference(ticketReference);
        transcriptionDocumentHideRequest.setAdminAction(adminActionRequest);

        // run the test
        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT_URL.replace(
                "${TRANSACTION_DOCUMENT_ID}", transcriptionDocumentEntity.getId().toString()))
                                                  .header("Content-Type", "application/json")
                                                  .content(objectMapper.writeValueAsString(transcriptionDocumentHideRequest)))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        TranscriptionDocumentEntity documentEntity = transcriptionDocumentRepository.findById(transcriptionDocumentEntity.getId()).get();
        List<ObjectAdminActionEntity> objectAdminActionEntity = objectAdminActionRepository.findByTranscriptionDocument_Id(transcriptionDocumentEntity.getId());

        TranscriptionDocumentHideResponse transcriptionResponse
            = objectMapper.readValue(mvcResult.getResponse().getContentAsByteArray(), TranscriptionDocumentHideResponse.class);

        // ensure that the database data is contained in the response
        assertEquals(documentEntity.getId(), transcriptionResponse.getId());
        assertEquals(documentEntity.isHidden(), transcriptionResponse.getIsHidden());
        assertEquals(objectAdminActionEntity.getFirst().getId(), transcriptionResponse.getAdminAction().getId());
        assertEquals(objectAdminActionEntity.getFirst().getComments(), transcriptionResponse.getAdminAction().getComments());
        assertEquals(objectAdminActionEntity.getFirst().getTicketReference(), transcriptionResponse.getAdminAction().getTicketReference());
        assertEquals(objectAdminActionEntity.getFirst().getObjectHiddenReason().getId(), transcriptionResponse.getAdminAction().getReasonId());
        assertFalse(objectAdminActionEntity.getFirst().isMarkedForManualDeletion());
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
    void testTranscriptionDocumentHideTwiceFailure() throws Exception {
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

        TranscriptionDocumentHideRequest transcriptionDocumentHideRequest = new TranscriptionDocumentHideRequest();
        transcriptionDocumentHideRequest.setIsHidden(true);

        String comment = "comments";
        String ticketReference = "reference";

        AdminActionRequest adminActionRequest = new AdminActionRequest();
        adminActionRequest.setReasonId(HiddenReason.OTHER_HIDE.getId());
        adminActionRequest.setComments(comment);
        adminActionRequest.setTicketReference(ticketReference);
        transcriptionDocumentHideRequest.setAdminAction(adminActionRequest);

        // run the test
        mockMvc.perform(post(ENDPOINT_URL.replace(
                "${TRANSACTION_DOCUMENT_ID}", transcriptionDocumentEntity.getId().toString()))
                            .header("Content-Type", "application/json")
                            .content(objectMapper.writeValueAsString(transcriptionDocumentHideRequest)))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        MvcResult hideSecondCall = mockMvc.perform(post(ENDPOINT_URL.replace(
                "${TRANSACTION_DOCUMENT_ID}", transcriptionDocumentEntity.getId().toString()))
                                                       .header("Content-Type", "application/json")
                                                       .content(objectMapper.writeValueAsString(transcriptionDocumentHideRequest)))
            .andExpect(status().isConflict())
            .andReturn();

        String content = hideSecondCall.getResponse().getContentAsString();
        Problem problemResponse = objectMapper.readValue(content, Problem.class);
        assertEquals(TranscriptionApiError.TRANSCRIPTION_ALREADY_HIDDEN.getType(), problemResponse.getType());
    }

    @Test
    void testTranscriptionDocumentShowSuccess() throws Exception {
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

        TranscriptionDocumentHideRequest transcriptionDocumentHideRequest = new TranscriptionDocumentHideRequest();
        transcriptionDocumentHideRequest.setIsHidden(true);

        String comment = "comments";
        String ticketReference = "reference";

        AdminActionRequest adminActionRequest = new AdminActionRequest();
        adminActionRequest.setReasonId(HiddenReason.OTHER_HIDE.getId());
        adminActionRequest.setComments(comment);
        adminActionRequest.setTicketReference(ticketReference);
        transcriptionDocumentHideRequest.setAdminAction(adminActionRequest);

        // hide the transcription document
        mockMvc.perform(post(ENDPOINT_URL.replace(
                "${TRANSACTION_DOCUMENT_ID}", transcriptionDocumentEntity.getId().toString()))
                            .header("Content-Type", "application/json")
                            .content(objectMapper.writeValueAsString(transcriptionDocumentHideRequest)))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        transcriptionDocumentHideRequest.setAdminAction(null);
        transcriptionDocumentHideRequest.setIsHidden(false);

        // now show the transcription document
        MvcResult showResult = mockMvc.perform(post(ENDPOINT_URL.replace(
                "${TRANSACTION_DOCUMENT_ID}", transcriptionDocumentEntity.getId().toString()))
                                                   .header("Content-Type", "application/json")
                                                   .content(objectMapper.writeValueAsString(transcriptionDocumentHideRequest)))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        // a follow up show even if already shown will not error
        mockMvc.perform(post(ENDPOINT_URL.replace(
                "${TRANSACTION_DOCUMENT_ID}", transcriptionDocumentEntity.getId().toString()))
                            .header("Content-Type", "application/json")
                            .content(objectMapper.writeValueAsString(transcriptionDocumentHideRequest)))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        // make the assertions against the response
        TranscriptionDocumentHideResponse transcriptionResponse
            = objectMapper.readValue(showResult.getResponse().getContentAsByteArray(), TranscriptionDocumentHideResponse.class);
        TranscriptionDocumentEntity documentEntity = transcriptionDocumentRepository.findById(transcriptionDocumentEntity.getId()).get();

        // ensure no object admin actions exist
        assertTrue(objectAdminActionRepository.findByTranscriptionDocument_Id(transcriptionDocumentEntity.getId()).isEmpty());

        // assert that the action data that existed before deletion is returned
        assertEquals(documentEntity.getId(), transcriptionResponse.getId());
        assertEquals(documentEntity.isHidden(), transcriptionResponse.getIsHidden());
        assertNull(transcriptionResponse.getAdminAction());
    }

    @Test
    void testTranscriptionDocumentShowForbidden() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity, SecurityRoleEnum.SUPER_USER);

        TranscriptionDocumentHideRequest transcriptionDocumentHideRequest = new TranscriptionDocumentHideRequest();

        mockMvc.perform(post(ENDPOINT_URL.replace(
                "${TRANSACTION_DOCUMENT_ID}", Integer.valueOf(-12).toString()))
                            .content(objectMapper.writeValueAsString(transcriptionDocumentHideRequest))
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

        TranscriptionDocumentHideRequest transcriptionDocumentHideRequest = new TranscriptionDocumentHideRequest();
        transcriptionDocumentHideRequest.setIsHidden(true);


        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT_URL.replace(
                "${TRANSACTION_DOCUMENT_ID}", Integer.valueOf(-12).toString()))
                                                  .header("Content-Type", "application/json")
                                                  .content(objectMapper.writeValueAsString(transcriptionDocumentHideRequest)))
            .andExpect(status().isNotFound())
            .andReturn();

        String content = mvcResult.getResponse().getContentAsString();
        Problem problemResponse = objectMapper.readValue(content, Problem.class);
        assertEquals(TranscriptionApiError.TRANSCRIPTION_DOCUMENT_ID_NOT_FOUND.getType(), problemResponse.getType());
    }

    @Test
    void testTranscriptionDocumentIdHideNoAdminAction() throws Exception {
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

        TranscriptionDocumentHideRequest transcriptionDocumentHideRequest = new TranscriptionDocumentHideRequest();
        transcriptionDocumentHideRequest.setIsHidden(true);

        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT_URL.replace(
                "${TRANSACTION_DOCUMENT_ID}", transcriptionDocumentEntity.getId().toString()))
                                                  .content(objectMapper.writeValueAsString(transcriptionDocumentHideRequest))
                                                  .header("Content-Type", "application/json"))

            .andExpect(status().isUnprocessableEntity())
            .andReturn();
        String content = mvcResult.getResponse().getContentAsString();
        Problem problemResponse = objectMapper.readValue(content, Problem.class);
        assertEquals(TranscriptionApiError.TRANSCRIPTION_DOCUMENT_HIDE_ACTION_PAYLOAD_INCORRECT_USAGE.getType(), problemResponse.getType());
    }

    @Test
    void testTranscriptionDocumentIdHideAdminActionWithIncorrectReason() throws Exception {
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

        TranscriptionDocumentHideRequest transcriptionDocumentHideRequest = new TranscriptionDocumentHideRequest();
        transcriptionDocumentHideRequest.setIsHidden(true);

        AdminActionRequest adminActionRequest = new AdminActionRequest();
        Integer invalidReasonId = -121;
        adminActionRequest.setReasonId(invalidReasonId);
        adminActionRequest.setComments("");
        adminActionRequest.setTicketReference("");
        transcriptionDocumentHideRequest.setAdminAction(adminActionRequest);


        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT_URL.replace(
                "${TRANSACTION_DOCUMENT_ID}", transcriptionDocumentEntity.getId().toString()))
                                                  .content(objectMapper.writeValueAsString(transcriptionDocumentHideRequest))
                                                  .header("Content-Type", "application/json"))

            .andExpect(status().isUnprocessableEntity())
            .andReturn();
        String content = mvcResult.getResponse().getContentAsString();
        Problem problemResponse = objectMapper.readValue(content, Problem.class);
        assertEquals(TranscriptionApiError.TRANSCRIPTION_DOCUMENT_HIDE_ACTION_REASON_NOT_FOUND.getType(), problemResponse.getType());
    }

    @Test
    void testTranscriptionDocumentIdHideAdminActionWithDeletedReason() throws Exception {
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

        TranscriptionDocumentHideRequest transcriptionDocumentHideRequest = new TranscriptionDocumentHideRequest();
        transcriptionDocumentHideRequest.setIsHidden(true);

        AdminActionRequest adminActionRequest = new AdminActionRequest();
        Integer deletedReason = HiddenReason.PUBLIC_INTEREST_IMMUNITY.getId();
        adminActionRequest.setReasonId(deletedReason);
        adminActionRequest.setComments("");
        adminActionRequest.setTicketReference("");
        transcriptionDocumentHideRequest.setAdminAction(adminActionRequest);

        mockMvc.perform(post(ENDPOINT_URL.replace(
                "${TRANSACTION_DOCUMENT_ID}", transcriptionDocumentEntity.getId().toString()))
                            .content(objectMapper.writeValueAsString(transcriptionDocumentHideRequest))
                            .header("Content-Type", "application/json"))

            .andExpect(status().is2xxSuccessful())
            .andReturn();
    }

    @Test
    void testTranscriptionDocumentShowWithAdminActionFailure() throws Exception {
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

        TranscriptionDocumentHideRequest transcriptionDocumentHideRequest = new TranscriptionDocumentHideRequest();
        transcriptionDocumentHideRequest.setIsHidden(true);

        String comment = "comments";
        String ticketReference = "reference";

        AdminActionRequest adminActionRequest = new AdminActionRequest();
        adminActionRequest.setReasonId(HiddenReason.OTHER_HIDE.getId());
        adminActionRequest.setComments(comment);
        adminActionRequest.setTicketReference(ticketReference);
        transcriptionDocumentHideRequest.setAdminAction(adminActionRequest);

        // hide the transcription document
        mockMvc.perform(post(ENDPOINT_URL.replace(
                "${TRANSACTION_DOCUMENT_ID}", transcriptionDocumentEntity.getId().toString()))
                            .header("Content-Type", "application/json")
                            .content(objectMapper.writeValueAsString(transcriptionDocumentHideRequest)))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        transcriptionDocumentHideRequest.setIsHidden(false);

        // now show the transcription document
        MvcResult showResult = mockMvc.perform(post(ENDPOINT_URL.replace(
                "${TRANSACTION_DOCUMENT_ID}", transcriptionDocumentEntity.getId().toString()))
                                                   .header("Content-Type", "application/json")
                                                   .content(objectMapper.writeValueAsString(transcriptionDocumentHideRequest)))
            .andExpect(status().isUnprocessableEntity())
            .andReturn();

        String content = showResult.getResponse().getContentAsString();
        Problem problemResponse = objectMapper.readValue(content, Problem.class);
        assertEquals(TranscriptionApiError.TRANSCRIPTION_DOCUMENT_SHOW_ACTION_PAYLOAD_INCORRECT_USAGE.getType(), problemResponse.getType());
    }

}