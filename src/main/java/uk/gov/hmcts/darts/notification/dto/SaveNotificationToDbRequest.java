package uk.gov.hmcts.darts.notification.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SaveNotificationToDbRequest {
    String eventId;
    Integer caseId;
    String emailAddresses;
    String templateValues;
}
