package uk.gov.hmcts.darts.common.helper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Use these methods in the code any time the current date/time is used, so that the tests can mock the method
 * and override the time the code thinks it is.
 * Please use java.time.Clock.Clock see {@link uk.gov.hmcts.darts.common.entity.listener.UserAuditListener} as an example of how this can be used.
 *
 * @deprecated This class is deprecated and will be removed in the future.
 */
@Component
@RequiredArgsConstructor
@Deprecated(since = "10/12/2024")
public class CurrentTimeHelper {

    public OffsetDateTime currentOffsetDateTime() {
        return OffsetDateTime.now();
    }

    public LocalDate currentLocalDate() {
        return LocalDate.now();
    }

}
