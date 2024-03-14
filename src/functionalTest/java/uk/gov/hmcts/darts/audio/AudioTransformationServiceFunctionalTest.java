package uk.gov.hmcts.darts.audio;

import io.restassured.builder.MultiPartSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.MultiPartSpecification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.FunctionalTest;

import java.io.IOException;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings({"VariableDeclarationUsageDistance", "CommentsIndentation"})
class AudioTransformationServiceFunctionalTest extends FunctionalTest {

    private static final String CASES_PATH = "/cases";

    private static final String AUDIOS_PATH = "/audios";
    private static final Object CONTENT_TYPE_APPLICATION_JSON = "application/json";
    public static final String AUDIO_FUNCTIONAL_TEST_CH_1_MP_2 = "audio/functional-test-ch1.mp2";


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

        String audioMetadata = """
            {
              "started_at": "2024-03-11T10:12:05.362Z",
              "ended_at": "2022-03-11T10:22:05.362Z",
              "channel": 1,
              "total_channels": 4,
              "format": "mp2",
              "filename": "functional-test-ch1.mp2",
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

        String audio1 = getContentsFromFile(AUDIO_FUNCTIONAL_TEST_CH_1_MP_2);

        MultiPartSpecification multiPartSpecification = new MultiPartSpecBuilder(audio1.getBytes())
            .fileName("functional-test-ch1.mp2")
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


}