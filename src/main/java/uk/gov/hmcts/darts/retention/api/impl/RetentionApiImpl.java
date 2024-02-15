package uk.gov.hmcts.darts.retention.api.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.RetentionPolicyTypeEntity;
import uk.gov.hmcts.darts.retention.api.RetentionApi;
import uk.gov.hmcts.darts.retention.helper.RetentionDateHelper;

import java.time.LocalDate;

@RequiredArgsConstructor
@Service
public class RetentionApiImpl implements RetentionApi {

    private final RetentionDateHelper retentionDateHelper;

    @Override
    public LocalDate applyPolicyStringToDate(LocalDate dateToAppend, String policyString, RetentionPolicyTypeEntity retentionPolicyType) {
        return retentionDateHelper.applyPolicyString(dateToAppend, policyString, retentionPolicyType);
    }
}
