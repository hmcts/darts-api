package uk.gov.hmcts.darts.dailylist.validation;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.dailylist.exception.DailyListError;
import uk.gov.hmcts.darts.dailylist.model.DailyListPostRequest;

@UtilityClass
public class DailyListPostValidator {

    public void validate(DailyListPostRequest request) {
        if (request.getDailyListJson() == null) {
            if (StringUtils.isBlank(request.getDailyListXml())) {
                throw new DartsApiException(DailyListError.XML_OR_JSON_NEEDS_TO_BE_PROVIDED);
            }
            if (StringUtils.isNotBlank(request.getDailyListXml())
                && (StringUtils.isBlank(request.getCourthouse())
                    || request.getHearingDate() == null
                    || StringUtils.isBlank(request.getUniqueId())
                    || request.getPublishedDateTime() == null)) {
                throw new DartsApiException(DailyListError.XML_EXTRA_PARAMETERS_MISSING);
            }
        }
    }
}
