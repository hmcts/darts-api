package uk.gov.hmcts.darts.cases.repository;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Limit;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.CaseDocumentEntity;
import uk.gov.hmcts.darts.common.entity.CaseRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionLinkedCaseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.AnnotationRepository;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionRepository;
import uk.gov.hmcts.darts.common.util.DateConverterUtil;
import uk.gov.hmcts.darts.retention.enums.CaseRetentionStatus;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.CourtCaseStub;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

class CaseRepositoryIntTest extends IntegrationBase {
    private static final String SOME_COURTHOUSE = "SOME-COURTHOUSE";
    private static final String SOME_ROOM = "some-room";
    private static final String SOME_CASE_NUMBER_1 = "CASE1";
    private static final String SOME_CASE_NUMBER_2 = "CASE2";

    private final OffsetDateTime testTime = OffsetDateTime.of(2020, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC);

    @Autowired
    private CourtCaseStub caseStub;

    @Autowired
    private CaseRepository caseRepository;

    @Autowired
    private TranscriptionRepository transcriptionRepository;

    @Autowired
    private AnnotationRepository annotationRepository;

    @Test
    void findByIsRetentionUpdatedTrueAndRetentionRetriesLessThan_ReturnsResults() {
        // given
        caseStub.createAndSaveCourtCase(courtCase -> {
            courtCase.setRetentionUpdated(true);
            courtCase.setRetentionRetries(3);
        });
        caseStub.createAndSaveCourtCase(courtCase -> {
            courtCase.setRetentionUpdated(true);
            courtCase.setRetentionRetries(4);
        });
        caseStub.createAndSaveCourtCase(courtCase -> {
            courtCase.setRetentionUpdated(false);
            courtCase.setRetentionRetries(1);
        });
        var matchingCase = caseStub.createAndSaveCourtCase(courtCase -> {
            courtCase.setRetentionUpdated(true);
            courtCase.setRetentionRetries(1);
        });
        CaseRetentionEntity caseRetentionObject1 = dartsDatabase.createCaseRetentionObject(
            matchingCase, CaseRetentionStatus.COMPLETE, OffsetDateTime.now().plusDays(30), false);
        dartsDatabase.save(caseRetentionObject1);


        // when
        var result = caseRepository.findIdsByIsRetentionUpdatedTrueAndRetentionRetriesLessThan(3, Limit.of(1000));

        // then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst()).isEqualTo(matchingCase.getId());
    }

    @Test
    void findIdsByIsRetentionUpdatedTrueAndRetentionRetriesLessThan_caseHasNoAssocaitedCaseRetentionEntity_shouldNotReturn() {
        // given
        caseStub.createAndSaveCourtCase(courtCase -> {
            courtCase.setRetentionUpdated(true);
            courtCase.setRetentionRetries(1);
        });
        // when
        var result = caseRepository.findIdsByIsRetentionUpdatedTrueAndRetentionRetriesLessThan(3, Limit.of(1000));
        // then
        assertThat(result).isEmpty();
    }

    @Test
    void findIdsByIsRetentionUpdatedTrueAndRetentionRetriesLessThan_oneCaseHasMultipleRetentionObjects_shouldOnlyReturnIdOnce() {
        // given
        var matchingCase = caseStub.createAndSaveCourtCase(courtCase -> {
            courtCase.setRetentionUpdated(true);
            courtCase.setRetentionRetries(1);
        });
        CaseRetentionEntity caseRetentionObject1 = dartsDatabase.createCaseRetentionObject(
            matchingCase, CaseRetentionStatus.COMPLETE, OffsetDateTime.now().plusDays(30), false);
        dartsDatabase.save(caseRetentionObject1);

        CaseRetentionEntity caseRetentionObject2 = dartsDatabase.createCaseRetentionObject(
            matchingCase, CaseRetentionStatus.COMPLETE, OffsetDateTime.now().plusDays(30), false);
        dartsDatabase.save(caseRetentionObject2);

        // when
        var result = caseRepository.findIdsByIsRetentionUpdatedTrueAndRetentionRetriesLessThan(3, Limit.of(1000));

        // then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst()).isEqualTo(matchingCase.getId());
    }

    @Test
    void findCasesIdsNeedingCaseDocumentGenerated_ReturnsMatchingCases() {
        // given
        caseStub.createAndSaveCourtCase(courtCase -> {
            courtCase.setClosed(true);
            courtCase.setCaseClosedTimestamp(OffsetDateTime.now().minusDays(27));
        });

        var courtCaseWithCaseDocument = caseStub.createAndSaveCourtCase(courtCase -> {
            courtCase.setClosed(true);
            courtCase.setCaseClosedTimestamp(OffsetDateTime.now().minusDays(29));
        });
        dartsDatabase.getCaseDocumentStub().createAndSaveCaseDocumentEntity(courtCaseWithCaseDocument);

        caseStub.createAndSaveCourtCase(courtCase -> courtCase.setClosed(false));

        var matchingCase1 = caseStub.createAndSaveCourtCase(courtCase -> {
            courtCase.setClosed(true);
            courtCase.setCaseClosedTimestamp(OffsetDateTime.now().minusDays(28));
            courtCase.setRetentionUpdated(true);
            courtCase.setRetentionRetries(1);
        });
        CaseRetentionEntity caseRetentionObject1 = dartsDatabase.createCaseRetentionObject(
            matchingCase1, CaseRetentionStatus.COMPLETE, OffsetDateTime.now().plusDays(28), false);
        dartsDatabase.save(caseRetentionObject1);

        var matchingCase2 = caseStub.createAndSaveCourtCase(courtCase -> {
            courtCase.setClosed(true);
            courtCase.setCaseClosedTimestamp(OffsetDateTime.now().minusDays(29));
            courtCase.setRetentionUpdated(true);
            courtCase.setRetentionRetries(1);
        });
        CaseRetentionEntity caseRetentionObject2 = dartsDatabase.createCaseRetentionObject(
            matchingCase2, CaseRetentionStatus.COMPLETE, OffsetDateTime.now().plusDays(29), false);
        dartsDatabase.save(caseRetentionObject2);

        var matchingCase3 = caseStub.createAndSaveCourtCase(courtCase -> {
            courtCase.setClosed(true);
            courtCase.setCaseClosedTimestamp(OffsetDateTime.now().minusDays(30));
            courtCase.setRetentionUpdated(true);
            courtCase.setRetentionRetries(1);
        });
        CaseRetentionEntity caseRetentionObject3 = dartsDatabase.createCaseRetentionObject(
            matchingCase3, CaseRetentionStatus.COMPLETE, OffsetDateTime.now().plusDays(30), false);
        dartsDatabase.save(caseRetentionObject3);

        assertThat(dartsDatabase.getCaseRepository().findAll()).hasSize(6);

        // when
        List<Integer> result = caseRepository.findCasesIdsNeedingCaseDocumentGenerated(
            OffsetDateTime.now().minusDays(28), Limit.unlimited());

        // then
        assertThat(result).hasSize(3);
        assertThat(result.getFirst()).isEqualTo(matchingCase1.getId());
        assertThat(result.get(1)).isEqualTo(matchingCase2.getId());
        assertThat(result.get(2)).isEqualTo(matchingCase3.getId());
    }

    @Test
    void findCasesNeedingCaseDocumentForRetentionDateGeneration_AllSuccess() {
        // given
        Function<Boolean, CourtCaseEntity> createValidCourtCase = getCourtCaseEntityFunction();

        CourtCaseEntity courtCase1 = createValidCourtCase.apply(true);
        CaseRetentionEntity caseRetentionObject1 = dartsDatabase.createCaseRetentionObject(
            courtCase1, CaseRetentionStatus.COMPLETE, OffsetDateTime.now().plusDays(28), false);
        dartsDatabase.save(caseRetentionObject1);

        CourtCaseEntity courtCase2 = createValidCourtCase.apply(false);
        CaseRetentionEntity caseRetentionObject2 = dartsDatabase.createCaseRetentionObject(
            courtCase2, CaseRetentionStatus.COMPLETE, OffsetDateTime.now().plusDays(28), false);
        dartsDatabase.save(caseRetentionObject2);

        CourtCaseEntity courtCase3 = createValidCourtCase.apply(true);
        CaseRetentionEntity caseRetentionObject3 = dartsDatabase.createCaseRetentionObject(
            courtCase3, CaseRetentionStatus.COMPLETE, OffsetDateTime.now().plusDays(28), false);
        dartsDatabase.save(caseRetentionObject3);

        CourtCaseEntity courtCase4 = createValidCourtCase.apply(false);
        CaseRetentionEntity caseRetentionObject4 = dartsDatabase.createCaseRetentionObject(
            courtCase4, CaseRetentionStatus.COMPLETE, OffsetDateTime.now().plusDays(28), false);
        dartsDatabase.save(caseRetentionObject4);

        OffsetDateTime currentTimestamp = OffsetDateTime.now();
        // when
        List<Integer> result = caseRepository.findCasesNeedingCaseDocumentForRetentionDateGeneration(
            currentTimestamp.plusDays(28), currentTimestamp.minusDays(28), Limit.of(4));

        // then
        assertThat(result).hasSize(4);
        assertThat(result.getFirst()).isEqualTo(courtCase2.getId());
        assertThat(result.get(1)).isEqualTo(courtCase4.getId());
        assertThat(result.get(2)).isEqualTo(courtCase1.getId());
        assertThat(result.get(3)).isEqualTo(courtCase3.getId());

    }

    private @NotNull Function<Boolean, CourtCaseEntity> getCourtCaseEntityFunction() {
        AtomicInteger suffix = new AtomicInteger(1);
        return (isRetentionUpdated) -> {
            String caseNumber = "CASE" + suffix.getAndIncrement();
            CourtCaseEntity courtCase = dartsDatabase.createCase(SOME_COURTHOUSE, caseNumber);

            dartsDatabase.getHearingStub().createHearing(SOME_COURTHOUSE, SOME_ROOM, caseNumber,
                                                         DateConverterUtil.toLocalDateTime(testTime));

            CaseRetentionEntity caseRetentionObject1 = dartsDatabase.createCaseRetentionObject(
                courtCase, CaseRetentionStatus.COMPLETE, OffsetDateTime.now().plusDays(30), false);
            dartsDatabase.save(caseRetentionObject1);

            CaseRetentionEntity caseRetentionObject2 = dartsDatabase.createCaseRetentionObject(
                courtCase, CaseRetentionStatus.COMPLETE, OffsetDateTime.now().plusDays(20), false);
            dartsDatabase.save(caseRetentionObject2);

            courtCase.setRetentionUpdated(isRetentionUpdated);
            return dartsDatabase.save(courtCase);
        };

    }

    @Test
    void findCasesNeedingCaseDocumentForRetentionDateGeneration_returnIsRetentionUpdatedFirst() {
        // given
        CourtCaseEntity courtCaseEntityWithNoCaseDocuments = dartsDatabase.createCase(SOME_COURTHOUSE, SOME_CASE_NUMBER_1);

        dartsDatabase.getHearingStub().createHearing(SOME_COURTHOUSE, SOME_ROOM, SOME_CASE_NUMBER_1,
                                                     DateConverterUtil.toLocalDateTime(testTime));

        CaseRetentionEntity caseRetentionObject1 = dartsDatabase.createCaseRetentionObject(
            courtCaseEntityWithNoCaseDocuments, CaseRetentionStatus.COMPLETE, OffsetDateTime.now().plusDays(30), false);
        dartsDatabase.save(caseRetentionObject1);

        CaseRetentionEntity caseRetentionObject2 = dartsDatabase.createCaseRetentionObject(
            courtCaseEntityWithNoCaseDocuments, CaseRetentionStatus.COMPLETE, OffsetDateTime.now().plusDays(20), false);
        dartsDatabase.save(caseRetentionObject2);

        CourtCaseEntity courtCaseEntityWithCaseDocument = dartsDatabase.createCase(SOME_COURTHOUSE, SOME_CASE_NUMBER_2);

        dartsDatabase.getHearingStub().createHearing(SOME_COURTHOUSE, SOME_ROOM, SOME_CASE_NUMBER_2,
                                                     DateConverterUtil.toLocalDateTime(testTime));

        CaseRetentionEntity caseRetentionObject3 = dartsDatabase.createCaseRetentionObject(
            courtCaseEntityWithCaseDocument, CaseRetentionStatus.COMPLETE, OffsetDateTime.now().plusDays(10), false);
        dartsDatabase.save(caseRetentionObject3);

        UserAccountEntity uploadedBy = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        CaseDocumentEntity caseDocument = dartsDatabase.getCaseDocumentStub().createAndSaveCaseDocumentEntity(
            courtCaseEntityWithCaseDocument, uploadedBy);
        caseDocument.setCreatedDateTime(OffsetDateTime.now().minusDays(30));
        dartsDatabase.save(caseDocument);

        OffsetDateTime currentTimestamp = OffsetDateTime.now();

        // when
        List<Integer> result = caseRepository.findCasesNeedingCaseDocumentForRetentionDateGeneration(
            currentTimestamp.plusDays(28), currentTimestamp.minusDays(28), Limit.of(2));

        // then
        assertThat(result).hasSize(2);
        assertThat(result.getFirst()).isEqualTo(courtCaseEntityWithNoCaseDocuments.getId());
        assertThat(result.get(1)).isEqualTo(courtCaseEntityWithCaseDocument.getId());

    }

    @Test
    void findCasesNeedingCaseDocumentForRetentionDateGeneration_WhereRetentionDateToFarInTheFuture() {
        // given
        CourtCaseEntity courtCaseEntityWithNoCaseDocuments = dartsDatabase.createCase(SOME_COURTHOUSE, SOME_CASE_NUMBER_1);

        dartsDatabase.getHearingStub().createHearing(SOME_COURTHOUSE, SOME_ROOM, SOME_CASE_NUMBER_1,
                                                     DateConverterUtil.toLocalDateTime(testTime));

        CaseRetentionEntity caseRetentionObject1 = dartsDatabase.createCaseRetentionObject(
            courtCaseEntityWithNoCaseDocuments, CaseRetentionStatus.COMPLETE, OffsetDateTime.now().plusDays(30), false);
        dartsDatabase.save(caseRetentionObject1);

        CourtCaseEntity courtCaseEntityWithCaseDocument = dartsDatabase.createCase(SOME_COURTHOUSE, SOME_CASE_NUMBER_2);

        dartsDatabase.getHearingStub().createHearing(SOME_COURTHOUSE, SOME_ROOM, SOME_CASE_NUMBER_2,
                                                     DateConverterUtil.toLocalDateTime(testTime));

        CaseRetentionEntity caseRetentionObject3 = dartsDatabase.createCaseRetentionObject(
            courtCaseEntityWithCaseDocument, CaseRetentionStatus.COMPLETE, OffsetDateTime.now().plusDays(40), false);
        dartsDatabase.save(caseRetentionObject3);

        UserAccountEntity uploadedBy = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        CaseDocumentEntity caseDocument = dartsDatabase.getCaseDocumentStub().createAndSaveCaseDocumentEntity(courtCaseEntityWithCaseDocument, uploadedBy);
        caseDocument.setCreatedDateTime(OffsetDateTime.now().minusDays(30));
        dartsDatabase.save(caseDocument);

        OffsetDateTime currentTimestamp = OffsetDateTime.now();

        // when
        List<Integer> result = caseRepository.findCasesNeedingCaseDocumentForRetentionDateGeneration(
            currentTimestamp.plusDays(28), currentTimestamp.minusDays(28), Limit.of(2));

        // then
        assertThat(result).hasSize(0);

    }

    @Test
    void findCasesNeedingCaseDocumentForRetentionDateGeneration_WithRecentDocuments() {
        // given
        CourtCaseEntity courtCaseEntityWithCaseDocuments1 = dartsDatabase.createCase(SOME_COURTHOUSE, SOME_CASE_NUMBER_1);

        dartsDatabase.getHearingStub().createHearing(SOME_COURTHOUSE, SOME_ROOM, SOME_CASE_NUMBER_1,
                                                     DateConverterUtil.toLocalDateTime(testTime));

        CaseRetentionEntity caseRetentionObject1 = dartsDatabase.createCaseRetentionObject(
            courtCaseEntityWithCaseDocuments1, CaseRetentionStatus.COMPLETE, OffsetDateTime.now().plusDays(30), false);
        dartsDatabase.save(caseRetentionObject1);

        CaseRetentionEntity caseRetentionObject2 = dartsDatabase.createCaseRetentionObject(
            courtCaseEntityWithCaseDocuments1, CaseRetentionStatus.COMPLETE, OffsetDateTime.now().plusDays(20), false);
        dartsDatabase.save(caseRetentionObject2);

        UserAccountEntity uploadedBy = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        CaseDocumentEntity caseDocument1 = dartsDatabase.getCaseDocumentStub().createAndSaveCaseDocumentEntity(
            courtCaseEntityWithCaseDocuments1, uploadedBy);
        caseDocument1.setCreatedDateTime(OffsetDateTime.now().minusDays(27));
        dartsDatabase.save(caseDocument1);

        CourtCaseEntity courtCaseEntityWithCaseDocument2 = dartsDatabase.createCase(SOME_COURTHOUSE, SOME_CASE_NUMBER_2);

        dartsDatabase.getHearingStub().createHearing(SOME_COURTHOUSE, SOME_ROOM, SOME_CASE_NUMBER_2,
                                                     DateConverterUtil.toLocalDateTime(testTime));

        CaseRetentionEntity caseRetentionObject3 = dartsDatabase.createCaseRetentionObject(
            courtCaseEntityWithCaseDocument2, CaseRetentionStatus.COMPLETE, OffsetDateTime.now().plusDays(10), false);
        dartsDatabase.save(caseRetentionObject3);

        CaseDocumentEntity caseDocument = dartsDatabase.getCaseDocumentStub().createAndSaveCaseDocumentEntity(
            courtCaseEntityWithCaseDocument2, uploadedBy);
        dartsDatabase.save(caseDocument);

        OffsetDateTime currentTimestamp = OffsetDateTime.now();

        // when
        List<Integer> result = caseRepository.findCasesNeedingCaseDocumentForRetentionDateGeneration(
            currentTimestamp.plusDays(28), currentTimestamp.minusDays(28), Limit.of(2));

        // then
        assertThat(result).hasSize(0);

    }

    @Test
    void resetRetentionProcessingForCasesLinkedToTranscription_setRetentionUpdatedAndResetRetentionRetries_eligibleLinkedCases() {
        // given
        OffsetDateTime retainUntilAppliedOn = OffsetDateTime.now().minusDays(5);
        CourtCaseEntity matchingCase = createCaseWithRetention(
            "MATCHING-TRANSCRIPTION-CASE",
            CaseRetentionStatus.COMPLETE,
            retainUntilAppliedOn,
            retainUntilAppliedOn.minusMinutes(1)
        );
        Long transcriptionId = createLinkedTranscriptionDocument(matchingCase, retainUntilAppliedOn.plusDays(1));

        CourtCaseEntity openCase = createCaseWithRetention(
            "OPEN-TRANSCRIPTION-RETENTION",
            CaseRetentionStatus.COMPLETE,
            retainUntilAppliedOn,
            retainUntilAppliedOn.minusMinutes(1)
        );
        openCase.setClosed(false);
        dartsDatabase.save(openCase);
        createTranscriptionLinkedCase(openCase, transcriptionId);

        CourtCaseEntity pendingRetention = createCaseWithRetention(
            "PENDING-TRANSCRIPTION-RETENTION",
            CaseRetentionStatus.PENDING,
            retainUntilAppliedOn,
            retainUntilAppliedOn.minusMinutes(1)
        );
        createTranscriptionLinkedCase(pendingRetention, transcriptionId);

        // when
        List<Integer> caseIds = caseRepository.findCaseIdsLinkedToTranscription(transcriptionId);
        int updatedCases = caseRepository.resetRetentionProcessingForCases(caseIds);

        // then
        assertThat(caseIds).containsExactly(matchingCase.getId());
        assertThat(updatedCases).isEqualTo(1);
        assertCaseRetentionProcessingReset(matchingCase.getId());
        assertCaseRetentionProcessingUnchanged(openCase.getId(), 0);
        assertCaseRetentionProcessingUnchanged(pendingRetention.getId(), 0);
    }

    @Test
    @Transactional
    void resetRetentionProcessingForCasesLinkedToAnnotation_setRetentionUpdatedAndResetRetentionRetries_eligibleLinkedCases() {
        // given
        OffsetDateTime retainUntilAppliedOn = OffsetDateTime.now().minusDays(5);
        CourtCaseEntity matchingCase = createCaseWithRetention(
            "MATCHING-ANNOTATION-CASE",
            CaseRetentionStatus.COMPLETE,
            retainUntilAppliedOn,
            retainUntilAppliedOn.minusMinutes(1)
        );
        Integer annotationId = createLinkedAnnotationDocument(matchingCase, retainUntilAppliedOn.plusDays(1));

        CourtCaseEntity openCase = createCaseWithRetention(
            "OPEN-ANNOTATION-RETENTION",
            CaseRetentionStatus.COMPLETE,
            retainUntilAppliedOn,
            retainUntilAppliedOn.minusMinutes(1)
        );
        openCase.setClosed(false);
        dartsDatabase.save(openCase);
        createHearingAnnotation(openCase, annotationId);

        CourtCaseEntity pendingRetention = createCaseWithRetention(
            "PENDING-ANNOTATION-RETENTION",
            CaseRetentionStatus.PENDING,
            retainUntilAppliedOn,
            retainUntilAppliedOn.minusMinutes(1)
        );
        createHearingAnnotation(pendingRetention, annotationId);

        // when
        List<Integer> caseIds = caseRepository.findCaseIdsLinkedToAnnotation(annotationId);
        int updatedCases = caseRepository.resetRetentionProcessingForCases(caseIds);

        // then
        assertThat(caseIds).containsExactly(matchingCase.getId());
        assertThat(updatedCases).isEqualTo(1);
        assertCaseRetentionProcessingReset(matchingCase.getId());
        assertCaseRetentionProcessingUnchanged(openCase.getId(), 0);
        assertCaseRetentionProcessingUnchanged(pendingRetention.getId(), 0);
    }

    @Test
    void findAllWithIdMatchingOneOf_ReturnsResults() {
        // given
        CourtCaseEntity case1 = dartsDatabase.createCase(SOME_COURTHOUSE, SOME_CASE_NUMBER_1);
        dartsDatabase.createCase(SOME_COURTHOUSE, SOME_CASE_NUMBER_2);
        CourtCaseEntity case3 = dartsDatabase.createCase(SOME_COURTHOUSE, "SOME_CASE_NUMBER_3");
        CourtCaseEntity case4 = dartsDatabase.createCase("SOME OTHER COURTHOUSE", "SOME_CASE_NUMBER_0");

        dartsDatabase.getHearingStub().createHearing("SOME OTHER COURTHOUSE", SOME_ROOM, "SOME_CASE_NUMBER_0",
                                                     DateConverterUtil.toLocalDateTime(testTime));

        // when
        List<CourtCaseEntity> returnedCourtCases = caseRepository.findAllWithIdMatchingOneOf(List.of(
            case1.getId(), case3.getId(), case4.getId()
        ));

        // then
        assertThat(returnedCourtCases).hasSize(3);
        assertThat(returnedCourtCases.getFirst().getId()).isEqualTo(case4.getId());
        assertThat(returnedCourtCases.get(1).getId()).isEqualTo(case1.getId());
        assertThat(returnedCourtCases.get(2).getId()).isEqualTo(case3.getId());
    }

    private CourtCaseEntity createCaseWithRetention(String caseNumber,
                                                    CaseRetentionStatus retentionStatus,
                                                    OffsetDateTime retainUntilAppliedOn,
                                                    OffsetDateTime createdDateTime) {
        CourtCaseEntity courtCase = dartsDatabase.createCase(SOME_COURTHOUSE, caseNumber);
        courtCase.setClosed(true);
        courtCase.setRetentionUpdated(false);
        courtCase.setRetentionRetries(0);
        courtCase = dartsDatabase.save(courtCase);
        createCaseRetention(courtCase, retentionStatus, retainUntilAppliedOn, createdDateTime);
        return courtCase;
    }

    private void createCaseRetention(CourtCaseEntity courtCase,
                                     CaseRetentionStatus retentionStatus,
                                     OffsetDateTime retainUntilAppliedOn,
                                     OffsetDateTime createdDateTime) {
        CaseRetentionEntity caseRetention = dartsDatabase.createCaseRetentionObject(
            courtCase,
            retentionStatus,
            OffsetDateTime.now().plusDays(30),
            false
        );
        caseRetention.setRetainUntilAppliedOn(retainUntilAppliedOn);
        caseRetention.setCreatedDateTime(createdDateTime);
        dartsDatabase.save(caseRetention);
    }

    private Long createLinkedTranscriptionDocument(CourtCaseEntity courtCase, OffsetDateTime uploadedAt) {
        var transcription = dartsDatabase.getTranscriptionStub().createTranscription(courtCase);
        var transcriptionDocument = dartsDatabase.getTranscriptionDocumentStub().createTranscriptionDocumentForTranscription(transcription);
        transcriptionDocument.setUploadedDateTime(uploadedAt);
        dartsDatabase.save(transcriptionDocument);
        return transcription.getId();
    }

    private void createTranscriptionLinkedCase(CourtCaseEntity courtCase, Long transcriptionId) {
        var transcriptionLinkedCase = TranscriptionLinkedCaseEntity.builder()
            .transcription(transcriptionRepository.getReferenceById(transcriptionId))
            .courtCase(courtCase)
            .build();
        dartsDatabase.save(transcriptionLinkedCase);
    }

    private Integer createLinkedAnnotationDocument(CourtCaseEntity courtCase, OffsetDateTime uploadedAt) {
        var hearing = dartsDatabase.getHearingStub().createHearing(
            SOME_COURTHOUSE,
            SOME_ROOM,
            courtCase.getCaseNumber(),
            DateConverterUtil.toLocalDateTime(testTime)
        );
        var uploadedBy = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        var annotation = dartsDatabase.getAnnotationStub().createAndSaveAnnotationEntityWith(uploadedBy, "annotation", hearing);
        var annotationDocument = dartsDatabase.getAnnotationStub().createAndSaveAnnotationDocumentEntityWith(
            annotation,
            "annotation.txt",
            "text/plain",
            100,
            uploadedBy,
            uploadedAt,
            "checksum"
        );
        annotationDocument.setUploadedDateTime(uploadedAt);
        dartsDatabase.save(annotationDocument);
        return annotation.getId();
    }

    private void createHearingAnnotation(CourtCaseEntity courtCase, Integer annotationId) {
        var hearing = dartsDatabase.getHearingStub().createHearing(
            SOME_COURTHOUSE,
            SOME_ROOM,
            courtCase.getCaseNumber(),
            DateConverterUtil.toLocalDateTime(testTime)
        );
        var annotation = annotationRepository.findById(annotationId).orElseThrow();
        annotation.addHearing(hearing);
        annotationRepository.save(annotation);
    }

    private void assertCaseRetentionProcessingReset(Integer caseId) {
        CourtCaseEntity courtCase = caseRepository.findById(caseId).orElseThrow();
        assertThat(courtCase.isRetentionUpdated()).isTrue();
        assertThat(courtCase.getRetentionRetries()).isZero();
    }

    private void assertCaseRetentionProcessingUnchanged(Integer caseId, int expectedRetentionRetries) {
        CourtCaseEntity courtCase = caseRepository.findById(caseId).orElseThrow();
        assertThat(courtCase.isRetentionUpdated()).isFalse();
        assertThat(courtCase.getRetentionRetries()).isEqualTo(expectedRetentionRetries);
    }
}
