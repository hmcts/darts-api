package uk.gov.hmcts.darts.audit;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import uk.gov.hmcts.darts.audit.model.AuditSearchQuery;

public class AuditSearchQueryValidator implements Validator {
    @Override
    public boolean supports(Class<?> clazz) {
        return AuditSearchQuery.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        AuditSearchQuery auditSearchQuery = (AuditSearchQuery) target;
        if (auditSearchQuery.getCaseId() == null
            && auditSearchQuery.getEventId() == null
            && auditSearchQuery.getFromDate() == null
            && auditSearchQuery.getToDate() == null) {
            errors.rejectValue(null, "filters.empty", "All filters were empty.");
            return;
        }

        if (auditSearchQuery.getFromDate() == null ^ auditSearchQuery.getToDate() == null) {
            errors.rejectValue(null, "dates.empty", "When using date filters, both must be provided.");
        }
    }
}
