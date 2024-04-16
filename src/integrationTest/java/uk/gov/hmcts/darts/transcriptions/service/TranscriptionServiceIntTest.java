package uk.gov.hmcts.darts.transcriptions.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.repository.TranscriptionRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionWorkflowRepository;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.HearingStub;
import uk.gov.hmcts.darts.testutils.stubs.TranscriptionStub;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.APPROVED;

public class TranscriptionServiceIntTest extends IntegrationBase {

    @Autowired
    HearingStub hearingStub;
    @Autowired
    TranscriptionStub transcriptionStub;
    @Autowired
    TranscriptionRepository transcriptionRepository;
    @Autowired
    TranscriptionWorkflowRepository transcriptionWorkflowRepository;
    @Autowired
    TranscriptionService transcriptionService;

    @Test
    void searchTranscriptionsByUser() {

        var tr1 = transcriptionStub.createTranscription(hearingStub.createMinimalHearing());

        //username 'integrationtest.user@example.com'
        var trWorkflow = transcriptionStub.createTranscriptionWorkflowEntity(tr1, tr1.getLastModifiedBy(),
                                                            tr1.getCreatedDateTime().plusHours(1),
                                                            transcriptionStub.getTranscriptionStatusByEnum(APPROVED));


        var alternativeUser = dartsDatabase.getUserAccountStub().getSeparateIntegrationTestUserAccountEntity();
        var trWorkflowAlternativeUser = transcriptionStub.createTranscriptionWorkflowEntity(tr1, alternativeUser,
                                                                             tr1.getCreatedDateTime().plusHours(2),
                                                                             transcriptionStub.getTranscriptionStatusByEnum(APPROVED));

//        transcriptionStub.createTranscriptionWorkflowEntity(tr1, tr1.getLastModifiedBy(),
//                                                            tr1.getCreatedDateTime().plusHours(1),
//                                                            transcriptionStub.getTranscriptionStatusByEnum(APPROVED));

        tr1.getTranscriptionWorkflowEntities()
            .addAll(List.of(trWorkflow));
        dartsDatabase.getTranscriptionRepository().save(tr1);

        assertThat(transcriptionRepository.findAll()).isNotEmpty();
        assertThat(transcriptionWorkflowRepository.findAll()).isNotEmpty();

        List<TranscriptionEntity> result = transcriptionService.searchTranscriptionsByUserName("%integration%");

        assertThat(result.get(0).getId()).isEqualTo(tr1.getId());
    }

}
