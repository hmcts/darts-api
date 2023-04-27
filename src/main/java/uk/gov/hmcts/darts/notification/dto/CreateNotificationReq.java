package uk.gov.hmcts.darts.notification.dto;

import lombok.Data;

@Data
public class CreateNotificationReq {
    String eventId;
    String caseId;
    String emailAddress;
    String templateValues;
}
