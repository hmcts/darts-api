package uk.gov.hmcts.darts.hearings;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.FunctionalTest;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;

class HearingsGetEventsFunctionalTest extends FunctionalTest {

    public static final String ENDPOINT_URL = "/hearings/{hearingId}/events";
    public static final String ADD_EVENT_URL = "/events";
    public static final String CASE_SEARCH_URL = "/cases/search";

    private static final String COURTHOUSE = "Swansea";

    @AfterEach
    void cleanData() {
        buildRequestWithExternalAuth()
            .baseUri(getUri("/functional-tests/clean"))
            .redirects().follow(false)
            .delete();
    }

    @Test
    void success() {
        String courtroomName = "func-swansea-room-" + randomAlphanumeric(7);

        createCourtroomAndCourthouse(COURTHOUSE, courtroomName);

        String randomCaseNumber = randomCaseNumber();
        String randomEventText1 = randomAlphanumeric(15);
        String requestBody = String.format(
            """
            {
              "message_id": "12345",
              "type": "1000",
              "sub_type": "1002",
              "event_id": "12345",
              "courthouse": "%s",
              "courtroom": "%s",
              "case_numbers": [
                "%s"
              ],
              "event_text": "%s",
              "date_time": "2023-08-08T14:01:06.085Z"
            }""",
            COURTHOUSE, courtroomName, randomCaseNumber, randomEventText1);

        buildRequestWithExternalAuth()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
            .baseUri(getUri(ADD_EVENT_URL))
            .redirects().follow(false)
            .post();


        String randomEventText2 = randomAlphanumeric(15);
        requestBody = String.format(
            """
            {
              "message_id": "12345",
              "type": "1000",
              "sub_type": "1002",
              "event_id": "12345",
              "courthouse": "%s",
              "courtroom": "%s",
              "case_numbers": [
                "%s"
              ],
              "event_text": "%s",
              "date_time": "2023-08-08T14:01:06.085Z"
            }""",
            COURTHOUSE, courtroomName, randomCaseNumber, randomEventText2);

        buildRequestWithExternalAuth()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
            .baseUri(getUri(ADD_EVENT_URL))
            .redirects().follow(false)
            .post();


        int hearingId = getHearingIdByCaseNumber(randomCaseNumber);
        Response response = buildRequestWithExternalAuth()
            .pathParam("hearingId", hearingId)
            .when()
            .redirects().follow(false)
            .get(getUri(ENDPOINT_URL))
            .then()
            .extract().response();

        String responsePrettyString = response.asPrettyString();
        assertThat(responsePrettyString).contains(randomEventText1);
        assertThat(responsePrettyString).contains(randomEventText2);
    }

    private int getHearingIdByCaseNumber(String caseNumber) {
        Response response = buildRequestWithExternalAuth()
            .contentType(ContentType.JSON)
            .param("case_number", caseNumber)
            .when()
            .redirects().follow(false)
            .get(getUri(CASE_SEARCH_URL))
            .then()
            .extract().response();
        return response.jsonPath().get("[0].hearings.id[0]");
    }

}
