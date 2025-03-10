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
        boolean courthouseProvided = StringUtils.length(request.getCourthouse()) >= SEARCH_TEXT_LENGTH_THRESHOLD;
        boolean anyDateProvided = request.getDateFrom() != null || request.getDateTo() != null;

        if (courthouseProvided && anyDateProvided) {
            //allowed case
            return;
        }

        int totalPoints = 0;
        //give a point for every letter more than 3
        totalPoints += Math.max(0, StringUtils.length(request.getCaseNumber()) - SEARCH_TEXT_LENGTH_THRESHOLD);
        totalPoints += courthouseProvided ? 1 : 0;
        boolean courtroomProvided = request.getCourtroom() != null;
        totalPoints += courtroomProvided ? 1 : 0;
        boolean judgeNameLengthOk = StringUtils.length(request.getJudgeName()) >= SEARCH_TEXT_LENGTH_THRESHOLD;
        totalPoints += judgeNameLengthOk ? 1 : 0;
        boolean defendantNameLengthOk = StringUtils.length(request.getDefendantName()) >= SEARCH_TEXT_LENGTH_THRESHOLD;
        totalPoints += defendantNameLengthOk ? 1 : 0;
        totalPoints += anyDateProvided ? 1 : 0;
        boolean specificDateProvided = request.getDateFrom() != null && request.getDateFrom().equals(request.getDateTo());
        totalPoints += specificDateProvided ? 1 : 0;
        boolean eventTextLengthOk = StringUtils.length(request.getEventTextContains()) >= SEARCH_TEXT_LENGTH_THRESHOLD;
        totalPoints += eventTextLengthOk ? 1 : 0;

        //Do this at the end to ensure getPoints CRITERIA_TOO_BROAD gets chucked first
        boolean courthouseProvided = getPoints(request.getCourthouse()) != 0 || CollectionUtils.isNotEmpty(request.getCourthouseIds());
        boolean anyDateProvided = request.getDateFrom() != null || request.getDateTo() != null;
        totalPoints += anyDateProvided ? 1 : 0;
        totalPoints += courthouseProvided ? 1 : 0;

        if (totalPoints < SEARCH_COMPLEXITY_THRESHOLD) {
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