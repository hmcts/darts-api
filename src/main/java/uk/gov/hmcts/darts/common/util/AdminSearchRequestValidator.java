package uk.gov.hmcts.darts.common.util;

import io.micrometer.common.util.StringUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.exception.DartsApiError;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.model.PostAdminSearchRequest;

import java.time.Period;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Component
public class AdminSearchRequestValidator {

    @Value("${darts.admin-search.hearing-dates-max-search-period}")
    private String hearingDatesMaxSearchPeriod;

    public void validate(PostAdminSearchRequest postAdminSearchRequest, DartsApiError criteriaTooBroad, DartsApiError invalidRequest) {
        // if hearing dates are provided, validate them
        validateHearingDatesDuration(postAdminSearchRequest, invalidRequest);

        if (StringUtils.isEmpty(postAdminSearchRequest.getCaseNumber())
            && (CollectionUtils.isEmpty(postAdminSearchRequest.getCourthouseIds())
            || isNull(postAdminSearchRequest.getHearingStartAt())
            || isNull(postAdminSearchRequest.getHearingEndAt()))) {
            throw new DartsApiException(criteriaTooBroad);
        }
    }

    private void validateHearingDatesDuration(PostAdminSearchRequest request, DartsApiError invalidRequest) {
        if (nonNull(request.getHearingStartAt()) && nonNull(request.getHearingEndAt())) {
            if (request.getHearingStartAt().isAfter(request.getHearingEndAt())) {
                throw new DartsApiException(invalidRequest, "The hearing start date cannot be after the end date.");
            }

            Period hearingPeriod = Period.between(request.getHearingStartAt(), request.getHearingEndAt());
            Period hearingDatesMaxSearch = Period.parse(hearingDatesMaxSearchPeriod);

            if (hearingPeriod.toTotalMonths() > hearingDatesMaxSearch.toTotalMonths()) {
                throw new DartsApiException(
                    invalidRequest, "The time between the start and end date cannot be more than " + hearingDatesMaxSearch.toTotalMonths() + " months");
            }
        }
    }
}
