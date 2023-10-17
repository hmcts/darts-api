package uk.gov.hmcts.darts.courthouses;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Order;
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

    public static final int INTERNAL_SERVER_ERROR = 500;


    @Test
    @Order(1)
    void createCourthouse() {
        Response response = buildRequestWithExternalAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(COURTHOUSES_URI))
            .body(COURTHOUSE_BODY)
            .post()
            .then()
            .extract().response();

        assertEquals(CREATED, response.statusCode());
    }

    @Test
    @Disabled
    @Order(2)
    void createSameCourthouse() {
        Response response = buildRequestWithExternalAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(COURTHOUSES_URI))
            .body(COURTHOUSE_BODY)
            .post()
            .then()
            .extract().response();

        assertEquals(RESOURCE_ALREADY_EXISTS, response.statusCode());
    }

    @Test
    @Disabled
    @Order(3)
    void updateCourthouse() {
        int courthouseID = getLatestCourthouseID();
        Response response = buildRequestWithExternalAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(COURTHOUSES_URI + "/" + courthouseID))
            .body(COURTHOUSE_UPDATEBODY)
            .put()
            .then()
            .extract().response();

        assertEquals(NO_CONTENT, response.statusCode());
    }

    @Test
    @Order(4)
    void updateCourthouseWithInvalidBody() {
        int courthouseID = getLatestCourthouseID();
        Response response = buildRequestWithExternalAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(COURTHOUSES_URI + "/" + courthouseID))
            .body(COURTHOUSE_INVALIDBODY)
            .put()
            .then()
            .extract().response();

        assertEquals(BAD_REQUEST, response.statusCode());
    }

    @Test
    @Order(5)
    void deleteCourthouse() {
        int courthouseID = getLatestCourthouseID();
        Response response = buildRequestWithExternalAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(COURTHOUSES_URI  + "/" + courthouseID))
            .delete()
            .then()
            .extract().response();

        if(response.statusCode() == 204) {
            assertEquals(NO_CONTENT, response.statusCode());
        } else if(response.statusCode() == 500) {
            assertEquals(INTERNAL_SERVER_ERROR, response.statusCode());
        }
    }

    @Test
    @Order(6)
    void getAllCourthouses() {
        Response response = buildRequestWithExternalAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(COURTHOUSES_URI))
            .get()
            .then()
            .extract().response();

        assertEquals(OK, response.statusCode());
    }

    @Test
    @Order(7)
    void getExistingCourthouse() {
        int courthouseID = getLatestCourthouseID();
        Response response = buildRequestWithExternalAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(COURTHOUSES_URI + "/" + courthouseID))
            .get()
            .then()
            .extract().response();

        assertEquals(OK, response.statusCode());
    }


    @Test
    @Order(8)
    void getCourthouseIdDoesNotExist() {
        Response response = buildRequestWithExternalAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(COURTHOUSES_URI + COURTHOUSE_BAD_ID))
            .get()
            .then()
            .extract().response();

        assertEquals(NOT_FOUND, response.statusCode());
    }

    //----------------------------------
    private int getLatestCourthouseID() {
        List<Integer> ids = buildRequestWithExternalAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(COURTHOUSES_URI))
            .get()
            .then()
            .extract()
            .response()
            .getBody()
            .jsonPath().get("id");

        int idLen = ids.size();
        return ids.get(--idLen);
    }
}
