package uk.gov.hmcts.darts.casedocument.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class EventHandlerCaseDocument extends CreatedCaseDocument {

    private final Integer id;
    private final String type;
    private final String subType;
    private final String eventName;
    private final String handler;
    private final Boolean active;
    private final Boolean isReportingRestriction;
}
