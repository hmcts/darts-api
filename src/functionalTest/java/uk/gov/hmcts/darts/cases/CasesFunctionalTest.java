package uk.gov.hmcts.darts.cases;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import uk.gov.hmcts.darts.FunctionalTest;
import uk.gov.hmcts.darts.cases.model.AdvancedSearchResult;
import uk.gov.hmcts.darts.cases.model.ScheduledCase;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CasesFunctionalTest extends FunctionalTest {
    private static final String CASES_PATH = "/cases";
    private static final String EVENTS_PATH = "/events";
    private static final String CASE_NUMBER = "FUNC-CASE-" + randomAlphanumeric(7);
    private static final String COURTHOUSE = "SWANSEA";
    private static final String COURTROOM = "FUNC-CASE-1";
    private static int caseId;

    @AfterAll
    void cleanUp() {
        buildRequestWithExternalAuth()
            .baseUri(getUri("/functional-tests/clean"))
            .redirects().follow(false)
            .delete();
    }

    @Test
    @Order(1)
    void createCaseAndEvent() {
        String caseBody = """
            {
                "courthouse": "<<courthouse>>",
                "case_number": "<<caseNumber>>",
                "defendants": ["Defendant A"],
                "judges": ["Judge 1"],
                "prosecutors": ["Prosecutor A"],
                "defenders": ["Defender A"]
            }
                """;
        caseBody = caseBody.replace("<<courthouse>>", COURTHOUSE);
        caseBody = caseBody.replace("<<caseNumber>>", CASE_NUMBER);

        Response caseResponse = buildRequestWithExternalGlobalAccessAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(CASES_PATH))
            .body(caseBody)
            .post()
            .then()
            .extract().response();

        assertEquals(201, caseResponse.statusCode());

        String eventBody = """
            {
              "message_id": "100",
              "type": "1000",
              "sub_type": "1002",
              "event_id": "12345",
              "courthouse": "<<courthouse>>",
              "courtroom": "<<courtroom>>",
              "case_numbers": [
                "<<caseNumber>>"
              ],
              "event_text": "A temporary event created by functional test",
              "date_time": "2023-08-08T14:01:06Z"
            }""";

        eventBody = eventBody.replace("<<courthouse>>", COURTHOUSE);
        eventBody = eventBody.replace("<<courtroom>>", COURTROOM);
        eventBody = eventBody.replace("<<caseNumber>>", CASE_NUMBER);

        Response eventResponse = buildRequestWithExternalGlobalAccessAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(EVENTS_PATH))
            .body(eventBody)
            .post()
            .then()
            .extract().response();

        assertEquals(201, eventResponse.statusCode());
    }

    @Test
    @Order(2)
    void getCases() {
        Response response = buildRequestWithExternalGlobalAccessAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(CASES_PATH))
            .param("courthouse", COURTHOUSE)
            .param("courtroom", COURTROOM)
            .param("date", "2023-08-08")
            .get()
            .then()
            .extract().response();
        assertEquals(200, response.statusCode());

        var caseList = response.jsonPath().getList("", ScheduledCase.class);
        assertEquals(1, caseList.size());
        var firstCase = caseList.get(0);
        assertEquals(CASE_NUMBER, firstCase.getCaseNumber());
    }

    @Test
    @Order(3)
    void searchPostCase() {
        String caseBody = """
            {
                "case_number": "<<caseNumber>>"
            }
                """;

        caseBody = caseBody.replace("<<caseNumber>>", CASE_NUMBER);

        // search for case using case number
        Response response = buildRequestWithExternalGlobalAccessAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(CASES_PATH + "/search"))
            .body(caseBody)
            .post()
            .then()
            .extract().response();

        assertEquals(200, response.statusCode());
        var caseList = response.jsonPath().getList("", AdvancedSearchResult.class);
        assertEquals(1, caseList.size());
        var firstCase = caseList.get(0);
        caseId = firstCase.getCaseId();
    }

    @Test
    @Order(4)
    void getCaseById() {
        Response getCaseresponse = buildRequestWithExternalAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(CASES_PATH + "/" + caseId))
            .get()
            .then()
            .extract().response();

        assertEquals(200, getCaseresponse.statusCode());
        assertEquals(CASE_NUMBER, getCaseresponse.jsonPath().get("case_number"));
    }

    @Test
    @Order(5)
    void getCaseByIdNotFound() {
        Response response = buildRequestWithExternalAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(CASES_PATH + "/-999"))
            .get()
            .then()
            .extract().response();

        assertEquals(404, response.statusCode());
    }

    @Test
    @Order(6)
    void getEventsByCaseId() {
        Response getCaseResponse = buildRequestWithExternalAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(CASES_PATH + "/" + caseId + EVENTS_PATH))
            .get()
            .then()
            .extract().response();

        assertEquals(200, getCaseResponse.statusCode());
        JSONArray jsonResponseArray = new JSONArray(getCaseResponse.asString());
        assertFalse(jsonResponseArray.isEmpty());
    }

}
