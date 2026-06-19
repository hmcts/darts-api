package uk.gov.hmcts.darts.openapi;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.report.ValidationReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Nested;
import uk.gov.hmcts.darts.util.ValidationConstants;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TranscriptionsOpenApiContractTest {

    private static final OpenApiInteractionValidator VALIDATOR =
        OpenApiInteractionValidator.createForSpecificationUrl(
            Objects.requireNonNull(TranscriptionsOpenApiContractTest.class
                                       .getResource("/openapi/transcriptions.yaml"))
                .toExternalForm()
        ).build();

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class TranscriptionsIdDocumentGet {

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
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class TranscriptionsPost {

        @Test
        void openApi_ShouldReturnNoError_WhenValidTranscriptsRequestUsed() {
            Request request = SimpleRequest.Builder
                .post("/transcriptions")
                .withContentType("application/json")
                .withBody("""
                              {
                                "hearing_id": 1234,
                                "case_id": 4567,
                                "transcription_urgency_id": 2,
                                "transcription_type_id": 3,
                                "comment": "Please expedite my transcription request",
                                "start_date_time": "2023-07-31T14:32:24.0Z",
                                "end_date_time": "2023-07-31T14:32:24.0Z"
                              }
                              """).build();

            ValidationReport report = VALIDATOR.validateRequest(request);

            assertTrue(report.getMessages().isEmpty(), "Expected no validation errors for a valid transcription_id");

        }
    }
}
