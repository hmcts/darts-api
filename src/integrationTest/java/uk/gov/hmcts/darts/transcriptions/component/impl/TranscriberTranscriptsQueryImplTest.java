package uk.gov.hmcts.darts.transcriptions.component.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.DartsDatabaseStub;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;
import uk.gov.hmcts.darts.transcriptions.model.TranscriberViewSummary;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;

class TranscriberTranscriptsQueryImplTest extends IntegrationBase {

    @Autowired
    protected DartsDatabaseStub dartsDatabase;

    @Autowired
    TranscriberTranscriptsQueryImpl transcriberTranscriptsQuery;

    private UserAccountEntity userAccountEntity;

    private CourtCaseEntity courtCaseEntity;

    private HearingEntity hearingEntity;

    private static final OffsetDateTime NOW = now(UTC);

    @BeforeEach
    void setUp() {
        CourthouseEntity courthouse = dartsDatabase.getCourthouseStub().createMinimalCourthouse();
        userAccountEntity = dartsDatabase.getUserAccountStub().createTranscriptionCompanyUser(courthouse);

        hearingEntity = dartsDatabase.getHearingStub().createMinimalHearing();
        courtCaseEntity = hearingEntity.getCourtCase();
    }

    @Test
    void getTranscriberTranscriptions() {
        TranscriptionEntity transcriptionWithoutDocument = dartsDatabase.getTranscriptionStub().createAndSaveCompletedTranscription(
            userAccountEntity, courtCaseEntity, hearingEntity, NOW, false
        );
        TranscriptionEntity transcriptionWithDocument = dartsDatabase.getTranscriptionStub().createAndSaveCompletedTranscriptionWithDocument(
            userAccountEntity, courtCaseEntity, hearingEntity, NOW, false
        );
        // should not be returned
        TranscriptionEntity transcriptionWithHiddenDocument = dartsDatabase.getTranscriptionStub().createAndSaveCompletedTranscriptionWithDocument(
            userAccountEntity, courtCaseEntity, hearingEntity, NOW, true
        );

        List<TranscriberViewSummary> transcriberTranscriptions = transcriberTranscriptsQuery.getTranscriberTranscriptions(userAccountEntity.getId());

        assertThat(transcriberTranscriptions.size()).isEqualTo(2);
        assertThat(transcriberTranscriptions.stream().filter(t -> t.getTranscriptionId().equals(transcriptionWithoutDocument.getId()))).isNotNull();
        assertThat(transcriberTranscriptions.stream().filter(t -> t.getTranscriptionId().equals(transcriptionWithDocument.getId()))).isNotNull();
        assertThat(transcriberTranscriptions
                       .stream()
                       .filter(t -> t.getTranscriptionId().equals(transcriptionWithHiddenDocument.getId()))
                       .findFirst()
        ).isEmpty();
    }

    @Test
    void getTranscriberTranscriptionsWithMultipleWithTranscriberWorkflows() {
        var yesterday = NOW.minusDays(1);
        TranscriptionEntity transcriptionEntity = dartsDatabase.getTranscriptionStub().createAndSaveWithTranscriberTranscription(
            userAccountEntity, courtCaseEntity, hearingEntity, NOW.minusDays(3), false
        );
        var backToApproved = dartsDatabase.getTranscriptionStub().createAndSaveTranscriptionWorkflow(
            transcriptionEntity, NOW.minusDays(2), dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(TranscriptionStatusEnum.APPROVED)
        );
        var withTranscriber = dartsDatabase.getTranscriptionStub().createAndSaveTranscriptionWorkflow(
            transcriptionEntity, yesterday, dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(TranscriptionStatusEnum.WITH_TRANSCRIBER)
        );
        transcriptionEntity.getTranscriptionWorkflowEntities()
            .addAll(List.of(backToApproved, withTranscriber));
        dartsDatabase.getTranscriptionRepository().saveAndFlush(transcriptionEntity);

        List<TranscriberViewSummary> transcriberTranscriptions = transcriberTranscriptsQuery.getTranscriberTranscriptions(userAccountEntity.getId());

        assertThat(transcriberTranscriptions.size()).isEqualTo(1);
        var format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        assertThat(transcriberTranscriptions.get(0).getStateChangeTs().format(format)).isEqualTo(yesterday.format(format));
    }
}