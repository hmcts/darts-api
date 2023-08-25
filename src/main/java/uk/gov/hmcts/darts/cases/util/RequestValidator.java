package uk.gov.hmcts.darts.cases.util;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.darts.cases.exception.CaseApiError;
import uk.gov.hmcts.darts.cases.model.GetCasesSearchRequest;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

@UtilityClass
public class RequestValidator {

    public void validate(GetCasesSearchRequest request) {
        checkNoCriteriaProvided(request);

        checkOnlyCourthouse(request);

        checkOnlyCourtroom(request);

        checkDates(request);
    }

    private static void checkDates(GetCasesSearchRequest request) {
        if (request.getDateFrom() != null
            && request.getDateTo() != null
            && request.getDateFrom().isAfter(request.getDateTo())) {
            throw new DartsApiException(CaseApiError.INVALID_REQUEST, "The 'From' date cannot be after the 'To' date.");
        }
    }

    private static void checkOnlyCourtroom(GetCasesSearchRequest request) {
        if (BooleanUtils.and(new boolean[]{
            StringUtils.isBlank(request.getCaseNumber()),
            StringUtils.isBlank(request.getCourthouse()),
            StringUtils.isNotBlank(request.getCourtroom()),
            StringUtils.isBlank(request.getJudgeName()),
            StringUtils.isBlank(request.getDefendantName()),
            request.getDateFrom() == null,
            request.getDateTo() == null,
            StringUtils.isBlank(request.getEventTextContains())
        })) {
            throw new DartsApiException(CaseApiError.CRITERIA_TOO_BROAD);
        }
    }

    private static void checkOnlyCourthouse(GetCasesSearchRequest request) {
        if (BooleanUtils.and(new boolean[]{
            StringUtils.isBlank(request.getCaseNumber()),
            StringUtils.isNotBlank(request.getCourthouse()),
            StringUtils.isBlank(request.getCourtroom()),
            StringUtils.isBlank(request.getJudgeName()),
            StringUtils.isBlank(request.getDefendantName()),
            request.getDateFrom() == null,
            request.getDateTo() == null,
            StringUtils.isBlank(request.getEventTextContains())
        })) {
            throw new DartsApiException(CaseApiError.CRITERIA_TOO_BROAD);
        }
    }

    private static void checkNoCriteriaProvided(GetCasesSearchRequest request) {
        if (BooleanUtils.and(new boolean[]{
            StringUtils.isBlank(request.getCaseNumber()),
            StringUtils.isBlank(request.getCourthouse()),
            StringUtils.isBlank(request.getCourtroom()),
            StringUtils.isBlank(request.getJudgeName()),
            StringUtils.isBlank(request.getDefendantName()),
            request.getDateFrom() == null,
            request.getDateTo() == null,
            StringUtils.isBlank(request.getEventTextContains())
        })) {
            throw new DartsApiException(CaseApiError.NO_CRITERIA_SPECIFIED);
        }
    }
}
