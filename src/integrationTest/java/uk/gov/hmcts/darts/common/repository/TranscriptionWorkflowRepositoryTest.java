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
    void findWorkflowForUserWithTranscriptionState_shouldReturnTranscriptions_whenLinkedHearing() {
        setup();
        transcriptionEntityWithHearing = PersistableFactory.getTranscriptionTestData().someMinimalBuilderHolder()
            .getBuilder()
            .requestedBy(accountEntity)
            .hearings(List.of(headerEntity))
            .isManualTranscription(true)
            .build().getEntity();
            createTranscriptionWorkflow(accountEntity, OffsetDateTime.now(), WITH_TRANSCRIBER, transcriptionEntityWithHearing);
        transactionalUtil.executeInTransaction(() -> {
            List<TranscriptionEntity> transcriptionLst = transcriptionWorkflowRepository
                .findWorkflowForUserWithTranscriptionState(accountEntity.getId(), transcriptionEntityWithHearing.getTranscriptionStatus().getId());
            assertEquals(transcriptionEntityWithHearing.getCourtCase().getCaseNumber(),
                         transcriptionLst.getFirst().getCourtCase().getCaseNumber());
            assertEquals(1, transcriptionLst.size());
            assertEquals(transcriptionEntityWithHearing.getId(), transcriptionLst.getFirst().getId());
            assertNotNull(transcriptionEntityWithHearing.getHearing().getHearingDate());
            assertEquals(transcriptionEntityWithHearing.getId(), transcriptionLst.getFirst().getId());
            assertTrue(transcriptionLst.getFirst().getIsManualTranscription());
            assertEquals(WITH_TRANSCRIBER.getId(),
                         transcriptionLst.getFirst().getTranscriptionStatus().getId());
            assertNotNull(transcriptionEntityWithHearing.getCreatedDateTime());
        });
    }

    @Test
    void findWorkflowForUserWithTranscriptionState_shouldReturnTranscriptions_whenLinkedCase() {
            setup();
        transcriptionEntityWithCase = PersistableFactory.getTranscriptionTestData().someMinimalBuilderHolder()
            .getBuilder()
            .requestedBy(accountEntity)
            .courtCases(List.of(courtCase))
            .isManualTranscription(true)
            .build().getEntity();
        createTranscriptionWorkflow(accountEntity, OffsetDateTime.now(), WITH_TRANSCRIBER, transcriptionEntityWithCase);
        transactionalUtil.executeInTransaction(() -> {
            List<TranscriptionEntity> transcriptionLst = transcriptionWorkflowRepository
                    .findWorkflowForUserWithTranscriptionState(accountEntity.getId(), transcriptionEntityWithCase.getTranscriptionStatus().getId());
            assertEquals(transcriptionEntityWithCase.getCourtCase().getCaseNumber(),
                         transcriptionLst.getFirst().getCourtCase().getCaseNumber());
            assertEquals(1, transcriptionLst.size());
            assertEquals(transcriptionEntityWithCase.getId(), transcriptionLst.getFirst().getId());
            assertTrue(transcriptionLst.getFirst().getIsManualTranscription());
            assertEquals(WITH_TRANSCRIBER.getId(),
                         transcriptionLst.getFirst().getTranscriptionStatus().getId());
            assertEquals(transcriptionEntityWithCase.getCourtCase().getCourthouse().getId(),
                         transcriptionLst.getFirst().getCourtCase().getCourthouse().getId());
            assertNotNull(transcriptionEntityWithCase.getCreatedDateTime());
            });
    }

    @Test
    void findTranscriptionForUserOnOrAfterDate_shouldReturnTranscriptions_whenOnOrAfterCreatedDateNullAndLinkedHearing() {
        setup();
        transcriptionEntityWithHearing = PersistableFactory.getTranscriptionTestData().someMinimalBuilderHolder()
            .getBuilder()
            .requestedBy(accountEntity)
            .hearings(List.of(headerEntity))
            .isManualTranscription(true)
            .build().getEntity();
        createTranscriptionWorkflow(accountEntity, OffsetDateTime.now(), WITH_TRANSCRIBER, transcriptionEntityWithHearing);
        transactionalUtil.executeInTransaction(() -> {
            List<TranscriptionEntity> transcriptionLst = transcriptionRepository
                .findTranscriptionForUserOnOrAfterDate(accountEntity.getId(),
                                                       null);
            assertEquals(1, transcriptionLst.size());
            assertEquals(transcriptionEntityWithHearing.getId(), transcriptionLst.getFirst().getId());
            assertNotNull(transcriptionEntityWithHearing.getHearing().getHearingDate());
            assertEquals(transcriptionEntityWithHearing.getId(), transcriptionLst.getFirst().getId());
            assertTrue(transcriptionLst.getFirst().getIsManualTranscription());
            assertEquals(WITH_TRANSCRIBER.getId(),
                         transcriptionLst.getFirst().getTranscriptionStatus().getId());
            assertEquals(transcriptionEntityWithHearing.getCourtCase().getCourthouse().getId(),
                         transcriptionLst.getFirst().getCourtCase().getCourthouse().getId());
            assertNotNull(transcriptionEntityWithHearing.getCreatedDateTime());
        });
    }

    @Test
    void findTranscriptionForUserOnOrAfterDate_shouldReturnTranscriptions_whenOnOrAfterCreatedDateNullAndLinkedCase() {
        setup();
        transcriptionEntityWithHearing = PersistableFactory.getTranscriptionTestData().someMinimalBuilderHolder()
            .getBuilder()
            .requestedBy(accountEntity)
            .courtCases(List.of(courtCase))
            .isManualTranscription(true)
            .build().getEntity();
        createTranscriptionWorkflow(accountEntity, OffsetDateTime.now(), WITH_TRANSCRIBER, transcriptionEntityWithHearing);
        transactionalUtil.executeInTransaction(() -> {
            List<TranscriptionEntity> transcriptionLst = transcriptionRepository
                .findTranscriptionForUserOnOrAfterDate(accountEntity.getId(),
                                                       null);
            assertEquals(1, transcriptionLst.size());
            assertEquals(transcriptionEntityWithHearing.getId(), transcriptionLst.getFirst().getId());
            assertEquals(transcriptionEntityWithHearing.getId(), transcriptionLst.getFirst().getId());
            assertTrue(transcriptionLst.getFirst().getIsManualTranscription());
            assertEquals(WITH_TRANSCRIBER.getId(),
                         transcriptionLst.getFirst().getTranscriptionStatus().getId());
            assertEquals(transcriptionEntityWithHearing.getCourtCase().getCourthouse().getId(),
                         transcriptionLst.getFirst().getCourtCase().getCourthouse().getId());
            assertNotNull(transcriptionEntityWithHearing.getCreatedDateTime());
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
            List<TranscriptionEntity> transcriptionLst = transcriptionRepository
                .findTranscriptionForUserOnOrAfterDate(accountEntity.getId(),
                                                       transcriptionEntityWithHearing.getCreatedDateTime());
            assertEquals(1, transcriptionLst.size());
            assertEquals(transcriptionEntityWithHearing.getId(), transcriptionLst.getFirst().getId());
            assertNotNull(transcriptionEntityWithHearing.getHearing().getHearingDate());
            assertEquals(transcriptionEntityWithHearing.getId(), transcriptionLst.getFirst().getId());
            assertTrue(transcriptionLst.getFirst().getIsManualTranscription());
            assertEquals(WITH_TRANSCRIBER.getId(),
                         transcriptionLst.getFirst().getTranscriptionStatus().getId());
            assertEquals(transcriptionEntityWithHearing.getCourtCase().getCourthouse().getId(),
                         transcriptionLst.getFirst().getCourtCase().getCourthouse().getId());
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
            List<TranscriptionEntity> transcriptionLst = transcriptionRepository
                .findTranscriptionForUserOnOrAfterDate(accountEntity.getId(),
                                                       transcriptionEntityWithCase.getCreatedDateTime());
            assertEquals(1, transcriptionLst.size());
            assertEquals(transcriptionEntityWithCase.getId(), transcriptionLst.getFirst().getId());
            assertTrue(transcriptionLst.getFirst().getIsManualTranscription());
            assertEquals(WITH_TRANSCRIBER.getId(),
                         transcriptionLst.getFirst().getTranscriptionStatus().getId());
            assertEquals(transcriptionEntityWithCase.getCourtCase().getCourthouse().getId(),
                         transcriptionLst.getFirst().getCourtCase().getCourthouse().getId());
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