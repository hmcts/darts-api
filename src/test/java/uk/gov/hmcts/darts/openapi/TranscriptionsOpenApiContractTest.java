package uk.gov.hmcts.darts.openapi;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.report.ValidationReport;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.node.ObjectNode;
import uk.gov.hmcts.darts.util.ValidationConstants;

import java.math.BigInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

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
    class TranscriptionsPatch {

        @Test
        void openApi_ShouldNotReturnAnError_WhenValidTranscriptionsRequestUsed() {
            Request request = patchTranscriptionsRequest(patchRequestBody(body -> { }));

            ValidationReport report = VALIDATOR.validateRequest(request);

            assertTrue(report.getMessages().isEmpty(), "Expected no validation errors for a valid transcriptions patch request");
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("invalidTranscriptionsPatchRequestFields")
        void openApi_ShouldReturnAnError_WhenTranscriptionsRequestFieldIsInvalid(
            String testName, String body, String expectedMessage
        ) {
            Request request = patchTranscriptionsRequest(body);

            ValidationReport report = VALIDATOR.validateRequest(request);

            assertHasMessageContaining(report, expectedMessage);
        }

        private Stream<Arguments> invalidTranscriptionsPatchRequestFields() {
            return Stream.of(
                arguments(
                    "transcription id is required",
                    patchRequestBody(body -> body.remove("transcription_id")),
                    "Object has missing required properties"
                ),
                arguments(
                    "transcription id exceeds minLength",
                    patchRequestBody(body -> body.set("transcription_id", JsonNodeFactory.instance.numberNode(BigInteger.ZERO))),
                    "Numeric instance is lower than the required minimum"
                ),
                arguments(
                    "transcription id exceeds maxLength",
                    patchRequestBody(body -> body.set("transcription_id", JsonNodeFactory.instance.numberNode(new BigInteger("9223372036854775808")))),
                    "Numeric instance is greater than the required maximum"
                ),
                arguments(
                    "transcription id is NaN",
                    patchRequestBody(body -> body.put("transcription_id", "not-a-number")),
                    "Instance type (string) does not match any allowed primitive type"
                ),
                arguments(
                    "hide request from requestor is not a boolean",
                    patchRequestBody(body -> body.put("hide_request_from_requestor", "true")),
                    "Instance type (string) does not match any allowed primitive type"
                )
            );
        }
    }

    private static SimpleRequest patchTranscriptionsRequest(String body) {
        return SimpleRequest.Builder
            .patch("/transcriptions")
            .withContentType("application/json")
            .withBody(body)
            .build();
    }

    private String patchRequestBody(Consumer<ObjectNode> bodyMutation) {
        ObjectNode body = validTranscriptionsPatchRequestBody();
        bodyMutation.accept(body);
        return JsonNodeFactory.instance.arrayNode().add(body).toString();
    }

    private ObjectNode validTranscriptionsPatchRequestBody() {
        ObjectNode body = objectNode();
        body.put("transcription_id", 9_223_372_036_854_775_807L);
        body.put("hide_request_from_requestor", true);
        return body;
    }

    private ObjectNode objectNode() {
        return JsonNodeFactory.instance.objectNode();
    }

    private void assertHasMessageContaining(ValidationReport report, String expectedSubstring) {
        assertTrue(
            report.getMessages().stream()
                .anyMatch(message -> message.getMessage().contains(expectedSubstring)),
            () -> "Expected validation message containing '%s' but got %s".formatted(expectedSubstring, report.getMessages())
        );
    }
}
