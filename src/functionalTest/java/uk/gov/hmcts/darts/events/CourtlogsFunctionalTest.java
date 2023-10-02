package uk.gov.hmcts.darts.events;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CourtlogsFunctionalTest extends FunctionalTest {


    public static final String ENDPOINT_URL = "/courtlogs";

    // To do: Not sure how to clean up for tests that are inter-dependent
    @Test
    @Order(1)
    void postSuccess() {
        buildRequestWithExternalAuth()
            .baseUri(getUri("/functional-tests/courthouse/func-liverpool/courtroom/1"))
            .redirects().follow(false)
            .post();

        Response response = buildRequestWithExternalAuth()
            .contentType(ContentType.JSON)
            .body("""
                      {
                        "log_entry_date_time": "1999-05-23T09:15:25Z",
                        "courthouse": "func-liverpool",
                        "courtroom": "1",
                        "case_numbers": [
                          "func-CASE1001"
                        ],
                        "text": "System : Start Recording : Record: Case Code:0008, New Case"
                      }""")
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
        Response response = buildRequestWithExternalAuth()
            .contentType(ContentType.JSON)
            .body("""
                      {
                        "log_entry_date_time": "2023-05-23T09:15:25Z",
                        "courthouse": "",
                        "courtroom": "1",
                        "case_numbers": [
                          "func-CASE1001"
                        ],
                        "text": "System : Start Recording : Record: Case Code:0008, New Case"
                      }""")
            .when()
            .baseUri(getUri(ENDPOINT_URL))
            .redirects().follow(false)
            .post()
            .then()
            .extract().response();

        assertEquals(400, response.statusCode());
    }

    @Test
    @Order(3)
    void getSuccess() {
        Response response = buildRequestWithExternalAuth()
            .contentType(ContentType.JSON)
            .param("courthouse", "func-liverpool")
            .param("case_number", "func-CASE1001")
            .param("start_date_time", "1999-05-23T09:15:25Z")
            .param("end_date_time", "1999-05-23T09:15:25Z")
            .when()
            .baseUri(getUri(ENDPOINT_URL))
            .redirects().follow(false)
            .get()
            .then()
            .extract().response();

        assertEquals("""
                         [
                             {
                                 "courthouse": "func-liverpool",
                                 "caseNumber": "func-CASE1001",
                                 "timestamp": "1999-05-23T09:15:25Z",
                                 "eventText": "System : Start Recording : Record: Case Code:0008, New Case"
                             }
                         ]""", response.asPrettyString());
    }

    @Test
    @Order(4)
    void getFail() {
        Response response = buildRequestWithExternalAuth()
            .contentType(ContentType.JSON)
            .param("courthouse", "func-liverpool")
            .param("case_number", "func-CASE1001")
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
