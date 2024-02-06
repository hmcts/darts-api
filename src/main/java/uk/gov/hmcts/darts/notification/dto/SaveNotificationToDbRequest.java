package uk.gov.hmcts.darts.notification.dto;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class SaveNotificationToDbRequest {

    String eventId;
    Integer caseId;
    String emailAddresses;
    List<UserAccountEntity> userAccountsToEmail;
    Map<String, String> templateValues;
}
