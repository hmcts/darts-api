package uk.gov.hmcts.darts.common.repository;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.test.common.data.UserAccountTestData;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.AuthorisationStub;
import uk.gov.hmcts.darts.testutils.stubs.TranscriptionStub;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertEquals(1, fndTranscriptionLst.size());
        assertEquals(transcriptionEntity.getId(), fndTranscriptionLst.get(0).getId());
        assertNotNull(transcriptionEntity.getHearing().getHearingDate());
        assertEquals(transcriptionEntity.getId(), fndTranscriptionLst.get(0).getId());
        assertTrue(fndTranscriptionLst.get(0).getIsManualTranscription());
        assertEquals(transcriptionEntity.getCourtCase().getCaseNumber(),
                                fndTranscriptionLst.get(0).getCourtCase().getCaseNumber());
        assertEquals(TranscriptionStatusEnum.WITH_TRANSCRIBER.getId(),
                                fndTranscriptionLst.get(0).getTranscriptionStatus().getId());
        assertEquals(transcriptionEntity.getCourtCase().getCourthouse().getId(),
                                fndTranscriptionLst.get(0).getCourtCase().getCourthouse().getId());
        assertNotNull(transcriptionEntity.getCreatedDateTime());
    }

    @Test
    void testFindTranscriptionForUser() {
        List<TranscriptionEntity> fndTranscriptionLst = transcriptionRepository
            .findTranscriptionForUserOnOrAfterDate(accountEntity.getId(),
                                                   null);
        assertEquals(1, fndTranscriptionLst.size());
        assertEquals(transcriptionEntity.getId(), fndTranscriptionLst.get(0).getId());
        assertNotNull(transcriptionEntity.getHearing().getHearingDate());
        assertEquals(transcriptionEntity.getId(), fndTranscriptionLst.get(0).getId());
        assertTrue(fndTranscriptionLst.get(0).getIsManualTranscription());
        assertEquals(transcriptionEntity.getCourtCase().getCaseNumber(),
                                fndTranscriptionLst.get(0).getCourtCase().getCaseNumber());
        assertEquals(TranscriptionStatusEnum.WITH_TRANSCRIBER.getId(),
                                fndTranscriptionLst.get(0).getTranscriptionStatus().getId());
        assertEquals(transcriptionEntity.getCourtCase().getCourthouse().getId(),
                                fndTranscriptionLst.get(0).getCourtCase().getCourthouse().getId());
        assertNotNull(transcriptionEntity.getCreatedDateTime());
    }

    @Test
    void testFindTranscriptionForUserWithCreatedDateNowAndBeyond() {
        List<TranscriptionEntity> fndTranscriptionLst = transcriptionRepository
            .findTranscriptionForUserOnOrAfterDate(accountEntity.getId(),
                                                   transcriptionEntity.getCreatedDateTime());
        assertEquals(1, fndTranscriptionLst.size());
        assertEquals(transcriptionEntity.getId(), fndTranscriptionLst.get(0).getId());
        assertNotNull(transcriptionEntity.getHearing().getHearingDate());
        assertEquals(transcriptionEntity.getId(), fndTranscriptionLst.get(0).getId());
        assertTrue(fndTranscriptionLst.get(0).getIsManualTranscription());
        assertEquals(transcriptionEntity.getCourtCase().getCaseNumber(),
                                fndTranscriptionLst.get(0).getCourtCase().getCaseNumber());
        assertEquals(TranscriptionStatusEnum.WITH_TRANSCRIBER.getId(),
                                fndTranscriptionLst.get(0).getTranscriptionStatus().getId());
        assertEquals(transcriptionEntity.getCourtCase().getCourthouse().getId(),
                                fndTranscriptionLst.get(0).getCourtCase().getCourthouse().getId());
        assertNotNull(transcriptionEntity.getCreatedDateTime());
    }
}