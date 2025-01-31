package uk.gov.hmcts.darts.common.helper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Use these methods in the code any time the current date/time is used, so that the tests can mock the method
 * and override the time the code thinks it is.
 */
@Component
@RequiredArgsConstructor
public class CurrentTimeHelper {

    private final Clock clock;

    public OffsetDateTime currentOffsetDateTime() {
        return OffsetDateTime.now(clock);
    }

    public LocalDate currentLocalDate() {
        return LocalDate.now(clock);
    }

}
