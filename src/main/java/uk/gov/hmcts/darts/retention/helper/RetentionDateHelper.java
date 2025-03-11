package uk.gov.hmcts.darts.retention.helper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class RetentionDateHelper {

    private static final int POLICY_STRING_YEAR_LOCATION = 0;
    private static final int POLICY_STRING_MONTH_LOCATION = 2;
    private static final int POLICY_STRING_DAY_LOCATION = 4;
    private final RetentionPolicyTypeRepository retentionPolicyTypeRepository;
    private final CurrentTimeHelper currentTimeHelper;
    private static final Pattern POLICY_FORMAT = Pattern.compile("^\\d+Y\\d+M\\d+D$");

    @Value("${darts.retention.overridable-fixed-policy-keys}")
    List<String> overridableFixedPolicyKeys;


    public LocalDate getRetentionDateForPolicy(CourtCaseEntity courtCase, RetentionPolicyEnum policy) {
        RetentionPolicyTypeEntity retentionPolicy = getRetentionPolicy(policy);
        return applyPolicyString(courtCase.getCaseClosedTimestamp().toLocalDate(), retentionPolicy.getDuration());
    }

    public RetentionPolicyTypeEntity getRetentionPolicy(RetentionPolicyEnum policy) {
        List<RetentionPolicyTypeEntity> retentionPolicyList = retentionPolicyTypeRepository.findCurrentWithFixedPolicyKey(
            policy.getPolicyKey(),
            currentTimeHelper.currentOffsetDateTime()
        );
        if (retentionPolicyList.isEmpty()) {
            throw new DartsApiException(
                RetentionApiError.INTERNAL_SERVER_ERROR,
                MessageFormat.format("Cannot find Policy with FixedPolicyKey ''{0}''", policy.getPolicyKey())
            );
        } else if (retentionPolicyList.size() > 1) {
            throw new DartsApiException(RetentionApiError.INTERNAL_SERVER_ERROR,
                                        MessageFormat.format("More than 1 retention policy found for fixedPolicyKey ''{0}''", policy.getPolicyKey()));
        }
        return retentionPolicyList.getFirst();
    }

    public LocalDate applyPolicyString(LocalDate dateToAppend, String policyString) {
        if (StringUtils.isBlank(policyString) || !POLICY_FORMAT.matcher(policyString).matches()) {
            throw new DartsApiException(
                RetentionApiError.INTERNAL_SERVER_ERROR,
                MessageFormat.format("PolicyString ''{0}'', is not in the required format.", policyString)
            );
        }

        String[] policyArray = StringUtils.splitByCharacterType(StringUtils.trimToEmpty(policyString));

        LocalDate newDate = dateToAppend.plusYears(NumberUtils.toInt(policyArray[POLICY_STRING_YEAR_LOCATION]));
        newDate = newDate.plusMonths(NumberUtils.toInt(policyArray[POLICY_STRING_MONTH_LOCATION]));
        newDate = newDate.plusDays(NumberUtils.toInt(policyArray[POLICY_STRING_DAY_LOCATION]));
        return newDate;
    }

    public LocalDate applyPolicyString(LocalDate dateToAppend, String policyString, RetentionPolicyTypeEntity retentionPolicyType) {
        if (overridableFixedPolicyKeys.contains(retentionPolicyType.getFixedPolicyKey())) {
            //take max of policy/totalSentence
            LocalDate policyDate = applyPolicyString(dateToAppend, retentionPolicyType.getDuration());
            LocalDate termInRequest = applyPolicyString(dateToAppend, policyString);
            return latestDate(policyDate, termInRequest);
        }

        return applyPolicyString(dateToAppend, retentionPolicyType.getDuration());
    }

    private LocalDate latestDate(LocalDate date1, LocalDate date2) {
        return date1.isAfter(date2) ? date1 : date2;
    }
}
