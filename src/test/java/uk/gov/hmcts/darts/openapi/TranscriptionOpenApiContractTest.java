package uk.gov.hmcts.darts.openapi;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.report.ValidationReport;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import uk.gov.hmcts.darts.util.ValidationConstants;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TranscriptionOpenApiContractTest {

    private static final OpenApiInteractionValidator VALIDATOR =
        OpenApiInteractionValidator.createForSpecificationUrl(
            TranscriptionOpenApiContractTest.class
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
    class TranscriptionsTranscriptionIdDocumentPost {

        @Test
        void openApi_ShouldReturnNoError_WhenValidTranscriptionIdUsed() {
            String boundary = "test-boundary";
            Request request = SimpleRequest.Builder
                .post("/transcriptions/1/document")
                .withContentType("multipart/form-data; boundary=" + boundary)
                .withBody(multipartTranscriptBody(boundary))
                .build();

            ValidationReport report = VALIDATOR.validateRequest(request);

            assertTrue(report.getMessages().isEmpty(), "Expected no validation errors for a valid transcript upload request");
        }

        @Test
        void openApi_ShouldReturnError_WhenTranscriptionIdIsNotNumeric() {
            Request request = SimpleRequest.Builder
                .get("/transcriptions/not-a-number/document")
                .build();

            ValidationReport report = VALIDATOR.validateRequest(request);

            assertHasMessageContaining(report, "Instance type (string) does not match any allowed primitive type");
        }

        @Test
        void openApi_ShouldReturnError_WhenUnsupportedContentTypeUsed() {
            Request request = SimpleRequest.Builder
                .post("/transcriptions/1/document")
                .withContentType("application/json")
                .withBody("{\"transcript\":\"Transcript content\"}")
                .build();

            ValidationReport report = VALIDATOR.validateRequest(request);

            assertHasMessageContaining(report, "does not match any allowed types");
        }

        @Test
        void openApi_ShouldReturnError_WhenRequestBodyIsMissing() {
            Request request = SimpleRequest.Builder
                .post("/transcriptions/1/document")
                .withContentType("multipart/form-data")
                .build();

            ValidationReport report = VALIDATOR.validateRequest(request);

            assertHasMessageContaining(report, "A request body is required but none found");
        }

        private String multipartTranscriptBody(String boundary) {
            return String.join("\r\n",
                               "--" + boundary,
                               "Content-Disposition: form-data; name=\"transcript\"; filename=\"transcript.txt\"",
                               "Content-Type: text/plain",
                               "",
                               "Transcript content",
                               "--" + boundary + "--",
                               "");
        }
    }

    private void assertHasMessageContaining(ValidationReport report, String expectedSubstring) {
        assertTrue(
            report.getMessages().stream()
                .anyMatch(message -> message.getMessage().contains(expectedSubstring)),
            () -> "Expected validation message containing '%s' but got %s".formatted(expectedSubstring, report.getMessages())
        );
    }
}
