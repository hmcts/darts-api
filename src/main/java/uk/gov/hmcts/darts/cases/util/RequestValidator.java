package uk.gov.hmcts.darts.cases.util;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestParam;
import uk.gov.hmcts.darts.cases.exception.CaseApiError;
import uk.gov.hmcts.darts.cases.model.GetCasesSearchRequest;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import java.time.LocalDate;

@UtilityClass
public class RequestValidator {

    public void validate(GetCasesSearchRequest request) {
        checkNoCriteriaProvided(request);

        checkParameterSizes(request);

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

    private static void checkParameterSizes(GetCasesSearchRequest request) {
        if (request.getCaseNumber().length() > 20) {
            throw new DartsApiException(CaseApiError.INVALID_REQUEST, "CaseNumber is larger than 20 Characters");
        } else if (request.getCourthouse().length() > 30) {
            throw new DartsApiException(CaseApiError.INVALID_REQUEST, "Courthouse is larger than 30 Characters");
        } else if (request.getCourtroom().length() > 30) {
            throw new DartsApiException(CaseApiError.INVALID_REQUEST, "Courtroom is larger than 30 Characters");
        } else if (request.getJudgeName().length() > 30) {
            throw new DartsApiException(CaseApiError.INVALID_REQUEST, "JudgeName is larger than 30 Characters");
        } else if (request.getDefendantName().length() > 30) {
            throw new DartsApiException(CaseApiError.INVALID_REQUEST, "DefendantsName is larger than 30 Characters");
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
