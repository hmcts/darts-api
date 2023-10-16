package uk.gov.hmcts.darts.transcriptions.service;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.JudgeEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionTypeEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionUrgencyEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.testutils.data.CaseTestData.createCaseAt;
import static uk.gov.hmcts.darts.testutils.data.CourthouseTestData.someMinimalCourthouse;
import static uk.gov.hmcts.darts.testutils.data.CourtroomTestData.createCourtRoomAtCourthouse;
import static uk.gov.hmcts.darts.testutils.data.HearingTestData.createHearingWithDefaults;
import static uk.gov.hmcts.darts.testutils.data.JudgeTestData.createJudgeWithName;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.APPROVED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.AWAITING_AUTHORISATION;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.CLOSED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.COMPLETE;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.REJECTED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.REQUESTED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.WITH_TRANSCRIBER;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum.SPECIFIED_TIMES;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionUrgencyEnum.STANDARD;

@SpringBootTest
@ActiveProfiles({"intTest", "h2db"})
@SuppressWarnings({"PMD.ExcessiveImports"})
class TranscriptionServiceIntTest extends IntegrationBase {

    private static final OffsetDateTime CREATED_DATE = OffsetDateTime.parse("2023-07-31T12:00Z");

    @MockBean
    private UserIdentity mockUserIdentity;

    private CourtCaseEntity courtCase;
    private CourtroomEntity courtroom;
    private HearingEntity hearing;
    private UserAccountEntity systemUser;

    @Autowired
    private TranscriptionService transcriptionService;


    @BeforeEach
    void setupData() {
        systemUser = dartsDatabase.getUserAccountStub().getSystemUserAccountEntity();

        when(mockUserIdentity.getEmailAddress()).thenReturn(systemUser.getEmailAddress());
        when(mockUserIdentity.getUserAccount()).thenReturn(systemUser);

        CourthouseEntity courthouse = someMinimalCourthouse();
        courtroom = createCourtRoomAtCourthouse(courthouse);
        courtCase = createCaseAt(courthouse);

        JudgeEntity judge = createJudgeWithName("aJudge");
        var hearingEntity = createHearingWithDefaults(courtCase, courtroom, CREATED_DATE.toLocalDate(), judge);

        hearing = dartsDatabase.save(hearingEntity);
    }

    @Test
    void closeTranscriptionWithOldRequestedStatusReturnsClosedStatus() {
        TranscriptionTypeEntity transcriptionType = dartsDatabase.getTranscriptionStub().getTranscriptionTypeByEnum(SPECIFIED_TIMES);
        TranscriptionStatusEntity requestedTranscriptionStatus = dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(REQUESTED);
        final TranscriptionUrgencyEntity transcriptionUrgency = dartsDatabase.getTranscriptionStub().getTranscriptionUrgencyByEnum(STANDARD);

        TranscriptionEntity transcription = dartsDatabase.getTranscriptionStub()
            .createAndSaveTranscriptionEntity(courtCase, courtroom, hearing, transcriptionType, requestedTranscriptionStatus,
                                              transcriptionUrgency, systemUser);

        final TranscriptionEntity requestedTranscriptionEntity = dartsDatabase.getTranscriptionRepository()
            .findById(transcription.getId()).orElseThrow();
        assertEquals(REQUESTED.getId(), requestedTranscriptionEntity.getTranscriptionStatus().getId());

        requestedTranscriptionEntity.setCreatedDateTime(CREATED_DATE);
        dartsDatabase.save(requestedTranscriptionEntity);

        final TranscriptionEntity transcriptionEntityWithOldCreatedDate = dartsDatabase.getTranscriptionRepository()
            .findById(transcription.getId()).orElseThrow();
        assertEquals(CREATED_DATE, transcriptionEntityWithOldCreatedDate.getCreatedDateTime());

        transcriptionService.closeTranscriptions();

        TranscriptionStatusEntity closedTranscriptionStatus = dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(CLOSED);

        final TranscriptionEntity closedTranscriptionEntity = dartsDatabase.getTranscriptionRepository()
            .findById(transcription.getId()).orElseThrow();
        assertEquals(closedTranscriptionStatus.getId(), closedTranscriptionEntity.getTranscriptionStatus().getId());
    }

    @Test
    void closeTranscriptionWithNewRequestedStatusRemainsUnchanged() {
        TranscriptionTypeEntity transcriptionType = dartsDatabase.getTranscriptionStub().getTranscriptionTypeByEnum(SPECIFIED_TIMES);
        TranscriptionStatusEntity requestedTranscriptionStatus = dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(REQUESTED);
        TranscriptionUrgencyEntity transcriptionUrgency = dartsDatabase.getTranscriptionStub().getTranscriptionUrgencyByEnum(STANDARD);

        TranscriptionEntity transcription = dartsDatabase.getTranscriptionStub()
            .createAndSaveTranscriptionEntity(courtCase, courtroom, hearing, transcriptionType, requestedTranscriptionStatus,
                                              transcriptionUrgency, systemUser);

        final TranscriptionEntity requestedTranscriptionEntity = dartsDatabase.getTranscriptionRepository()
            .findById(transcription.getId()).orElseThrow();
        assertEquals(REQUESTED.getId(), requestedTranscriptionEntity.getTranscriptionStatus().getId());

        transcriptionService.closeTranscriptions();

        final TranscriptionEntity closedTranscriptionEntity = dartsDatabase.getTranscriptionRepository()
            .findById(transcription.getId()).orElseThrow();
        assertEquals(requestedTranscriptionStatus.getId(), closedTranscriptionEntity.getTranscriptionStatus().getId());
    }

    @Test
    void closeTranscriptionWithOldAwaitingAuthorisationStatusReturnsClosedStatus() {
        TranscriptionTypeEntity transcriptionType = dartsDatabase.getTranscriptionStub().getTranscriptionTypeByEnum(SPECIFIED_TIMES);
        TranscriptionStatusEntity awaitingAuthTranscriptionStatus = dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(AWAITING_AUTHORISATION);
        final TranscriptionUrgencyEntity transcriptionUrgency = dartsDatabase.getTranscriptionStub().getTranscriptionUrgencyByEnum(STANDARD);

        TranscriptionEntity transcription = dartsDatabase.getTranscriptionStub()
            .createAndSaveTranscriptionEntity(courtCase, courtroom, hearing, transcriptionType, awaitingAuthTranscriptionStatus,
                                              transcriptionUrgency, systemUser);

        final TranscriptionEntity requestedTranscriptionEntity = dartsDatabase.getTranscriptionRepository()
            .findById(transcription.getId()).orElseThrow();
        assertEquals(AWAITING_AUTHORISATION.getId(), requestedTranscriptionEntity.getTranscriptionStatus().getId());

        requestedTranscriptionEntity.setCreatedDateTime(CREATED_DATE);
        dartsDatabase.save(requestedTranscriptionEntity);

        final TranscriptionEntity transcriptionEntityWithOldCreatedDate = dartsDatabase.getTranscriptionRepository()
            .findById(transcription.getId()).orElseThrow();
        assertEquals(CREATED_DATE, transcriptionEntityWithOldCreatedDate.getCreatedDateTime());

        transcriptionService.closeTranscriptions();

        TranscriptionStatusEntity closedTranscriptionStatus = dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(CLOSED);
        final TranscriptionEntity closedTranscriptionEntity = dartsDatabase.getTranscriptionRepository()
            .findById(transcription.getId()).orElseThrow();
        assertEquals(closedTranscriptionStatus.getId(), closedTranscriptionEntity.getTranscriptionStatus().getId());
    }

    @Test
    void closeTranscriptionWithOldApprovedStatusReturnsClosedStatus() {
        TranscriptionTypeEntity transcriptionType = dartsDatabase.getTranscriptionStub().getTranscriptionTypeByEnum(SPECIFIED_TIMES);
        TranscriptionStatusEntity approvedTranscriptionStatus = dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(APPROVED);
        final TranscriptionUrgencyEntity transcriptionUrgency = dartsDatabase.getTranscriptionStub().getTranscriptionUrgencyByEnum(STANDARD);

        TranscriptionEntity transcription = dartsDatabase.getTranscriptionStub()
            .createAndSaveTranscriptionEntity(courtCase, courtroom, hearing, transcriptionType, approvedTranscriptionStatus,
                                              transcriptionUrgency, systemUser);

        final TranscriptionEntity requestedTranscriptionEntity = dartsDatabase.getTranscriptionRepository()
            .findById(transcription.getId()).orElseThrow();
        assertEquals(APPROVED.getId(), requestedTranscriptionEntity.getTranscriptionStatus().getId());

        requestedTranscriptionEntity.setCreatedDateTime(CREATED_DATE);
        dartsDatabase.save(requestedTranscriptionEntity);

        final TranscriptionEntity transcriptionEntityWithOldCreatedDate = dartsDatabase.getTranscriptionRepository()
            .findById(transcription.getId()).orElseThrow();
        assertEquals(CREATED_DATE, transcriptionEntityWithOldCreatedDate.getCreatedDateTime());

        transcriptionService.closeTranscriptions();

        TranscriptionStatusEntity closedTranscriptionStatus = dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(CLOSED);
        final TranscriptionEntity closedTranscriptionEntity = dartsDatabase.getTranscriptionRepository()
            .findById(transcription.getId()).orElseThrow();
        assertEquals(closedTranscriptionStatus.getId(), closedTranscriptionEntity.getTranscriptionStatus().getId());
    }

    @Test
    void closeTranscriptionWithOldRejectedStatusRemainsUnchanged() {
        TranscriptionTypeEntity transcriptionType = dartsDatabase.getTranscriptionStub().getTranscriptionTypeByEnum(SPECIFIED_TIMES);
        TranscriptionStatusEntity rejectedTranscriptionStatus = dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(REJECTED);
        TranscriptionUrgencyEntity transcriptionUrgency = dartsDatabase.getTranscriptionStub().getTranscriptionUrgencyByEnum(STANDARD);

        TranscriptionEntity transcription = dartsDatabase.getTranscriptionStub()
            .createAndSaveTranscriptionEntity(courtCase, courtroom, hearing, transcriptionType, rejectedTranscriptionStatus,
                                              transcriptionUrgency, systemUser);

        final TranscriptionEntity requestedTranscriptionEntity = dartsDatabase.getTranscriptionRepository()
            .findById(transcription.getId()).orElseThrow();
        assertEquals(REJECTED.getId(), requestedTranscriptionEntity.getTranscriptionStatus().getId());

        requestedTranscriptionEntity.setCreatedDateTime(CREATED_DATE);
        dartsDatabase.save(requestedTranscriptionEntity);

        final TranscriptionEntity transcriptionEntityWithOldCreatedDate = dartsDatabase.getTranscriptionRepository()
            .findById(transcription.getId()).orElseThrow();
        assertEquals(CREATED_DATE, transcriptionEntityWithOldCreatedDate.getCreatedDateTime());

        transcriptionService.closeTranscriptions();

        final TranscriptionEntity closedTranscriptionEntity = dartsDatabase.getTranscriptionRepository()
            .findById(transcription.getId()).orElseThrow();
        assertEquals(rejectedTranscriptionStatus.getId(), closedTranscriptionEntity.getTranscriptionStatus().getId());
    }

    @Test
    void closeTranscriptionWithOldWithTranscriberStatusReturnsClosedStatus() {
        TranscriptionTypeEntity transcriptionType = dartsDatabase.getTranscriptionStub().getTranscriptionTypeByEnum(SPECIFIED_TIMES);
        TranscriptionStatusEntity withTranscriberTranscriptionStatus = dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(WITH_TRANSCRIBER);
        final TranscriptionUrgencyEntity transcriptionUrgency = dartsDatabase.getTranscriptionStub().getTranscriptionUrgencyByEnum(STANDARD);

        TranscriptionEntity transcription = dartsDatabase.getTranscriptionStub()
            .createAndSaveTranscriptionEntity(courtCase, courtroom, hearing, transcriptionType, withTranscriberTranscriptionStatus,
                                              transcriptionUrgency, systemUser);

        final TranscriptionEntity requestedTranscriptionEntity = dartsDatabase.getTranscriptionRepository()
            .findById(transcription.getId()).orElseThrow();
        assertEquals(WITH_TRANSCRIBER.getId(), requestedTranscriptionEntity.getTranscriptionStatus().getId());

        requestedTranscriptionEntity.setCreatedDateTime(CREATED_DATE);
        dartsDatabase.save(requestedTranscriptionEntity);

        final TranscriptionEntity transcriptionEntityWithOldCreatedDate = dartsDatabase.getTranscriptionRepository()
            .findById(transcription.getId()).orElseThrow();
        assertEquals(CREATED_DATE, transcriptionEntityWithOldCreatedDate.getCreatedDateTime());

        transcriptionService.closeTranscriptions();

        TranscriptionStatusEntity closedTranscriptionStatus = dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(CLOSED);
        final TranscriptionEntity closedTranscriptionEntity = dartsDatabase.getTranscriptionRepository()
            .findById(transcription.getId()).orElseThrow();
        assertEquals(closedTranscriptionStatus.getId(), closedTranscriptionEntity.getTranscriptionStatus().getId());
    }

    @Test
    void closeTranscriptionWithOldCompleteStatusRemainsUnchanged() {
        TranscriptionTypeEntity transcriptionType = dartsDatabase.getTranscriptionStub().getTranscriptionTypeByEnum(SPECIFIED_TIMES);
        TranscriptionStatusEntity completeTranscriptionStatus = dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(COMPLETE);
        TranscriptionUrgencyEntity transcriptionUrgency = dartsDatabase.getTranscriptionStub().getTranscriptionUrgencyByEnum(STANDARD);

        TranscriptionEntity transcription = dartsDatabase.getTranscriptionStub()
            .createAndSaveTranscriptionEntity(courtCase, courtroom, hearing, transcriptionType, completeTranscriptionStatus,
                                              transcriptionUrgency, systemUser);

        final TranscriptionEntity requestedTranscriptionEntity = dartsDatabase.getTranscriptionRepository()
            .findById(transcription.getId()).orElseThrow();
        assertEquals(COMPLETE.getId(), requestedTranscriptionEntity.getTranscriptionStatus().getId());

        requestedTranscriptionEntity.setCreatedDateTime(CREATED_DATE);
        dartsDatabase.save(requestedTranscriptionEntity);

        final TranscriptionEntity transcriptionEntityWithOldCreatedDate = dartsDatabase.getTranscriptionRepository()
            .findById(transcription.getId()).orElseThrow();
        assertEquals(CREATED_DATE, transcriptionEntityWithOldCreatedDate.getCreatedDateTime());

        transcriptionService.closeTranscriptions();

        final TranscriptionEntity closedTranscriptionEntity = dartsDatabase.getTranscriptionRepository()
            .findById(transcription.getId()).orElseThrow();
        assertEquals(completeTranscriptionStatus.getId(), closedTranscriptionEntity.getTranscriptionStatus().getId());
    }

    @Test
    void closeTranscriptionWithOldClosedStatusRemainsUnchanged() {
        TranscriptionTypeEntity transcriptionType = dartsDatabase.getTranscriptionStub().getTranscriptionTypeByEnum(SPECIFIED_TIMES);
        TranscriptionStatusEntity closedTranscriptionStatus = dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(CLOSED);
        TranscriptionUrgencyEntity transcriptionUrgency = dartsDatabase.getTranscriptionStub().getTranscriptionUrgencyByEnum(STANDARD);

        TranscriptionEntity transcription = dartsDatabase.getTranscriptionStub()
            .createAndSaveTranscriptionEntity(courtCase, courtroom, hearing, transcriptionType, closedTranscriptionStatus,
                                              transcriptionUrgency, systemUser);

        final TranscriptionEntity requestedTranscriptionEntity = dartsDatabase.getTranscriptionRepository()
            .findById(transcription.getId()).orElseThrow();
        assertEquals(CLOSED.getId(), requestedTranscriptionEntity.getTranscriptionStatus().getId());

        requestedTranscriptionEntity.setCreatedDateTime(CREATED_DATE);
        dartsDatabase.save(requestedTranscriptionEntity);

        final TranscriptionEntity transcriptionEntityWithOldCreatedDate = dartsDatabase.getTranscriptionRepository()
            .findById(transcription.getId()).orElseThrow();
        assertEquals(CREATED_DATE, transcriptionEntityWithOldCreatedDate.getCreatedDateTime());

        transcriptionService.closeTranscriptions();

        final TranscriptionEntity closedTranscriptionEntity = dartsDatabase.getTranscriptionRepository()
            .findById(transcription.getId()).orElseThrow();
        assertEquals(closedTranscriptionStatus.getId(), closedTranscriptionEntity.getTranscriptionStatus().getId());
    }

}
