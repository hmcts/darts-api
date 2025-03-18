package uk.gov.hmcts.darts.cases.util;

import lombok.experimental.UtilityClass;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.darts.cases.exception.CaseApiError;
import uk.gov.hmcts.darts.cases.model.GetCasesSearchRequest;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

@UtilityClass
public class RequestValidator {

    public static final int SEARCH_COMPLEXITY_THRESHOLD = 3;
    public static final int SEARCH_TEXT_LENGTH_THRESHOLD = 3;

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
        totalPoints += Math.max(0, StringUtils.length(request.getCaseNumber()) - SEARCH_TEXT_LENGTH_THRESHOLD);
        boolean courtroomProvided = request.getCourtroom() != null;
        totalPoints += courtroomProvided ? 1 : 0;
        totalPoints += getPoints(request.getJudgeName());
        totalPoints += getPoints(request.getDefendantName());
        totalPoints += getPoints(request.getEventTextContains());
        boolean specificDateProvided = request.getDateFrom() != null && request.getDateFrom().equals(request.getDateTo());
        totalPoints += specificDateProvided ? 1 : 0;

        //Do this at the end to ensure getPoints CRITERIA_TOO_BROAD gets chucked first
        boolean courthouseProvided = getPoints(request.getCourthouse()) != 0 || CollectionUtils.isNotEmpty(request.getCourthouseIds());
        boolean anyDateProvided = request.getDateFrom() != null || request.getDateTo() != null;
        totalPoints += anyDateProvided ? 1 : 0;
        totalPoints += courthouseProvided ? 1 : 0;

        if (courthouseProvided && anyDateProvided) {
            //allowed case
            return;
        }
        if (totalPoints < SEARCH_COMPLEXITY_THRESHOLD) {
            throw new DartsApiException(CaseApiError.CRITERIA_TOO_BROAD);
        }
    }

    private static int getPoints(String str) {
        long length = StringUtils.length(str);
        if (length == 0) {
            return 0;
        }
        if (length >= SEARCH_TEXT_LENGTH_THRESHOLD) {
            return 1;
        }
        throw new DartsApiException(CaseApiError.CRITERIA_TOO_BROAD,
                                    "Please include at least " + SEARCH_TEXT_LENGTH_THRESHOLD + " characters.");
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