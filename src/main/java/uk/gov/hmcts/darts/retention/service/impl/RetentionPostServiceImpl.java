package uk.gov.hmcts.darts.retention.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audit.api.AuditActivity;
import uk.gov.hmcts.darts.audit.api.AuditApi;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.common.entity.CaseRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.RetentionPolicyTypeEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.CaseRetentionRepository;
import uk.gov.hmcts.darts.common.repository.RetentionPolicyTypeRepository;
import uk.gov.hmcts.darts.common.util.DateConverterUtil;
import uk.gov.hmcts.darts.retention.enums.CaseRetentionStatus;
import uk.gov.hmcts.darts.retention.enums.RetentionPolicyEnum;
import uk.gov.hmcts.darts.retention.exception.RetentionApiError;
import uk.gov.hmcts.darts.retention.service.RetentionPostService;
import uk.gov.hmcts.darts.retentions.model.PostRetentionRequest;

import java.text.MessageFormat;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RetentionPostServiceImpl implements RetentionPostService {

    public static final List<SecurityRoleEnum> JUDGE_AND_ADMIN_ROLES = List.of(SecurityRoleEnum.JUDGE, SecurityRoleEnum.ADMIN);
    private final CaseRepository caseRepository;
    private final CaseRetentionRepository caseRetentionRepository;
    private final AuthorisationApi authorisationApi;
    private final RetentionPolicyTypeRepository retentionPolicyTypeRepository;
    private final CurrentTimeHelper currentTimeHelper;
    private final AuditApi auditApi;

    @Override
    public void postRetention(PostRetentionRequest postRetentionRequest) {

        Optional<CourtCaseEntity> caseOpt = caseRepository.findById(postRetentionRequest.getCaseId());
        if (caseOpt.isEmpty()) {
            throw new DartsApiException(
                RetentionApiError.CASE_NOT_FOUND,
                MessageFormat.format("The selected caseId ''{0}'' cannot be found.", postRetentionRequest.getCaseId().toString())
            );
        }

        CourtCaseEntity courtCase = caseOpt.get();
        validation(postRetentionRequest, courtCase);

        OffsetDateTime newRetentionDate;
        if (BooleanUtils.isTrue(postRetentionRequest.getIsPermanentRetention())) {
            newRetentionDate = currentTimeHelper.currentOffsetDateTime().plus(99, ChronoUnit.YEARS);
        } else {
            newRetentionDate = DateConverterUtil.toOffsetDateTime(postRetentionRequest.getRetentionDate());
        }
        createNewCaseRetention(postRetentionRequest, courtCase, newRetentionDate);

    }

    private void validation(PostRetentionRequest postRetentionRequest, CourtCaseEntity courtCase) {
        //No retention can be applied/amended when then case is open
        if (BooleanUtils.isNotTrue(courtCase.getClosed())) {
            log.error("A retention policy of {} was attempted to be applied to an open case", postRetentionRequest);
            throw new DartsApiException(
                RetentionApiError.CASE_NOT_CLOSED,
                MessageFormat.format("caseId ''{0}'' must be closed before the retention period can be amended.", courtCase.getId().toString())
            );
        }


        //No retention can be applied/amended when no current retention policy has been applied
        CaseRetentionEntity lastCompletedAutomatedCaseRetention = getLatestCompleteAutomatedCaseRetention(courtCase);

        if (BooleanUtils.isNotTrue(postRetentionRequest.getIsPermanentRetention())) {
            //No users can reduce the retention date to earlier than the last Completed automated date.
            OffsetDateTime latestCompletedRetentionDate = lastCompletedAutomatedCaseRetention.getRetainUntil();
            OffsetDateTime newRetentionDate = DateConverterUtil.toOffsetDateTime(postRetentionRequest.getRetentionDate());
            if (newRetentionDate.isBefore(latestCompletedRetentionDate)) {
                throw new DartsApiException(
                    RetentionApiError.RETENTION_DATE_TOO_EARLY,
                    MessageFormat.format(
                        "caseId ''{0}'' must have a retention date after the last completed automated retention date ''{1}''.",
                        courtCase.getId().toString(),
                        latestCompletedRetentionDate
                    )
                );
            }


            //Only Judges and Admin can reduce a set retention date
            OffsetDateTime currentRetentionDate = getLatestCompletedCaseRetention(courtCase).getRetainUntil();
            if (newRetentionDate.isBefore(currentRetentionDate)) {
                if (!authorisationApi.userHasOneOfRoles(JUDGE_AND_ADMIN_ROLES)) {
                    throw new DartsApiException(
                        RetentionApiError.NO_PERMISSION_REDUCE_RETENTION, "You do not have permission to reduce the retention period."
                    );
                }
            }

        }
    }

    private CaseRetentionEntity getLatestCompleteAutomatedCaseRetention(CourtCaseEntity courtCase) {
        Optional<CaseRetentionEntity> latestCompletedAutomatedRetentionOpt = caseRetentionRepository.findLatestCompletedAutomatedRetention(courtCase);
        if (latestCompletedAutomatedRetentionOpt.isEmpty()) {
            throw new DartsApiException(
                RetentionApiError.NO_RETENTION_POLICIES_APPLIED,
                MessageFormat.format("caseId ''{0}'' must have a retention policy applied before being changed.", courtCase.getId().toString())
            );
        }
        return latestCompletedAutomatedRetentionOpt.get();
    }

    private CaseRetentionEntity getLatestCompletedCaseRetention(CourtCaseEntity courtCase) {
        Optional<CaseRetentionEntity> latestCompletedAutomatedRetentionOpt = caseRetentionRepository.findLatestCompletedRetention(courtCase);
        if (latestCompletedAutomatedRetentionOpt.isEmpty()) {
            throw new DartsApiException(
                RetentionApiError.NO_RETENTION_POLICIES_APPLIED,
                MessageFormat.format("caseId ''{0}'' must have a retention policy applied before being changed.", courtCase.getId().toString())
            );
        }
        return latestCompletedAutomatedRetentionOpt.get();
    }

    private CaseRetentionEntity createNewCaseRetention(PostRetentionRequest postRetentionRequest, CourtCaseEntity courtCase,
                                                       OffsetDateTime newRetentionDate) {
        CaseRetentionEntity caseRetention = new CaseRetentionEntity();
        caseRetention.setCourtCase(courtCase);
        UserAccountEntity currentUser = authorisationApi.getCurrentUser();
        caseRetention.setLastModifiedBy(currentUser);
        caseRetention.setCreatedBy(currentUser);
        caseRetention.setSubmittedBy(currentUser);
        caseRetention.setComments(postRetentionRequest.getComments());
        caseRetention.setRetainUntil(newRetentionDate);
        caseRetention.setCurrentState(String.valueOf(CaseRetentionStatus.COMPLETE));
        caseRetention.setRetainUntilAppliedOn(currentTimeHelper.currentOffsetDateTime());

        caseRetention.setRetentionPolicyType(getRetentionPolicy(postRetentionRequest.getIsPermanentRetention()));

        caseRetentionRepository.saveAndFlush(caseRetention);
        auditApi.recordAudit(
            AuditActivity.APPLY_RETENTION,
            currentUser,
            courtCase
        );
        return caseRetention;
    }

    private RetentionPolicyTypeEntity getRetentionPolicy(Boolean isPermanent) {

        String policyKey;
        if (BooleanUtils.isTrue(isPermanent)) {
            policyKey = RetentionPolicyEnum.PERMANENT.getPolicyKey();
        } else {
            policyKey = RetentionPolicyEnum.MANUAL.getPolicyKey();
        }

        Optional<RetentionPolicyTypeEntity> manualPolicyEntityOpt = retentionPolicyTypeRepository.findCurrentWithFixedPolicyKey(
            policyKey,
            currentTimeHelper.currentOffsetDateTime()
        );
        if (manualPolicyEntityOpt.isEmpty()) {
            throw new DartsApiException(
                RetentionApiError.INTERNAL_SERVER_ERROR,
                MessageFormat.format("Cannot find Policy with FixedPolicyKey ''{0}''", policyKey)
            );
        }
        return manualPolicyEntityOpt.get();
    }

}
