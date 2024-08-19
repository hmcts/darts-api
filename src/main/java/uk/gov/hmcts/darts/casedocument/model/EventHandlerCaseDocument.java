package uk.gov.hmcts.darts.casedocument.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class EventHandlerCaseDocument extends CreatedCaseDocument {

    private Integer id;
    private String type;
    private String subType;
    private String eventName;
    private String handler;
    private Boolean active;
    private boolean isReportingRestriction;
}
