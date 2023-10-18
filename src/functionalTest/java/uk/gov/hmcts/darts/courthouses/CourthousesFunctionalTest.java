package uk.gov.hmcts.darts.courthouses;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.FunctionalTest;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
class CourthousesFunctionalTest extends FunctionalTest {
    public static final String COURTHOUSES_URI = "/courthouses";
    public static final String COURTHOUSE_BODY = """
        {"courthouse_name": """ +
        getRandomCourtnameAsTimestamp() +
        """
            ,"code": """ +
        getCode() +
        """
            }""";

    public static final String COURTHOUSE_UPDATEBODY = """
        {"courthouse_name": "MANCHESTER","code": 2112}""";
    public static final String COURTHOUSE_INVALIDBODY = """
        {"courthouse_name": "READING", code: "1234"}""";
    public static final String CLEAN_UP_DELETE_COURTHOUSE = "Clean Up: Delete courthouse: ";

    private int createdCourthouseId;
    public static final String COURTHOUSE_BAD_ID = "/99999";
    public static final int OK = 200;
    public static final int CREATED = 201;
    public static final int NO_CONTENT = 204;
    public static final int BAD_REQUEST = 400;
    public static final int NOT_FOUND = 404;
    public static final int RESOURCE_ALREADY_EXISTS = 409;


    @Test
    @Order(1)
    void createAndDeleteCourthouse() {
        /*
        Response response = buildRequestWithExternalAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(COURTHOUSES_URI))
            .body(COURTHOUSE_BODY)
            .post()
            .then()
            .extract().response();
*/
        Response response = createCourthouse(COURTHOUSE_BODY);
        createdCourthouseId = getCreatedCourthouseId(response);

        log.info("Created id: " + createdCourthouseId);

        assertEquals(CREATED, response.statusCode());

        cleanUpCourthouse(createdCourthouseId);
    }

    @Test
    @Order(2)
    void createSameCourthouse() {
        String courthouseBody = COURTHOUSE_BODY;
        createCourthouse(courthouseBody);
        int courthouseId = createdCourthouseId;

        log.info("Courthouse created id: " + courthouseId);
        log.info("Create same courthouse with the same body.");

        Response response = buildRequestWithExternalAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(COURTHOUSES_URI))
            .body(courthouseBody)
            .post()
            .then()
            .extract().response();
        assertEquals(RESOURCE_ALREADY_EXISTS, response.statusCode());

        cleanUpCourthouse(courthouseId);
    }

    @Test
    @Order(3)
    void updateCourthouse() {
        createCourthouse(COURTHOUSE_BODY);
        int courthouseId = createdCourthouseId;

        log.info("Courthouse created id: " + courthouseId);
        log.info("Update same courthouse with the same body.");

        Response response = buildRequestWithExternalAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(COURTHOUSES_URI + "/" + courthouseId))
            .body(COURTHOUSE_UPDATEBODY)
            .put()
            .then()
            .extract().response();

        assertEquals(NO_CONTENT, response.statusCode());

        cleanUpCourthouse(courthouseId);
    }

    @Test
    @Order(4)
    void updateCourthouseWithInvalidBody() {
        createCourthouse(COURTHOUSE_BODY);
        int courthouseId = createdCourthouseId;
        Response response = buildRequestWithExternalAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(COURTHOUSES_URI + "/" + courthouseId))
            .body(COURTHOUSE_INVALIDBODY)
            .put()
            .then()
            .extract().response();

        assertEquals(BAD_REQUEST, response.statusCode());

        cleanUpCourthouse(courthouseId);
    }

    @Test
    @Order(5)
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
    @Order(6)
    void getExistingCourthouse() {
        createCourthouse(COURTHOUSE_BODY);
        int courthouseID = createdCourthouseId;

        Response response = buildRequestWithExternalAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(COURTHOUSES_URI + "/" + courthouseID))
            .get()
            .then()
            .extract().response();

        assertEquals(OK, response.statusCode());

        cleanUpCourthouse(courthouseID);
    }

    @Test
    @Order(7)
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

    //================

    Response createCourthouse(String courthouseBody) {
        Response response = buildRequestWithExternalAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(COURTHOUSES_URI))
            .body(courthouseBody)
            .post()
            .then()
            .extract().response();

        createdCourthouseId = getCreatedCourthouseId(response);

        log.info("Created id: " + createdCourthouseId);

        assertEquals(CREATED, response.statusCode());

        return response;
    }

    private void cleanUpCourthouse(int courthouseID) {
        log.info(CLEAN_UP_DELETE_COURTHOUSE + courthouseID);
        deleteCourthouse(courthouseID);
    }

    void deleteCourthouse(int courthouseId) {
        log.info("Delete id: " + courthouseId);

        Response response = buildRequestWithExternalAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(COURTHOUSES_URI  + "/" + courthouseId))
            .delete()
            .then()
            .extract().response();

        assertEquals(NO_CONTENT, response.statusCode());
    }

    //----------------------------------

    private static long getRandomCourtnameAsTimestamp() {
        return System.currentTimeMillis();
    }

    private static int getCode() {
        Random random = new Random(System.currentTimeMillis());
        return 10_000 + random.nextInt(20_000);
    }

    private static int getCreatedCourthouseId(Response response) {
        return response.getBody().jsonPath().get("id");
    }
}
