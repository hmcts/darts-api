package uk.gov.hmcts.darts.events;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PostEventsFunctionalTest extends FunctionalTest {


    public static final String ENDPOINT_URL = "/events";

    @Test
    @Disabled
    void success() {
        Response response = buildRequestWithAuth()
            .contentType(ContentType.JSON)
            .body("""
                      {
                        "message_id": "100",
                        "type": "1000",
                        "sub_type": "1002",
                        "event_id": "12345",
                        "courthouse": "swansea",
                        "courtroom": "1",
                        "case_numbers": [
                          "Swansea_case_1"
                        ],
                        "event_text": "some text for the event",
                        "date_time": "2023-08-08T14:01:06.085Z"
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
    void fail() {
        Response response = buildRequestWithAuth()
            .contentType(ContentType.JSON)
            .body("""
                      {
                        "message_id": "100",
                        "type": "1000",
                        "sub_type": "1002",
                        "event_id": "12345",
                        "courthouse": "",
                        "courtroom": "1",
                        "case_numbers": [
                          "Swansea_case_1"
                        ],
                        "event_text": "some text for the event",
                        "date_time": "2023-08-08T14:01:06.085Z"
                      }""")
            .when()
            .baseUri(getUri(ENDPOINT_URL))
            .redirects().follow(false)
            .post()
            .then()
            .extract().response();

        assertEquals(400, response.statusCode());
    }
}
