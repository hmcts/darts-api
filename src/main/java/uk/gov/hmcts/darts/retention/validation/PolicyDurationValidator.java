package uk.gov.hmcts.darts.retention.validation;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.component.validation.Validator;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.retention.exception.RetentionApiError;

import java.util.Map;

/**
 *  Validates that a duration, provided in the DARTS-specific format nYnMnD (where n can be one or many digits) exceeds the specified number of days.
 *  As the duration of a month (or a year) is ambiguous, this implementation only supports durations less than 28 days.
 */
@Component
public class PolicyDurationValidator implements Validator<String> {

    private static final String YEAR_DELIMITER = "Y";
    private static final String MONTH_DELIMITER = "M";
    private static final String DAY_DELIMITER = "D";
    private static final int MIN_ALLOWABLE_DURATION_DAYS = 1;

    @Override
    public void validate(String durationString) {
        int yearIndex = durationString.indexOf(YEAR_DELIMITER);
        int yearValue = Integer.parseInt(durationString.substring(0, yearIndex));

        int monthIndex = durationString.indexOf(MONTH_DELIMITER);
        int monthValue = Integer.parseInt(durationString.substring(yearIndex + 1, monthIndex));

        // Assuming MIN_ALLOWABLE_DURATION_DAYS is always less than the number of days in a short month (or year), then a positive non-zero value for
        // months or years automatically means the duration is sufficient.
        if (monthValue > 0 || yearValue > 0) {
            return;
        }

        int dayIndex = durationString.indexOf(DAY_DELIMITER);
        int dayValue = Integer.parseInt(durationString.substring(monthIndex + 1, dayIndex));

        if (dayValue < MIN_ALLOWABLE_DURATION_DAYS) {
            throw new DartsApiException(RetentionApiError.DURATION_TOO_SHORT, Map.of("min_allowable_days", MIN_ALLOWABLE_DURATION_DAYS));
        }
    }

}
