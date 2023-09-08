package uk.gov.hmcts.darts.cases;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.FunctionalTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
class CasesFunctionalTest  extends FunctionalTest {
    public static final String CASES_URI = "/cases";
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

        printDebug(response);

        assertEquals(CREATED, response.statusCode());
    }

    /**
     * TBD: Review
     */
    @Test
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

        printDebug(response);

        assertEquals(OK, response.statusCode());
    }

    /**
     * TBD: Review
     */
    @Test
    void getAllCases2() {
        Response response = buildRequestWithAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(CASES_URI))
            .param(COURTHOUSE, "SWANSEA")
            .param(COURTROOM, "ROOM")
            .param(CASE_DATE, DATE1)
            .get()
            .then()
            .extract().response();

        printDebug(response);

        assertEquals(OK, response.statusCode());
    }

    @Test
    void getExistingCase() {
//        String caseNum = getCaseNumber();
        int caseId = getCaseId();
        Response response = buildRequestWithAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(CASES_URI + "/" + caseId))
            .get()
            .then()
            .extract().response();

        printDebug(response);

        assertEquals(OK, response.statusCode());
    }

    @Test
    void getCaseBadRequest() {
        Response response = buildRequestWithAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(CASES_URI + CASE_BAD_ID))
            .get()
            .then()
            .extract().response();

        printDebug(response);

        assertEquals(NOT_FOUND, response.statusCode());
    }

    @Test
    void patchCase() {
        Response response = buildRequestWithAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(CASES_URI + CASE_ID))
            .body("""
                      {
                        "retain_until": "2023-09-07T11:49:44.618Z"
                      }
                      """)
            .patch()
            .then()
            .extract().response();

        printDebug(response);

        assertEquals(OK, response.statusCode());
    }
    @Test
    void searchCase() {
        Response response = buildRequestWithAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(CASES_URI + SEARCH))
//            .param("case_id", "41")
            .param("case_number","42GD2391421")
            .param("courthouse","SWANSEA")
            .param("courtroom","1")
            .param("defendant_name","DefendantName Surname")
            .param("judge_name", "Judgename Surname")
            .param("date_from", "2020-06-20")
            .param("date_to","2020-06-20")
            .param("event_test_contains","")
            .get()
            .then()
            .extract().response();

        printDebug(response);

        assertEquals(OK, response.statusCode());
    }



    @Test
    void getCaseHearing() {
        Response response = buildRequestWithAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(CASES_URI + CASE_ID + HEARINGS))
            .get()
            .then()
            .extract().response();

        printDebug(response);

        assertEquals(OK, response.statusCode());
    }

    @Test
    void getCaseHearingEvents() {
        Response response = buildRequestWithAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(CASES_URI + HEARINGS + HEARING_ID + EVENTS))
            .get()
            .then()
            .extract().response();

        printDebug(response);

        assertEquals(OK, response.statusCode());
    }


    private static void printDebug(Response response) {
        log.debug("<===============================CASES-HEADERS==================================>");
        log.debug("HEADERS: " + response.getHeaders());
        log.debug("<=========================COURTHOUSES-HEADERS==================================>");
        log.debug("<=========================COURTHOUSES-BODY=====================================>");
        log.debug("BODY: " + response.getBody().prettyPrint());
        log.debug("<==============================CASES-BODY======================================>");
    }


    public int getCaseId() {
        List<Integer> ids = buildRequestWithAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(CASES_URI + SEARCH))
            .param("case_number","CA")
            .get()
            .then()
            .extract()
            .response()
            .getBody()
            .jsonPath().get("case_id");

        log.debug("*************> CASE_ID: " + ids.get(ids.size()-1) + "<*************" );

        return ids.get(ids.size()-1);
    }

    private String getCaseNumber() {
        List<String> ids = buildRequestWithAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(CASES_URI))
            .param(COURTHOUSE, COURTHOUSE1)
            .param(COURTROOM, COURTHOUSE_ROOM)
            .param(CASE_DATE, DATE1)
            .get()
            .then()
            .extract()
            .response()
            .getBody()
            .jsonPath().get("case_number");

        log.debug("*************> CASE_NUM: " + ids.get(ids.size()-1) + "<*************" );

        return ids.get(ids.size()-1);
    }


}
