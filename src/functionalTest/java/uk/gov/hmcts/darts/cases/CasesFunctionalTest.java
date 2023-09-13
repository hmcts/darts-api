package uk.gov.hmcts.darts.cases;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.FunctionalTest;

import java.math.BigInteger;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
class CasesFunctionalTest  extends FunctionalTest {
    public static final String CASES_URI = "/cases";
    public static final String SEARCH = "/search";
    public static final String HEARINGS = "/hearings";
    public static final String EVENTS = "/events";
    public static final String COURTHOUSE = "courthouse";
    public static final String COURTROOM = "courtroom";
    public static final String CASE_NUMBER = "case_number";
    public static final String CASE_NUMBERS = "case_numbers";
    public static final String CASE_DATE = "date";
    public static final String COURTHOUSE1 = "LEEDS";
    public static final String COURTHOUSE_ROOM = "ROOM_A";
    public static final String DATE1 = "2023-09-12";
    public static final String CASE_BAD_ID = "/0";
    public static final String JUDGE_NAME = "judge_name";
    public static final String CASE_ID = "case_id";
    public static final String ID = "id";
    public static final String DEFENDANT_NAME = "defendant_name";
    public static final String DATE_FROM = "date_from";
    public static final String DATE_TO = "date_to";
    public static final String EVENT_TEST_CONTAINS = "event_test_contains";
    public static final int NOT_FOUND = 404;
    public static final int OK = 200;
    public static final int CREATED = 201;


    @Test
    @Order(1)
    void createCaseAndEvent() {
        String uniqueCaseNum = generateUniquesCaseNum();

        String casePayload = "{ \"" + COURTHOUSE + "\" : \"LEEDS\",\"" + CASE_NUMBER + "\" : \"" + uniqueCaseNum + "\",\"defendants\": [\"defendantC\"],\"judges\": [\"judgeC\"],\"prosecutors\": [\"prosecutorC\"],\"defenders\": [\"defenderC\"]}";

        Response caseResponse = buildRequestWithAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(CASES_URI))
            .body(casePayload)
            .post()
            .then()
            .extract().response();

        assertEquals(CREATED, caseResponse.statusCode());

        String eventsPayload = "{\"message_id\": \"54321\",\"type\": \"1000\",\"sub_type\": \"1002\",\"event_id\": \"12345\",\"" + COURTHOUSE + "\": \"" + COURTHOUSE1 + "\",\"" + COURTROOM + "\" : \"" + COURTHOUSE_ROOM + "\",\"" + CASE_NUMBERS + "\": [\"" + uniqueCaseNum + "\"],\"event_text\": \"FunctionalTestSetup2\",\"date_time\": \"2023-09-12T12:57:18.596Z\",\"retention_policy\": {\"case_retention_fixed_policy\": \"unknown\",\"case_total_sentence\": \"unknown\"}}";
        Response eventResponse = buildRequestWithAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(EVENTS))
            .body(eventsPayload)
            .post()
            .then()
            .extract().response();

        assertEquals(CREATED, eventResponse.statusCode());
    }


    @Test
    @Order(2)
    void createEvent() {
        String caseNum = getCaseNumber();
        String payload = "{\"message_id\": \"54321\",\"type \": \"1000\",\"sub_type\": \"1002\",\"event_id\": \"12345\",\"" + COURTHOUSE + "\": \"" + COURTHOUSE1 + "\",\"" + COURTROOM + "\": \"" + COURTHOUSE_ROOM + "\",\"" + CASE_NUMBER + "\": [\"" + caseNum + "\"],\"event_text\": \"Functional Test Setup\",\"date_time\": \"2023-09-12T12:57:18.596Z\",\"retention_policy\": {\"case_retention_fixed_policy\": \"unknown\",\"case_total_sentence\": \"unknown\"}}";
        Response response = buildRequestWithAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(EVENTS))
            .body(payload)
            .post()
            .then()
            .extract().response();

        assertEquals(CREATED, response.statusCode());
    }


    @Test
    @Order(3)
    void getAllCases() {
        Response response = buildRequestWithAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(CASES_URI))
            .param(COURTHOUSE, COURTHOUSE1)
            .param(COURTROOM, COURTHOUSE_ROOM)
            .param(CASE_DATE, DATE1)
            .get()
            .then()
            .extract().response();

        assertEquals(OK, response.statusCode());
    }

    @Test
    @Order(4)
    void getExistingCase() {
        int caseId = getCaseId();

        if (caseId == -1) {
            assertEquals(NOT_FOUND, 404);
        } else {
            Response response = buildRequestWithAuth()
                .contentType(ContentType.JSON)
                .when()
                .baseUri(getUri(CASES_URI + "/" + caseId))
                .get()
                .then()
                .extract().response();

            assertEquals(OK, response.statusCode());
        }
    }

    @Test
    @Order(5)
    void getCaseBadRequest() {
        Response response = buildRequestWithAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(CASES_URI + CASE_BAD_ID))
            .get()
            .then()
            .extract().response();

        assertEquals(NOT_FOUND, response.statusCode());
    }

    @Test
    @Order(6)
    void patchCase() {
        int caseId = getCaseId();

        if (caseId == -1) {
            assertEquals(NOT_FOUND, 404);
        } else {
            String payload = "{\"retain_until\": \"2023-10-07T11:49:44.618Z\"}";

            Response response = buildRequestWithAuth()
                .contentType(ContentType.JSON)
                .when()
                .baseUri(getUri(CASES_URI + "/" + caseId))
                .body(payload)
                .patch()
                .then()
                .extract().response();

            assertEquals(OK, response.statusCode());
        }
    }

    @Test
    @Order(7)
    void searchCase() {
        String caseNum = getCaseNumber();

        Response response = buildRequestWithAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(CASES_URI + SEARCH))
            .param(CASE_NUMBER, caseNum)
            .param(COURTHOUSE,COURTHOUSE1)
            .param(COURTROOM,"")
            .param(DEFENDANT_NAME, "")
            .param(JUDGE_NAME, "")
            .param(DATE_FROM, "")
            .param(DATE_TO, "")
            .param(EVENT_TEST_CONTAINS, "")
            .get()
            .then()
            .extract().response();

        assertEquals(OK, response.statusCode());
    }

    @Test
    @Order(8)
    void getCaseHearing() {
        int caseId = getCaseId();

        if (caseId == -1) {
            assertEquals(NOT_FOUND, 404);
        } else {
            Response response = buildRequestWithAuth()
                .contentType(ContentType.JSON)
                .when()
                .baseUri(getUri(CASES_URI + "/" + caseId + HEARINGS))
                .get()
                .then()
                .extract().response();

            assertEquals(OK, response.statusCode());
        }
    }

    @Test
    @Order(9)
    void getCaseHearingEvents() {
        int hearingId = getCaseHearingId();

        if (hearingId == -1) {
            assertEquals(NOT_FOUND, 404);
        } else {
            Response response = buildRequestWithAuth()
                .contentType(ContentType.JSON)
                .when()
                .baseUri(getUri(CASES_URI + HEARINGS + "/" + hearingId + EVENTS))
                .get()
                .then()
                .extract().response();

            assertEquals(OK, response.statusCode());
        }
    }

    private static String generateUniquesCaseNum() {
        String generateUUIDNo = String.format("%010d",
                                              new BigInteger(UUID.randomUUID().
                                                                 toString().
                                                                 replace("-", ""),
                                                             16));

        generateUUIDNo = generateUUIDNo.substring( generateUUIDNo.length() - 15);

        return generateUUIDNo;
    }

    public String getCaseNumber() {
        String caseNum;

        List<String> caseNums = buildRequestWithAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(CASES_URI + SEARCH))
            .param(COURTHOUSE, COURTHOUSE1)
            .param(JUDGE_NAME, "judgeC")
            .get()
            .then()
            .extract()
            .response()
            .getBody()
            .jsonPath().get(CASE_NUMBER);

        caseNum = getCaseNumberFromList(caseNums);

        return caseNum;
    }

    public int getCaseId() {
        String caseNum = getCaseNumber();
        if("-1".equals(caseNum)) {
            assertEquals(NOT_FOUND, 404);
        }

        int caseId;

        List<Integer> caseIds = buildRequestWithAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(CASES_URI + SEARCH))
            .param(CASE_NUMBER, caseNum)
            .get()
            .then()
            .extract()
            .response()
            .getBody()
            .jsonPath().get(CASE_ID);

        caseId = getIdFromList(caseIds);

        return caseId;
    }

    Integer getCaseHearingId() {
        int caseId = getCaseId();

        if (caseId == -1) {
            return -1;
        }

        int hearingId;

        List<Integer> hearingIds = buildRequestWithAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(CASES_URI + "/" + caseId + HEARINGS))
            .get()
            .then()
            .extract()
            .response()
            .getBody()
            .jsonPath().get(ID);

        hearingId = getIdFromList(hearingIds);

        return hearingId;
    }

    private static int getIdFromList(List<Integer> listIds) {
        int listId;
        int len;
        if (listIds.isEmpty()) {
            listId = -1;
        } else {
            len = listIds.size();
            listId = listIds.get(--len);
        }
        return listId;
    }

    private static String getCaseNumberFromList(List<String> listIds) {
        String listId;
        if (listIds.isEmpty()) {
            listId = "-1";
        } else {
            int len = listIds.size();
            listId = listIds.get(--len);
        }
        return listId;
    }
}
