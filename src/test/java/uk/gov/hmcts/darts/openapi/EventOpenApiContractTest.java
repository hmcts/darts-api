package uk.gov.hmcts.darts.openapi;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.report.ValidationReport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EventOpenApiContractTest {

    private static final OpenApiInteractionValidator VALIDATOR =
        OpenApiInteractionValidator.createForSpecificationUrl(
            EventOpenApiContractTest.class
                .getResource("/openapi/event.yaml")
                .toExternalForm()
        ).build();

    @Test
    void openApi_ShouldReturnError_WhenEventTextExceedsMaxLength() {
        ValidationReport report = VALIDATOR.validateRequest(postEventRequest("""
            {
              "message_id": "18422",
              "type": "21300",
              "sub_type": "21300",
              "event_id": "2",
              "courthouse": "SNARESBROOK",
              "courtroom": "1",
              "case_numbers": ["T20190441"],
              "event_text": "%s",
              "date_time": "2023-06-14T08:37:30.945Z"
            }
            """.formatted("a".repeat(2001))));

        assertHasMessageContaining(report, "maximum allowed: 2000");
    }

    @Test
    void openApi_ShouldReturnError_WhenTooManyCaseNumbersProvided() {
        String caseNumbers = ("\"T20190441\",").repeat(100) + "\"T20190441\"";

        ValidationReport report = VALIDATOR.validateRequest(postEventRequest("""
            {
              "message_id": "18422",
              "type": "10100",
              "sub_type": "10100",
              "event_id": "1",
              "courthouse": "SNARESBROOK",
              "courtroom": "1",
              "case_numbers": [%s],
              "date_time": "2023-06-14T08:37:30.945Z"
            }
            """.formatted(caseNumbers)));

        assertHasMessageContaining(report, "must have at most 100 elements");
    }

    @Test
    void openApi_ShouldReturnError_WhenEventIdIsNotNumeric() {
        ValidationReport report = VALIDATOR.validateRequest(postEventRequest("""
            {
              "message_id": "18422",
              "type": "10100",
              "sub_type": "10100",
              "event_id": "ABC123",
              "courthouse": "SNARESBROOK",
              "courtroom": "1",
              "case_numbers": ["A20230049"],
              "date_time": "2023-06-14T08:37:30.945Z"
            }
            """));

        assertHasMessageContaining(report, "does not match input string");
    }

    @Test
    void openApi_ShouldReturnError_WhenRequestContainsAdditionalProperty() {
        ValidationReport report = VALIDATOR.validateRequest(postEventRequest("""
            {
              "message_id": "18422",
              "type": "10100",
              "sub_type": "10100",
              "event_id": "1",
              "courthouse": "SNARESBROOK",
              "courtroom": "1",
              "case_numbers": ["A20230049"],
              "date_time": "2023-06-14T08:37:30.945Z",
              "unexpected": "value"
            }
            """));

        assertHasMessageContaining(report, "properties which are not allowed");
    }

    @Test
    void openApi_ShouldReturnError_WhenRetentionPolicyContainsAdditionalProperty() {
        ValidationReport report = VALIDATOR.validateRequest(postEventRequest("""
            {
              "message_id": "18422",
              "type": "40750",
              "sub_type": "11504",
              "event_id": "3",
              "courthouse": "SNARESBROOK",
              "courtroom": "1",
              "case_numbers": ["T20190441"],
              "event_text": "[Defendant: DEFENDANT ONE]",
              "date_time": "2023-06-14T08:37:30.945Z",
              "retention_policy": {
                "case_retention_fixed_policy": "4",
                "case_total_sentence": "26Y0M0D",
                "unexpected": "value"
              }
            }
            """));

        assertHasMessageContaining(report, "properties which are not allowed");
    }

    @Test
    void openApi_ShouldReturnNoError_WhenValidEventRequestUsed() {
        ValidationReport report = VALIDATOR.validateRequest(postEventRequest("""
            {
              "message_id": "18422",
              "type": "10100",
              "sub_type": "10100",
              "event_id": "1",
              "courthouse": "SNARESBROOK",
              "courtroom": "1",
              "case_numbers": ["A20230049"],
              "date_time": "2023-06-14T08:37:30.945Z"
            }
            """));

        assertTrue(report.getMessages().isEmpty(), "Expected no validation errors for a valid event request");
    }

    private static Request postEventRequest(String body) {
        return SimpleRequest.Builder
            .post("/events")
            .withContentType("application/json")
            .withBody(body)
            .build();
    }

    private static void assertHasMessageContaining(ValidationReport report, String expectedSubstring) {
        assertTrue(
            report.getMessages().stream()
                .anyMatch(message -> message.getMessage().contains(expectedSubstring)),
            () -> "Expected validation message containing '%s' but got %s".formatted(expectedSubstring, report.getMessages())
        );
    }
}
