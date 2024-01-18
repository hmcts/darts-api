package uk.gov.hmcts.darts.retention.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.common.entity.CaseOverflowEntity;
import uk.gov.hmcts.darts.common.entity.CaseRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.CaseOverflowRepository;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.CaseRetentionRepository;
import uk.gov.hmcts.darts.retention.enums.CaseRetentionStatus;
import uk.gov.hmcts.darts.retention.exception.RetentionApiError;
import uk.gov.hmcts.darts.retention.service.RetentionPostService;
import uk.gov.hmcts.darts.retentions.model.PostRetentionRequest;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RetentionPostServiceImpl implements RetentionPostService {

    public static final List<SecurityRoleEnum> JUDGE_AND_ADMIN = List.of(SecurityRoleEnum.JUDGE, SecurityRoleEnum.ADMIN);
    private final CaseRepository caseRepository;
    private final CaseRetentionRepository caseRetentionRepository;
    private final AuthorisationApi authorisationApi;
    private final CaseOverflowRepository caseOverflowRepository;
    private final CurrentTimeHelper currentTimeHelper;

    @Override
    public void postRetention(PostRetentionRequest postRetentionRequest) {

        Optional<CourtCaseEntity> caseOpt = caseRepository.findById(postRetentionRequest.getCaseId());
        if (caseOpt.isEmpty()) {
            throw new DartsApiException(
                RetentionApiError.CASE_NOT_FOUND,
                MessageFormat.format("The selected caseId ''{0}'' cannot be found.", postRetentionRequest.getCaseId())
            );
        }

        CourtCaseEntity courtCase = caseOpt.get();
        validation(postRetentionRequest, courtCase);

        OffsetDateTime newRetentionDate;
        if (postRetentionRequest.getIsPermanentRetention()) {

            newRetentionDate = currentTimeHelper.currentOffsetDateTime().plus(99, ChronoUnit.YEARS);
        } else {
            newRetentionDate = postRetentionRequest.getRetentionDate().atTime(OffsetTime.of(0, 0, 0, 0, ZoneOffset.UTC));
        }
        createNewCaseRetention(postRetentionRequest, courtCase, newRetentionDate);

    }

    private void validation(PostRetentionRequest postRetentionRequest, CourtCaseEntity courtCase) {
        //No retention can be applied/amended when then case is open
        if (BooleanUtils.isNotTrue(courtCase.getClosed())) {
            log.error("A retention police of {} was attempted to be applied to an open case", postRetentionRequest);
            throw new DartsApiException(
                RetentionApiError.CASE_NOT_CLOSED,
                MessageFormat.format("caseId ''{0}'' must be closed before the retention period can be amended.", courtCase.getId())
            );
        }


        //No retention can be applied/amended when no current retention policy has been applied
        List<CaseRetentionEntity> caseRetentions = caseRetentionRepository.findByCourtCase_Id(courtCase.getId());
        if (caseRetentions.isEmpty()) {
            throw new DartsApiException(
                RetentionApiError.NO_RETENTION_POLICIES_APPLIED,
                MessageFormat.format("caseId ''{0}'' must have a retention policy applied before being changed.", courtCase.getId())
            );
        }

        if (!postRetentionRequest.getIsPermanentRetention()) {
            //No users can reduce the retention date to earlier than the initial Case Management date input.
            CaseRetentionEntity firstCaseRetention = getFirstCaseRetention(caseRetentions);
            LocalDate originalRetentionDate = firstCaseRetention.getRetainUntil().toLocalDate();
            LocalDate newRetentionDate = postRetentionRequest.getRetentionDate();
            if (newRetentionDate.isBefore(originalRetentionDate)) {
                throw new DartsApiException(
                    RetentionApiError.RETENTION_DATE_TO_EARLY,
                    MessageFormat.format("caseId ''{0}'' must have a retention date after the original {1}.", courtCase.getId(), originalRetentionDate)
                );
            }


            //Only Judges and Admin can reduce a set retention date
            CaseOverflowEntity caseOverflow = getCaseOverflowEntity(courtCase);
            LocalDate currentRetentionDate = caseOverflow.getRetainUntilTs().toLocalDate();
            if (newRetentionDate.isBefore(currentRetentionDate)) {
                if (!authorisationApi.userHasOneOfGlobalRoles(JUDGE_AND_ADMIN)) {
                    throw new DartsApiException(
                        RetentionApiError.NO_PERMISSION_REDUCE_RETENTION_ERROR, "You do not have permission to reduce the retention period."
                    );

                }
            }

        }
    }

    private CaseOverflowEntity getCaseOverflowEntity(CourtCaseEntity courtCase) {
        Optional<CaseOverflowEntity> caseOverflowOpt = caseOverflowRepository.findById(courtCase.getId());
        if (caseOverflowOpt.isEmpty()) {
            throw new DartsApiException(
                RetentionApiError.NO_RETENTION_POLICIES_APPLIED,
                MessageFormat.format("caseId ''{0}'' must have a corresponding case Overflow record.", courtCase.getId())
            );
        }
        CaseOverflowEntity caseOverflow = caseOverflowOpt.get();
        return caseOverflow;
    }

    private CaseRetentionEntity getFirstCaseRetention(List<CaseRetentionEntity> caseRetentions) {
        return caseRetentions.stream()
            .sorted(Comparator.comparing(CaseRetentionEntity::getCreatedDateTime))
            .findFirst().orElse(null);
    }

    private CaseRetentionEntity createNewCaseRetention(PostRetentionRequest postRetentionRequest, CourtCaseEntity courtCase,
                                                       OffsetDateTime newRetentionDate) {
        CaseRetentionEntity caseRetention = new CaseRetentionEntity();
        caseRetention.setCourtCase(courtCase);
        caseRetention.setManualOverride(true);
        UserAccountEntity currentUser = authorisationApi.getCurrentUser();
        caseRetention.setLastModifiedBy(currentUser);
        caseRetention.setCreatedBy(currentUser);
        caseRetention.setSubmittedBy(currentUser);
        caseRetention.setComments(postRetentionRequest.getComments());
        caseRetention.setRetainUntil(newRetentionDate);
        caseRetention.setCurrentState(String.valueOf(CaseRetentionStatus.PENDING));
        caseRetention.setRetainUntilAppliedOn(currentTimeHelper.currentOffsetDateTime());
        caseRetentionRepository.save(caseRetention);
        return caseRetention;
    }

}
