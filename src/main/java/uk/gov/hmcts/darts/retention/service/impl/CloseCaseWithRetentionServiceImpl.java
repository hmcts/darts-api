package uk.gov.hmcts.darts.retention.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.common.entity.CaseManagementRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CaseRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.CaseRetentionRepository;
import uk.gov.hmcts.darts.event.model.CreatedHearingAndEvent;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.event.model.DartsEventRetentionPolicy;
import uk.gov.hmcts.darts.event.model.stopandclosehandler.PendingRetention;
import uk.gov.hmcts.darts.event.service.CaseManagementRetentionService;
import uk.gov.hmcts.darts.retention.api.RetentionApi;
import uk.gov.hmcts.darts.retention.enums.CaseRetentionStatus;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceCategoryEnum;
import uk.gov.hmcts.darts.retention.enums.RetentionPolicyEnum;
import uk.gov.hmcts.darts.retention.service.CloseCaseWithRetentionService;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static java.lang.Boolean.TRUE;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloseCaseWithRetentionServiceImpl implements CloseCaseWithRetentionService {

    private final CaseRetentionRepository caseRetentionRepository;
    private final CaseManagementRetentionService caseManagementRetentionService;
    private final AuthorisationApi authorisationApi;
    private final RetentionApi retentionApi;
    private final CaseRepository caseRepository;

    @Value("${darts.retention.overridable-fixed-policy-keys}")
    List<String> overridableFixedPolicyKeys;

    @Override
    public void closeCaseAndSetRetention(DartsEvent dartsEvent, CreatedHearingAndEvent hearingAndEvent, CourtCaseEntity courtCase) {
        setDefaultPolicyIfNotDefined(dartsEvent);

        CaseManagementRetentionEntity caseManagementRetentionEntity = caseManagementRetentionService.createCaseManagementRetention(
            hearingAndEvent.getEventEntity(),
            hearingAndEvent.getHearingEntity().getCourtCase(),
            dartsEvent.getRetentionPolicy());

        Optional<CaseRetentionEntity> latestCompletedManualRetention = caseRetentionRepository.findLatestCompletedManualRetention(courtCase);
        if (latestCompletedManualRetention.isPresent()) {
            log.info("Ignoring retention for event with id {} because there is an existing manual retention for caseId {}.",
                     dartsEvent.getEventId(), courtCase.getId());
            return;
        }

        // ignore the caseTotalSentence if it's not an overridable policy
        if (dartsEvent.getRetentionPolicy().getCaseTotalSentence() != null && !overridableFixedPolicyKeys.contains(
            dartsEvent.getRetentionPolicy().getCaseRetentionFixedPolicy())) {
            dartsEvent.getRetentionPolicy().setCaseTotalSentence(null);
        }

        closeCase(dartsEvent, courtCase);

        Optional<PendingRetention> latestPendingRetentionOpt = caseRetentionRepository.findLatestPendingRetention(courtCase);
        if (latestPendingRetentionOpt.isEmpty()) {
            createRetention(caseManagementRetentionEntity, hearingAndEvent, dartsEvent);
        } else {
            PendingRetention latestPendingRetention = latestPendingRetentionOpt.get();
            if (dartsEvent.getDateTime().isAfter(latestPendingRetention.getEventTimestamp())) {
                updateExistingRetention(caseManagementRetentionEntity, latestPendingRetention.getCaseRetention(), dartsEvent);
            } else {
                log.info("Ignoring event with id {} because its event time {} is not after the latest pending entry {} for caseId {}.", dartsEvent.getEventId(),
                         dartsEvent.getDateTime(), latestPendingRetention.getEventTimestamp(), courtCase.getId());
            }
        }
    }


    private void setDefaultPolicyIfNotDefined(DartsEvent dartsEvent) {
        if (dartsEvent.getRetentionPolicy() == null) {
            DartsEventRetentionPolicy defaultRetentionPolicy = new DartsEventRetentionPolicy();
            defaultRetentionPolicy.setCaseRetentionFixedPolicy(RetentionPolicyEnum.DEFAULT.getPolicyKey());
            dartsEvent.setRetentionPolicy(defaultRetentionPolicy);
        }
    }

    private void closeCase(DartsEvent dartsEvent, CourtCaseEntity courtCase) {
        courtCase.setClosed(TRUE);
        courtCase.setCaseClosedTimestamp(dartsEvent.getDateTime());
        caseRepository.saveAndFlush(
            retentionApi.updateCourtCaseConfidenceAttributesForRetention(courtCase, RetentionConfidenceCategoryEnum.CASE_CLOSED)
        );
    }

    private void updateExistingRetention(CaseManagementRetentionEntity caseManagementRetentionEntity, CaseRetentionEntity existingCaseRetention,
                                         DartsEvent dartsEvent) {
        DartsEventRetentionPolicy dartsEventRetentionPolicy = dartsEvent.getRetentionPolicy();
        existingCaseRetention.setRetentionPolicyType(caseManagementRetentionEntity.getRetentionPolicyTypeEntity());
        existingCaseRetention.setTotalSentence(dartsEventRetentionPolicy.getCaseTotalSentence());

        OffsetDateTime eventTimestamp = dartsEvent.getDateTime();
        LocalDate eventDate = eventTimestamp.toLocalDate();
        LocalDate retentionDate = retentionApi.applyPolicyStringToDate(eventDate,
                                                                       dartsEventRetentionPolicy.getCaseTotalSentence(),
                                                                       caseManagementRetentionEntity.getRetentionPolicyTypeEntity());

        existingCaseRetention.setRetainUntil(retentionDate.atStartOfDay().atOffset(ZoneOffset.UTC));
        existingCaseRetention.setCaseManagementRetention(caseManagementRetentionEntity);
        UserAccountEntity currentUser = authorisationApi.getCurrentUser();
        existingCaseRetention.setSubmittedBy(currentUser);
        existingCaseRetention.setCreatedBy(currentUser);
        existingCaseRetention.setLastModifiedBy(currentUser);
        existingCaseRetention.setConfidenceCategory(RetentionConfidenceCategoryEnum.CASE_CLOSED);
        caseRetentionRepository.save(existingCaseRetention);
    }

    private void createRetention(CaseManagementRetentionEntity caseManagementRetentionEntity,
                                 CreatedHearingAndEvent hearingAndEvent,
                                 DartsEvent dartsEvent) {
        DartsEventRetentionPolicy dartsEventRetentionPolicy = dartsEvent.getRetentionPolicy();
        CourtCaseEntity courtCase = hearingAndEvent.getHearingEntity().getCourtCase();

        CaseRetentionEntity caseRetentionEntity = new CaseRetentionEntity();
        caseRetentionEntity.setCourtCase(courtCase);
        caseRetentionEntity.setRetentionPolicyType(caseManagementRetentionEntity.getRetentionPolicyTypeEntity());
        caseRetentionEntity.setCaseManagementRetention(caseManagementRetentionEntity);
        caseRetentionEntity.setTotalSentence(dartsEventRetentionPolicy.getCaseTotalSentence());
        caseRetentionEntity.setConfidenceCategory(RetentionConfidenceCategoryEnum.CASE_CLOSED);
        OffsetDateTime eventTimestamp = dartsEvent.getDateTime();
        LocalDate eventDate = eventTimestamp.toLocalDate();
        LocalDate retentionDate = retentionApi.applyPolicyStringToDate(eventDate,
                                                                       dartsEventRetentionPolicy.getCaseTotalSentence(),
                                                                       caseManagementRetentionEntity.getRetentionPolicyTypeEntity());

        caseRetentionEntity.setRetainUntil(retentionDate.atStartOfDay().atOffset(ZoneOffset.UTC));
        caseRetentionEntity.setCurrentState(CaseRetentionStatus.PENDING.name());
        UserAccountEntity currentUser = authorisationApi.getCurrentUser();
        caseRetentionEntity.setSubmittedBy(currentUser);
        caseRetentionEntity.setCreatedBy(currentUser);
        caseRetentionEntity.setLastModifiedBy(currentUser);
        caseRetentionRepository.save(caseRetentionEntity);
    }

}
