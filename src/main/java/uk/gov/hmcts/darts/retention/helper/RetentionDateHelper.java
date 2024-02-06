package uk.gov.hmcts.darts.retention.helper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class RetentionDateHelper {

    private final RetentionPolicyTypeRepository retentionPolicyTypeRepository;
    private final CurrentTimeHelper currentTimeHelper;

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

    private LocalDate applyPolicyString(LocalDate dateToAppend, String policyString) {
        String[] policyArray = StringUtils.splitByCharacterType(StringUtils.trimToEmpty(policyString));

        int years = getValueFromPolicyArray(policyArray, "Y");
        int months = getValueFromPolicyArray(policyArray, "M");
        int days = getValueFromPolicyArray(policyArray, "D");

        LocalDate newDate = dateToAppend.plusYears(years);
        newDate = newDate.plusMonths(months);
        newDate = newDate.plusDays(days);
        return newDate;
    }

    private int getValueFromPolicyArray(String[] policyArray, String searchString) {
        for (int counter = 0; counter < policyArray.length; counter++) {
            if (policyArray[counter].equals(searchString)) {
                return NumberUtils.toInt(ArrayUtils.get(policyArray, counter - 1));
            }
        }
        return 0;
    }
}
