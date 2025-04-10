package uk.gov.hmcts.darts.common.util;

import io.micrometer.common.util.StringUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.exception.CommonApiError;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.model.PostAdminSearchRequest;

import java.time.LocalDate;
import java.time.Period;

import static java.util.Objects.isNull;

@Component
public class AdminSearchRequestValidator {

    @Value("${darts.admin-search.hearing-dates-max-search-period}")
    String hearingDatesMaxSearchPeriod;

    public void validate(PostAdminSearchRequest postAdminSearchRequest) {
        // if hearing dates are provided, validate them
        validateHearingDatesDuration(postAdminSearchRequest);

        if (StringUtils.isEmpty(postAdminSearchRequest.getCaseNumber())
            && (CollectionUtils.isEmpty(postAdminSearchRequest.getCourthouseIds())
            || isNull(postAdminSearchRequest.getHearingStartAt())
            || isNull(postAdminSearchRequest.getHearingEndAt()))) {
            throw new DartsApiException(CommonApiError.CRITERIA_TOO_BROAD);
        }
    }

    private void validateHearingDatesDuration(PostAdminSearchRequest request) {
        LocalDate hearingStart = request.getHearingStartAt();
        LocalDate hearingEnd = request.getHearingEndAt();
        if (hearingStart != null && hearingEnd != null) {
            if (hearingStart.isAfter(hearingEnd)) {
                throw new DartsApiException(CommonApiError.INVALID_REQUEST, "The hearing start date cannot be after the end date.");
            }

            Period hearingDatesMaxSearch = Period.parse(hearingDatesMaxSearchPeriod);

            if (hearingStart.plus(hearingDatesMaxSearch).compareTo(hearingEnd) < 0) {
                throw new DartsApiException(
                    CommonApiError.INVALID_REQUEST,
                    "The time between the start and end date cannot be more than " + hearingDatesMaxSearch.toTotalMonths() + " months");
            }
        }
    }
}
