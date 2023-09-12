package uk.gov.hmcts.darts.audit;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import uk.gov.hmcts.darts.audit.model.AuditSearchQuery;
import uk.gov.hmcts.darts.common.exception.AuditApiError;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

public class AuditSearchQueryValidator implements Validator {
    @Override
    public boolean supports(Class<?> clazz) {
        return AuditSearchQuery.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        AuditSearchQuery auditSearchQuery = (AuditSearchQuery) target;
        if (auditSearchQuery.getCaseId() == null
            && auditSearchQuery.getAuditActivityId() == null
            && auditSearchQuery.getFromDate() == null
            && auditSearchQuery.getToDate() == null) {
            throw new DartsApiException(AuditApiError.FILTERS_WERE_EMPTY);
        }

        if (auditSearchQuery.getFromDate() == null ^ auditSearchQuery.getToDate() == null) {
            throw new DartsApiException(AuditApiError.DATE_EMPTY);
        }
    }
}
