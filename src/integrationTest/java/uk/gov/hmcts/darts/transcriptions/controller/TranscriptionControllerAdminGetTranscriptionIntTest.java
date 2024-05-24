package uk.gov.hmcts.darts.transcriptions.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionStatusEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.repository.TranscriptionStatusRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.test.common.data.UserAccountTestData;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.SuperAdminUserStub;
import uk.gov.hmcts.darts.testutils.stubs.TranscriptionStub;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;
import uk.gov.hmcts.darts.transcriptions.model.GetTranscriptionDetailAdminResponse;
import uk.gov.hmcts.darts.transcriptions.model.Problem;
import uk.gov.hmcts.darts.usermanagement.exception.UserManagementError;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class TranscriptionControllerAdminGetTranscriptionIntTest extends IntegrationBase {

    private static final String ENDPOINT_URL = "/admin/transcriptions?user_id=${USERID}&requested_at_from=${REQUESTED_FROM}";

    private static final String ENDPOINT_URL_NO_DATE = "/admin/transcriptions?user_id=${USERID}";

    private static final String ENDPOINT_URL_NO_USER = "/admin/transcriptions?requested_at_from=${REQUESTED_FROM}";

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

    @MockBean
    private UserIdentity userIdentity;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getTransactionsForUserWithoutDate() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        CourtroomEntity courtroomAtNewcastleEntity = dartsDatabase.createCourtroomUnlessExists("Newcastle", "room_a");
        HearingEntity headerEntity = dartsDatabase.createHearing(
            courtroomAtNewcastleEntity.getCourthouse().getCourthouseName(),
            courtroomAtNewcastleEntity.getName(),
            "c1",
            LocalDateTime.of(2020, 6, 20, 10, 0, 0)
        );

        TranscriptionEntity transcriptionEntity = transcriptionStub.createTranscription(headerEntity);

        MvcResult mvcResult = mockMvc.perform(get(getEndpointUrl(transcriptionEntity.getCreatedBy().getId().toString(), null)))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        GetTranscriptionDetailAdminResponse[] transcriptionResponses
            = objectMapper.readValue(mvcResult.getResponse().getContentAsByteArray(), GetTranscriptionDetailAdminResponse[].class);

        assertEquals(1, transcriptionResponses.length);
        assertNotNull(transcriptionResponses[0].getHearingDate());
        assertEquals(transcriptionEntity.getId(), transcriptionResponses[0].getTranscriptionId());
        assertTrue(transcriptionResponses[0].getIsManualTranscription());
        assertEquals(headerEntity.getCourtCase().getCaseNumber(), transcriptionResponses[0].getCaseNumber());
        assertEquals(TranscriptionStatusEnum.APPROVED.getId(), transcriptionResponses[0].getTranscriptionStatusId());
        assertEquals(headerEntity.getCourtCase().getCourthouse().getId(),
                                transcriptionResponses[0].getCourthouseId());
        assertEquals(transcriptionEntity.getCreatedDateTime()
                                    .atZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime(), transcriptionResponses[0].getRequestedAt());
    }

    @Test
    void getTransactionsForUserWithoutDateAndWithoutHearing() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        UserAccountEntity userAccountEntity = UserAccountTestData.minimalUserAccount();
        userAccountEntity = userAccountRepository.save(userAccountEntity);

        TranscriptionEntity transcriptionEntity
            = transcriptionStub.createTranscription((HearingEntity) null, userAccountEntity);

        MvcResult mvcResult = mockMvc.perform(get(getEndpointUrl(transcriptionEntity.getCreatedBy().getId().toString(),
                                                       null)))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        GetTranscriptionDetailAdminResponse[] transcriptionResponses = objectMapper
            .readValue(mvcResult.getResponse().getContentAsByteArray(), GetTranscriptionDetailAdminResponse[].class);

        assertEquals(1, transcriptionResponses.length);
        assertNull(transcriptionResponses[0].getHearingDate());
        assertEquals(transcriptionEntity.getId(), transcriptionResponses[0].getTranscriptionId());
        assertTrue(transcriptionResponses[0].getIsManualTranscription());
        assertNull(transcriptionResponses[0].getCaseNumber());
        assertEquals(TranscriptionStatusEnum.APPROVED.getId(), transcriptionResponses[0].getTranscriptionStatusId());
        assertNull(transcriptionResponses[0].getCourthouseId());
        assertEquals(transcriptionEntity.getCreatedDateTime()
                                    .atZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime(), transcriptionResponses[0].getRequestedAt());
    }

    @Test
    void getTransactionsForUserBeyondOrEqualToDate() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        CourtroomEntity courtroomAtNewcastleEntity = dartsDatabase.createCourtroomUnlessExists("Newcastle", "room_a");
        HearingEntity headerEntity = dartsDatabase.createHearing(
            courtroomAtNewcastleEntity.getCourthouse().getCourthouseName(),
            courtroomAtNewcastleEntity.getName(),
            "c1",
            LocalDateTime.of(2020, 6, 20, 10, 0, 0)
        );

        TranscriptionEntity transcriptionEntity = transcriptionStub.createTranscription(headerEntity);

        MvcResult mvcResult = mockMvc.perform(get(getEndpointUrl(transcriptionEntity.getCreatedBy().getId().toString(),
                                                         OffsetDateTime.now().minusMonths(2).toString())))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        GetTranscriptionDetailAdminResponse[] transcriptionResponses = objectMapper
            .readValue(mvcResult.getResponse().getContentAsByteArray(), GetTranscriptionDetailAdminResponse[].class);

        assertEquals(1, transcriptionResponses.length);
        assertNotNull(transcriptionResponses[0].getHearingDate());
        assertEquals(transcriptionEntity.getId(), transcriptionResponses[0].getTranscriptionId());
        assertTrue(transcriptionResponses[0].getIsManualTranscription());
        assertEquals(headerEntity.getCourtCase().getCaseNumber(), transcriptionResponses[0].getCaseNumber());
        assertEquals(TranscriptionStatusEnum.APPROVED.getId(), transcriptionResponses[0].getTranscriptionStatusId());
        assertEquals(headerEntity.getCourtCase().getCourthouse().getId(), transcriptionResponses[0].getCourthouseId());
        assertEquals(transcriptionEntity.getCreatedDateTime()
                                    .atZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime(), transcriptionResponses[0].getRequestedAt());
    }

    @Test
    void getTransactionsForUserBeyondOrEqualToDateWithAssociatedWorkflow() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        CourtroomEntity courtroomAtNewcastleEntity = dartsDatabase.createCourtroomUnlessExists("Newcastle", "room_a");
        HearingEntity headerEntity = dartsDatabase.createHearing(
            courtroomAtNewcastleEntity.getCourthouse().getCourthouseName(),
            courtroomAtNewcastleEntity.getName(),
            "c1",
            LocalDateTime.of(2020, 6, 20, 10, 0, 0)
        );


        TranscriptionStatusEntity statusEntity = transcriptionStub
            .getTranscriptionStatusByEnum(TranscriptionStatusEnum.WITH_TRANSCRIBER);

        TranscriptionEntity transcriptionEntity = transcriptionStub.createTranscription(headerEntity);
        TranscriptionEntity transcriptionEntity1
            = transcriptionStub.createTranscription(headerEntity, transcriptionEntity.getCreatedBy(), TranscriptionStatusEnum.WITH_TRANSCRIBER);


        transcriptionStub.createTranscriptionWorkflowEntity(transcriptionEntity1,
                                                            courtroomAtNewcastleEntity.getCreatedBy(),
                                                            OffsetDateTime.now(),
                                                            statusEntity);

        MvcResult mvcResult = mockMvc.perform(get(getEndpointUrl(transcriptionEntity.getCreatedBy().getId().toString(),
                                                       OffsetDateTime.now().minusMonths(2).toString())))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        GetTranscriptionDetailAdminResponse[] transcriptionResponses
            = objectMapper.readValue(mvcResult.getResponse().getContentAsByteArray(), GetTranscriptionDetailAdminResponse[].class);

        assertEquals(2, transcriptionResponses.length);
        assertNotNull(transcriptionResponses[0].getHearingDate());
        assertEquals(transcriptionEntity.getId(), transcriptionResponses[0].getTranscriptionId());
        assertTrue(transcriptionResponses[0].getIsManualTranscription());
        assertEquals(headerEntity.getCourtCase().getCaseNumber(), transcriptionResponses[0].getCaseNumber());
        assertEquals(TranscriptionStatusEnum.APPROVED.getId(), transcriptionResponses[0].getTranscriptionStatusId());
        assertEquals(headerEntity.getCourtCase().getCourthouse().getId(), transcriptionResponses[0].getCourthouseId());
        assertEquals(transcriptionEntity.getCreatedDateTime()
                                    .atZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime(), transcriptionResponses[0].getRequestedAt());

        assertNotNull(transcriptionResponses[1].getHearingDate());
        assertEquals(transcriptionEntity1.getId(), transcriptionResponses[1].getTranscriptionId());
        assertTrue(transcriptionResponses[1].getIsManualTranscription());
        assertEquals(headerEntity.getCourtCase().getCaseNumber(), transcriptionResponses[1].getCaseNumber());
        assertEquals(TranscriptionStatusEnum.WITH_TRANSCRIBER.getId(), transcriptionResponses[1].getTranscriptionStatusId());
        assertEquals(headerEntity.getCourtCase().getCourthouse().getId(), transcriptionResponses[1].getCourthouseId());
        assertEquals(transcriptionEntity1.getCreatedDateTime()
                                    .atZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime(), transcriptionResponses[1].getRequestedAt());
    }

    @Test
    void getTransactionsForUserBeyondOrEqualToDateUserNotExist() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        CourtroomEntity courtroomAtNewcastleEntity = dartsDatabase.createCourtroomUnlessExists("Newcastle", "room_a");
        HearingEntity headerEntity = dartsDatabase.createHearing(
            courtroomAtNewcastleEntity.getCourthouse().getCourthouseName(),
            courtroomAtNewcastleEntity.getName(),
            "c1",
            LocalDateTime.of(2020, 6, 20, 10, 0, 0)
        );

        transcriptionStub.createTranscription(headerEntity);

        Integer userNotExistId = -500;
        MvcResult mvcResult = mockMvc.perform(get(getEndpointUrl(userNotExistId.toString(),
               OffsetDateTime.now().minusMonths(2).toString())))
            .andExpect(status().isNotFound())
            .andReturn();

        Problem problem = objectMapper.readValue(mvcResult.getResponse().getContentAsString(),
                                                 Problem.class);
        assertEquals(UserManagementError.USER_NOT_FOUND.getErrorTypeNumeric(), problem.getType().toString());
    }

    @Test
    void getTransactionsForUserBeyondOrEqualToDateNoTransactions() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);
        UserAccountEntity userAccountEntity = UserAccountTestData.minimalUserAccount();
        userAccountRepository.save(userAccountEntity);

        MvcResult mvcResult = mockMvc.perform(get(getEndpointUrl(userAccountEntity.getId().toString(),
                                                                 OffsetDateTime.now().minusMonths(2).toString())))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        GetTranscriptionDetailAdminResponse[] transcriptionResponses
            = objectMapper.readValue(mvcResult.getResponse().getContentAsByteArray(), GetTranscriptionDetailAdminResponse[].class);

        assertEquals(0, transcriptionResponses.length);
    }

    @Test
    void getTransactionsForUserBeyondOrEqualToDateCallerForbidden() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity, SecurityRoleEnum.DAR_PC);

        mockMvc.perform(get(getEndpointUrl("2",
                                                                 OffsetDateTime.now().minusMonths(2).toString())))
            .andExpect(status().isForbidden())
            .andReturn();
    }

    @Test
    void getTransactionsForUserBeyondOrEqualToDateNoUserSpecified() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity, SecurityRoleEnum.DAR_PC);

        mockMvc.perform(get(getEndpointUrl(null,
                                                                 OffsetDateTime.now().minusMonths(2).toString())))
            .andExpect(status().isBadRequest())
            .andReturn();
    }

    private String getEndpointUrl(String userId, String dateAndTime) {
        if (dateAndTime != null && userId != null) {
            return ENDPOINT_URL.replace("${USERID}", userId).replace("${REQUESTED_FROM}", dateAndTime);
        } else if (dateAndTime != null && userId == null) {
            return ENDPOINT_URL_NO_USER.replace("${REQUESTED_FROM}", dateAndTime);
        } else  if (dateAndTime == null && userId != null) {
            return ENDPOINT_URL_NO_DATE.replace("${USERID}", userId);
        }

        throw new UnsupportedOperationException("Endpoint URL configuration is not correct");
    }
}