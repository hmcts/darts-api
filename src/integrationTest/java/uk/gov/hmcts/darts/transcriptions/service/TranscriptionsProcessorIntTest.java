package uk.gov.hmcts.darts.transcriptions.service;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionTypeEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionUrgencyEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.util.DateConverterUtil;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.APPROVED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.AWAITING_AUTHORISATION;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.CLOSED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.COMPLETE;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.REJECTED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.REQUESTED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.WITH_TRANSCRIBER;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum.SPECIFIED_TIMES;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionUrgencyEnum.STANDARD;

class TranscriptionsProcessorIntTest extends IntegrationBase {

    private static final OffsetDateTime CREATED_DATE = OffsetDateTime.parse("2023-07-31T12:00Z");
    private static final OffsetDateTime SOME_DATE_TIME = OffsetDateTime.parse("2023-09-01T12:00Z");
    private static final String SOME_COURTHOUSE = "SOME-COURTHOUSE";
    private static final String SOME_COURTROOM = "some-courtroom";
    private static final String SOME_CASE_ID = "1";

    @MockitoBean
    private UserIdentity mockUserIdentity;

    private HearingEntity hearing;
    private UserAccountEntity systemUser;

    @Autowired
    private TranscriptionsProcessor transcriptionsProcessor;


    private void setupData() {
        systemUser = dartsDatabase.getUserAccountStub().getSystemUserAccountEntity();

        when(mockUserIdentity.getUserAccount()).thenReturn(systemUser);

        HearingEntity hearingEntity = dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            SOME_CASE_ID,
            SOME_COURTHOUSE,
            SOME_COURTROOM,
            DateConverterUtil.toLocalDateTime(SOME_DATE_TIME)
        );

        hearing = dartsDatabase.save(hearingEntity);
    }

    @Test
    void closeTranscriptionWithOldRequestedStatusReturnsClosedStatus() {
        TranscriptionEntity transcriptionEntity = transactionalUtil.executeInTransaction(() -> {
            setupData();
            TranscriptionTypeEntity transcriptionType = dartsDatabase.getTranscriptionStub().getTranscriptionTypeByEnum(SPECIFIED_TIMES);
            TranscriptionStatusEntity requestedTranscriptionStatus = dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(REQUESTED);
            final TranscriptionUrgencyEntity transcriptionUrgency = dartsDatabase.getTranscriptionStub().getTranscriptionUrgencyByEnum(STANDARD);

            TranscriptionEntity transcription = dartsDatabase.getTranscriptionStub()
                .createAndSaveTranscriptionEntity(hearing, transcriptionType, requestedTranscriptionStatus,
                                                  Optional.of(transcriptionUrgency), systemUser);

            assertEquals(REQUESTED.getId(), transcription.getTranscriptionStatus().getId());
            transcription.setCreatedDateTime(CREATED_DATE);
            return dartsDatabase.save(transcription);
        });

        dartsDatabase.updateCreatedBy(transcriptionEntity, CREATED_DATE);
        final TranscriptionEntity transcriptionEntityWithOldCreatedDate = dartsDatabase.getTranscriptionRepository()
            .findById(transcriptionEntity.getId()).orElseThrow();
        assertEquals(CREATED_DATE, transcriptionEntityWithOldCreatedDate.getCreatedDateTime());

        transcriptionsProcessor.closeTranscriptions(1000);

        TranscriptionStatusEntity closedTranscriptionStatus = dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(CLOSED);

        final TranscriptionEntity closedTranscriptionEntity = dartsDatabase.getTranscriptionRepository()
            .findById(transcriptionEntity.getId()).orElseThrow();
        assertEquals(closedTranscriptionStatus.getId(), closedTranscriptionEntity.getTranscriptionStatus().getId());
    }

    @Test
    void closeTranscriptionWithNewRequestedStatusRemainsUnchanged() {
        TranscriptionEntity transcriptionEntity = transactionalUtil.executeInTransaction(() -> {
            setupData();
            TranscriptionTypeEntity transcriptionType = dartsDatabase.getTranscriptionStub().getTranscriptionTypeByEnum(SPECIFIED_TIMES);
            TranscriptionStatusEntity requestedTranscriptionStatus = dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(REQUESTED);
            TranscriptionUrgencyEntity transcriptionUrgency = dartsDatabase.getTranscriptionStub().getTranscriptionUrgencyByEnum(STANDARD);

            TranscriptionEntity transcription = dartsDatabase.getTranscriptionStub()
                .createAndSaveTranscriptionEntity(hearing, transcriptionType, requestedTranscriptionStatus,
                                                  Optional.of(transcriptionUrgency), systemUser);

            final TranscriptionEntity requestedTranscriptionEntity = dartsDatabase.getTranscriptionRepository()
                .findById(transcription.getId()).orElseThrow();
            assertEquals(REQUESTED.getId(), requestedTranscriptionEntity.getTranscriptionStatus().getId());
            return transcription;
        });
        transcriptionsProcessor.closeTranscriptions(1000);

        final TranscriptionEntity closedTranscriptionEntity = dartsDatabase.getTranscriptionRepository()
            .findById(transcriptionEntity.getId()).orElseThrow();
        assertEquals(REQUESTED.getId(), closedTranscriptionEntity.getTranscriptionStatus().getId());
    }

    @Test
    void closeTranscriptionWithOldAwaitingAuthorisationStatusReturnsClosedStatus() {
        TranscriptionEntity transcriptionEntity = transactionalUtil.executeInTransaction(() -> {
            setupData();
            TranscriptionTypeEntity transcriptionType = dartsDatabase.getTranscriptionStub().getTranscriptionTypeByEnum(SPECIFIED_TIMES);
            TranscriptionStatusEntity awaitingAuthTranscriptionStatus = dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(
                AWAITING_AUTHORISATION);
            final TranscriptionUrgencyEntity transcriptionUrgency = dartsDatabase.getTranscriptionStub().getTranscriptionUrgencyByEnum(STANDARD);

            TranscriptionEntity transcription = dartsDatabase.getTranscriptionStub()
                .createAndSaveTranscriptionEntity(hearing, transcriptionType, awaitingAuthTranscriptionStatus,
                                                  Optional.of(transcriptionUrgency), systemUser);


            assertEquals(AWAITING_AUTHORISATION.getId(), transcription.getTranscriptionStatus().getId());
            transcription.setCreatedDateTime(CREATED_DATE);
            return dartsDatabase.save(transcription);
        });

        dartsDatabase.updateCreatedBy(transcriptionEntity, CREATED_DATE);
        final TranscriptionEntity transcriptionEntityWithOldCreatedDate = dartsDatabase.getTranscriptionRepository()
            .findById(transcriptionEntity.getId()).orElseThrow();
        assertEquals(CREATED_DATE, transcriptionEntityWithOldCreatedDate.getCreatedDateTime());

        transcriptionsProcessor.closeTranscriptions(1000);

        TranscriptionStatusEntity closedTranscriptionStatus = dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(CLOSED);
        final TranscriptionEntity closedTranscriptionEntity = dartsDatabase.getTranscriptionRepository()
            .findById(transcriptionEntity.getId()).orElseThrow();
        assertEquals(closedTranscriptionStatus.getId(), closedTranscriptionEntity.getTranscriptionStatus().getId());
    }

    @Test
    void closeTranscriptionWithOldApprovedStatusReturnsClosedStatus() {
        TranscriptionEntity transcriptionEntity = transactionalUtil.executeInTransaction(() -> {
            setupData();

            TranscriptionTypeEntity transcriptionType = dartsDatabase.getTranscriptionStub().getTranscriptionTypeByEnum(SPECIFIED_TIMES);
            TranscriptionStatusEntity approvedTranscriptionStatus = dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(APPROVED);
            final TranscriptionUrgencyEntity transcriptionUrgency = dartsDatabase.getTranscriptionStub().getTranscriptionUrgencyByEnum(STANDARD);

            TranscriptionEntity transcription = dartsDatabase.getTranscriptionStub()
                .createAndSaveTranscriptionEntity(hearing, transcriptionType, approvedTranscriptionStatus,
                                                  Optional.of(transcriptionUrgency), systemUser);

            assertEquals(APPROVED.getId(), transcription.getTranscriptionStatus().getId());
            transcription.setCreatedDateTime(CREATED_DATE);
            return dartsDatabase.save(transcription);
        });

        dartsDatabase.updateCreatedBy(transcriptionEntity, CREATED_DATE);
        final TranscriptionEntity transcriptionEntityWithOldCreatedDate = dartsDatabase.getTranscriptionRepository()
            .findById(transcriptionEntity.getId()).orElseThrow();
        assertEquals(CREATED_DATE, transcriptionEntityWithOldCreatedDate.getCreatedDateTime());

        transcriptionsProcessor.closeTranscriptions(1000);

        TranscriptionStatusEntity closedTranscriptionStatus = dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(CLOSED);
        final TranscriptionEntity closedTranscriptionEntity = dartsDatabase.getTranscriptionRepository()
            .findById(transcriptionEntity.getId()).orElseThrow();
        assertEquals(closedTranscriptionStatus.getId(), closedTranscriptionEntity.getTranscriptionStatus().getId());
    }

    @Test
    void closeTranscriptionWithOldRejectedStatusRemainsUnchanged() {
        TranscriptionTypeEntity transcriptionType = dartsDatabase.getTranscriptionStub().getTranscriptionTypeByEnum(SPECIFIED_TIMES);
        TranscriptionStatusEntity rejectedTranscriptionStatus = dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(REJECTED);
        TranscriptionUrgencyEntity transcriptionUrgency = dartsDatabase.getTranscriptionStub().getTranscriptionUrgencyByEnum(STANDARD);

        TranscriptionEntity transcription = dartsDatabase.getTranscriptionStub()
            .createAndSaveTranscriptionEntity(hearing, transcriptionType, rejectedTranscriptionStatus,
                                              Optional.of(transcriptionUrgency), systemUser);

        transactionalUtil.executeInTransaction(() -> {
            TranscriptionEntity transcriptionEntity = dartsDatabase.getTranscriptionRepository()
                .findById(transcription.getId()).orElseThrow();
            assertEquals(REJECTED.getId(), transcriptionEntity.getTranscriptionStatus().getId());
            transcriptionEntity.setCreatedDateTime(CREATED_DATE);
            return dartsDatabase.save(transcriptionEntity);
        });

        final TranscriptionEntity transcriptionEntityWithOldCreatedDate = dartsDatabase.getTranscriptionRepository()
            .findById(transcription.getId()).orElseThrow();
        assertEquals(CREATED_DATE, transcriptionEntityWithOldCreatedDate.getCreatedDateTime());

        transcriptionsProcessor.closeTranscriptions(1000);

        final TranscriptionEntity closedTranscriptionEntity = dartsDatabase.getTranscriptionRepository()
            .findById(transcription.getId()).orElseThrow();
        assertEquals(rejectedTranscriptionStatus.getId(), closedTranscriptionEntity.getTranscriptionStatus().getId());
    }

    @Test
    void closeTranscriptionWithOldWithTranscriberStatusReturnsClosedStatus() {
        TranscriptionEntity transcriptionEntity = transactionalUtil.executeInTransaction(() -> {
            setupData();
            TranscriptionTypeEntity transcriptionType = dartsDatabase.getTranscriptionStub().getTranscriptionTypeByEnum(SPECIFIED_TIMES);
            TranscriptionStatusEntity withTranscriberTranscriptionStatus = dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(WITH_TRANSCRIBER);
            final TranscriptionUrgencyEntity transcriptionUrgency = dartsDatabase.getTranscriptionStub().getTranscriptionUrgencyByEnum(STANDARD);

            TranscriptionEntity transcription = dartsDatabase.getTranscriptionStub()
                .createAndSaveTranscriptionEntity(hearing, transcriptionType, withTranscriberTranscriptionStatus,
                                                  Optional.of(transcriptionUrgency), systemUser);


            assertEquals(WITH_TRANSCRIBER.getId(), transcription.getTranscriptionStatus().getId());
            transcription.setCreatedDateTime(CREATED_DATE);
            return dartsDatabase.save(transcription);
        });
        dartsDatabase.updateCreatedBy(transcriptionEntity, CREATED_DATE);
        final TranscriptionEntity transcriptionEntityWithOldCreatedDate = dartsDatabase.getTranscriptionRepository()
            .findById(transcriptionEntity.getId()).orElseThrow();
        assertEquals(CREATED_DATE, transcriptionEntityWithOldCreatedDate.getCreatedDateTime());

        transcriptionsProcessor.closeTranscriptions(1000);

        transactionalUtil.executeInTransaction(() -> {
            final TranscriptionEntity closedTranscriptionEntity = dartsDatabase.getTranscriptionRepository()
                .findById(transcriptionEntity.getId()).orElseThrow();
            assertEquals(CLOSED.getId(), closedTranscriptionEntity.getTranscriptionStatus().getId());
        });
    }

    @Test
    void closeTranscriptionWithOldCompleteStatusRemainsUnchanged() {
        TranscriptionTypeEntity transcriptionType = dartsDatabase.getTranscriptionStub().getTranscriptionTypeByEnum(SPECIFIED_TIMES);
        TranscriptionStatusEntity completeTranscriptionStatus = dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(COMPLETE);
        TranscriptionUrgencyEntity transcriptionUrgency = dartsDatabase.getTranscriptionStub().getTranscriptionUrgencyByEnum(STANDARD);

        TranscriptionEntity transcription = dartsDatabase.getTranscriptionStub()
            .createAndSaveTranscriptionEntity(hearing, transcriptionType, completeTranscriptionStatus,
                                              Optional.of(transcriptionUrgency), systemUser);

        transactionalUtil.executeInTransaction(() -> {
            TranscriptionEntity transcriptionEntity = dartsDatabase.getTranscriptionRepository()
                .findById(transcription.getId()).orElseThrow();
            assertEquals(COMPLETE.getId(), transcriptionEntity.getTranscriptionStatus().getId());
            transcriptionEntity.setCreatedDateTime(CREATED_DATE);
            return dartsDatabase.save(transcriptionEntity);
        });

        final TranscriptionEntity transcriptionEntityWithOldCreatedDate = dartsDatabase.getTranscriptionRepository()
            .findById(transcription.getId()).orElseThrow();
        assertEquals(CREATED_DATE, transcriptionEntityWithOldCreatedDate.getCreatedDateTime());

        transcriptionsProcessor.closeTranscriptions(1000);

        final TranscriptionEntity closedTranscriptionEntity = dartsDatabase.getTranscriptionRepository()
            .findById(transcription.getId()).orElseThrow();
        assertEquals(completeTranscriptionStatus.getId(), closedTranscriptionEntity.getTranscriptionStatus().getId());
    }

    @Test
    void closeTranscriptionWithOldClosedStatusRemainsUnchanged() {
        TranscriptionEntity transcriptionEntity = transactionalUtil.executeInTransaction(() -> {
            setupData();
            TranscriptionTypeEntity transcriptionType = dartsDatabase.getTranscriptionStub().getTranscriptionTypeByEnum(SPECIFIED_TIMES);
            TranscriptionStatusEntity closedTranscriptionStatus = dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(CLOSED);
            TranscriptionUrgencyEntity transcriptionUrgency = dartsDatabase.getTranscriptionStub().getTranscriptionUrgencyByEnum(STANDARD);

            TranscriptionEntity transcription = dartsDatabase.getTranscriptionStub()
                .createAndSaveTranscriptionEntity(hearing, transcriptionType, closedTranscriptionStatus,
                                                  Optional.of(transcriptionUrgency), systemUser);


            assertEquals(CLOSED.getId(), transcription.getTranscriptionStatus().getId());
            transcription.setCreatedDateTime(CREATED_DATE);
            return dartsDatabase.save(transcription);
        });


        dartsDatabase.updateCreatedBy(transcriptionEntity, CREATED_DATE);
        final TranscriptionEntity transcriptionEntityWithOldCreatedDate = dartsDatabase.getTranscriptionRepository()
            .findById(transcriptionEntity.getId()).orElseThrow();
        assertEquals(CREATED_DATE, transcriptionEntityWithOldCreatedDate.getCreatedDateTime());

        transcriptionsProcessor.closeTranscriptions(1000);

        final TranscriptionEntity closedTranscriptionEntity = dartsDatabase.getTranscriptionRepository()
            .findById(transcriptionEntity.getId()).orElseThrow();
        assertEquals(CLOSED.getId(), closedTranscriptionEntity.getTranscriptionStatus().getId());
    }

}