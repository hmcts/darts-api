package uk.gov.hmcts.darts.retention.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.cases.service.CaseService;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.exception.DartsException;
import uk.gov.hmcts.darts.common.repository.AnnotationDocumentRepository;
import uk.gov.hmcts.darts.common.repository.CaseDocumentRepository;
import uk.gov.hmcts.darts.common.repository.CaseRetentionRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.retention.service.ApplyRetentionCaseAssociatedObjectsSingleCaseProcessor;
import uk.gov.hmcts.darts.transcriptions.service.TranscriptionService;

import java.time.OffsetDateTime;
import java.util.List;

import static java.lang.String.format;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplyRetentionCaseAssociatedObjectsSingleCaseProcessorImpl implements ApplyRetentionCaseAssociatedObjectsSingleCaseProcessor {

    private final CaseRetentionRepository caseRetentionRepository;
    private final CaseService caseService;
    private final ExternalObjectDirectoryRepository eodRepository;
    private final MediaRepository mediaRepository;
    private final AnnotationDocumentRepository annotationDocumentRepository;
    private final TranscriptionService transcriptionService;
    private final CaseDocumentRepository caseDocumentRepository;

    @Transactional
    @Override
    public void processApplyRetentionToCaseAssociatedObjects(Integer caseId) {

        log.info("applying retention to associated objects for case id '{}'", caseId);

        var courtCase = caseService.getCourtCaseById(caseId);

        applyRetentionToMedias(courtCase);
        applyRetentionToAnnotations(courtCase);
        applyRetentionToTranscriptions(courtCase);
        applyRetentionToCaseDocuments(courtCase);
    }

    private void applyRetentionToMedias(CourtCaseEntity courtCase) {

        var medias = mediaRepository.findAllByCaseId(courtCase.getId());

        for (var media : medias) {
            var cases = media.associatedCourtCases();
            if (allClosed(cases)) {
                var longestRetentionDate = findLongestRetentionDate(cases);
                if (longestRetentionDate != null) {
                    media.setRetainUntilTs(longestRetentionDate);
                    var armEods = eodRepository.findByMediaAndExternalLocationType(media, EodHelper.armLocation());
                    updateArmEodRetention(
                        armEods,
                        format("Expecting one arm EOD for media '%s' but found zero or more than one", media.getId())
                    );
                } else {
                    throw new DartsException(format("No retentions found on cases for media '%s'", media.getId()));
                }
            }
        }
    }

    private void applyRetentionToAnnotations(CourtCaseEntity courtCase) {

        var annotationDocuments = annotationDocumentRepository.findAllByCaseId(courtCase.getId());

        for (var annotationDoc : annotationDocuments) {
            var cases = annotationDoc.associatedCourtCases();
            if (allClosed(cases)) {
                var longestRetentionDate = findLongestRetentionDate(cases);
                if (longestRetentionDate != null) {
                    annotationDoc.setRetainUntilTs(longestRetentionDate);
                    var armEods = eodRepository.findByAnnotationDocumentEntityAndExternalLocationType(annotationDoc, EodHelper.armLocation());
                    updateArmEodRetention(
                        armEods,
                        format("Expecting one arm EOD for annotationDocument '%s' but found zero or more than one", annotationDoc.getId())
                    );
                } else {
                    throw new DartsException(format("No retentions found on cases for annotationDocument '%s'", annotationDoc.getId()));
                }
            }
        }
    }

    private void applyRetentionToTranscriptions(CourtCaseEntity courtCase) {

        var transcriptionDocuments = transcriptionService.getAllCaseTranscriptionDocuments(courtCase.getId());

        for (var transcriptionDoc : transcriptionDocuments) {
            var cases = transcriptionDoc.getTranscription().getAssociatedCourtCases();
            if (allClosed(cases)) {
                var longestRetentionDate = findLongestRetentionDate(cases);
                if (longestRetentionDate != null) {
                    transcriptionDoc.setRetainUntilTs(longestRetentionDate);
                    var armEods = eodRepository.findByTranscriptionDocumentEntityAndExternalLocationType(transcriptionDoc, EodHelper.armLocation());
                    updateArmEodRetention(
                        armEods,
                        format("Expecting one arm EOD for transcriptionDocument '%s' but found zero or more than one", transcriptionDoc.getId())
                    );
                } else {
                    throw new DartsException(format("No retentions found on cases for transcriptionDocument '%s'", transcriptionDoc.getId()));
                }
            }
        }
    }

    private void applyRetentionToCaseDocuments(CourtCaseEntity courtCase) {

        var caseDocuments = caseDocumentRepository.findByCourtCase(courtCase);

        for (var caseDocument : caseDocuments) {
            if (allClosed(List.of(courtCase))) {
                var longestRetentionDate = findLongestRetentionDate(List.of(courtCase));
                if (longestRetentionDate != null) {
                    caseDocument.setRetainUntilTs(longestRetentionDate);
                    var armEods = eodRepository.findByCaseDocumentAndExternalLocationType(caseDocument, EodHelper.armLocation());
                    updateArmEodRetention(
                        armEods,
                        format("Expecting one arm EOD for caseDocument '%s' but found zero or more than one", caseDocument.getId())
                    );
                } else {
                    throw new DartsException(format("No retentions found on courtCase for caseDocument '%s'", caseDocument.getId()));
                }
            }
        }
    }

    private boolean allClosed(List<CourtCaseEntity> cases) {
        return cases.stream().allMatch(CourtCaseEntity::getClosed);
    }

    private OffsetDateTime findLongestRetentionDate(List<CourtCaseEntity> cases) {
        OffsetDateTime longestRetentionDate = null;
        for (var courCase : cases) {
            var mostRecentRetentionRecordOpt = caseRetentionRepository.findTopByCourtCaseOrderByRetainUntilAppliedOnDesc(courCase);
            if (mostRecentRetentionRecordOpt.isPresent()) {
                var retention = mostRecentRetentionRecordOpt.get().getRetainUntil();
                if (longestRetentionDate == null) {
                    longestRetentionDate = retention;
                } else if (retention.isAfter(longestRetentionDate)) {
                    longestRetentionDate = retention;
                }
            }
        }
        return longestRetentionDate;
    }

    private void updateArmEodRetention(List<ExternalObjectDirectoryEntity> armEods, String errorMessage) {
        if (armEods.size() == 1) {
            armEods.get(0).setUpdateRetention(true);
        } else {
            throw new DartsException(errorMessage);
        }
    }
}

