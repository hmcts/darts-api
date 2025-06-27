package uk.gov.hmcts.darts.transcriptions.component.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.DartsDatabaseStub;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;
import uk.gov.hmcts.darts.transcriptions.model.TranscriberViewSummary;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.APPROVED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.AWAITING_AUTHORISATION;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.COMPLETE;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.REQUESTED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.WITH_TRANSCRIBER;

class TranscriberTranscriptsQueryImplIntTest extends IntegrationBase {

    @Autowired
    protected DartsDatabaseStub dartsDatabase;

    @Autowired
    private TranscriberTranscriptsQueryImpl transcriberTranscriptsQuery;

    private UserAccountEntity userAccountEntity;

    private CourthouseEntity courthouse;

    private CourtCaseEntity courtCaseEntity;

    private HearingEntity hearingEntity;

    private static final OffsetDateTime NOW = now(UTC);

    @BeforeEach
    void setUp() {
        courthouse = dartsDatabase.getCourthouseStub().createMinimalCourthouse();
        userAccountEntity = dartsDatabase.getUserAccountStub().createTranscriptionCompanyUser(courthouse);

        hearingEntity = dartsDatabase.getHearingStub().createMinimalHearing();
        courtCaseEntity = hearingEntity.getCourtCase();
    }


    @Test
    void getTranscriptRequests_shouldReturnTranscriptionWithMultipleWorkflows_whenLinkedHearing() {
        var transcriptionEntity = PersistableFactory.getTranscriptionTestData().someMinimalBuilder()
            .requestedBy(userAccountEntity)
            .hearings(List.of(hearingEntity))
            .createdDateTime(NOW.minusDays(3))
            .build().getEntity();
        createTranscriptionWorkflow(userAccountEntity, NOW.minusDays(2), AWAITING_AUTHORISATION, transcriptionEntity);
        createTranscriptionWorkflow(userAccountEntity, NOW.minusDays(1), APPROVED, transcriptionEntity);
        List<TranscriberViewSummary> transcriberTranscriptions = transcriberTranscriptsQuery.getTranscriptRequests(userAccountEntity.getId());

        assertThat(transcriberTranscriptions.size()).isEqualTo(1);
    }

    @Test
    void getTranscriptRequests_shouldReturnTranscriptionWithMultipleWorkflows_whenLinkedCase() {
        var transcriptionEntity = PersistableFactory.getTranscriptionTestData().someMinimalBuilder()
            .requestedBy(userAccountEntity)
            .courtCases(List.of(courtCaseEntity))
            .createdDateTime(NOW.minusDays(3))
            .build().getEntity();
        createTranscriptionWorkflow(userAccountEntity, NOW.minusDays(2), AWAITING_AUTHORISATION, transcriptionEntity);
        createTranscriptionWorkflow(userAccountEntity, NOW.minusDays(1), APPROVED, transcriptionEntity);
        List<TranscriberViewSummary> transcriberTranscriptions = transcriberTranscriptsQuery.getTranscriptRequests(userAccountEntity.getId());

        assertThat(transcriberTranscriptions.size()).isEqualTo(1);
    }

    @Test
    void getTranscriptRequests_shouldReturnTranscriptionsWithMultipleWorkflowsWithSameWorkflowTimeStamps_whenLinkedHearing() {
        var yesterday = NOW.minusDays(1);

        var transcriptionEntity = PersistableFactory.getTranscriptionTestData().someMinimalBuilder()
            .requestedBy(userAccountEntity)
            .hearings(List.of(hearingEntity))
            .createdDateTime(yesterday)
            .build().getEntity();
        createTranscriptionWorkflow(userAccountEntity, yesterday, REQUESTED, transcriptionEntity);
        createTranscriptionWorkflow(userAccountEntity, yesterday, APPROVED, transcriptionEntity);
        List<TranscriberViewSummary> transcriberTranscriptions = transcriberTranscriptsQuery.getTranscriptRequests(userAccountEntity.getId());

        assertThat(transcriberTranscriptions.size()).isEqualTo(1);
    }

    @Test
    void getTranscriptRequests_shouldReturnTranscriptionsWithMultipleWorkflowsWithSameWorkflowTimeStamps_whenLinkedCase() {
        var yesterday = NOW.minusDays(1);

        var transcriptionEntity = PersistableFactory.getTranscriptionTestData().someMinimalBuilder()
            .requestedBy(userAccountEntity)
            .courtCases(List.of(courtCaseEntity))
            .createdDateTime(yesterday)
            .build().getEntity();
        createTranscriptionWorkflow(userAccountEntity, yesterday, REQUESTED, transcriptionEntity);
        createTranscriptionWorkflow(userAccountEntity, yesterday, APPROVED, transcriptionEntity);
        List<TranscriberViewSummary> transcriberTranscriptions = transcriberTranscriptsQuery.getTranscriptRequests(userAccountEntity.getId());

        assertThat(transcriberTranscriptions.size()).isEqualTo(1);
    }

    @Test
    void getTranscriberTranscriptions_shouldReturnNonHiddenTranscriptions_whenLinkedHearing() {
        TranscriptionEntity transcriptionWithoutDocument = PersistableFactory.getTranscriptionTestData().someMinimalBuilderHolder()
            .getBuilder()
            .requestedBy(userAccountEntity)
            .hearings(List.of(hearingEntity))
            .build().getEntity();
        createTranscriptionWorkflow(userAccountEntity, NOW, REQUESTED, transcriptionWithoutDocument);
        createTranscriptionWorkflow(userAccountEntity, NOW, COMPLETE, transcriptionWithoutDocument);

        TranscriptionEntity transcriptionWithDocument = PersistableFactory.getTranscriptionTestData().someMinimalBuilderHolder()
            .getBuilder()
            .requestedBy(userAccountEntity)
            .hearings(List.of(hearingEntity))
            .build().getEntity();
        TranscriptionDocumentEntity transcriptionDocumentEntity = PersistableFactory.getTranscriptionDocument()
            .someMinimalBuilder().transcription(transcriptionWithDocument)
            .uploadedBy(userAccountEntity).build().getEntity();
        dartsPersistence.save(transcriptionDocumentEntity);
        transcriptionWithDocument.setTranscriptionDocumentEntities(new ArrayList<>(List.of(transcriptionDocumentEntity)));
        createTranscriptionWorkflow(userAccountEntity, NOW, REQUESTED, transcriptionWithDocument);
        createTranscriptionWorkflow(userAccountEntity, NOW, COMPLETE, transcriptionWithDocument);

        // should not be returned
        TranscriptionEntity transcriptionWithHiddenDocument = PersistableFactory.getTranscriptionTestData().someMinimalBuilderHolder()
            .getBuilder()
            .requestedBy(userAccountEntity)
            .hearings(List.of(hearingEntity))
            .build().getEntity();
        TranscriptionDocumentEntity hiddenTranscriptionDocumentEntity = PersistableFactory.getTranscriptionDocument()
            .someMinimalBuilder().transcription(transcriptionWithHiddenDocument)
            .uploadedBy(userAccountEntity).isHidden(true).build().getEntity();
        dartsPersistence.save(hiddenTranscriptionDocumentEntity);
        transcriptionWithHiddenDocument.setTranscriptionDocumentEntities(new ArrayList<>(List.of(hiddenTranscriptionDocumentEntity)));
        createTranscriptionWorkflow(userAccountEntity, NOW, REQUESTED, transcriptionWithHiddenDocument);
        createTranscriptionWorkflow(userAccountEntity, NOW, COMPLETE, transcriptionWithHiddenDocument);

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
    void getTranscriberTranscriptions_shouldReturnNonHiddenTranscriptions_whenLinkedCase() {
        TranscriptionEntity transcriptionWithoutDocument = PersistableFactory.getTranscriptionTestData().someMinimalBuilderHolder()
            .getBuilder()
            .requestedBy(userAccountEntity)
            .courtCases(List.of(courtCaseEntity))
            .build().getEntity();
        createTranscriptionWorkflow(userAccountEntity, NOW, REQUESTED, transcriptionWithoutDocument);
        createTranscriptionWorkflow(userAccountEntity, NOW, COMPLETE, transcriptionWithoutDocument);

        TranscriptionEntity transcriptionWithDocument = PersistableFactory.getTranscriptionTestData().someMinimalBuilderHolder()
            .getBuilder()
            .requestedBy(userAccountEntity)
            .courtCases(List.of(courtCaseEntity))
            .build().getEntity();
        TranscriptionDocumentEntity transcriptionDocumentEntity = PersistableFactory.getTranscriptionDocument()
            .someMinimalBuilder().transcription(transcriptionWithDocument)
            .uploadedBy(userAccountEntity).build().getEntity();
        dartsPersistence.save(transcriptionDocumentEntity);
        transcriptionWithDocument.setTranscriptionDocumentEntities(new ArrayList<>(List.of(transcriptionDocumentEntity)));
        createTranscriptionWorkflow(userAccountEntity, NOW, REQUESTED, transcriptionWithDocument);
        createTranscriptionWorkflow(userAccountEntity, NOW, COMPLETE, transcriptionWithDocument);

        // should not be returned
        TranscriptionEntity transcriptionWithHiddenDocument = PersistableFactory.getTranscriptionTestData().someMinimalBuilderHolder()
            .getBuilder()
            .requestedBy(userAccountEntity)
            .courtCases(List.of(courtCaseEntity))
            .build().getEntity();
        TranscriptionDocumentEntity hiddenTranscriptionDocumentEntity = PersistableFactory.getTranscriptionDocument()
            .someMinimalBuilder().transcription(transcriptionWithHiddenDocument)
            .uploadedBy(userAccountEntity).isHidden(true).build().getEntity();
        dartsPersistence.save(hiddenTranscriptionDocumentEntity);
        transcriptionWithHiddenDocument.setTranscriptionDocumentEntities(new ArrayList<>(List.of(hiddenTranscriptionDocumentEntity)));
        createTranscriptionWorkflow(userAccountEntity, NOW, REQUESTED, transcriptionWithHiddenDocument);
        createTranscriptionWorkflow(userAccountEntity, NOW, COMPLETE, transcriptionWithHiddenDocument);

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
    void getTranscriberTranscriptions_shouldReturnTranscriptionsWithMultipleWithTranscriberWorkflows_whenLinkedHearing() {
        var yesterday = NOW.minusDays(1);
        TranscriptionEntity transcriptionEntity = PersistableFactory.getTranscriptionTestData().someMinimalBuilderHolder()
            .getBuilder()
            .requestedBy(userAccountEntity)
            .hearings(List.of(hearingEntity))
            .build().getEntity();
        createTranscriptionWorkflow(userAccountEntity, NOW.minusDays(4), REQUESTED, transcriptionEntity);
        createTranscriptionWorkflow(userAccountEntity, NOW.minusDays(3), WITH_TRANSCRIBER, transcriptionEntity);
        createTranscriptionWorkflow(userAccountEntity, NOW.minusDays(2), APPROVED, transcriptionEntity);
        createTranscriptionWorkflow(userAccountEntity, yesterday, WITH_TRANSCRIBER, transcriptionEntity);

        List<TranscriberViewSummary> transcriberTranscriptions = transcriberTranscriptsQuery.getTranscriberTranscriptions(userAccountEntity.getId());

        assertThat(transcriberTranscriptions.size()).isEqualTo(1);
        var format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        assertThat(transcriberTranscriptions.getFirst().getStateChangeTs().format(format)).isEqualTo(yesterday.format(format));
    }

    @Test
    void getTranscriberTranscriptions_shouldReturnTranscriptionsWithMultipleWithTranscriberWorkflows_whenLinkedCase() {
        var yesterday = NOW.minusDays(1);
        TranscriptionEntity transcriptionEntity = PersistableFactory.getTranscriptionTestData().someMinimalBuilderHolder()
            .getBuilder()
            .requestedBy(userAccountEntity)
            .courtCases(List.of(courtCaseEntity))
            .build().getEntity();
        createTranscriptionWorkflow(userAccountEntity, NOW.minusDays(4), REQUESTED, transcriptionEntity);
        createTranscriptionWorkflow(userAccountEntity, NOW.minusDays(3), WITH_TRANSCRIBER, transcriptionEntity);
        createTranscriptionWorkflow(userAccountEntity, NOW.minusDays(2), APPROVED, transcriptionEntity);
        createTranscriptionWorkflow(userAccountEntity, NOW.minusDays(1), WITH_TRANSCRIBER, transcriptionEntity);

        List<TranscriberViewSummary> transcriberTranscriptions = transcriberTranscriptsQuery.getTranscriberTranscriptions(userAccountEntity.getId());

        assertThat(transcriberTranscriptions.size()).isEqualTo(1);
        var format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        assertThat(transcriberTranscriptions.getFirst().getStateChangeTs().format(format)).isEqualTo(yesterday.format(format));
    }

    @Test
    void getTranscriberTranscriptions_shouldReturnTranscriptionsWithMultipleWorkflowsWithSameWorkflowTimestamp_withLinkedHearing() {
        var yesterday = NOW.minusDays(1);
        TranscriptionEntity transcriptionEntity = PersistableFactory.getTranscriptionTestData().someMinimalBuilderHolder()
            .getBuilder()
            .requestedBy(userAccountEntity)
            .hearings(List.of(hearingEntity))
            .build().getEntity();

        createTranscriptionWorkflow(userAccountEntity, yesterday, REQUESTED, transcriptionEntity);
        createTranscriptionWorkflow(userAccountEntity, yesterday, WITH_TRANSCRIBER, transcriptionEntity);

        List<TranscriberViewSummary> transcriberTranscriptions = transcriberTranscriptsQuery.getTranscriberTranscriptions(userAccountEntity.getId());

        assertThat(transcriberTranscriptions.size()).isEqualTo(1);
    }

    @Test
    void getTranscriberTranscriptions_shouldReturnTranscriptionsWithMultipleWorkflowsWithSameWorkflowTimestamp_withLinkedCase() {
        var yesterday = NOW.minusDays(1);
        TranscriptionEntity transcriptionEntity = PersistableFactory.getTranscriptionTestData().someMinimalBuilderHolder()
            .getBuilder()
            .requestedBy(userAccountEntity)
            .courtCases(List.of(courtCaseEntity))
            .build().getEntity();

        createTranscriptionWorkflow(userAccountEntity, yesterday, REQUESTED, transcriptionEntity);
        createTranscriptionWorkflow(userAccountEntity, yesterday, WITH_TRANSCRIBER, transcriptionEntity);

        List<TranscriberViewSummary> transcriberTranscriptions = transcriberTranscriptsQuery.getTranscriberTranscriptions(userAccountEntity.getId());

        assertThat(transcriberTranscriptions.size()).isEqualTo(1);
    }

    @Test
    void getTranscriberTranscriptions_shouldReturnTranscriptionsWithMultipleWithTranscriberWorkflowsWithSameWorkflowTimestamp_whenLinkedHearing() {
        var yesterday = NOW.minusDays(1);
        TranscriptionEntity transcriptionEntity = PersistableFactory.getTranscriptionTestData().someMinimalBuilderHolder()
            .getBuilder()
            .requestedBy(userAccountEntity)
            .hearings(List.of(hearingEntity))
            .build().getEntity();

        createTranscriptionWorkflow(userAccountEntity, yesterday, REQUESTED, transcriptionEntity);
        createTranscriptionWorkflow(userAccountEntity, yesterday, WITH_TRANSCRIBER, transcriptionEntity);
        createTranscriptionWorkflow(userAccountEntity, yesterday, WITH_TRANSCRIBER, transcriptionEntity);

        List<TranscriberViewSummary> transcriberTranscriptions = transcriberTranscriptsQuery.getTranscriberTranscriptions(userAccountEntity.getId());

        assertThat(transcriberTranscriptions.size()).isEqualTo(1);
    }

    @Test
    void getTranscriberTranscriptions_shouldReturnTranscriptionsWithMultipleWithTranscriberWorkflowsWithSameWorkflowTimestamp_whenLinkedCase() {
        var yesterday = NOW.minusDays(1);
        TranscriptionEntity transcriptionEntity = PersistableFactory.getTranscriptionTestData().someMinimalBuilderHolder()
            .getBuilder()
            .requestedBy(userAccountEntity)
            .courtCases(List.of(courtCaseEntity))
            .build().getEntity();

        createTranscriptionWorkflow(userAccountEntity, yesterday, REQUESTED, transcriptionEntity);
        createTranscriptionWorkflow(userAccountEntity, yesterday, WITH_TRANSCRIBER, transcriptionEntity);
        createTranscriptionWorkflow(userAccountEntity, yesterday, WITH_TRANSCRIBER, transcriptionEntity);

        List<TranscriberViewSummary> transcriberTranscriptions = transcriberTranscriptsQuery.getTranscriberTranscriptions(userAccountEntity.getId());

        assertThat(transcriberTranscriptions.size()).isEqualTo(1);
    }

    @Test
    void getTranscriberTranscriptions_ShouldReturnTranscriptions_whenLinkedHearing() {
        var yesterday = NOW.minusDays(1);
        TranscriptionEntity transcriptionEntity = PersistableFactory.getTranscriptionTestData().someMinimalBuilderHolder()
            .getBuilder()
            .requestedBy(userAccountEntity)
            .hearings(List.of(hearingEntity))
            .build().getEntity();
        createTranscriptionWorkflow(userAccountEntity, yesterday, REQUESTED, transcriptionEntity);
        createTranscriptionWorkflow(userAccountEntity, yesterday, WITH_TRANSCRIBER, transcriptionEntity);

        List<TranscriberViewSummary> transcriberTranscriptions = transcriberTranscriptsQuery.getTranscriberTranscriptions(userAccountEntity.getId());
        assertThat(transcriberTranscriptions.size()).isEqualTo(1);
    }

    @Test
    void getTranscriberTranscriptions_ShouldReturnTranscriptions_whenLinkedCase() {
            var yesterday = NOW.minusDays(1);
            TranscriptionEntity transcriptionEntity = PersistableFactory.getTranscriptionTestData().someMinimalBuilderHolder()
                .getBuilder()
                .requestedBy(userAccountEntity)
                .courtCases(List.of(courtCaseEntity))
                .build().getEntity();
            createTranscriptionWorkflow(userAccountEntity, yesterday, REQUESTED, transcriptionEntity);
            createTranscriptionWorkflow(userAccountEntity, yesterday, WITH_TRANSCRIBER, transcriptionEntity);

        List<TranscriberViewSummary> transcriberTranscriptions = transcriberTranscriptsQuery.getTranscriberTranscriptions(userAccountEntity.getId());
        assertThat(transcriberTranscriptions.size()).isEqualTo(1);
    }

    @Test
    void getApprovedTranscriptionsCountForCourthouses_shouldReturnApprovedTranscriptions_whenLinkedHearing() {
        TranscriptionEntity transcriptionEntity = PersistableFactory.getTranscriptionTestData().someMinimalBuilderHolder()
            .getBuilder()
            .requestedBy(userAccountEntity)
            .hearings(List.of(hearingEntity))
            .build().getEntity();
        createTranscriptionWorkflow(userAccountEntity, NOW.minusDays(3), REQUESTED, transcriptionEntity);
        createTranscriptionWorkflow(userAccountEntity, NOW.minusDays(2), APPROVED, transcriptionEntity);
        createTranscriptionWorkflow(userAccountEntity, NOW.minusDays(1), WITH_TRANSCRIBER, transcriptionEntity);
        createTranscriptionWorkflow(userAccountEntity, NOW, APPROVED, transcriptionEntity);

        Integer approvedTranscriptionCount = transcriberTranscriptsQuery.getTranscriptionsCountForCourthouses(
            List.of(courthouse.getId()),
            APPROVED.getId(),
            userAccountEntity.getId()
        );

        assertThat(approvedTranscriptionCount).isEqualTo(1);
    }

    @Test
    void getApprovedTranscriptionsCountForCourthouses_shouldReturnApprovedTranscriptions_whenLinkedCase() {
        TranscriptionEntity transcriptionEntity = PersistableFactory.getTranscriptionTestData().someMinimalBuilderHolder()
            .getBuilder()
            .requestedBy(userAccountEntity)
            .courtCases(List.of(courtCaseEntity))
            .build().getEntity();
        createTranscriptionWorkflow(userAccountEntity, NOW.minusDays(3), REQUESTED, transcriptionEntity);
        createTranscriptionWorkflow(userAccountEntity, NOW.minusDays(2), APPROVED, transcriptionEntity);
        createTranscriptionWorkflow(userAccountEntity, NOW.minusDays(1), WITH_TRANSCRIBER, transcriptionEntity);
        createTranscriptionWorkflow(userAccountEntity, NOW, APPROVED, transcriptionEntity);

        Integer approvedTranscriptionCount = transcriberTranscriptsQuery.getTranscriptionsCountForCourthouses(
            List.of(courthouse.getId()),
            APPROVED.getId(),
            userAccountEntity.getId()
        );

        assertThat(approvedTranscriptionCount).isEqualTo(1);
    }

    @Test
    void getTranscriptionsCountForCourthouses_shouldReturnWithTranscriberTranscriptions_whenLinkedHearing() {

        TranscriptionEntity transcriptionEntity = PersistableFactory.getTranscriptionTestData().someMinimalBuilderHolder()
            .getBuilder()
            .requestedBy(userAccountEntity)
            .hearings(List.of(hearingEntity))
            .build().getEntity();
        createTranscriptionWorkflow(userAccountEntity, NOW.minusDays(2), WITH_TRANSCRIBER, transcriptionEntity);
        createTranscriptionWorkflow(userAccountEntity, NOW.minusDays(1), APPROVED, transcriptionEntity);
        createTranscriptionWorkflow(userAccountEntity, NOW, WITH_TRANSCRIBER, transcriptionEntity);

        Integer withTranscriberTranscriptionCount = transcriberTranscriptsQuery.getTranscriptionsCountForCourthouses(
            List.of(courthouse.getId()),
            WITH_TRANSCRIBER.getId(),
            userAccountEntity.getId()
        );

        assertThat(withTranscriberTranscriptionCount).isEqualTo(1);
    }

    @Test
    void getTranscriptionsCountForCourthouses_shouldReturnWithTranscriberTranscriptions_whenLinkedCase() {
        TranscriptionEntity transcriptionEntity = PersistableFactory.getTranscriptionTestData().someMinimalBuilderHolder()
            .getBuilder()
            .requestedBy(userAccountEntity)
            .courtCases(List.of(courtCaseEntity))
            .build().getEntity();
        createTranscriptionWorkflow(userAccountEntity, NOW.minusDays(2), WITH_TRANSCRIBER, transcriptionEntity);
        createTranscriptionWorkflow(userAccountEntity, NOW.minusDays(1), APPROVED, transcriptionEntity);
        createTranscriptionWorkflow(userAccountEntity, NOW, WITH_TRANSCRIBER, transcriptionEntity);

        Integer approvedTranscriptionCount = transcriberTranscriptsQuery.getTranscriptionsCountForCourthouses(
            List.of(courthouse.getId()),
            WITH_TRANSCRIBER.getId(),
            userAccountEntity.getId()
        );

        assertThat(approvedTranscriptionCount).isEqualTo(1);
    }

    @Test
    void getTranscriptRequests_ReturnsTranscriptionWithMultipleTranscriberWorkflowsWithWorkflowReversed_whenLinkedHearing() {
        var transcriptionDate = OffsetDateTime.parse("2025-03-20T09:00:00Z");
        var requestedDate = OffsetDateTime.parse("2025-03-20T11:00:00Z");
        var reRunRequestedDate = OffsetDateTime.parse("2025-03-20T13:00:00Z");
        var approvedDate = OffsetDateTime.parse("2025-03-20T15:00:00Z");
        // Create a transcription with multiple workflows REQUESTED -> AWAITING_AUTHORISATION
        TranscriptionEntity transcriptionEntity = PersistableFactory.getTranscriptionTestData().someMinimalBuilderHolder()
            .getBuilder()
            .requestedBy(userAccountEntity)
            .hearings(List.of(hearingEntity))
            .build().getEntity();
        createTranscriptionWorkflow(userAccountEntity, transcriptionDate, REQUESTED, transcriptionEntity);
        createTranscriptionWorkflow(userAccountEntity, requestedDate, AWAITING_AUTHORISATION, transcriptionEntity);

        // Rewind back to REQUESTED
        createTranscriptionWorkflow(userAccountEntity, reRunRequestedDate.plusSeconds(1), AWAITING_AUTHORISATION, transcriptionEntity);
        // Move workflow to APPROVED
        createTranscriptionWorkflow(userAccountEntity, approvedDate, APPROVED, transcriptionEntity);

        List<TranscriberViewSummary> transcriberTranscriptions = transcriberTranscriptsQuery.getTranscriptRequests(userAccountEntity.getId());

        assertThat(transcriberTranscriptions.size()).isEqualTo(1);
        var format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        assertThat(transcriberTranscriptions.getFirst().getRequestedTs().format(format)).isEqualTo(transcriptionDate.format(format));
    }

    @Test
    void getTranscriptRequests_ReturnsTranscriptionWithMultipleTranscriberWorkflowsWithWorkflowReversed_whenLinkedCase() {
        var transcriptionDate = OffsetDateTime.parse("2025-03-20T09:00:00Z");
        var requestedDate = OffsetDateTime.parse("2025-03-20T11:00:00Z");
        var reRunRequestedDate = OffsetDateTime.parse("2025-03-20T13:00:00Z");
        var approvedDate = OffsetDateTime.parse("2025-03-20T15:00:00Z");
        // Create a transcription with multiple workflows REQUESTED -> AWAITING_AUTHORISATION
        TranscriptionEntity transcriptionEntity = PersistableFactory.getTranscriptionTestData().someMinimalBuilderHolder()
            .getBuilder()
            .requestedBy(userAccountEntity)
            .courtCases(List.of(courtCaseEntity))
            .build().getEntity();
        createTranscriptionWorkflow(userAccountEntity, transcriptionDate, REQUESTED, transcriptionEntity);
        createTranscriptionWorkflow(userAccountEntity, requestedDate, AWAITING_AUTHORISATION, transcriptionEntity);

        // Rewind back to REQUESTED
        createTranscriptionWorkflow(userAccountEntity, reRunRequestedDate.plusSeconds(1), AWAITING_AUTHORISATION, transcriptionEntity);
        // Move workflow to APPROVED
        createTranscriptionWorkflow(userAccountEntity, approvedDate, APPROVED, transcriptionEntity);

        List<TranscriberViewSummary> transcriberTranscriptions = transcriberTranscriptsQuery.getTranscriptRequests(userAccountEntity.getId());

        assertThat(transcriberTranscriptions.size()).isEqualTo(1);
        var format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        assertThat(transcriberTranscriptions.getFirst().getRequestedTs().format(format)).isEqualTo(transcriptionDate.format(format));
    }

    @Test
    void getTranscriptRequests_ReturnsTranscriptionWithMultipleTranscriberWorkflowsWithWorkflowReversedToApproved_whenLinkedHearing() {
        var requestedDate = OffsetDateTime.parse("2025-03-20T08:00:00Z");
        var transcriptionDate = OffsetDateTime.parse("2025-03-20T09:00:00Z");
        var approvedDate = OffsetDateTime.parse("2025-03-20T15:00:00Z");
        var reRunApprovedDate = OffsetDateTime.parse("2025-03-20T17:00:00Z");
        // Create a transcription with multiple workflows
        // AWAITING_AUTH -> APPROVED -> WITH_TRANSCRIBER -> APPROVED
        TranscriptionEntity transcriptionEntity = PersistableFactory.getTranscriptionTestData().someMinimalBuilderHolder()
            .getBuilder()
            .requestedBy(userAccountEntity)
            .hearings(List.of(hearingEntity))
            .build().getEntity();
        createTranscriptionWorkflow(userAccountEntity, requestedDate, REQUESTED, transcriptionEntity);
        createTranscriptionWorkflow(userAccountEntity, transcriptionDate, AWAITING_AUTHORISATION, transcriptionEntity);
        // Move workflow to APPROVED
        createTranscriptionWorkflow(userAccountEntity, approvedDate, APPROVED, transcriptionEntity);
        // Move workflow to WITH_TRANSCRIBER
        createTranscriptionWorkflow(userAccountEntity, approvedDate, WITH_TRANSCRIBER, transcriptionEntity);
        // Rewind back to APPROVED
        createTranscriptionWorkflow(userAccountEntity, reRunApprovedDate, APPROVED, transcriptionEntity);

        List<TranscriberViewSummary> transcriberTranscriptions = transcriberTranscriptsQuery.getTranscriptRequests(userAccountEntity.getId());

        assertThat(transcriberTranscriptions.size()).isEqualTo(1);
        var format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        assertThat(transcriberTranscriptions.getFirst().getApprovedTs().format(format)).isEqualTo(approvedDate.format(format));
    }

    @Test
    void getTranscriptRequests_ReturnsTranscriptionWithMultipleTranscriberWorkflowsWithWorkflowReversedToApproved_whenLinkedCase() {
        var requestedDate = OffsetDateTime.parse("2025-03-20T08:00:00Z");
        var transcriptionDate = OffsetDateTime.parse("2025-03-20T09:00:00Z");
        var approvedDate = OffsetDateTime.parse("2025-03-20T15:00:00Z");
        var reRunApprovedDate = OffsetDateTime.parse("2025-03-20T17:00:00Z");
        // Create a transcription with multiple workflows
        // AWAITING_AUTH -> APPROVED -> WITH_TRANSCRIBER -> APPROVED
        TranscriptionEntity transcriptionEntity = PersistableFactory.getTranscriptionTestData().someMinimalBuilderHolder()
            .getBuilder()
            .requestedBy(userAccountEntity)
            .courtCases(List.of(courtCaseEntity))
            .build().getEntity();
        createTranscriptionWorkflow(userAccountEntity, requestedDate, REQUESTED, transcriptionEntity);
        createTranscriptionWorkflow(userAccountEntity, transcriptionDate, AWAITING_AUTHORISATION, transcriptionEntity);
        // Move workflow to APPROVED
        createTranscriptionWorkflow(userAccountEntity, approvedDate, APPROVED, transcriptionEntity);
        // Move workflow to WITH_TRANSCRIBER
        createTranscriptionWorkflow(userAccountEntity, approvedDate, WITH_TRANSCRIBER, transcriptionEntity);
        // Rewind back to APPROVED
        createTranscriptionWorkflow(userAccountEntity, reRunApprovedDate, APPROVED, transcriptionEntity);

        List<TranscriberViewSummary> transcriberTranscriptions = transcriberTranscriptsQuery.getTranscriptRequests(userAccountEntity.getId());

        assertThat(transcriberTranscriptions.size()).isEqualTo(1);
        var format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        assertThat(transcriberTranscriptions.getFirst().getApprovedTs().format(format)).isEqualTo(approvedDate.format(format));
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