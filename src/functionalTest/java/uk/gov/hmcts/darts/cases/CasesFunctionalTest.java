package uk.gov.hmcts.darts.cases;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.FunctionalTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
class CasesFunctionalTest  extends FunctionalTest {
    public static final String CASES_URI = "/cases";
    public static final String ENDPOINT_URL = "/events";
    public static final String SEARCH = "/search";
    public static final String HEARINGS = "/hearings";
    public static final String EVENTS = "/events";
    public static final String COURTHOUSE = "courthouse";
    public static final String COURTROOM = "courtroom";
    public static final String CASE_DATE = "date";
    public static final String COURTHOUSE1 = "SWANSEA";
    public static final String COURTHOUSE_ROOM = "1";
    public static final String DATE1 = "2020-06-20"; //Hearing
    public static final int NOT_FOUND = 404;
    public static final int OK = 200;
    public static final int CREATED = 201;
    public static final String CASE_ID = "/41";
    public static final String CASE_BAD_ID = "/0";
    public static final String HEARING_ID = "/1";



    @Test
    @Order(1)
    void createCase() {
        Response response = buildRequestWithAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(CASES_URI))
            .body("""
                      {
                      "courthouse": "LEEDS",
                        "case_number": "CASE1002",
                        "defendants": [
                          "defendantB"
                        ],
                        "judges": [
                          "judgeB"
                        ],
                        "prosecutors": [
                          "prosecuterB"
                        ],
                        "defenders": [
                          "defenderB"
                        ]
                        }""")
            .post()
            .then()
            .extract().response();

        assertEquals(CREATED, response.statusCode());
    }

    @Test
    @Order(2)
    void createEvent() {
        Response response = buildRequestWithAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(EVENTS))
            .body("""
                      {
                          "message_id": "54321",
                          "type": "1000",
                          "sub_type": "1002",
                          "event_id": "12345",
                          "courthouse": "LEEDS",
                          "courtroom": "ROOM_A",
                          "case_numbers": [
                            "CASE1002"
                          ],
                          "event_text": "Functional Test Setup",
                          "date_time": "2023-09-12T12:57:18.596Z",
                          "retention_policy": {
                            "case_retention_fixed_policy": "unknown",
                            "case_total_sentence": "unknown"
                          }
                        }""")
            .post()
            .then()
            .extract().response();

        assertEquals(CREATED, response.statusCode());
    }


    @Test
    @Order(3)
    void getAllCases() {
        Response response = buildRequestWithAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(CASES_URI))
            .param(COURTHOUSE, COURTHOUSE1)
            .param(COURTROOM, COURTHOUSE_ROOM)
            .param(CASE_DATE, DATE1)
            .get()
            .then()
            .extract().response();

        assertEquals(OK, response.statusCode());
    }

    @Test
    @Order(4)
    void getExistingCase() {
        int caseId = getCaseId();

        Response response = buildRequestWithAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(CASES_URI + "/" + caseId))
            .get()
            .then()
            .extract().response();

        assertEquals(OK, response.statusCode());
    }

    @Test
    @Order(5)
    void getCaseBadRequest() {
        Response response = buildRequestWithAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(CASES_URI + CASE_BAD_ID))
            .get()
            .then()
            .extract().response();

        assertEquals(NOT_FOUND, response.statusCode());
    }

    @Test
    @Order(6)
    void patchCase() {
        int caseId = getCaseId();
        Response response = buildRequestWithAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(CASES_URI + "/" + caseId))
            .body("""
                      {
                        "retain_until": "2023-11-07T11:49:44.618Z"
                      }
                      """)
            .patch()
            .then()
            .extract().response();

        assertEquals(OK, response.statusCode());
    }

    @Test
    @Order(7)
    void searchCase() {
        Response response = buildRequestWithAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(CASES_URI + SEARCH))
            .param("case_number","CASE1002")
            .param("courthouse","LEEDS")
            .param("courtroom","")
            .param("defendant_name","")
            .param("judge_name", "")
            .param("date_from", "")
            .param("date_to","")
            .param("event_test_contains","")
            .get()
            .then()
            .extract().response();

        assertEquals(OK, response.statusCode());
    }

    @Test
    @Order(8)
    void getCaseHearing() {
        int caseId = getCaseId();
        Response response = buildRequestWithAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(CASES_URI + "/" + caseId + HEARINGS))
            .get()
            .then()
            .extract().response();

        assertEquals(OK, response.statusCode());
    }

    @Test
    @Order(9)
    void getCaseHearingEvents() {
        int hearingId = getCaseHearingId();

        Response response = buildRequestWithAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(CASES_URI + HEARINGS + "/" + hearingId + EVENTS))
            .get()
            .then()
            .extract().response();

        assertEquals(OK, response.statusCode());
    }

    @Test
    @Order(10)
    public int getCaseId() {
        int caseId = 0;
        List<Integer> ids = buildRequestWithAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(CASES_URI + SEARCH))
            .param("case_number","CASE1002")
            .get()
            .then()
            .extract()
            .response()
            .getBody()
            .jsonPath().get("case_id");

        int len = ids.size();
        caseId = len > 0 ? ids.get(--len) : -1;

        if (caseId == -1) {
            assertEquals(NOT_FOUND, 404);
        }

        return caseId;
    }

    @Test
    @Order(11)
    Integer getCaseHearingId() {
        int hearingId = 0;
        int caseId = getCaseId();

        List<Integer> hearingIds = buildRequestWithAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(CASES_URI + "/" + caseId + HEARINGS))
            .get()
            .then()
            .extract()
            .response()
            .getBody()
            .jsonPath().get("id");

        int len = hearingIds.size();
        hearingId = len > 0 ? hearingIds.get(--len) : -1;

        if (hearingId == -1) {
            assertEquals(NOT_FOUND, 404);
        }

        return hearingId;
    }
}
