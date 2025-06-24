package uk.gov.hmcts.darts.common.repository;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.test.common.data.UserAccountTestData;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.WITH_TRANSCRIBER;

class TranscriptionWorkflowRepositoryTest extends IntegrationBase {
    @Autowired
    private TranscriptionWorkflowRepository transcriptionWorkflowRepository;

    @Autowired
    private TranscriptionRepository transcriptionRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    private UserAccountEntity accountEntity;

    private HearingEntity headerEntity;

    private CourtCaseEntity courtCase;

    private TranscriptionEntity transcriptionEntityWithHearing;

    private TranscriptionEntity transcriptionEntityWithCase;

    private void setup() {
        accountEntity = UserAccountTestData.minimalUserAccount();
        userAccountRepository.save(accountEntity);

        CourtroomEntity courtroomEntity = dartsDatabase.createCourtroomUnlessExists("Newcastle", "room_a");
        headerEntity = dartsDatabase.createHearing(
            courtroomEntity.getCourthouse().getCourthouseName(),
            courtroomEntity.getName(),
            "c1",
            LocalDateTime.of(2020, 6, 20, 10, 0, 0)
        );

        courtCase = headerEntity.getCourtCase();
    }

    @Test
    void testFindWorkflowForUserWithTranscriptionState_shouldReturnTranscriptions_whenLinkedHearing() {
        setup();
        transcriptionEntityWithHearing = PersistableFactory.getTranscriptionTestData().someMinimalBuilderHolder()
            .getBuilder()
            .requestedBy(accountEntity)
            .hearings(List.of(headerEntity))
            .isManualTranscription(true)
            .build().getEntity();
            createTranscriptionWorkflow(accountEntity, OffsetDateTime.now(), WITH_TRANSCRIBER, transcriptionEntityWithHearing);
        transactionalUtil.executeInTransaction(() -> {
            List<TranscriptionEntity> fndTranscriptionLst = transcriptionWorkflowRepository
                .findWorkflowForUserWithTranscriptionState(accountEntity.getId(), transcriptionEntityWithHearing.getTranscriptionStatus().getId());
            assertEquals(transcriptionEntityWithHearing.getCourtCase().getCaseNumber(),
                         fndTranscriptionLst.getFirst().getCourtCase().getCaseNumber());
            assertEquals(1, fndTranscriptionLst.size());
            assertEquals(transcriptionEntityWithHearing.getId(), fndTranscriptionLst.getFirst().getId());
            assertNotNull(transcriptionEntityWithHearing.getHearing().getHearingDate());
            assertEquals(transcriptionEntityWithHearing.getId(), fndTranscriptionLst.getFirst().getId());
            assertTrue(fndTranscriptionLst.getFirst().getIsManualTranscription());
            assertEquals(WITH_TRANSCRIBER.getId(),
                         fndTranscriptionLst.getFirst().getTranscriptionStatus().getId());
            assertNotNull(transcriptionEntityWithHearing.getCreatedDateTime());
        });
    }

    @Test
    void testFindWorkflowForUserWithTranscriptionState_shouldReturnTranscriptions_whenLinkedCase() {
            setup();
        transcriptionEntityWithCase = PersistableFactory.getTranscriptionTestData().someMinimalBuilderHolder()
            .getBuilder()
            .requestedBy(accountEntity)
            .courtCases(List.of(courtCase))
            .isManualTranscription(true)
            .build().getEntity();
        createTranscriptionWorkflow(accountEntity, OffsetDateTime.now(), WITH_TRANSCRIBER, transcriptionEntityWithCase);
        transactionalUtil.executeInTransaction(() -> {
            List<TranscriptionEntity> fndTranscriptionLst = transcriptionWorkflowRepository
                    .findWorkflowForUserWithTranscriptionState(accountEntity.getId(), transcriptionEntityWithCase.getTranscriptionStatus().getId());
            assertEquals(transcriptionEntityWithCase.getCourtCase().getCaseNumber(),
                                 fndTranscriptionLst.getFirst().getCourtCase().getCaseNumber());
            assertEquals(1, fndTranscriptionLst.size());
            assertEquals(transcriptionEntityWithCase.getId(), fndTranscriptionLst.getFirst().getId());
            assertTrue(fndTranscriptionLst.getFirst().getIsManualTranscription());
            assertEquals(WITH_TRANSCRIBER.getId(),
                         fndTranscriptionLst.getFirst().getTranscriptionStatus().getId());
            assertEquals(transcriptionEntityWithCase.getCourtCase().getCourthouse().getId(),
                         fndTranscriptionLst.getFirst().getCourtCase().getCourthouse().getId());
            assertNotNull(transcriptionEntityWithCase.getCreatedDateTime());
            });
    }

    @Test
    void findTranscriptionForUserOnOrAfterDate_shouldReturnTranscriptions_whenLinkedHearing() {
        setup();
        transcriptionEntityWithHearing = PersistableFactory.getTranscriptionTestData().someMinimalBuilderHolder()
            .getBuilder()
            .requestedBy(accountEntity)
            .hearings(List.of(headerEntity))
            .isManualTranscription(true)
            .build().getEntity();
        createTranscriptionWorkflow(accountEntity, OffsetDateTime.now(), WITH_TRANSCRIBER, transcriptionEntityWithHearing);
        transactionalUtil.executeInTransaction(() -> {
            List<TranscriptionEntity> fndTranscriptionLst = transcriptionRepository
                .findTranscriptionForUserOnOrAfterDate(accountEntity.getId(),
                                                       null);
            assertEquals(1, fndTranscriptionLst.size());
            assertEquals(transcriptionEntityWithHearing.getId(), fndTranscriptionLst.getFirst().getId());
            assertNotNull(transcriptionEntityWithHearing.getHearing().getHearingDate());
            assertEquals(transcriptionEntityWithHearing.getId(), fndTranscriptionLst.getFirst().getId());
            assertTrue(fndTranscriptionLst.getFirst().getIsManualTranscription());
            assertEquals(WITH_TRANSCRIBER.getId(),
                         fndTranscriptionLst.getFirst().getTranscriptionStatus().getId());
            assertEquals(transcriptionEntityWithHearing.getCourtCase().getCourthouse().getId(),
                         fndTranscriptionLst.getFirst().getCourtCase().getCourthouse().getId());
            assertNotNull(transcriptionEntityWithHearing.getCreatedDateTime());
        });
    }

    @Test
    void findTranscriptionForUserOnOrAfterDate_shouldReturnTranscriptions_whenLinkedCase() {
        setup();
        transcriptionEntityWithCase = PersistableFactory.getTranscriptionTestData().someMinimalBuilderHolder()
            .getBuilder()
            .requestedBy(accountEntity)
            .courtCases(List.of(courtCase))
            .isManualTranscription(true)
            .build().getEntity();
        createTranscriptionWorkflow(accountEntity, OffsetDateTime.now(), WITH_TRANSCRIBER, transcriptionEntityWithCase);
        transactionalUtil.executeInTransaction(() -> {
            List<TranscriptionEntity> fndTranscriptionLst = transcriptionRepository
                .findTranscriptionForUserOnOrAfterDate(accountEntity.getId(),
                                                       null);
            assertEquals(1, fndTranscriptionLst.size());
            assertEquals(transcriptionEntityWithCase.getId(), fndTranscriptionLst.getFirst().getId());
            assertEquals(transcriptionEntityWithCase.getId(), fndTranscriptionLst.getFirst().getId());
            assertTrue(fndTranscriptionLst.getFirst().getIsManualTranscription());
            assertEquals(WITH_TRANSCRIBER.getId(),
                         fndTranscriptionLst.getFirst().getTranscriptionStatus().getId());
            assertEquals(transcriptionEntityWithCase.getCourtCase().getCourthouse().getId(),
                         fndTranscriptionLst.getFirst().getCourtCase().getCourthouse().getId());
            assertNotNull(transcriptionEntityWithCase.getCreatedDateTime());
        });
    }

    @Test
    void testFindTranscriptionForUserWithCreatedDateNowAndBeyond_shouldReturnTranscriptions_whenLinkedHearing() {
        setup();
        transcriptionEntityWithHearing = PersistableFactory.getTranscriptionTestData().someMinimalBuilderHolder()
            .getBuilder()
            .requestedBy(accountEntity)
            .hearings(List.of(headerEntity))
            .isManualTranscription(true)
            .build().getEntity();
        createTranscriptionWorkflow(accountEntity, OffsetDateTime.now(), WITH_TRANSCRIBER, transcriptionEntityWithHearing);

        transactionalUtil.executeInTransaction(() -> {
            List<TranscriptionEntity> fndTranscriptionLst = transcriptionRepository
                .findTranscriptionForUserOnOrAfterDate(accountEntity.getId(),
                                                       transcriptionEntityWithHearing.getCreatedDateTime());
            assertEquals(1, fndTranscriptionLst.size());
            assertEquals(transcriptionEntityWithHearing.getId(), fndTranscriptionLst.getFirst().getId());
            assertNotNull(transcriptionEntityWithHearing.getHearing().getHearingDate());
            assertEquals(transcriptionEntityWithHearing.getId(), fndTranscriptionLst.getFirst().getId());
            assertTrue(fndTranscriptionLst.getFirst().getIsManualTranscription());
            assertEquals(transcriptionEntityWithHearing.getCourtCase().getCaseNumber(),
                         fndTranscriptionLst.getFirst().getCourtCase().getCaseNumber());
            assertEquals(WITH_TRANSCRIBER.getId(),
                         fndTranscriptionLst.getFirst().getTranscriptionStatus().getId());
            assertEquals(transcriptionEntityWithHearing.getCourtCase().getCourthouse().getId(),
                         fndTranscriptionLst.getFirst().getCourtCase().getCourthouse().getId());
            assertNotNull(transcriptionEntityWithHearing.getCreatedDateTime());
        });
    }

    @Test
    void testFindTranscriptionForUserWithCreatedDateNowAndBeyond_shouldReturnTranscriptions_whenLinkedCase() {
        setup();
        transcriptionEntityWithCase = PersistableFactory.getTranscriptionTestData().someMinimalBuilderHolder()
            .getBuilder()
            .requestedBy(accountEntity)
            .courtCases(List.of(courtCase))
            .isManualTranscription(true)
            .build().getEntity();
        createTranscriptionWorkflow(accountEntity, OffsetDateTime.now(), WITH_TRANSCRIBER, transcriptionEntityWithCase);

        transactionalUtil.executeInTransaction(() -> {
            List<TranscriptionEntity> fndTranscriptionLst = transcriptionRepository
                .findTranscriptionForUserOnOrAfterDate(accountEntity.getId(),
                                                       transcriptionEntityWithCase.getCreatedDateTime());
            assertEquals(1, fndTranscriptionLst.size());
            assertEquals(transcriptionEntityWithCase.getId(), fndTranscriptionLst.getFirst().getId());
            assertEquals(transcriptionEntityWithCase.getId(), fndTranscriptionLst.getFirst().getId());
            assertTrue(fndTranscriptionLst.getFirst().getIsManualTranscription());
            assertEquals(transcriptionEntityWithCase.getCourtCase().getCaseNumber(),
                         fndTranscriptionLst.getFirst().getCourtCase().getCaseNumber());
            assertEquals(WITH_TRANSCRIBER.getId(),
                         fndTranscriptionLst.getFirst().getTranscriptionStatus().getId());
            assertEquals(transcriptionEntityWithCase.getCourtCase().getCourthouse().getId(),
                         fndTranscriptionLst.getFirst().getCourtCase().getCourthouse().getId());
            assertNotNull(transcriptionEntityWithCase.getCreatedDateTime());
        });
    }

    private void createTranscriptionWorkflow(UserAccountEntity userAccount, OffsetDateTime dateTime, TranscriptionStatusEnum transcriptionStatusEnum,
                                             TranscriptionEntity transcriptionEntity) {
        TranscriptionWorkflowEntity transcriptionWorkflowEntity =
            PersistableFactory.getTranscriptionWorkflowTestData()
                .workflowForTranscriptionWithStatus(transcriptionEntity, transcriptionStatusEnum);
        transcriptionWorkflowEntity.setWorkflowActor(userAccount);
        transcriptionWorkflowEntity.setWorkflowTimestamp(dateTime);
        transcriptionEntity.getTranscriptionWorkflowEntities().add(transcriptionWorkflowEntity);
        transcriptionEntity.setTranscriptionStatus(transcriptionWorkflowEntity.getTranscriptionStatus());
        dartsPersistence.save(transcriptionWorkflowEntity);
        dartsPersistence.save(transcriptionEntity);
    }
}