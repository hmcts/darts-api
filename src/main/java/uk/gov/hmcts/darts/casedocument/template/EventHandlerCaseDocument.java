package uk.gov.hmcts.darts.casedocument.template;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class EventHandlerCaseDocument {

    private OffsetDateTime createdDateTime;
    private Integer createdBy;
    private Integer id;
    private String type;
    private String subType;
    private String eventName;
    private String handler;
    private Boolean active;
    private Boolean isReportingRestriction;
}
