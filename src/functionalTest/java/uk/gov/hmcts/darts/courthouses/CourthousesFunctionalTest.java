package uk.gov.hmcts.darts.courthouses;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.FunctionalTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
class CourthousesFunctionalTest extends FunctionalTest {
    public static final String COURTHOUSES_URI = "/courthouses";
    public static final String COURTHOUSE_BODY = """
        {"courthouse_name": "BIRMINGHAM","code": 5705}""";
    public static final String COURTHOUSE_UPDATEBODY = """
        {"courthouse_name": "MANCHESTER","code": 2112}""";
    public static final String COURTHOUSE_INVALIDBODY = """
        {"courthouse_name": "READING", code: "1234"}""";
    public static final String COURTHOUSE_BAD_ID = "/99999";
    public static final int OK = 200;
    public static final int CREATED = 201;
    public static final int NO_CONTENT = 204;
    public static final int BAD_REQUEST = 400;
    public static final int NOT_FOUND = 404;
    public static final int RESOURCE_ALREADY_EXISTS = 409;


    @Test
    void createCourthouse() {
        Response response = buildRequestWithAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(COURTHOUSES_URI))
            .body(COURTHOUSE_BODY)
            .post()
            .then()
            .extract().response();

        printDebug(response);

        assertEquals(CREATED, response.statusCode());
    }

    @Test
    void createSameCourthouse() {
        Response response = buildRequestWithAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(COURTHOUSES_URI))
            .body(COURTHOUSE_BODY)
            .post()
            .then()
            .extract().response();

        printDebug(response);

        assertEquals(RESOURCE_ALREADY_EXISTS, response.statusCode());
    }

    @Test
    void updateCourthouse() {
        int courthouseID = getLatestCourthouseID();
        Response response = buildRequestWithAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(COURTHOUSES_URI + "/" + courthouseID))
            .body(COURTHOUSE_UPDATEBODY)
            .put()
            .then()
            .extract().response();

        printDebug(response);

        assertEquals(NO_CONTENT, response.statusCode());
    }

    @Test
    void updateCourthouseWithInvalidBody() {
        int courthouseID = getLatestCourthouseID();
        Response response = buildRequestWithAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(COURTHOUSES_URI + "/" + courthouseID))
            .body(COURTHOUSE_INVALIDBODY)
            .put()
            .then()
            .extract().response();

        printDebug(response);

        assertEquals(BAD_REQUEST, response.statusCode());
    }

    @Test
    void deleteCourthouse() {
        int courthouseID = getLatestCourthouseID();
        Response response = buildRequestWithAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(COURTHOUSES_URI  + "/" + courthouseID))
            .delete()
            .then()
            .extract().response();

        printDebug(response);

        assertEquals(NO_CONTENT, response.statusCode());
    }

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
        int courthouseID = getLatestCourthouseID();
        Response response = buildRequestWithAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(COURTHOUSES_URI + "/" + courthouseID))
            .get()
            .then()
            .extract().response();

        printDebug(response);

        assertEquals(OK, response.statusCode());
    }



    @Test
    void getCourthouseIdDoesNotExist() {
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


    private int getLatestCourthouseID() {
        List<Integer> ids = buildRequestWithAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(COURTHOUSES_URI))
            .get()
            .then()
            .extract()
            .response()
            .getBody()
            .jsonPath().get("id");

        int len = ids.size() - 1;
        return ids.get(len);
    }
}
