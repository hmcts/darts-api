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

import static org.junit.jupiter.api.Assertions.assertFalse;
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

        assertFalse(report.getMessages().isEmpty(), () -> "Expected validation errors for a negative transcription id");
    }

    @Test
    void openApi_ShouldReturnError_WhenAboveMaximumTranscriptionIdUsed() {
        String maxTranscriptionId = ValidationConstants.MaxValues.MAX_LONG_VALUE.toString();
        String exceededTranscriptionId = maxTranscriptionId + "99";
        Request request = SimpleRequest.Builder
            .get("/transcriptions/" + exceededTranscriptionId + "/document")
            .build();

        ValidationReport report = VALIDATOR.validateRequest(request);

        assertFalse(report.getMessages().isEmpty(), () -> "Expected validation errors for an oversized transcription id");
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
        void openApi_ShouldReturnAnError_WhenTranscriptionsRequestFieldIsInvalid(String testName, String body) {
            Request request = patchTranscriptionsRequest(body);

            ValidationReport report = VALIDATOR.validateRequest(request);

            assertFalse(report.getMessages().isEmpty(), () -> "Expected validation errors for " + testName);
        }

        private Stream<Arguments> invalidTranscriptionsPatchRequestFields() {
            return Stream.of(
                arguments(
                    "transcription id is required",
                    patchRequestBody(body -> body.remove("transcription_id"))
                ),
                arguments(
                    "transcription id is below minimum",
                    patchRequestBody(body -> body.set("transcription_id", JsonNodeFactory.instance.numberNode(BigInteger.ZERO)))
                ),
                arguments(
                    "transcription id exceeds maximum",
                    patchRequestBody(body -> body.set("transcription_id", JsonNodeFactory.instance.numberNode(new BigInteger("9223372036854775808"))))
                ),
                arguments(
                    "transcription id is NaN",
                    patchRequestBody(body -> body.put("transcription_id", "not-a-number"))
                ),
                arguments(
                    "hide request from requestor is not a boolean",
                    patchRequestBody(body -> body.put("hide_request_from_requestor", "true"))
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

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class TranscriptionsPost {

        @Test
        void openApi_ShouldReturnNoError_WhenValidTranscriptionsRequestUsed() {
            Request request = postTranscriptionsRequest(validTranscriptionsRequestBody().toString());

            ValidationReport report = VALIDATOR.validateRequest(request);

            assertTrue(report.getMessages().isEmpty(), "Expected no validation errors for a valid transcription_id");
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("invalidTranscriptionsPostRequests")
        void openApi_ShouldReturnError_WhenInvalidTranscriptionsRequestUsed(String testName, String body) {
            Request request = postTranscriptionsRequest(body);

            ValidationReport report = VALIDATOR.validateRequest(request);

            assertFalse(report.getMessages().isEmpty(), () -> "Expected validation errors for " + testName);
        }

        Stream<Arguments> invalidTranscriptionsPostRequests() {
            return Stream.of(
                arguments(
                    "hearing id is not integer",
                    transcriptionsRequestBody(body -> body.put("hearing_id", "not-a-number"))
                ),
                arguments(
                    "hearing id is below minimum",
                    transcriptionsRequestBody(body -> body.put("hearing_id", 0))
                ),
                arguments(
                    "hearing id is above maximum",
                    transcriptionsRequestBody(body -> body.put("hearing_id", 2_147_483_648L))
                ),
                arguments(
                    "case id is not integer",
                    transcriptionsRequestBody(body -> body.put("case_id", "not-a-number"))
                ),
                arguments(
                    "case id is below minimum",
                    transcriptionsRequestBody(body -> body.put("case_id", 0))
                ),
                arguments(
                    "case id is above maximum",
                    transcriptionsRequestBody(body -> body.put("case_id", 2_147_483_648L))
                ),
                arguments(
                    "transcription urgency id is not integer",
                    transcriptionsRequestBody(body -> body.put("transcription_urgency_id", "not-a-number"))
                ),
                arguments(
                    "transcription urgency id is below minimum",
                    transcriptionsRequestBody(body -> body.put("transcription_urgency_id", 0))
                ),
                arguments(
                    "transcription urgency id is above maximum",
                    transcriptionsRequestBody(body -> body.put("transcription_urgency_id", 2_147_483_648L))
                ),
                arguments(
                    "transcription type id is not integer",
                    transcriptionsRequestBody(body -> body.put("transcription_type_id", "not-a-number"))
                ),
                arguments(
                    "transcription type id is below minimum",
                    transcriptionsRequestBody(body -> body.put("transcription_type_id", 0))
                ),
                arguments(
                    "transcription type id is above maximum",
                    transcriptionsRequestBody(body -> body.put("transcription_type_id", 2_147_483_648L))
                ),
                arguments(
                    "comment exceeds maxLength",
                    transcriptionsRequestBody(body -> body.put("comment", "a".repeat(2001)))
                ),
                arguments(
                    "start date time is not date-time",
                    transcriptionsRequestBody(body -> body.put("start_date_time", "not-a-date"))
                ),
                arguments(
                    "end date time is not date-time",
                    transcriptionsRequestBody(body -> body.put("end_date_time", "not-a-date"))
                ),
                arguments(
                    "transcription urgency id is required",
                    transcriptionsRequestBody(body -> body.remove("transcription_urgency_id"))
                ),
                arguments(
                    "transcription type id is required",
                    transcriptionsRequestBody(body -> body.remove("transcription_type_id"))
                )
            );
        }
    }

    private static SimpleRequest postTranscriptionsRequest(String body) {
        return SimpleRequest.Builder
            .post("/transcriptions")
            .withContentType("application/json")
            .withBody(body)
            .build();
    }

    private String transcriptionsRequestBody(Consumer<ObjectNode> bodyMutation) {
        ObjectNode body = validTranscriptionsRequestBody();
        bodyMutation.accept(body);
        return body.toString();
    }

    private ObjectNode validTranscriptionsRequestBody() {
        ObjectNode body = JsonNodeFactory.instance.objectNode();
        body.put("hearing_id", 1234);
        body.put("case_id", 4567);
        body.put("transcription_urgency_id", 2);
        body.put("transcription_type_id", 3);
        body.put("comment", "Please expedite my transcription request");
        body.put("start_date_time", "2023-07-31T14:32:24.0Z");
        body.put("end_date_time", "2023-07-31T14:32:24.0Z");
        return body;
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

}
