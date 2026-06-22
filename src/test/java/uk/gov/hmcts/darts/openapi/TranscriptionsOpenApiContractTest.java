package uk.gov.hmcts.darts.openapi;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.report.ValidationReport;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import uk.gov.hmcts.darts.util.ValidationConstants;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TranscriptionsOpenApiContractTest {

    private static final OpenApiInteractionValidator VALIDATOR =
        OpenApiInteractionValidator.createForSpecificationUrl(
            TranscriptionsOpenApiContractTest.class
                .getResource("/openapi/transcriptions.yaml")
                .toExternalForm()
        ).build();

    @Test
    void openApi_ShouldReturnError_WhenNegativeTranscriptionIdUsed() {
        Request request = SimpleRequest.Builder
            .get("/transcriptions/-123/document")
            .build();

        ValidationReport report = VALIDATOR.validateRequest(request);

        assertTrue(report.getMessages().stream()
            .anyMatch(m -> m.getMessage().contains("Numeric instance is lower than the required " +
                                                       "minimum (minimum: 1, found: -123)")));
    }

    @Test
    void openApi_ShouldReturnError_WhenAboveMaximumTranscriptionIdUsed() {
        String maxTranscriptionId = ValidationConstants.MaxValues.MAX_LONG_VALUE.toString();
        String exceededTranscriptionId = maxTranscriptionId + "99";
        Request request = SimpleRequest.Builder
            .get("/transcriptions/" + exceededTranscriptionId + "/document")
            .build();

        ValidationReport report = VALIDATOR.validateRequest(request);

        String expectedSubstring = "Numeric instance is greater than the required maximum (maximum: "
            + maxTranscriptionId + ", found: " + exceededTranscriptionId + ")";

        assertTrue(
            report.getMessages().stream().anyMatch(m -> m.getMessage().equals(expectedSubstring))
        );
    }

    @Test
    void openApi_ShouldReturnNoError_WhenValidTranscriptionIdUsed() {
        Request request = SimpleRequest.Builder
            .get("/transcriptions/1000/document")
            .build();

        ValidationReport report = VALIDATOR.validateRequest(request);

        assertTrue(report.getMessages().isEmpty(), "Expected no validation errors for a valid transcription_id");
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class TranscriptionsGet {

        @Test
        void openApi_ShouldReturnNoError_WhenValidUserIdUsed() {
            Request request = SimpleRequest.Builder
                .get("/transcriptions")
                .withHeader("user_id", "2147483647")
                .build();

            ValidationReport report = VALIDATOR.validateRequest(request);

            assertTrue(report.getMessages().isEmpty(), "Expected no validation errors for a valid user_id");
        }

        @ParameterizedTest
        @CsvSource({
            "0, '(minimum: 1, found: 0)'",
            "2147483648, '(maximum: 2147483647, found: 2147483648)'"
        })
        void openApi_ShouldReturnAnError_WhenAnInvalidUserIdUsed(String userId, String expectedError) {
            Request request = SimpleRequest.Builder
                .get("/transcriptions")
                .withHeader("user_id", userId)
                .build();

            ValidationReport report = VALIDATOR.validateRequest(request);

            assertTrue(report.getMessages().getFirst().toString().contains(expectedError));
        }
    }
}
