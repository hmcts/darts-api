package uk.gov.hmcts.darts.audio.validation;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AdminMediaSearchRequestValidatorTest {

    @Test
    void okJustTransformedMediaId() {
        AdminMediaSearchRequestValidator.validate(1, null);
    }

    @Test
    void okJustTranscriptionDocumentId() {
        AdminMediaSearchRequestValidator.validate(null, 1);
    }

    @Test
    void failProvidingBoth() {
        DartsApiException dartsApiException = assertThrows(DartsApiException.class, () ->
            AdminMediaSearchRequestValidator.validate(1, 1));
        assertEquals("Either transformed_media_id or transcription_document_id must be provided in the request, but not both.",
                     dartsApiException.getMessage());
    }

    @Test
    void failProvidingNeither() {
        DartsApiException dartsApiException = assertThrows(DartsApiException.class, () ->
            AdminMediaSearchRequestValidator.validate(null, null));
        assertEquals("Either transformed_media_id or transcription_document_id must be provided in the request, but not both.",
                     dartsApiException.getMessage());
    }

}