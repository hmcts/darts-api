package uk.gov.hmcts.darts.courthouses;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestMethodOrder;
import org.skyscreamer.jsonassert.ArrayValueMatcher;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.RegularExpressionValueMatcher;
import org.skyscreamer.jsonassert.comparator.ArraySizeComparator;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import uk.gov.hmcts.darts.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
class CourthousesFunctionalTest extends FunctionalTest {

    private static final String COURTHOUSES_URI = "/courthouses";
    private static final String ADMIN_COURTHOUSES_URI = "/admin/courthouses";
    private static final String ADMIN_REGION_URI = "/admin/regions";
    private static final String COURTHOUSE_PATCH_BODY = """
        {"display_name": "Swansea Modified Functional Test Courthouse"}""";
    private static final String COURTHOUSE_PATCH_INVALID_BODY = """
        {"courthouse_name": "READING","display_name": "Reading", code: "1234"}""";
    private static final String COURTHOUSE_BAD_ID = "/99999";
    private static final int OK = 200;
    private static final int CREATED = 201;
    private static final int BAD_REQUEST = 400;
    private static final int NOT_FOUND = 404;

    @AfterEach
    void tearDown() {
        clean();
    }

    @Test
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
    void createCourthouse() {
        Response response = executeCourthousesPost();

        assertEquals(CREATED, response.statusCode());

        JSONAssert.assertEquals(
            """
                {
                    "courthouse_name": "FUNC-SWANSEA",
                    "display_name": "Swansea Functional Test Courthouse",
                    "id": 0,
                    "security_group_ids": [ ],
                    "created_date_time": "",
                    "last_modified_date_time": ""
                }
                """,
            response.asString(),
            new CustomComparator(
                JSONCompareMode.NON_EXTENSIBLE,
                new Customization("id", new RegularExpressionValueMatcher<>("\\d+")),
                new Customization("security_group_ids", new ArrayValueMatcher<>(new ArraySizeComparator(JSONCompareMode.STRICT), 2)),
                new Customization("created_date_time", (actual, expected) -> isIsoDateTimeString(actual.toString())),
                new Customization("last_modified_date_time", (actual, expected) -> isIsoDateTimeString(actual.toString()))
            )
        );
    }

    @Test
    void patchCourthouse() {
        Response createCourthouseResponse = executeCourthousesPost();
        int testCourthouseId = new JSONObject(createCourthouseResponse.asString())
            .getInt("id");

        Response response = buildRequestWithExternalGlobalAccessAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(ADMIN_COURTHOUSES_URI + "/" + testCourthouseId))
            .body(COURTHOUSE_PATCH_BODY)
            .patch()
            .then()
            .extract().response();

        assertEquals(OK, response.statusCode());
    }

    @Test
    void patchCourthouseWithInvalidBody() {
        Response createCourthouseResponse = executeCourthousesPost();
        int testCourthouseId = new JSONObject(createCourthouseResponse.asString())
            .getInt("id");

        Response response = buildRequestWithExternalAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(ADMIN_COURTHOUSES_URI + "/" + testCourthouseId))
            .body(COURTHOUSE_PATCH_INVALID_BODY)
            .patch()
            .then()
            .extract().response();

        assertEquals(BAD_REQUEST, response.statusCode());
    }

    @Test
    void getExistingCourthouse() {
        Response createCourthouseResponse = executeCourthousesPost();
        int testCourthouseId = new JSONObject(createCourthouseResponse.asString())
            .getInt("id");

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

    private Response executeCourthousesPost() {
        return buildRequestWithExternalGlobalAccessAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(ADMIN_COURTHOUSES_URI))
            .body("""
                          {
                              "courthouse_name": "FUNC-SWANSEA",
                              "display_name": "Swansea Functional Test Courthouse"
                          }
                      """)
            .post()
            .thenReturn();
    }

}
