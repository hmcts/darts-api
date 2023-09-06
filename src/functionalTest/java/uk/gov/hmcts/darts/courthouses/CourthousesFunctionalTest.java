package uk.gov.hmcts.darts.courthouses;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
class CourthousesFunctionalTest extends FunctionalTest {
    public static final String COURTHOUSES_URI = "/courthouses";
    public static final String COURTHOUSE_ID = "/1";
    public static final String COURTHOUSE_BAD_ID = "/99";
    public static final String DATE1 = "2023-09-06";
    public static final int NOT_FOUND = 404;
    public static final int OK = 200;


    @Test
    void getAllCourthouses() {
        Response response = buildRequestWithAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(COURTHOUSES_URI))
            .get()
            .then()
            .extract().response();

        printDebug(response);

        assertEquals(OK, response.statusCode());
    }

    @Test
    void getExistingCourthouse() {
        Response response = buildRequestWithAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(COURTHOUSES_URI + COURTHOUSE_ID))
            .get()
            .then()
            .extract().response();

        printDebug(response);

        assertEquals(OK, response.statusCode());
    }

    @Test
    void getCourthouseBadRequest() {
        Response response = buildRequestWithAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(COURTHOUSES_URI + COURTHOUSE_BAD_ID))
            .get()
            .then()
            .extract().response();

        printDebug(response);

        assertEquals(NOT_FOUND, response.statusCode());
    }

    private static void printDebug(Response response) {
        log.debug("<=========================COURTHOUSES-HEADERS==================================>");
        log.debug("HEADERS: " + response.getHeaders());
        log.debug("<=========================COURTHOUSES-HEADERS==================================>");
        log.debug("<=========================COURTHOUSES-BODY=====================================>");
        log.debug("BODY: " + response.getBody().prettyPrint());
        log.debug("<=========================COURTHOUSES-BODY======================================>");
    }
}
