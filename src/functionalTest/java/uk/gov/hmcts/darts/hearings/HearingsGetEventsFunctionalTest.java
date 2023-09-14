package uk.gov.hmcts.darts.hearings;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import uk.gov.hmcts.darts.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;

class HearingsGetEventsFunctionalTest extends FunctionalTest {

    public static final String ENDPOINT_URL = "/hearings/{hearingId}/events";
    public static final String ADD_EVENT_URL = "/events";
    public static final String CASE_SEARCH_URL = "/cases/search";

    @Test
    @DisabledIfEnvironmentVariable(named = "TEST_URL", matches = "https://darts-api-staging.staging.platform.hmcts.net")
    void success() {

        String randomCaseNumber = RandomStringUtils.randomAlphanumeric(15);
        String requestBody = """
            {
              "message_id": "12345",
              "type": "1000",
              "sub_type": "1002",
              "event_id": "12345",
              "courthouse": "swansea",
              "courtroom": "1",
              "case_numbers": [
                "<<caseNumber>>"
              ],
              "event_text": "<<eventText>>",
              "date_time": "2023-08-08T14:01:06.085Z"
            }""";

        requestBody = requestBody.replace("<<caseNumber>>", randomCaseNumber);
        String randomEventText1 = RandomStringUtils.randomAlphanumeric(15);
        requestBody = requestBody.replace("<<eventText>>", randomEventText1);
        buildRequestWithAuth()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
            .baseUri(getUri(ADD_EVENT_URL))
            .redirects().follow(false)
            .post();


        requestBody = """
            {
              "message_id": "12345",
              "type": "1000",
              "sub_type": "1002",
              "event_id": "12345",
              "courthouse": "swansea",
              "courtroom": "1",
              "case_numbers": [
                "<<caseNumber>>"
              ],
              "event_text": "<<eventText>>",
              "date_time": "2023-08-08T14:01:06.085Z"
            }""";

        requestBody = requestBody.replace("<<caseNumber>>", randomCaseNumber);
        String randomEventText2 = RandomStringUtils.randomAlphanumeric(15);
        requestBody = requestBody.replace("<<eventText>>", randomEventText2);
        buildRequestWithAuth()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
            .baseUri(getUri(ADD_EVENT_URL))
            .redirects().follow(false)
            .post();


        int hearingId = getHearingIdByCaseNumber(randomCaseNumber);
        Response response = buildRequestWithAuth()
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
        Response response = buildRequestWithAuth()
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
