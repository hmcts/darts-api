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

@Component
public class AdminSearchRequestValidator {

    @Value("${darts.admin-search.hearing-dates-max-search-period}")
    private String hearingDatesMaxSearchPeriod;

    public void validate(PostAdminSearchRequest postAdminSearchRequest, DartsApiError criteriaTooBroad, DartsApiError invalidRequest) {
        if (StringUtils.isNotEmpty(postAdminSearchRequest.getCaseNumber())) {
            return;
        }
        if (CollectionUtils.isEmpty(postAdminSearchRequest.getCourthouseIds())
            || isNull(postAdminSearchRequest.getHearingStartAt())
            || isNull(postAdminSearchRequest.getHearingEndAt())
        ) {
            throw new DartsApiException(criteriaTooBroad);
        }
        validateHearingDatesDuration(postAdminSearchRequest, invalidRequest);
    }


    private void validateHearingDatesDuration(PostAdminSearchRequest request, DartsApiError invalidRequest) {
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
