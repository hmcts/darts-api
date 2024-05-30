package uk.gov.hmcts.darts.audio.validation;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import static uk.gov.hmcts.darts.audio.exception.AudioApiError.ADMIN_SEARCH_CRITERIA_NOT_PROVIDED;

@Slf4j
public class AdminMediaSearchRequestValidator {

    public static void validate(Integer transformedMediaId, Integer transcriptionDocumentId) {
        if ((transformedMediaId == null && transcriptionDocumentId == null) ||
            (transformedMediaId != null && transcriptionDocumentId != null)) {
            throw new DartsApiException(ADMIN_SEARCH_CRITERIA_NOT_PROVIDED);
        }

    }
}