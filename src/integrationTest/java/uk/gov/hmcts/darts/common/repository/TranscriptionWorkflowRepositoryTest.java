package uk.gov.hmcts.darts.common.repository;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionTypeEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionUrgencyEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.TranscriptionStub;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionUrgencyEnum;

import java.time.OffsetDateTime;
import java.util.List;

class TranscriptionWorkflowRepositoryTest  extends IntegrationBase {
    @Autowired
    private TranscriptionWorkflowRepository transcriptionWorkflowRepository;

    @Autowired
    private TranscriptionRepository transcriptionRepository;

    @Autowired
    private UserAccountRepository userAccountRepositoryRepository;

    @Autowired
    private CourtroomRepository courtroomRepository;

    @Autowired
    private CourthouseRepository courthouseRepository;

    @Autowired
    private TranscriptionUrgencyRepository transcriptionUrgencyRepository;

    @Autowired
    private TranscriptionStub transcriptionStub;

    @Test
    void testFindWorkflowForUserWithTranscriptionState() {

        UserAccountEntity accountEntity = new UserAccountEntity();
        accountEntity.setActive(true);
        accountEntity.setUserName("test");
        accountEntity.setIsSystemUser(false);
        userAccountRepositoryRepository.save(accountEntity);

        CourthouseEntity courthouseEntity = new CourthouseEntity();
        courthouseEntity.setCode(111);
        courthouseEntity.setCourthouseName("courtroom");
        courthouseEntity.setDisplayName("courthousedisplayname");
        courthouseRepository.save(courthouseEntity);

        CourtroomEntity courtroomEntity = new CourtroomEntity();
        courtroomEntity.setCourthouse(courthouseEntity);
        courtroomEntity.setName("courtname");
        courtroomRepository.save(courtroomEntity);

        TranscriptionStatusEntity transcriptionStatus = new TranscriptionStatusEntity();
        transcriptionStatus.setId(TranscriptionStatusEnum.WITH_TRANSCRIBER.getId());
        transcriptionStatus.setStatusType(TranscriptionStatusEnum.WITH_TRANSCRIBER.name());
        transcriptionStatus.setDisplayName(TranscriptionStatusEnum.WITH_TRANSCRIBER.name());

        TranscriptionUrgencyEntity urgencyEntity = transcriptionUrgencyRepository.findById(TranscriptionUrgencyEnum.STANDARD.getId()).get();

        TranscriptionTypeEntity typeEntity = transcriptionStub.getTranscriptionTypeByEnum(TranscriptionTypeEnum.SENTENCING_REMARKS);

        TranscriptionEntity transcriptionEntity = new TranscriptionEntity();
        transcriptionEntity.setCourtroom(courtroomEntity);
        transcriptionEntity.setTranscriptionStatus(transcriptionStatus);
        transcriptionEntity.setTranscriptionUrgency(urgencyEntity);
        transcriptionEntity.setTranscriptionType(typeEntity);
        transcriptionEntity.setLastModifiedBy(accountEntity);
        transcriptionEntity.setCreatedBy(accountEntity);
        transcriptionEntity.setIsManualTranscription(true);
        transcriptionEntity.setHideRequestFromRequestor(false);
        transcriptionRepository.save(transcriptionEntity);

        TranscriptionWorkflowEntity workflowEntity = new TranscriptionWorkflowEntity();
        workflowEntity.setTranscription(transcriptionEntity);
        workflowEntity.setTranscriptionStatus(transcriptionStatus);
        workflowEntity.setWorkflowActor(accountEntity);
        workflowEntity.setWorkflowTimestamp(OffsetDateTime.now());
        transcriptionWorkflowRepository.save(workflowEntity);

        List<TranscriptionEntity> fndTranscription = transcriptionWorkflowRepository
            .findWorkflowForUserWithTranscriptionState(accountEntity.getId(), transcriptionEntity.getTranscriptionStatus().getId());
        Assertions.assertEquals(1, fndTranscription.size());
        Assertions.assertEquals(workflowEntity.getId(), fndTranscription.get(0).getId());
    }
}