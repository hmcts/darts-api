package uk.gov.hmcts.darts.audio;

import io.restassured.builder.MultiPartSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.MultiPartSpecification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.FunctionalTest;
import uk.gov.hmcts.darts.cases.model.AdvancedSearchResult;

import java.io.IOException;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SuppressWarnings({"VariableDeclarationUsageDistance", "CommentsIndentation"})
class AudioTransformationServiceFunctionalTest extends FunctionalTest {

    private static final String CASES_PATH = "/cases";
    private static final String AUDIOS_PATH = "/audios";
    private static final String CASE_SEARCH_URL = "/cases/search";
    private static final String AUDIO_REQUESTS_PATH = "/audio-requests";


    @AfterEach
    void cleanData() {
        buildRequestWithExternalAuth()
            .baseUri(getUri("/functional-tests/clean"))
            .redirects().follow(false)
            .delete();
    }


    @Test
    void testPostAudio() throws IOException {
        String courthouseName = "func-swansea-house-" + randomAlphanumeric(7);
        String courtroomName = "func-swansea-room-" + randomAlphanumeric(7);
        String caseNumber = "func-case-" + randomAlphanumeric(7);

        createCourtroomAndCourthouse(courthouseName, courtroomName);

        createCase(courthouseName, caseNumber);

        postAudio(courthouseName, courtroomName, caseNumber, "functional-test-ch1.mp2", 1);
        postAudio(courthouseName, courtroomName, caseNumber, "functional-test-ch2.mp2", 2);
        postAudio(courthouseName, courtroomName, caseNumber, "functional-test-ch3.mp2", 3);
        postAudio(courthouseName, courtroomName, caseNumber, "functional-test-ch4.mp2", 4);

        int hearingId = getHearingIdByCaseNumber(caseNumber);

        assertNotNull(hearingId);

        String audioRequest = """
            {
              "hearing_id": <<hearingId>>,
              "requestor": 0,
              "start_time": "2024-03-11T10:15:00.000Z",
              "end_time": "2024-03-11T10:20:00.000Z",
              "request_type": "DOWNLOAD"
            }
            """;
        audioRequest = audioRequest.replace("<<hearingId>>", String.valueOf(hearingId));

        Response audioRequestResponse = buildRequestWithExternalGlobalAccessAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(AUDIO_REQUESTS_PATH))
            .body(audioRequest)
            .post()
            .then()
            .extract().response();

        Integer requestId = audioRequestResponse.path("request_id");
        assertNotNull(requestId);

        buildRequestWithExternalAuth()
            .baseUri(getUri("/functional-tests/handleKedaInvocationForMediaRequests/" + requestId))
            .redirects().follow(false)
            .post().then()
            .assertThat()
            .statusCode(200)
            .extract().response();

    }

    private void createCase(String courthouseName, String caseNumber) {
        String caseBody = """
            {
                "courthouse": "<<courthouse>>",
                "case_number": "<<caseNumber>>",
                "defendants": ["Defendant A"],
                "judges": ["Judge 1"],
                "prosecutors": ["Prosecutor A"],
                "defenders": ["Defender A"]
            }
                """;
        caseBody = caseBody.replace("<<courthouse>>", courthouseName);
        caseBody = caseBody.replace("<<caseNumber>>", caseNumber);

        Response caseResponse = buildRequestWithExternalGlobalAccessAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(CASES_PATH))
            .body(caseBody)
            .post()
            .then()
            .extract().response();

        assertEquals(201, caseResponse.statusCode());
    }

    private void postAudio(String courthouseName, String courtroomName, String caseNumber, String audioFilename, int channelNumber) throws IOException {
        String audioMetadata = """
            {
              "started_at": "2024-03-11T10:12:05.362Z",
              "ended_at": "2024-03-11T10:22:05.362Z",
              "channel": <<channelNumber>>,
              "total_channels": 4,
              "format": "mp2",
              "filename": "<<audioFilename>>",
              "courthouse": "<<courthouse>>",
              "courtroom": "<<courtroom>>",
              "file_size": 9600,
              "checksum": "c171d11d62ee00e414eb347f5a1fa024d4ed039621ef8185ff4a951b44a7a4d0",
              "cases": [
                  "<<casenumber>>"
                ]
            }
            """;
        audioMetadata = audioMetadata.replace("<<courthouse>>", courthouseName);
        audioMetadata = audioMetadata.replace("<<courtroom>>", courtroomName);
        audioMetadata = audioMetadata.replace("<<casenumber>>", caseNumber);
        audioMetadata = audioMetadata.replace("<<audioFilename>>", audioFilename);
        audioMetadata = audioMetadata.replace("<<channelNumber>>", String.valueOf(channelNumber));

        String audio = getContentsFromFile("audio/" + audioFilename);

        MultiPartSpecification multiPartSpecification = new MultiPartSpecBuilder(audio.getBytes())
            .fileName(audioFilename)
            .controlName("file")
            .mimeType("audio/mpeg")
            .build();

        Response postAudioResponse = buildRequestWithExternalGlobalAccessAuth()
            .contentType(ContentType.MULTIPART)
            .accept("application/json, text/plain, */*")
            .baseUri(getUri(AUDIOS_PATH))
            .multiPart(multiPartSpecification)
            .multiPart("metadata", audioMetadata, "application/json")
            .when()
            .post(getUri(AUDIOS_PATH))
            .then()
            .extract().response();

        assertEquals(200, postAudioResponse.statusCode());
    }


    private int getHearingIdByCaseNumber(String caseNumber) {
        String caseBody = """
            {
                "case_number": "<<caseNumber>>"
            }
            """;

        caseBody = caseBody.replace("<<caseNumber>>", caseNumber);

        // search for case using case number
        Response response = buildRequestWithExternalAuth()
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