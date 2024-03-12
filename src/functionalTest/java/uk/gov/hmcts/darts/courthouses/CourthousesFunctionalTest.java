package uk.gov.hmcts.darts.courthouses;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.AccessTokenClient;
import uk.gov.hmcts.darts.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
class CourthousesFunctionalTest extends FunctionalTest {

    public static final String COURTHOUSES_URI = "/courthouses";
    public static final String ADMIN_COURTHOUSES_URI = "/admin/courthouses";
    public static final String ADMIN_REGION_URI = "/admin/regions";
    public static final String COURTHOUSE_BODY = """
        {"courthouse_name": "BIRMINGHAM","display_name": "Birmingham","code": 5705}""";
    public static final String COURTHOUSE_PATCH_BODY = """
        {"courthouse_name": "MANCHESTER","display_name": "Manchester"}""";
    public static final String COURTHOUSE_PATCH_INVALID_BODY = """
        {"courthouse_name": "READING","display_name": "Reading", code: "1234"}""";
    public static final String COURTHOUSE_BAD_ID = "/99999";
    public static final int OK = 200;
    public static final int CREATED = 201;
    public static final int NO_CONTENT = 204;
    public static final int BAD_REQUEST = 400;
    public static final int NOT_FOUND = 404;
    public static final int RESOURCE_ALREADY_EXISTS = 409;

    public static final int INTERNAL_SERVER_ERROR = 500;

    private int testCourthouseId;

    @Autowired
    private AccessTokenClient externalGlobalAccessTokenClient;

    @Override
    public RequestSpecification buildRequestWithExternalGlobalAccessAuth() {
        return buildRequestWithAuth(externalGlobalAccessTokenClient);
    }

    @Test
    @Order(1)
    void getAllCourthouses() {
        Response response = buildRequestWithExternalAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(COURTHOUSES_URI))
            .get()
            .then()
            .assertThat()
            .statusCode(OK)
            .extract().response();

        assertNotNull(response);
    }

    @Test
    @Order(2)
    void createCourthouse() {
        testCourthouseId = buildRequestWithExternalAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(COURTHOUSES_URI))
            .body(COURTHOUSE_BODY)
            .post()
            .then()
            .assertThat()
            .statusCode(CREATED)
            .extract()
            .path("id");

        assertTrue(testCourthouseId > 0);
    }

    @Test
    @Order(3)
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
    @Order(4)
    void patchCourthouse() {
        Response response = buildRequestWithExternalAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(ADMIN_COURTHOUSES_URI + "/" + testCourthouseId))
            .body(COURTHOUSE_PATCH_BODY)
            .put()
            .then()
            .extract().response();

        assertEquals(NO_CONTENT, response.statusCode());
    }

    @Test
    @Order(5)
    void patchCourthouseWithInvalidBody() {
        Response response = buildRequestWithExternalAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(ADMIN_COURTHOUSES_URI + "/" + testCourthouseId))
            .body(COURTHOUSE_PATCH_INVALID_BODY)
            .put()
            .then()
            .extract().response();

        assertEquals(BAD_REQUEST, response.statusCode());
    }

    @Test
    @Order(6)
    void getExistingCourthouse() {
        Response response = buildRequestWithExternalGlobalAccessAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(ADMIN_COURTHOUSES_URI + "/" + testCourthouseId))
            .get()
            .then()
            .extract().response();

        assertEquals(OK, response.statusCode());
    }


    @Test
    @Order(7)
    void getCourthouseIdDoesNotExist() {
        Response response = buildRequestWithExternalGlobalAccessAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(ADMIN_COURTHOUSES_URI + COURTHOUSE_BAD_ID))
            .get()
            .then()
            .extract().response();

        assertEquals(NOT_FOUND, response.statusCode());
    }

    @Test
    @Order(8)
    void deleteCourthouse() {
        Response response = buildRequestWithExternalAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(COURTHOUSES_URI + "/" + testCourthouseId))
            .delete()
            .then()
            .statusCode(NO_CONTENT)
            .extract().response();

        assertNotNull(response);
    }

    @Test
    @Order(9)
    void getAllRegions() {
        Response response = buildRequestWithExternalGlobalAccessAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(ADMIN_REGION_URI))
            .get()
            .then()
            .assertThat()
            .statusCode(OK)
            .extract().response();

        assertNotNull(response);
    }

}
