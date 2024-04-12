package uk.gov.hmcts.darts.retention.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.cases.service.CaseService;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.exception.DartsException;
import uk.gov.hmcts.darts.common.repository.AnnotationDocumentRepository;
import uk.gov.hmcts.darts.common.repository.CaseRetentionRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.retention.service.ApplyRetentionCaseAssociatedObjectsSingleCaseProcessor;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplyRetentionCaseAssociatedObjectsSingleCaseProcessorImpl implements ApplyRetentionCaseAssociatedObjectsSingleCaseProcessor {

    private final CaseRetentionRepository caseRetentionRepository;
    private final CaseService caseService;
    private final ExternalObjectDirectoryRepository eodRepository;
    private final MediaRepository mediaRepository;
    private final AnnotationDocumentRepository annotationDocumentRepository;

    @Transactional
    public void processApplyRetentionToCaseAssociatedObjects(Integer caseId) {

        log.info("applying retention to associated objects for case id '{}'", caseId);

        var courtCase = caseService.getCourtCaseById(caseId);

        applyRetentionToMedias(courtCase);
        applyRetentionToAnnotations(courtCase);
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
                    if (armEods.size() == 1) {
                        armEods.get(0).setUpdateRetention(true);
                    } else {
                        throw new DartsException(String.format("Expecting one arm EOD for media '%s' but found zero or more", media.getId()));
                    }
                } else {
                    throw new DartsException(String.format("No retentions found on cases for media '%s'", media.getId()));
                }
            }
        }
    }

    private void applyRetentionToAnnotations(CourtCaseEntity courtCase) {

        var annotationDocuments = annotationDocumentRepository.findAllByCaseId(courtCase.getId());

        for (var annotationDocument : annotationDocuments) {
            var cases = annotationDocument.associatedCourtCases();
            if (allClosed(cases)) {
                var longestRetentionDate = findLongestRetentionDate(cases);
                if (longestRetentionDate != null) {
                    annotationDocument.setRetainUntilTs(longestRetentionDate);
                    var armEods = eodRepository.findByAnnotationDocumentEntityAndExternalLocationType(annotationDocument, EodHelper.armLocation());
                    if (armEods.size() == 1) {
                        armEods.get(0).setUpdateRetention(true);
                    } else {
                        throw new DartsException(
                            String.format("Expecting one arm EOD for annotationDocument '%s' but found zero or more", annotationDocument.getId()));
                    }
                } else {
                    throw new DartsException(String.format("No retentions found on cases for annotationDocument '%s'", annotationDocument.getId()));
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

}

