package uk.gov.hmcts.darts.transcriptions.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
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
import uk.gov.hmcts.darts.common.repository.TranscriptionStatusRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.data.UserAccountTestData;
import uk.gov.hmcts.darts.testutils.stubs.SuperAdminUserStub;
import uk.gov.hmcts.darts.testutils.stubs.TranscriptionStub;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;
import uk.gov.hmcts.darts.transcriptions.model.GetTranscriptionDetailResponse;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class TranscriptionIntTest extends IntegrationBase {

    private static final String ENDPOINT_URL = "/admin/transcriptions?user_id=${USERID}&requested_at_from=${REQUESTED_FROM}";

    private static final String ENDPOINT_URL_NO_DATE = "/admin/transcriptions?user_id=${USERID}";

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

        MvcResult mvcResult = mockMvc.perform(get(getEndpointUrl(transcriptionEntity.getCreatedBy().getId().toString(),
                                                       null)))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        GetTranscriptionDetailResponse[] transcriptionResponses
            = objectMapper.readValue(mvcResult.getResponse().getContentAsByteArray(), GetTranscriptionDetailResponse[].class);

        Assertions.assertEquals(1, transcriptionResponses.length);
        Assertions.assertNotNull(transcriptionResponses[0].getHearingDate());
        Assertions.assertEquals(transcriptionEntity.getId(), transcriptionResponses[0].getTranscriptionId());
        Assertions.assertTrue(transcriptionResponses[0].getIsManualTranscription());
        Assertions.assertEquals(headerEntity.getCourtCase().getCaseNumber(), transcriptionResponses[0].getCaseNumber());
        Assertions.assertEquals(TranscriptionStatusEnum.APPROVED.getId(), transcriptionResponses[0].getTranscriptionStatusId());
        Assertions.assertEquals(headerEntity.getCourtCase().getCourthouse().getId(),
                                transcriptionResponses[0].getCourthouseId());
        Assertions.assertEquals(transcriptionEntity.getCreatedDateTime()
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

        GetTranscriptionDetailResponse[] transcriptionResponses = objectMapper
            .readValue(mvcResult.getResponse().getContentAsByteArray(), GetTranscriptionDetailResponse[].class);

        Assertions.assertEquals(1, transcriptionResponses.length);
        Assertions.assertNull(transcriptionResponses[0].getHearingDate());
        Assertions.assertEquals(transcriptionEntity.getId(), transcriptionResponses[0].getTranscriptionId());
        Assertions.assertTrue(transcriptionResponses[0].getIsManualTranscription());
        Assertions.assertNull(transcriptionResponses[0].getCaseNumber());
        Assertions.assertEquals(TranscriptionStatusEnum.APPROVED.getId(), transcriptionResponses[0].getTranscriptionStatusId());
        Assertions.assertNull(transcriptionResponses[0].getCourthouseId());
        Assertions.assertEquals(transcriptionEntity.getCreatedDateTime()
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

        GetTranscriptionDetailResponse[] transcriptionResponses = objectMapper
            .readValue(mvcResult.getResponse().getContentAsByteArray(), GetTranscriptionDetailResponse[].class);

        Assertions.assertEquals(1, transcriptionResponses.length);
        Assertions.assertNotNull(transcriptionResponses[0].getHearingDate());
        Assertions.assertEquals(transcriptionEntity.getId(), transcriptionResponses[0].getTranscriptionId());
        Assertions.assertTrue(transcriptionResponses[0].getIsManualTranscription());
        Assertions.assertEquals(headerEntity.getCourtCase().getCaseNumber(), transcriptionResponses[0].getCaseNumber());
        Assertions.assertEquals(TranscriptionStatusEnum.APPROVED.getId(), transcriptionResponses[0].getTranscriptionStatusId());
        Assertions.assertEquals(headerEntity.getCourtCase().getCourthouse().getId(), transcriptionResponses[0].getCourthouseId());
        Assertions.assertEquals(transcriptionEntity.getCreatedDateTime()
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

        GetTranscriptionDetailResponse[] transcriptionResponses
            = objectMapper.readValue(mvcResult.getResponse().getContentAsByteArray(), GetTranscriptionDetailResponse[].class);

        Assertions.assertEquals(2, transcriptionResponses.length);
        Assertions.assertNotNull(transcriptionResponses[0].getHearingDate());
        Assertions.assertEquals(transcriptionEntity.getId(), transcriptionResponses[0].getTranscriptionId());
        Assertions.assertTrue(transcriptionResponses[0].getIsManualTranscription());
        Assertions.assertEquals(headerEntity.getCourtCase().getCaseNumber(), transcriptionResponses[0].getCaseNumber());
        Assertions.assertEquals(TranscriptionStatusEnum.APPROVED.getId(), transcriptionResponses[0].getTranscriptionStatusId());
        Assertions.assertEquals(headerEntity.getCourtCase().getCourthouse().getId(), transcriptionResponses[0].getCourthouseId());
        Assertions.assertEquals(transcriptionEntity.getCreatedDateTime()
                                    .atZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime(), transcriptionResponses[0].getRequestedAt());

        Assertions.assertNotNull(transcriptionResponses[1].getHearingDate());
        Assertions.assertEquals(transcriptionEntity1.getId(), transcriptionResponses[1].getTranscriptionId());
        Assertions.assertTrue(transcriptionResponses[1].getIsManualTranscription());
        Assertions.assertEquals(headerEntity.getCourtCase().getCaseNumber(), transcriptionResponses[1].getCaseNumber());
        Assertions.assertEquals(TranscriptionStatusEnum.WITH_TRANSCRIBER.getId(), transcriptionResponses[1].getTranscriptionStatusId());
        Assertions.assertEquals(headerEntity.getCourtCase().getCourthouse().getId(), transcriptionResponses[1].getCourthouseId());
        Assertions.assertEquals(transcriptionEntity1.getCreatedDateTime()
                                    .atZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime(), transcriptionResponses[1].getRequestedAt());
    }

    @Test
    void getTransactionsForUserBeyondOrEqualToDateNoResults() throws Exception {
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
        mockMvc.perform(get(getEndpointUrl(userNotExistId.toString(),
               OffsetDateTime.now().minusMonths(2).toString())))
            .andExpect(status().isNotFound())
            .andReturn();
    }

    @Test
    void getTransactionsForUserBeyondOrEqualToDateAuthorisationFailure() throws Exception {

    }

    private String getEndpointUrl(String userId, String dateAndTime) {
        if (dateAndTime != null) {
            return ENDPOINT_URL.replace("${USERID}", userId).replace("${REQUESTED_FROM}", dateAndTime);
        } else {
            return ENDPOINT_URL_NO_DATE.replace("${USERID}", userId);
        }
    }
}