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

        checkComplexity(request);

        checkDates(request);
    }

    private static void checkDates(GetCasesSearchRequest request) {
        if (request.getDateFrom() != null
            && request.getDateTo() != null
            && request.getDateFrom().isAfter(request.getDateTo())) {
            throw new DartsApiException(CaseApiError.INVALID_REQUEST, "The 'From' date cannot be after the 'To' date.");
        }
    }

    /*
    This is to try to calculate if the search terms are too broad. E.g. adding a judge name with just 2 letters is too broad,
    and doesn't get counted.
     */
    @SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.NPathComplexity"})
    private static void checkComplexity(GetCasesSearchRequest request) {
        int totalPoints = 0;
        //give a point for every letter more than 3
        totalPoints += Math.max(0, StringUtils.length(request.getCaseNumber()) - 3);
        totalPoints += (StringUtils.length(request.getCourthouse()) >= 3 || request.getCourthouseId() != null) ? 1 : 0;
        totalPoints += (request.getCourtroom() != null || request.getCourtroomId() != null) ? 1 : 0;
        totalPoints += StringUtils.length(request.getJudgeName()) >= 3 ? 1 : 0;
        totalPoints += StringUtils.length(request.getDefendantName()) >= 3 ? 1 : 0;
        totalPoints += (request.getDateFrom() != null || request.getDateTo() != null) ? 1 : 0;
        totalPoints += StringUtils.length(request.getEventTextContains()) >= 3 ? 1 : 0;

        int three = 3;
        if (totalPoints < three) {
            throw new DartsApiException(CaseApiError.CRITERIA_TOO_BROAD);
        }
    }


    @SuppressWarnings({"PMD.UnnecessaryVarargsArrayCreation"})
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
