package uk.gov.hmcts.darts.audio.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.audio.exception.AudioApiError;
import uk.gov.hmcts.darts.audio.model.MediaSearchData;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SearchMediaValidatorTest {
    private SearchMediaValidator searchMediaValidator;

    @BeforeEach
    public void beforeTest() {
        searchMediaValidator = new SearchMediaValidator();
    }

    @Test
    void testSuccessWithTransformedId() {
        Integer transformedMediaId = 200;
        MediaSearchData searchData = new MediaSearchData(transformedMediaId, null, null, null);
        searchMediaValidator.validate(searchData);
    }

    @Test
    void testSuccessWithHearing() {
        Integer hearingId = 200;
        Integer hearingId2 = 200;
        MediaSearchData searchData = new MediaSearchData(null, List.of(hearingId, hearingId2), null, null);
        searchMediaValidator.validate(searchData);
    }

    @Test
    void testSuccessWithStartDate() {
        MediaSearchData searchData = new MediaSearchData(null, null, OffsetDateTime.now(), null);
        searchMediaValidator.validate(searchData);
    }

    @Test
    void testSuccessWithEndDate() {
        MediaSearchData searchData = new MediaSearchData(null, null, null, OffsetDateTime.now());
        searchMediaValidator.validate(searchData);
    }

    @Test
    void testSuccessWithHearingStartAndEndDate() {
        Integer hearingId = 200;
        MediaSearchData searchData = new MediaSearchData(null, List.of(hearingId), OffsetDateTime.now(), OffsetDateTime.now());
        searchMediaValidator.validate(searchData);
    }

    @Test
    void testFailureUsingTransformedIdAndOtherParameters() {
        Integer hearingId = 200;
        Integer transformedMediaId = 200;
        MediaSearchData searchData = new MediaSearchData(transformedMediaId, List.of(hearingId), OffsetDateTime.now(), OffsetDateTime.now());
        DartsApiException exception = assertThrows(DartsApiException.class, () -> searchMediaValidator.validate(searchData));
        assertEquals(AudioApiError.ADMIN_SEARCH_CRITERIA_NOT_SUITABLE, exception.getError());
    }
}