package uk.gov.hmcts.darts.audio.validation;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.darts.audio.model.MediaSearchData;
import uk.gov.hmcts.darts.common.component.validation.Validator;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import static uk.gov.hmcts.darts.audio.exception.AudioApiError.ADMIN_SEARCH_CRITERIA_NOT_SUITABLE;

@Component
public class SearchMediaValidator implements Validator<MediaSearchData> {

    @Override
    public void validate(MediaSearchData data) {
        if (noParams(data) || (data.getTransformedMediaId() != null
            && (data.getHearingIds() != null && !data.getHearingIds().isEmpty()
            || data.getEndDateTime() != null
            || data.getStartDateTime() != null))) {
            throw new DartsApiException(ADMIN_SEARCH_CRITERIA_NOT_SUITABLE);
        }
    }

    private boolean noParams(MediaSearchData data) {
        return data.getTransformedMediaId() == null
            && CollectionUtils.isEmpty(data.getHearingIds())
            && data.getEndDateTime() == null
            && data.getStartDateTime() == null;
    }
}