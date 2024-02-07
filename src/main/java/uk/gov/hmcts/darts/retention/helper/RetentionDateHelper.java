package uk.gov.hmcts.darts.retention.helper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.RetentionPolicyTypeEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.RetentionPolicyTypeRepository;
import uk.gov.hmcts.darts.retention.enums.RetentionPolicyEnum;
import uk.gov.hmcts.darts.retention.exception.RetentionApiError;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class RetentionDateHelper {

    private final RetentionPolicyTypeRepository retentionPolicyTypeRepository;
    private final CurrentTimeHelper currentTimeHelper;
    private Pattern policyFormat = Pattern.compile("^\\d+Y\\d+M\\d+D$");

    public LocalDate getRetentionDateForPolicy(CourtCaseEntity courtCase, RetentionPolicyEnum policy) {
        RetentionPolicyTypeEntity retentionPolicy = getRetentionPolicy(policy);
        return applyPolicyString(courtCase.getCaseClosedTimestamp().toLocalDate(), retentionPolicy.getDuration());
    }

    public RetentionPolicyTypeEntity getRetentionPolicy(RetentionPolicyEnum policy) {
        Optional<RetentionPolicyTypeEntity> manualPolicyEntityOpt = retentionPolicyTypeRepository.findCurrentWithFixedPolicyKey(
                policy.getPolicyKey(),
                currentTimeHelper.currentOffsetDateTime()
        );
        if (manualPolicyEntityOpt.isEmpty()) {
            throw new DartsApiException(
                    RetentionApiError.INTERNAL_SERVER_ERROR,
                    MessageFormat.format("Cannot find Policy with FixedPolicyKey ''{0}''", policy.getPolicyKey())
            );
        }
        return manualPolicyEntityOpt.get();
    }

    public LocalDate applyPolicyString(LocalDate dateToAppend, String policyString) {
        if (!policyFormat.matcher(policyString).matches()) {
            throw new DartsApiException(
                    RetentionApiError.INTERNAL_SERVER_ERROR,
                    MessageFormat.format("PolicyString ''{0}'', is not in the required format.", policyString)
            );
        }

        String[] policyArray = StringUtils.splitByCharacterType(StringUtils.trimToEmpty(policyString));

        LocalDate newDate = dateToAppend.plusYears(NumberUtils.toInt(policyArray[0]));
        newDate = newDate.plusMonths(NumberUtils.toInt(policyArray[2]));
        newDate = newDate.plusDays(NumberUtils.toInt(policyArray[4]));
        return newDate;
    }

    public LocalDate applyPolicyString(LocalDate dateToAppend, String policyString, RetentionPolicyTypeEntity retentionPolicyType) {
        if (RetentionPolicyEnum.CUSTODIAL.getPolicyKey().equals(retentionPolicyType.getFixedPolicyKey())) {
            //take max of policy/totalSentence
            LocalDate policyDate = applyPolicyString(dateToAppend, retentionPolicyType.getDuration());
            LocalDate termInRequest = applyPolicyString(dateToAppend, policyString);
            return latestDate(policyDate, termInRequest);
        }

        return applyPolicyString(dateToAppend, retentionPolicyType.getDuration());
    }

    private LocalDate latestDate(LocalDate date1, LocalDate date2) {
        if (date1.isAfter(date2)) {
            return date1;
        } else {
            return date2;
        }
    }
}
