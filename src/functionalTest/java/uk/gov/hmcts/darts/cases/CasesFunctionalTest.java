package uk.gov.hmcts.darts.cases;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
class CasesFunctionalTest  extends FunctionalTest {
    public static final String CASES_URI = "/cases";
    public static final String COURTHOUSE = "courthouse";
    public static final String COURTROOM = "courtroom";
    public static final String CASE_DATE = "date";
    public static final String COURTHOUSE1 = "LEEDS";
    public static final String COURTHOUSE_ROOM = "ROOM";
    public static final String DATE1 = "2023-09-07";
    public static final int NOT_FOUND = 404;
    public static final int OK = 200;
    public static final int CREATED = 201;
    public static final String CASE_ID = "/21";
    public static final String CASE_BAD_ID = "/0";



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

    @Test
    void getExistingCase() {
        Response response = buildRequestWithAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(CASES_URI + CASE_ID))
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


    private static void printDebug(Response response) {
        log.debug("<===============================CASES-HEADERS==================================>");
        log.debug("HEADERS: " + response.getHeaders());
        log.debug("<=========================COURTHOUSES-HEADERS==================================>");
        log.debug("<=========================COURTHOUSES-BODY=====================================>");
        log.debug("BODY: " + response.getBody().prettyPrint());
        log.debug("<==============================CASES-BODY======================================>");
    }


}
