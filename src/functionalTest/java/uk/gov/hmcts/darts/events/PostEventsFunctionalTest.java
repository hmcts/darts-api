package uk.gov.hmcts.darts.events;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.FunctionalTest;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PostEventsFunctionalTest extends FunctionalTest {


    public static final String ENDPOINT_URL = "/events";

    @AfterEach
    void cleanData() {
        buildRequestWithExternalAuth()
            .baseUri(getUri("/functional-tests/clean"))
            .redirects().follow(false)
            .delete();
    }

    @Test
    void success() {
        String courthouseName = "func-swansea-house-" + randomAlphanumeric(7);
        String courtroomName = "func-swansea-room-" + randomAlphanumeric(7);

        createCourtroomAndCourthouse(courthouseName,courtroomName);

        String bodyText = """
                      {
                        "message_id": "100",
                        "type": "1000",
                        "sub_type": "1002",
                        "event_id": "12345",
                        "courthouse": "<<courtHouseName>>",
                        "courtroom": "<<courtroomName>>",
                        "case_numbers": [
                          "func-Swansea_case_1"
                        ],
                        "event_text": "some text for the event",
                        "date_time": "2023-08-08T14:01:06.085Z"
                      }""";
        bodyText = bodyText.replace("<<courtHouseName>>", courthouseName);
        bodyText = bodyText.replace("<<courtroomName>>", courtroomName);

        Response response = buildRequestWithExternalAuth()
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
    void fail() {
        Response response = buildRequestWithExternalAuth()
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
                          "func-Swansea_case_1"
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
