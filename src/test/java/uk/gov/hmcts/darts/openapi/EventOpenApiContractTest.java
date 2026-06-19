package uk.gov.hmcts.darts.openapi;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.report.ValidationReport;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class EventOpenApiContractTest {

    private final OpenApiInteractionValidator validator =
        OpenApiInteractionValidator.createForSpecificationUrl(
            Objects.requireNonNull(EventOpenApiContractTest.class
                                       .getResource("/openapi/event.yaml"))
                .toExternalForm()
        ).build();

    private static final String STRING_EXCEEDING_512_CHARS = "a".repeat(513);

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class CourtLogsPost {

        @ParameterizedTest(name = "/courtlogs POST schema field: {0}")
        @MethodSource("invalidCourtLogsPostRequests")
        void openApi_ShouldReturnError_WhenCourtLogsRequestIsInvalid(String testName, String body, String expectedMessage) {
            ValidationReport report = validator.validateRequest(postCourtLogsRequest(body));

            assertHasMessageContaining(report, expectedMessage);
        }

        @Test
        void openApi_ShouldReturnNoError_WhenValidCourtLogsRequestUsed() {
            ValidationReport report = validator.validateRequest(postCourtLogsRequest(validCourtLogsRequestBody().toString()));

            assertTrue(report.getMessages().isEmpty(), "Expected no validation errors for a valid courtlogs request");
        }

        Stream<Arguments> invalidCourtLogsPostRequests() {
            return Stream.of(
                arguments(
                    "log entry date time is not date-time",
                    courtLogsRequestBody(body -> body.put("log_entry_date_time", "not-a-date")),
                    "is invalid against requested date format"
                ),
                arguments(
                    "courthouse exceeds maxLength",
                    courtLogsRequestBody(body -> body.put("courthouse", "a".repeat(51))),
                    "maximum allowed: 50"
                ),
                arguments(
                    "courtroom exceeds maxLength",
                    courtLogsRequestBody(body -> body.put("courtroom", "a".repeat(26))),
                    "maximum allowed: 25"
                ),
                arguments(
                    "case numbers exceeds minItems",
                    courtLogsRequestBody(body -> body.putArray("case_numbers")),
                    "must have at least 1 element"
                ),
                arguments(
                    "case numbers exceeds maxItems",
                    courtLogsRequestBody(EventOpenApiContractTest.this::addTooManyCaseNumbers),
                    "must have at most 128 elements"
                ),
                arguments(
                    "case number exceeds maxLength",
                    courtLogsRequestBody(body -> {
                        ArrayNode caseNumbers = body.putArray("case_numbers");
                        caseNumbers.add("a".repeat(26));
                    }),
                    "maximum allowed: 25"
                ),
                arguments(
                    "text exceeds maxLength",
                    courtLogsRequestBody(body -> body.put("text", "a".repeat(257))),
                    "maximum allowed: 256"
                ),
                arguments(
                    "log entry date time is required",
                    courtLogsRequestBody(body -> body.remove("log_entry_date_time")),
                    "Object has missing required properties"
                ),
                arguments(
                    "courthouse is required",
                    courtLogsRequestBody(body -> body.remove("courthouse")),
                    "Object has missing required properties"
                ),
                arguments(
                    "courtroom is required",
                    courtLogsRequestBody(body -> body.remove("courtroom")),
                    "Object has missing required properties"
                ),
                arguments(
                    "case numbers is required",
                    courtLogsRequestBody(body -> body.remove("case_numbers")),
                    "Object has missing required properties"
                ),
                arguments(
                    "text is required",
                    courtLogsRequestBody(body -> body.remove("text")),
                    "Object has missing required properties"
                )
            );
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class EventsPost {

        @ParameterizedTest(name = "/events POST schema field: {0}")
        @MethodSource("invalidEventsPostRequests")
        void openApi_ShouldReturnError_WhenEventsRequestIsInvalid(String testName, String body, String expectedMessage) {
            ValidationReport report = validator.validateRequest(postEventRequest(body));

            assertHasMessageContaining(report, expectedMessage);
        }

        @Test
        void openApi_ShouldReturnNoError_WhenValidEventRequestUsed() {
            ValidationReport report = validator.validateRequest(postEventRequest(validEventRequestBody().toString()));

            assertTrue(report.getMessages().isEmpty(), "Expected no validation errors for a valid event request");
        }

        Stream<Arguments> invalidEventsPostRequests() {
            return Stream.of(
                arguments(
                    "request contains additional property",
                    eventRequestBody(body -> body.put("unexpected", "value")),
                    "properties which are not allowed"
                ),
                arguments(
                    "message id exceeds maxLength",
                    eventRequestBody(body -> body.put("message_id", STRING_EXCEEDING_512_CHARS)),
                    "maximum allowed: 512"
                ),
                arguments(
                    "type exceeds maxLength",
                    eventRequestBody(body -> body.put("type", STRING_EXCEEDING_512_CHARS)),
                    "maximum allowed: 512"
                ),
                arguments(
                    "sub type exceeds maxLength",
                    eventRequestBody(body -> body.put("sub_type", STRING_EXCEEDING_512_CHARS)),
                    "maximum allowed: 512"
                ),
                arguments(
                    "event id exceeds maxLength",
                    eventRequestBody(body -> body.put("event_id", "a".repeat(129))),
                    "maximum allowed: 128"
                ),
                arguments(
                    "courthouse exceeds maxLength",
                    eventRequestBody(body -> body.put("courthouse", "a".repeat(51))),
                    "maximum allowed: 50"
                ),
                arguments(
                    "courtroom exceeds maxLength",
                    eventRequestBody(body -> body.put("courtroom", "a".repeat(26))),
                    "maximum allowed: 25"
                ),
                arguments(
                    "case numbers exceeds maxItems",
                    eventRequestBody(EventOpenApiContractTest.this::addTooManyCaseNumbers),
                    "must have at most 128 elements"
                ),
                arguments(
                    "case number exceeds maxLength",
                    eventRequestBody(body -> {
                        ArrayNode caseNumbers = body.putArray("case_numbers");
                        caseNumbers.add("a".repeat(26));
                    }),
                    "maximum allowed: 25"
                ),
                arguments(
                    "event text exceeds maxLength",
                    eventRequestBody(body -> body.put("event_text", "a".repeat(2049))),
                    "maximum allowed: 2048"

                ),
                arguments(
                    "date time is not date-time",
                    eventRequestBody(body -> body.put("date_time", "not-a-date")),
                    "is invalid against requested date format"
                ),
                arguments(
                    "retention policy contains additional property",
                    eventRequestBody(body -> {
                        ObjectNode retentionPolicy = retentionPolicy("4", "26Y0M0D");
                        retentionPolicy.put("unexpected", "value");
                        body.set("retention_policy", retentionPolicy);
                    }),
                    "properties which are not allowed"
                ),
                arguments(
                    "case retention fixed policy exceeds maxLength",
                    eventRequestBody(body -> body.set("retention_policy", retentionPolicy("a".repeat(513), "26Y0M0D"))),
                    "maximum allowed: 512"
                ),
                arguments(
                    "case total sentence exceeds maxLength",
                    eventRequestBody(body -> body.set("retention_policy", retentionPolicy("4", "a".repeat(513)))),
                    "maximum allowed: 512"
                ),
                arguments(
                    "start time is not date-time",
                    eventRequestBody(body -> body.put("start_time", "not-a-date")),
                    "is invalid against requested date format"
                ),
                arguments(
                    "end time is not date-time",
                    eventRequestBody(body -> body.put("end_time", "not-a-date")),
                    "is invalid against requested date format"
                ),
                arguments(
                    "is mid tier is not boolean",
                    eventRequestBody(body -> body.put("is_mid_tier", "not-a-boolean")),
                    "Instance type (string) does not match any allowed primitive type"
                )
            );
        }
    }

    private Request postEventRequest(String body) {
        return SimpleRequest.Builder
            .post("/events")
            .withContentType("application/json")
            .withBody(body)
            .build();
    }

    private Request postCourtLogsRequest(String body) {
        return SimpleRequest.Builder
            .post("/courtlogs")
            .withContentType("application/json")
            .withBody(body)
            .build();
    }

    private String eventRequestBody(Consumer<ObjectNode> bodyMutation) {
        ObjectNode body = validEventRequestBody();
        bodyMutation.accept(body);
        return body.toString();
    }

    private ObjectNode validEventRequestBody() {
        ObjectNode body = objectNode();
        body.put("message_id", "18422");
        body.put("type", "10100");
        body.put("sub_type", "10100");
        body.put("event_id", "1");
        body.put("courthouse", "SNARESBROOK");
        body.put("courtroom", "1");
        body.put("date_time", "2023-06-14T08:37:30.945Z");
        body.putArray("case_numbers").add("A20230049");
        return body;
    }

    private String courtLogsRequestBody(Consumer<ObjectNode> bodyMutation) {
        ObjectNode body = validCourtLogsRequestBody();
        bodyMutation.accept(body);
        return body.toString();
    }

    private ObjectNode validCourtLogsRequestBody() {
        ObjectNode body = objectNode();
        body.put("log_entry_date_time", "2023-05-23T09:15:25Z");
        body.put("courthouse", "CARDIFF");
        body.put("courtroom", "1");
        body.put("text", "System : Start Recording : Record: Case Code:0008, New Case");
        body.putArray("case_numbers")
            .add("CASE1001")
            .add("CASE1002");
        return body;
    }

    private ObjectNode retentionPolicy(String caseRetentionFixedPolicy, String caseTotalSentence) {
        ObjectNode retentionPolicy = objectNode();
        retentionPolicy.put("case_retention_fixed_policy", caseRetentionFixedPolicy);
        retentionPolicy.put("case_total_sentence", caseTotalSentence);
        return retentionPolicy;
    }

    private void addTooManyCaseNumbers(ObjectNode body) {
        ArrayNode caseNumbers = body.putArray("case_numbers");
        for (int i = 0; i < 129; i++) {
            caseNumbers.add("T20190441");
        }
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
