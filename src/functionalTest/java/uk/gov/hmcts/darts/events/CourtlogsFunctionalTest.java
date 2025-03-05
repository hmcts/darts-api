package uk.gov.hmcts.darts.events;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import uk.gov.hmcts.darts.FunctionalTest;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

// To do: Not sure how to clean up for tests that are inter-dependent
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CourtlogsFunctionalTest extends FunctionalTest {

    public static final String ENDPOINT_URL = "/courtlogs";
    public final String courthouseName = "FUNC-SWANSEA-HOUSE-" + randomAlphanumeric(7);


    @AfterAll
    void cleanData() {
        buildRequestWithExternalAuth()
            .baseUri(getUri("/functional-tests/clean"))
            .redirects().follow(false)
            .delete();
    }

    @Test
    @Order(1)
    void postSuccess() {

        String courtroomName = "FUNC-SWANSEA-ROOM-" + randomAlphanumeric(7);

        createCourtroomAndCourthouse(courthouseName, courtroomName);

        String bodyText = """
            {
              "log_entry_date_time": "1999-05-23T09:15:25Z",
              "courthouse": "<<courtHouseName>>",
              "courtroom": "<<courtroomName>>",
              "case_numbers": [
                "FUNC-CASE1001"
              ],
              "text": "System : Start Recording : Record: Case Code:0008, New Case"
            }""";
        bodyText = bodyText.replace("<<courtHouseName>>", courthouseName);
        bodyText = bodyText.replace("<<courtroomName>>", courtroomName);

        Response response = buildRequestWithExternalGlobalAccessAuth()
            .contentType(ContentType.JSON)
            .body(bodyText)
            .when()
            .baseUri(getUri(ENDPOINT_URL))
            .redirects().follow(false)
            .post()
            .then()
            .extract().response();

        assertEquals(201, response.statusCode());
    }

    @Test
    @Order(2)
    void postFail() {
        Response response = buildRequestWithExternalGlobalAccessAuth()
            .contentType(ContentType.JSON)
            .body("""
                      {
                        "log_entry_date_time": "2023-05-23T09:15:25Z",
                        "courthouse": "Unknown Courthouse",
                        "courtroom": "1",
                        "case_numbers": [
                          "FUNC-CASE1001"
                        ],
                        "text": "System : Start Recording : Record: Case Code:0008, New Case"
                      }""")
            .when()
            .baseUri(getUri(ENDPOINT_URL))
            .redirects().follow(false)
            .post()
            .then()
            .extract().response();

        assertEquals(404, response.statusCode());
    }

    @Test
    @Order(3)
    void getSuccess() {
        Response response = buildRequestWithExternalGlobalAccessAuth()
            .contentType(ContentType.JSON)
            .param("courthouse", courthouseName)
            .param("case_number", "FUNC-CASE1001")
            .param("start_date_time", "1999-05-23T09:15:25Z")
            .param("end_date_time", "1999-05-23T09:15:25Z")
            .when()
            .baseUri(getUri(ENDPOINT_URL))
            .redirects().follow(false)
            .get()
            .then()
            .extract().response();

        String expectedResponse = """
            [
                {
                    "courthouse": "<<courtHouseName>>",
                    "caseNumber": "FUNC-CASE1001",
                    "timestamp": "1999-05-23T09:15:25Z",
                    "eventText": "System : Start Recording : Record: Case Code:0008, New Case"
                }
            ]""";
        expectedResponse = expectedResponse.replace("<<courtHouseName>>", StringUtils.toRootLowerCase(courthouseName));
        assertEquals(expectedResponse, response.asPrettyString());
    }

    @Test
    @Order(4)
    void getFail() {
        Response response = buildRequestWithExternalGlobalAccessAuth()
            .contentType(ContentType.JSON)
            .param("courthouse", "FUNC-liverpool")
            .param("case_number", "FUNC-CASE1001")
            .param("start_date_time1", "2023-05-24T09:15:25Z")
            .param("end_date_time", "2023-05-23T09:15:25Z")
            .when()
            .baseUri(getUri(ENDPOINT_URL))
            .redirects().follow(false)
            .get()
            .then()
            .extract().response();

        assertEquals(400, response.statusCode());
        assertThat(response.asPrettyString()).contains(
            "Required request parameter 'start_date_time' for method parameter type OffsetDateTime is not present");

    }

}
