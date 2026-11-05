package uk.gov.hmcts.darts.openapi;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.report.ValidationReport;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommonPlatformOpenApiContractTest {

    private static final String DAILY_LIST_PATH = "/external/cp/daily-lists";
    private static final String EVENT_PATH = "/external/cp/events";
    private static final String EVENT_BODY = """
        {
          "type": "1000",
          "sub_type": "1051",
          "event_id": "12345678",
          "date_time": "2026-09-16T11:01:34.000Z",
          "courthouse": "EXAMPLE COURT",
          "case_numbers": ["T20260001"]
        }
        """;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final OpenApiInteractionValidator VALIDATOR =
        OpenApiInteractionValidator.createForSpecificationUrl(
            CommonPlatformOpenApiContractTest.class.getResource("/openapi/commonplatform.yaml").toExternalForm()
        ).build();

    @ParameterizedTest
    @MethodSource("validMessageIds")
    void givenValidMessageIdHeader_whenValidatingRequest_thenAcceptsRequest(String path, String messageId) {
        Request request = createRequest(path).withHeader("X-Message-Id", messageId).build();

        ValidationReport report = VALIDATOR.validateRequest(request);

        assertTrue(report.getMessages().isEmpty(), () -> "Expected no validation errors but got: " + report.getMessages());
    }

    @ParameterizedTest
    @ValueSource(strings = {DAILY_LIST_PATH, EVENT_PATH})
    void givenMissingMessageIdHeader_whenValidatingRequest_thenRejectsRequest(String path) {
        ValidationReport report = VALIDATOR.validateRequest(createRequest(path).build());

        assertFalse(report.getMessages().isEmpty(), "Expected a validation error for a missing message ID header");
    }

    @ParameterizedTest
    @MethodSource("invalidMessageIds")
    void givenInvalidMessageIdHeader_whenValidatingRequest_thenRejectsRequest(String path, String messageId) {
        Request request = createRequest(path).withHeader("X-Message-Id", messageId).build();

        ValidationReport report = VALIDATOR.validateRequest(request);

        assertFalse(report.getMessages().isEmpty(), "Expected a validation error for an invalid message ID header");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "2026-09-16T11:01:34Z",
        "2026-09-16T12:01:34+01:00",
        "2026-09-16T06:01:34-05:00",
        "2026-09-16T11:01:34.123Z"
    })
    void givenValidTimestamp_whenValidatingEvent_thenAcceptsRequest(String dateTime) throws JsonProcessingException {
        ObjectNode body = eventBody();
        body.put("date_time", dateTime);

        ValidationReport report = validateEvent(body);

        assertTrue(report.getMessages().isEmpty(), () -> "Expected no validation errors but got: " + report.getMessages());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
        "2026-09-16T12:01:34",
        "2026-09-16",
        "2026-02-30T12:01:34Z",
        "2026-09-16T25:01:34Z",
        "not-a-timestamp"
    })
    void givenInvalidTimestamp_whenValidatingEvent_thenRejectsRequest(String dateTime) throws JsonProcessingException {
        ObjectNode body = eventBody();
        body.put("date_time", dateTime);

        ValidationReport report = validateEvent(body);

        assertFalse(report.getMessages().isEmpty(), "Expected a validation error for an invalid event timestamp");
    }

    @Test
    void givenSeparateDateTimeFieldsWithoutTimestamp_whenValidatingEvent_thenRejectsRequest() throws JsonProcessingException {
        ObjectNode body = eventBody();
        body.remove("date_time");
        body.put("year", 2026);
        body.put("month", 9);
        body.put("day", 16);
        body.put("hour", 12);
        body.put("minute", 1);
        body.put("second", 34);

        ValidationReport report = validateEvent(body);

        assertFalse(report.getMessages().isEmpty(), "Expected a validation error for a missing event timestamp");
    }

    @ParameterizedTest
    @ValueSource(strings = {"type", "sub_type", "event_id", "date_time", "courthouse", "case_numbers"})
    void givenMissingRequiredEventField_whenValidatingEvent_thenRejectsRequest(String field) throws JsonProcessingException {
        ObjectNode body = eventBody();
        body.remove(field);

        ValidationReport report = validateEvent(body);

        assertFalse(report.getMessages().isEmpty(), () -> "Expected a validation error for missing " + field);
    }

    @Test
    void givenDocumentWrapper_whenValidatingEvent_thenRejectsRequest() throws JsonProcessingException {
        ObjectNode event = eventBody();
        ObjectNode body = OBJECT_MAPPER.createObjectNode();
        body.set("type", event.remove("type"));
        body.set("sub_type", event.remove("sub_type"));
        body.set("document", event);

        ValidationReport report = validateEvent(body);

        assertFalse(report.getMessages().isEmpty(), "Expected a validation error for event fields inside a document wrapper");
    }

    @Test
    void givenFlatEventWithRetentionPolicy_whenValidatingEvent_thenAcceptsRequest() throws JsonProcessingException {
        ObjectNode body = eventBody();
        ObjectNode retentionPolicy = body.putObject("retention_policy");
        retentionPolicy.put("case_retention_fixed_policy", "3");
        retentionPolicy.put("case_total_sentence", "1Y2M0D");

        ValidationReport report = validateEvent(body);

        assertTrue(report.getMessages().isEmpty(), () -> "Expected no validation errors but got: " + report.getMessages());
    }

    private static ObjectNode eventBody() throws JsonProcessingException {
        return (ObjectNode) OBJECT_MAPPER.readTree(EVENT_BODY);
    }

    private static ValidationReport validateEvent(ObjectNode body) {
        Request request = SimpleRequest.Builder.post(EVENT_PATH)
            .withContentType("application/json")
            .withHeader("X-Message-Id", "00000000-0000-4000-8000-000000000001")
            .withBody(body.toString())
            .build();
        return VALIDATOR.validateRequest(request);
    }

    private static Stream<Arguments> validMessageIds() {
        return Stream.of(DAILY_LIST_PATH, EVENT_PATH)
            .flatMap(path -> Stream.of("a", "a".repeat(256)).map(messageId -> Arguments.of(path, messageId)));
    }

    private static Stream<Arguments> invalidMessageIds() {
        return Stream.of(DAILY_LIST_PATH, EVENT_PATH)
            .flatMap(path -> Stream.of("", "a".repeat(257)).map(messageId -> Arguments.of(path, messageId)));
    }

    private static SimpleRequest.Builder createRequest(String path) {
        if (DAILY_LIST_PATH.equals(path)) {
            return SimpleRequest.Builder.post(path).withContentType("application/xml").withBody("<DailyList/>");
        }
        return SimpleRequest.Builder.post(path).withContentType("application/json").withBody(EVENT_BODY);
    }
}
