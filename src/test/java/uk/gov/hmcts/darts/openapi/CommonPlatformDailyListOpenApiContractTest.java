package uk.gov.hmcts.darts.openapi;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.report.ValidationReport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.hmcts.darts.common.config.ObjectMapperConfig;
import uk.gov.hmcts.darts.commonplatform.model.CommonPlatformDailyListRequest;
import uk.gov.hmcts.darts.dailylist.model.DailyListJsonObject;

import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.darts.test.common.TestUtils.getContentsFromFile;

class CommonPlatformDailyListOpenApiContractTest {

    private static final String DAILY_LIST_PATH = "/external/cp/daily-lists";
    private static final String EXAMPLE_RESOURCE = "openapi/commonplatform-daily-list.json";
    private static final String DEFENDANT_PATH = "/court_lists/0/sittings/0/hearings/0/defendants/0";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapperConfig().objectMapper();
    private static final OpenApiInteractionValidator VALIDATOR =
        OpenApiInteractionValidator.createForSpecificationUrl(
            CommonPlatformDailyListOpenApiContractTest.class.getResource("/openapi/commonplatform.yaml").toExternalForm()
        ).build();

    @Test
    void givenSuppliedDailyListExample_whenValidatingRequest_thenAcceptsRequest() throws IOException {
        ValidationReport report = validateDailyList(dailyListBody());

        assertTrue(report.getMessages().isEmpty(), () -> "Expected no validation errors but got: " + report.getMessages());
    }

    @Test
    void givenOpenApiExample_whenValidatingRequest_thenAcceptsRequest() {
        OpenAPI specification = new OpenAPIV3Parser().read(getClass().getResource("/openapi/commonplatform.yaml").toExternalForm());
        Object example = specification.getPaths().get(DAILY_LIST_PATH).getPost().getRequestBody()
            .getContent().get("application/json").getExamples().get("dailyList").getValue();

        ValidationReport report = validateDailyList(OBJECT_MAPPER.valueToTree(example));

        assertTrue(report.getMessages().isEmpty(), () -> "Expected no validation errors but got: " + report.getMessages());
    }

    @Test
    void givenSuppliedDailyListExample_whenDeserializing_thenMatchesExistingDailyListModel() throws IOException {
        String json = getContentsFromFile(EXAMPLE_RESOURCE);
        CommonPlatformDailyListRequest cpRequest = OBJECT_MAPPER.readValue(json, CommonPlatformDailyListRequest.class);
        DailyListJsonObject existingRequest = OBJECT_MAPPER.readValue(json, DailyListJsonObject.class);
        JsonNode cpJson = OBJECT_MAPPER.valueToTree(cpRequest);

        assertThat(cpJson).isEqualTo(OBJECT_MAPPER.readTree(json));
        assertThat(cpJson).isEqualTo(OBJECT_MAPPER.valueToTree(existingRequest));
        assertThat(existingRequest.getDocumentId().getUniqueId()).isEqualTo("EXAMPLE_DAILY_LIST_001");
        assertThat(existingRequest.getDocumentId().getTimeStamp()).isEqualTo(OffsetDateTime.parse("2026-08-17T11:17:01.838Z"));
        assertThat(existingRequest.getListHeader().getPublishedTime()).isEqualTo(OffsetDateTime.parse("2026-08-17T12:17:01.928Z"));
        assertThat(existingRequest.getListHeader().getStartDate()).isEqualTo(LocalDate.of(2026, 8, 17));
        assertThat(existingRequest.getCourtLists()).hasSize(2);
        assertThat(existingRequest.getCourtLists().get(1).getSittings()).isEmpty();
        assertThat(existingRequest.getCourtLists().getFirst().getSittings().getFirst().getHearings()).hasSize(2);
        assertThat(existingRequest.getCourtLists().getFirst().getSittings().getFirst().getHearings().get(1).getDefendants()).hasSize(2);
        assertThat(existingRequest.getCourtLists().getFirst().getSittings().getFirst().getHearings().getFirst()
                       .getDefendants().getFirst().getUrn()).isEqualTo("EXAMPLE-URN-001");
    }

    @ParameterizedTest
    @ValueSource(strings = {"document_id", "list_header", "crown_court", "court_lists"})
    void givenMissingDailyListSection_whenValidatingRequest_thenRejectsRequest(String field) throws IOException {
        ObjectNode body = dailyListBody();
        body.remove(field);

        ValidationReport report = validateDailyList(body);

        assertFalse(report.getMessages().isEmpty(), () -> "Expected a validation error for missing " + field);
    }

    @ParameterizedTest
    @ValueSource(strings = {"document_id", "list_header", "crown_court", "court_lists"})
    void givenNullDailyListSection_whenValidatingRequest_thenRejectsRequest(String field) throws IOException {
        ObjectNode body = dailyListBody();
        body.putNull(field);

        ValidationReport report = validateDailyList(body);

        assertFalse(report.getMessages().isEmpty(), () -> "Expected a validation error for null " + field);
    }

    @ParameterizedTest
    @ValueSource(strings = {"document_id", "list_header", "crown_court", "court_lists"})
    void givenInvalidDailyListSectionType_whenValidatingRequest_thenRejectsRequest(String field) throws IOException {
        ObjectNode body = dailyListBody();
        body.put(field, "not-an-object-or-array");

        ValidationReport report = validateDailyList(body);

        assertFalse(report.getMessages().isEmpty(), () -> "Expected a validation error for the type of " + field);
    }

    @ParameterizedTest
    @MethodSource("invalidNestedFields")
    void givenInvalidNestedDailyListValue_whenValidatingRequest_thenRejectsRequest(String parent, String field, String value) throws IOException {
        ObjectNode body = dailyListBody();
        ((ObjectNode) body.at(parent)).put(field, value);

        ValidationReport report = validateDailyList(body);

        assertFalse(report.getMessages().isEmpty(), () -> "Expected a validation error for " + parent + "/" + field);
    }

    @Test
    void givenEmptyCourtLists_whenValidatingRequest_thenAcceptsRequest() throws IOException {
        ObjectNode body = dailyListBody();
        body.putArray("court_lists");

        ValidationReport report = validateDailyList(body);

        assertTrue(report.getMessages().isEmpty(), () -> "Expected no validation errors but got: " + report.getMessages());
    }

    @ParameterizedTest
    @ValueSource(strings = {"00:00:00", "23:59:59", ""})
    void givenLocalSittingTime_whenValidatingRequest_thenAcceptsRequest(String sittingAt) throws IOException {
        ObjectNode body = dailyListBody();
        ((ObjectNode) body.at("/court_lists/0/sittings/0")).put("sitting_at", sittingAt);

        ValidationReport report = validateDailyList(body);

        assertTrue(report.getMessages().isEmpty(), () -> "Expected no validation errors but got: " + report.getMessages());
    }

    @Test
    void givenOmittedSittingTime_whenValidatingRequest_thenAcceptsRequest() throws IOException {
        ObjectNode body = dailyListBody();
        ((ObjectNode) body.at("/court_lists/0/sittings/0")).remove("sitting_at");

        ValidationReport report = validateDailyList(body);

        assertTrue(report.getMessages().isEmpty(), () -> "Expected no validation errors but got: " + report.getMessages());
    }

    @Test
    void givenXmlDailyList_whenValidatingRequest_thenRejectsRequest() {
        Request request = SimpleRequest.Builder.post(DAILY_LIST_PATH)
            .withContentType("application/xml")
            .withHeader("X-Message-Id", "example-message")
            .withBody("<DailyList/>")
            .build();

        ValidationReport report = VALIDATOR.validateRequest(request);

        assertFalse(report.getMessages().isEmpty(), "Expected a validation error for an XML request");
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "[]", "null", "<DailyList/>"})
    void givenInvalidJsonBody_whenValidatingRequest_thenRejectsRequest(String body) {
        Request request = SimpleRequest.Builder.post(DAILY_LIST_PATH)
            .withContentType("application/json")
            .withHeader("X-Message-Id", "example-message")
            .withBody(body)
            .build();

        ValidationReport report = VALIDATOR.validateRequest(request);

        assertFalse(report.getMessages().isEmpty(), "Expected a validation error for an invalid daily-list body");
    }

    @Test
    void givenJsonEncodedString_whenValidatingRequest_thenRejectsRequest() throws IOException {
        Request request = SimpleRequest.Builder.post(DAILY_LIST_PATH)
            .withContentType("application/json")
            .withHeader("X-Message-Id", "example-message")
            .withBody(OBJECT_MAPPER.writeValueAsString(getContentsFromFile(EXAMPLE_RESOURCE)))
            .build();

        ValidationReport report = VALIDATOR.validateRequest(request);

        assertFalse(report.getMessages().isEmpty(), "Expected a validation error for JSON sent as an encoded string");
    }

    private static Stream<Arguments> invalidNestedFields() {
        return Stream.of(
            Arguments.of("/document_id", "time_stamp", "2026-08-17T12:17:01"),
            Arguments.of("/list_header", "published_time", "not-a-timestamp"),
            Arguments.of("/list_header", "start_date", "2026-08-32"),
            Arguments.of("/crown_court/court_house_code", "code", "999"),
            Arguments.of("/court_lists/0/sittings/0", "sitting_sequence_no", "1"),
            Arguments.of("/court_lists/0/sittings/0", "sitting_at", "25:00:00"),
            Arguments.of("/court_lists/0/sittings/0", "sitting_at", "10:00:00Z"),
            Arguments.of("/court_lists/0/sittings/0", "hearings", "not-an-array"),
            Arguments.of("/court_lists/0/sittings/0/hearings/0/hearing_details", "hearing_date", "not-a-date"),
            Arguments.of(DEFENDANT_PATH + "/personal_details", "is_masked", "false"),
            Arguments.of(DEFENDANT_PATH + "/charges/0", "indictment_count_number", "1")
        );
    }

    private static ObjectNode dailyListBody() throws IOException {
        return (ObjectNode) OBJECT_MAPPER.readTree(getContentsFromFile(EXAMPLE_RESOURCE));
    }

    private static ValidationReport validateDailyList(ObjectNode body) {
        Request request = SimpleRequest.Builder.post(DAILY_LIST_PATH)
            .withContentType("application/json")
            .withHeader("X-Message-Id", "example-message")
            .withBody(body.toString())
            .build();
        return VALIDATOR.validateRequest(request);
    }
}
