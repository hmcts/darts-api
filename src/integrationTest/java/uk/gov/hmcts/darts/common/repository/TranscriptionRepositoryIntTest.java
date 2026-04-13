package uk.gov.hmcts.darts.common.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Limit;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionStatusEntity;
import uk.gov.hmcts.darts.common.util.DateConverterUtil;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.TranscriptionDocumentStub;
import uk.gov.hmcts.darts.testutils.stubs.TranscriptionStub;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionSearchResult;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.hmcts.darts.test.common.data.TranscriptionDocumentTestData.minimalTranscriptionDocument;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.APPROVED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.COMPLETE;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.REQUESTED;

class TranscriptionRepositoryIntTest extends IntegrationBase {

    private static final String SOME_COURTHOUSE = "SOME-COURTHOUSE";
    private static final String SOME_COURTROOM = "some-courtroom";
    private static final String SOME_CASE_ID = "1";
    private static final OffsetDateTime SOME_DATE_TIME = OffsetDateTime.parse("2023-01-01T12:00Z");

    @Autowired
    private TranscriptionRepository transcriptionRepository;

    @Autowired
    private TranscriptionStub transcriptionStub;

    @Autowired
    private TranscriptionDocumentStub transcriptionDocumentStub;

    private CourtCaseEntity courtCaseEntity;
    private HearingEntity hearingEntity;

    private int caseId;

    @BeforeEach
    public void setupData() {
        hearingEntity = dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            SOME_CASE_ID,
            SOME_COURTHOUSE,
            SOME_COURTROOM,
            DateConverterUtil.toLocalDateTime(SOME_DATE_TIME)
        );
        courtCaseEntity = hearingEntity.getCourtCase();
        caseId = courtCaseEntity.getId();
    }

    @Test
    void searchTranscriptionsFilteringOn_shouldReturnProjectionForMatchingCaseNumber() {
        // given
        TranscriptionStatusEntity approvedStatus = transcriptionStub.getTranscriptionStatusByEnum(APPROVED);
        OffsetDateTime workflowTimestamp = OffsetDateTime.now().minusDays(1);

        TranscriptionEntity transcription = transcriptionStub.createAndSaveApprovedTranscription(
            dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity(),
            courtCaseEntity,
            hearingEntity,
            workflowTimestamp,
            false
        );
        transcription.setIsManualTranscription(true);
        transcription = dartsDatabase.save(transcription);

        // Ensure uploadedAt is non-null (query LEFT JOINs documents)
        var doc = transcriptionDocumentStub.createTranscriptionDocumentForTranscription(transcription);
        doc.setUploadedDateTime(OffsetDateTime.now());
        dartsDatabase.save(doc);

        // when
        List<TranscriptionSearchResult> results = transcriptionRepository.searchTranscriptionsFilteringOn(
            null,
            courtCaseEntity.getCaseNumber(),
            null,
            null,
            null,
            null,
            null,
            null,
            approvedStatus
        );

        // then
        assertThat(results)
            .isNotEmpty();

        Long transcriptionId = transcription.getId();

        TranscriptionSearchResult result = results.stream()
            .filter(r -> r.id().equals(transcriptionId))
            .findFirst()
            .orElseThrow();

        assertThat(result.caseId()).isEqualTo(courtCaseEntity.getId());
        assertThat(result.caseNumber()).isEqualTo(courtCaseEntity.getCaseNumber());
        assertThat(result.courthouseId()).isEqualTo(hearingEntity.getCourtroom().getCourthouse().getId());
        assertThat(result.hearingId()).isEqualTo(hearingEntity.getId());
        assertThat(result.hearingDate()).isEqualTo(hearingEntity.getHearingDate());
        assertThat(result.requestedAt()).isEqualTo(transcription.getCreatedDateTime());
        assertThat(result.transcriptionStatusId()).isEqualTo(transcription.getTranscriptionStatus().getId());
        assertThat(result.isManualTranscription()).isTrue();
        assertThat(result.approvedAt()).isNotNull();
        assertThat(result.uploadedAt()).isNotNull();
    }

    @Test
    void searchTranscriptionsFilteringOn_shouldFilterByIsManual() {
        // given
        TranscriptionStatusEntity approvedStatus = transcriptionStub.getTranscriptionStatusByEnum(APPROVED);
        OffsetDateTime workflowTimestamp = OffsetDateTime.now().minusDays(1);

        TranscriptionEntity manual = transcriptionStub.createAndSaveApprovedTranscription(
            dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity(),
            courtCaseEntity,
            hearingEntity,
            workflowTimestamp,
            false
        );
        manual.setIsManualTranscription(true);
        dartsDatabase.save(manual);
        transcriptionDocumentStub.createTranscriptionDocumentForTranscription(manual);

        TranscriptionEntity automated = transcriptionStub.createAndSaveApprovedTranscription(
            dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity(),
            courtCaseEntity,
            hearingEntity,
            workflowTimestamp,
            false
        );
        automated.setIsManualTranscription(false);
        dartsDatabase.save(automated);
        transcriptionDocumentStub.createTranscriptionDocumentForTranscription(automated);

        // when
        List<TranscriptionSearchResult> results = transcriptionRepository.searchTranscriptionsFilteringOn(
            null,
            courtCaseEntity.getCaseNumber(),
            null,
            null,
            null,
            null,
            true,
            null,
            approvedStatus
        );

        // then
        assertThat(results)
            .extracting(TranscriptionSearchResult::id)
            .contains(manual.getId())
            .doesNotContain(automated.getId());
    }

    @Test
    void findByCaseIdManualOrLegacy_shouldNotShowAutomatedTranscriptions() {
        TranscriptionEntity legacyTranscription = createTranscriptionWithDocument(courtCaseEntity, false);
        legacyTranscription.setIsManualTranscription(false);
        dartsDatabase.save(legacyTranscription);
        dartsDatabase.getTranscriptionDocumentStub().createTranscriptionDocumentForTranscription(legacyTranscription);

        List<TranscriptionEntity> transcriptionEntities = transcriptionRepository.findByCaseIdManualOrLegacy(caseId, true);
        assertThat(transcriptionEntities).isEmpty();
    }

    @Test
    void findByCaseIdManualOrLegacy_shouldReturnLegacyTranscriptionHasDocuments() {
        TranscriptionEntity legacyTranscription = createTranscriptionWithDocument(courtCaseEntity, false);
        legacyTranscription.setLegacyObjectId("legacy");
        legacyTranscription.setIsManualTranscription(false);
        dartsDatabase.save(legacyTranscription);
        List<TranscriptionEntity> transcriptionEntities = transcriptionRepository.findByCaseIdManualOrLegacy(caseId, true);
        assertThat(transcriptionEntities).hasSize(1);
        assertThat(transcriptionEntities.getFirst().getId())
            .isEqualTo(legacyTranscription.getId());
    }

    @Test
    void findByCaseIdManualOrLegacy_shouldNotReturnLegacyTranscriptionIfHasNoDocuments() {
        TranscriptionEntity legacyTranscription = transcriptionStub.createTranscription(courtCaseEntity);
        legacyTranscription.setLegacyObjectId("legacy");
        legacyTranscription.setIsManualTranscription(false);
        dartsDatabase.save(legacyTranscription);
        List<TranscriptionEntity> transcriptionEntities = transcriptionRepository.findByCaseIdManualOrLegacy(caseId, true);
        assertThat(transcriptionEntities).isEmpty();
    }

    @Test
    void findByCaseIdManualOrLegacy_shouldIncludeHidden() {
        createStandardTranscriptionWithDocument();
        List<TranscriptionEntity> transcriptionEntities = transcriptionRepository.findByCaseIdManualOrLegacy(caseId, true);
        assertThat(transcriptionEntities).hasSize(4);
    }

    @Test
    void findByCaseIdManualOrLegacy_shouldExcludeHidden() {
        createStandardTranscriptionWithDocument();
        var courtCase = PersistableFactory.getCourtCaseTestData().createSomeMinimalCase();
        persistTwoHiddenTwoNotHiddenTranscriptionsFor(courtCase);

        var transcriptionEntities = transcriptionRepository.findByCaseIdManualOrLegacy(courtCase.getId(), false);

        assertEquals(2, transcriptionEntities.size());
    }

    @Test
    void findAllByTranscriptionStatusNotInWithCreatedDateTimeBefore_ShouldSucceed() {
        // given
        createStandardTranscriptionWithDocument();
        TranscriptionStatusEntity completeTranscriptionStatus = dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(COMPLETE);
        TranscriptionEntity transcriptionCompleteOld =
            PersistableFactory.getTranscriptionTestData().minimalRawTranscription(completeTranscriptionStatus);
        transcriptionCompleteOld = dartsDatabase.save(transcriptionCompleteOld);
        transcriptionCompleteOld.setCreatedDateTime(OffsetDateTime.now().minusHours(2));
        dartsDatabase.save(transcriptionCompleteOld);

        TranscriptionEntity transcriptionCompleteNew =
            PersistableFactory.getTranscriptionTestData().minimalRawTranscription(completeTranscriptionStatus);
        dartsDatabase.save(transcriptionCompleteNew);

        TranscriptionStatusEntity approvedTranscriptionStatus = dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(REQUESTED);
        TranscriptionEntity transcriptionApproved = PersistableFactory.getTranscriptionTestData().minimalRawTranscription(approvedTranscriptionStatus);
        transcriptionApproved = dartsDatabase.save(transcriptionApproved);
        transcriptionApproved.setCreatedDateTime(OffsetDateTime.now().minusHours(2));
        dartsDatabase.save(transcriptionApproved);

        OffsetDateTime createdDateTime = OffsetDateTime.now().minusHours(1);

        List<TranscriptionStatusEntity> excludedStatuses = List.of(transcriptionApproved.getTranscriptionStatus());

        // when
        List<Long> result = transcriptionRepository.findAllByTranscriptionStatusNotInWithCreatedDateTimeBefore(
            excludedStatuses, createdDateTime, Limit.of(10)
        );

        // then
        assertEquals(1, result.size());
        assertEquals(transcriptionCompleteOld.getId(), result.getFirst());
    }

    @Test
    void findAllByTranscriptionStatusNotInWithCreatedDateTimeBefore_NoResultsFound() {
        // given
        createStandardTranscriptionWithDocument();
        TranscriptionEntity transcriptionApproved = PersistableFactory.getTranscriptionTestData().minimalTranscription();
        OffsetDateTime createdDateTime = OffsetDateTime.now().minusDays(1);
        List<TranscriptionStatusEntity> excludedStatuses = List.of(transcriptionApproved.getTranscriptionStatus());

        // when
        List<Long> result = transcriptionRepository.findAllByTranscriptionStatusNotInWithCreatedDateTimeBefore(
            excludedStatuses, createdDateTime, Limit.of(10)
        );

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void findByHearingIdManualOrLegacyIncludeDeletedTranscriptionDocuments_shouldReturnLegacyIfHasDocument() {
        TranscriptionEntity legacyTranscription = createTranscriptionWithDocument(courtCaseEntity, false);
        legacyTranscription.addHearing(hearingEntity);
        legacyTranscription.setLegacyObjectId("legacy");
        legacyTranscription.setIsManualTranscription(false);
        dartsDatabase.save(legacyTranscription);


        List<TranscriptionEntity> transcriptionEntities = transcriptionRepository.findByHearingIdManualOrLegacyIncludeDeletedTranscriptionDocuments(
            hearingEntity.getId()
        );

        assertThat(transcriptionEntities).hasSize(1);
        assertThat(transcriptionEntities.getFirst().getId())
            .isEqualTo(legacyTranscription.getId());
    }

    @Test
    void findByHearingIdManualOrLegacyIncludeDeletedTranscriptionDocuments_shouldNotReturnLegacyIfNoDocument() {
        TranscriptionEntity legacyTranscription = transcriptionStub.createTranscription(courtCaseEntity);
        legacyTranscription.addHearing(hearingEntity);
        legacyTranscription.setLegacyObjectId("legacy");
        legacyTranscription.setIsManualTranscription(false);
        dartsDatabase.save(legacyTranscription);
        
        List<TranscriptionEntity> transcriptionEntities = transcriptionRepository.findByHearingIdManualOrLegacyIncludeDeletedTranscriptionDocuments(
            hearingEntity.getId()
        );

        assertThat(transcriptionEntities).isEmpty();
    }

    private void persistTwoHiddenTwoNotHiddenTranscriptionsFor(CourtCaseEntity courtCaseEntity) {
        range(0, 4)
            .forEach(i -> {
                var transcriptionDocument = minimalTranscriptionDocument();
                var transcription = transcriptionDocument.getTranscription();
                transcription.setIsManualTranscription(true);
                transcription.setCourtCases(Set.of(courtCaseEntity));
                transcriptionDocument.setHidden(i % 2 == 0);
                dartsDatabase.save(transcription);
            });
    }

    private void createTranscriptionWithDocument(HearingEntity hearingEntity, Boolean hiddenDoc) {
        TranscriptionEntity transcription = transcriptionStub.createTranscription(hearingEntity);
        createTranscriptionDocument(transcription, hiddenDoc);
    }

    private TranscriptionEntity createTranscriptionWithDocument(CourtCaseEntity courtCaseEntity, Boolean hiddenDoc) {
        TranscriptionEntity transcription = transcriptionStub.createTranscription(courtCaseEntity);
        createTranscriptionDocument(transcription, hiddenDoc);
        return transcription;
    }

    private void createTranscriptionDocument(TranscriptionEntity transcription, Boolean hiddenDoc) {
        TranscriptionDocumentEntity transcriptionDocument = transcriptionDocumentStub.createTranscriptionDocumentForTranscription(transcription);
        transcriptionDocument.setHidden(hiddenDoc);
    }

    private void createStandardTranscriptionWithDocument() {
        createTranscriptionWithDocument(hearingEntity, false);
        createTranscriptionWithDocument(hearingEntity, true);
        createTranscriptionWithDocument(courtCaseEntity, false);
        createTranscriptionWithDocument(courtCaseEntity, true);
    }

}
