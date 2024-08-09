package uk.gov.hmcts.darts.notification.dto;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.util.List;
import java.util.Map;

/**
 * Data Transfer Object (DTO) used to specify data for saving a notification to the database.
 *
 * <p>This DTO offers two ways to define notification recipients:
 * - **`userAccountsToEmail`**: This list of {@link UserAccountEntity} objects is preferred as it
 * allows verification of user account activity before scheduling notifications.
 * - **`emailAddresses`**: This property accepts a comma-separated string of email addresses.
 * Use this option only when notifying users without associated user accounts is necessary.
 *
 * <p>It's recommended to prioritize `userAccountsToEmail` for better notification management
 * and to ensure notifications reach active users.
 */
@Data
@Builder
public class SaveNotificationToDbRequest {
    String eventId;
    Integer caseId;
    String emailAddresses;
    List<UserAccountEntity> userAccountsToEmail;
    Map<String, String> templateValues;
    UserAccountEntity userAccount;
}
