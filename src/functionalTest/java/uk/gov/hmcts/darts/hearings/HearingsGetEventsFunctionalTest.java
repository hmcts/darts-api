package uk.gov.hmcts.darts.hearings;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.FunctionalTest;
import uk.gov.hmcts.darts.cases.model.AdvancedSearchResult;

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
        String courtroomName = "FUNC-SWANSEA-ROOM-" + randomAlphanumeric(7);

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
              "date_time": "2023-08-08T14:01:06Z"
            }""",
            COURTHOUSE, courtroomName, randomCaseNumber, randomEventText1);

        buildRequestWithExternalGlobalAccessAuth()
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
              "message_id": "444",
              "type": "1000",
              "sub_type": "1002",
              "event_id": "98765",
              "courthouse": "%s",
              "courtroom": "%s",
              "case_numbers": [
                "%s"
              ],
              "event_text": "%s",
              "date_time": "2023-08-08T14:01:06Z"
            }""",
            COURTHOUSE, courtroomName, randomCaseNumber, randomEventText2);

        buildRequestWithExternalGlobalAccessAuth()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
            .baseUri(getUri(ADD_EVENT_URL))
            .redirects().follow(false)
            .post();


        String versionedEventText3 = randomAlphanumeric(15);
        requestBody = String.format(
            """
            {
              "message_id": "888",
              "type": "1000",
              "sub_type": "1002",
              "event_id": "98765",
              "courthouse": "%s",
              "courtroom": "%s",
              "case_numbers": [
                "%s"
              ],
              "event_text": "%s",
              "date_time": "2023-08-08T14:01:06Z"
            }""",
            COURTHOUSE, courtroomName, randomCaseNumber, versionedEventText3);

        buildRequestWithExternalGlobalAccessAuth()
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
        assertThat(responsePrettyString).doesNotContain(randomEventText2);
        assertThat(responsePrettyString).contains(versionedEventText3);
    }

    private int getHearingIdByCaseNumber(String caseNumber) {
        String caseBody = """
        {
            "case_number": "<<caseNumber>>"
        }
            """;

        caseBody = caseBody.replace("<<caseNumber>>", caseNumber);

        // search for case using case number
        Response response = buildRequestWithExternalGlobalAccessAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(CASE_SEARCH_URL))
            .body(caseBody)
            .post()
            .then()
            .extract().response();

        var caseList = response.jsonPath().getList("", AdvancedSearchResult.class);
        var firstCase = caseList.get(0);
        return firstCase.getHearings().get(0).getId();
    }

}
