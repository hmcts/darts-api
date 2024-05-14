package uk.gov.hmcts.darts.common.repository;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.data.UserAccountTestData;
import uk.gov.hmcts.darts.testutils.stubs.AuthorisationStub;
import uk.gov.hmcts.darts.testutils.stubs.TranscriptionStub;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

class TranscriptionWorkflowRepositoryTest  extends IntegrationBase {
    @Autowired
    private TranscriptionWorkflowRepository transcriptionWorkflowRepository;

    @Autowired
    private TranscriptionRepository transcriptionRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private TranscriptionStub transcriptionStub;

    @Autowired
    private AuthorisationStub authorisationStub;

    private UserAccountEntity accountEntity;

    private TranscriptionEntity transcriptionEntity;


    @BeforeEach
    public void beforeAll() {
        accountEntity = UserAccountTestData.minimalUserAccount();
        userAccountRepository.save(accountEntity);

        CourtroomEntity courtroomEntity = dartsDatabase.createCourtroomUnlessExists("Newcastle", "room_a");
        HearingEntity headerEntity = dartsDatabase.createHearing(
            courtroomEntity.getCourthouse().getCourthouseName(),
            courtroomEntity.getName(),
            "c1",
            LocalDateTime.of(2020, 6, 20, 10, 0, 0)
        );

        var courtCase = authorisationStub.getCourtCaseEntity();
        transcriptionEntity = transcriptionStub.createAndSaveWithTranscriberTranscription(
            accountEntity, courtCase, headerEntity, OffsetDateTime.now(), false);

    }

    @Test
    void testFindWorkflowForUserWithTranscriptionState() {
        List<TranscriptionEntity> fndTranscriptionLst = transcriptionWorkflowRepository
            .findWorkflowForUserWithTranscriptionState(accountEntity.getId(), transcriptionEntity.getTranscriptionStatus().getId());
        Assertions.assertEquals(1, fndTranscriptionLst.size());
        Assertions.assertEquals(transcriptionEntity.getId(), fndTranscriptionLst.get(0).getId());
        Assertions.assertNotNull(transcriptionEntity.getHearing().getHearingDate());
        Assertions.assertEquals(transcriptionEntity.getId(), fndTranscriptionLst.get(0).getId());
        Assertions.assertTrue(fndTranscriptionLst.get(0).getIsManualTranscription());
        Assertions.assertEquals(transcriptionEntity.getCourtCase().getCaseNumber(),
                                fndTranscriptionLst.get(0).getCourtCase().getCaseNumber());
        Assertions.assertEquals(TranscriptionStatusEnum.WITH_TRANSCRIBER.getId(),
                                fndTranscriptionLst.get(0).getTranscriptionStatus().getId());
        Assertions.assertEquals(transcriptionEntity.getCourtCase().getCourthouse().getId(),
                                fndTranscriptionLst.get(0).getCourtCase().getCourthouse().getId());
        Assertions.assertNotNull(transcriptionEntity.getCreatedDateTime());
    }

    @Test
    void testFindTranscriptionForUser() {
        List<TranscriptionEntity> fndTranscriptionLst = transcriptionRepository
            .findTranscriptionForUserOnOrAfterDate(accountEntity.getId(),
                                                   null);
        Assertions.assertEquals(1, fndTranscriptionLst.size());
        Assertions.assertEquals(transcriptionEntity.getId(), fndTranscriptionLst.get(0).getId());
        Assertions.assertNotNull(transcriptionEntity.getHearing().getHearingDate());
        Assertions.assertEquals(transcriptionEntity.getId(), fndTranscriptionLst.get(0).getId());
        Assertions.assertTrue(fndTranscriptionLst.get(0).getIsManualTranscription());
        Assertions.assertEquals(transcriptionEntity.getCourtCase().getCaseNumber(),
                                fndTranscriptionLst.get(0).getCourtCase().getCaseNumber());
        Assertions.assertEquals(TranscriptionStatusEnum.WITH_TRANSCRIBER.getId(),
                                fndTranscriptionLst.get(0).getTranscriptionStatus().getId());
        Assertions.assertEquals(transcriptionEntity.getCourtCase().getCourthouse().getId(),
                                fndTranscriptionLst.get(0).getCourtCase().getCourthouse().getId());
        Assertions.assertNotNull(transcriptionEntity.getCreatedDateTime());
    }

    @Test
    void testFindTranscriptionForUserWithCreatedDateNowAndBeyond() {
        List<TranscriptionEntity> fndTranscriptionLst = transcriptionRepository
            .findTranscriptionForUserOnOrAfterDate(accountEntity.getId(),
                                                   transcriptionEntity.getCreatedDateTime());
        Assertions.assertEquals(1, fndTranscriptionLst.size());
        Assertions.assertEquals(transcriptionEntity.getId(), fndTranscriptionLst.get(0).getId());
        Assertions.assertNotNull(transcriptionEntity.getHearing().getHearingDate());
        Assertions.assertEquals(transcriptionEntity.getId(), fndTranscriptionLst.get(0).getId());
        Assertions.assertTrue(fndTranscriptionLst.get(0).getIsManualTranscription());
        Assertions.assertEquals(transcriptionEntity.getCourtCase().getCaseNumber(),
                                fndTranscriptionLst.get(0).getCourtCase().getCaseNumber());
        Assertions.assertEquals(TranscriptionStatusEnum.WITH_TRANSCRIBER.getId(),
                                fndTranscriptionLst.get(0).getTranscriptionStatus().getId());
        Assertions.assertEquals(transcriptionEntity.getCourtCase().getCourthouse().getId(),
                                fndTranscriptionLst.get(0).getCourtCase().getCourthouse().getId());
        Assertions.assertNotNull(transcriptionEntity.getCreatedDateTime());
    }
}