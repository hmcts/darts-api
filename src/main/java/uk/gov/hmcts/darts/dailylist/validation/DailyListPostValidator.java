package uk.gov.hmcts.darts.dailylist.validation;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.dailylist.enums.SourceType;
import uk.gov.hmcts.darts.dailylist.exception.DailyListError;
import uk.gov.hmcts.darts.dailylist.model.DailyListPostRequestInternal;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@UtilityClass
public class DailyListPostValidator {

    public void validate(DailyListPostRequestInternal request) {
        if (isNull(request.getDailyListJson())) {
            validateXmlRequest(request);
        } else {
            validateJsonRequest(request.getSourceSystem());
        }
    }

    private void validateJsonRequest(String sourceSystem) {

        if (nonNull(sourceSystem)) {
            checkSourceSystem(sourceSystem);
        }
    }

    @SuppressWarnings({"PMD.CyclomaticComplexity"})
    private void validateXmlRequest(DailyListPostRequestInternal request) {
        if (StringUtils.isBlank(request.getDailyListXml())) {
            throw new DartsApiException(DailyListError.XML_OR_JSON_NEEDS_TO_BE_PROVIDED);
        }
        if (StringUtils.isBlank(request.getCourthouse())
            || request.getHearingDate() == null
            || StringUtils.isBlank(request.getUniqueId())
            || request.getPublishedDateTime() == null
            || request.getMessageId() == null
            || request.getSourceSystem() == null
        ) {
            throw new DartsApiException(DailyListError.XML_EXTRA_PARAMETERS_MISSING);
        }
        checkSourceSystem(request.getSourceSystem());
    }

    private void checkSourceSystem(String sourceSystem) {
        if (!sourceSystem.equals(SourceType.CPP.toString()) && !sourceSystem.equals(SourceType.XHB.toString())) {
            throw new DartsApiException(DailyListError.INVALID_SOURCE_SYSTEM);
        }
    }
}
