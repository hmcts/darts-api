package uk.gov.hmcts.darts.datamanagement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.FunctionalTest;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestSummary;

import java.util.List;

@Slf4j
class DataManagementServiceFunctionalTest extends FunctionalTest {

    private static final String TEST_BINARY_STRING = "Test String to be converted to binary!";
    public static final String CREATE_CONTAINER = "/test/create-container";
    public static final String STORE_BLOB_URL = "/test/create-blob";
    public static final String ADD_AUDIO_REQUEST_URL = "/audio-requests";

    public static final String LIST_AUDIO_REQUESTS = "/audio-requests";

    public static final String PROCESS_AUDIO = "/test/process-audio";

    public static final String MOVE_AUDIO = "/test/move-audio";

    //public static final String DELETE_AUDIO_REQUEST = "/audio-requests/{audio_request_id}";
    public static final String DELETE_AUDIO_REQUEST = "/audio-requests";

    public static final String DELETE_BLOB = "/test/delete-blob";

    public static final String CREATE_HEARING = "/test/create-hearing";

    public static final Integer USER_ID = 0;

    public Integer mediaRequestId;

    public String unstructuredUuid;

    public String outboundUuid;

    @BeforeEach
    void createDataStores() {
        //darts-outbound
        buildRequestWithAuth()
            .param("containerName", "darts-unstructured")
            .when()
            .baseUri(getUri(CREATE_CONTAINER))
            .redirects().follow(false)
            .post();

        buildRequestWithAuth()
            .param("containerName", "darts-outbound")
            .when()
            .baseUri(getUri(CREATE_CONTAINER))
            .redirects().follow(false)
            .post();
    }


    @Test
    void audioLifecycle() {
        //store a blob
        unstructuredUuid = storeBlob();

        processAudioRequest();

        Response response = buildRequestWithAuth()
            .contentType(ContentType.JSON)
            .param("unstructuredUuid", unstructuredUuid)
            .when()
            .baseUri(getUri(MOVE_AUDIO))
            .redirects().follow(false)
            .get()
            .then()
            .extract().response();

        JsonPath jsonPath = response.jsonPath();
        outboundUuid = jsonPath.getString("uuid");

        log.info("outboundUuid is: " + outboundUuid);


    }

    private String storeBlob() {

        Response response = buildRequestWithAuth()
            .when()
            .contentType(ContentType.TEXT)
            .body(TEST_BINARY_STRING)
            .baseUri(getUri(STORE_BLOB_URL))
            .accept(ContentType.JSON)
            .redirects().follow(false)
            .post();

        JsonPath jsonPath = response.jsonPath();
        String uuid = jsonPath.getString("uuid");

        log.info("uuid is: " + uuid);
        return uuid;
    }

    /*
    This test completes but is not working, it fails when I send the process
    request call as the blob is not connectoed to the hearing

     */
    private void processAudioRequest() {
        //need a hearing to create and audio request
        Response response = buildRequestWithAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(CREATE_HEARING))
            .redirects().follow(false)
            .post()
            .then()
            .extract().response();

        JsonPath jsonPath = response.jsonPath();
        String hearingId = jsonPath.getString("hearing_id");

        String requestBody = """
            {
              "hearing_id": "hearingId",
              "start_time": "2023-05-31T09:00:00Z",
              "end_time": "2023-05-31T12:00:00Z",
              "requestor": "0",
              "request_type": "DOWNLOAD"
            }""";

        requestBody = requestBody.replace("hearingId", hearingId);

        // would be good to get the request id back from this call
        buildRequestWithAuth()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
            .baseUri(getUri(ADD_AUDIO_REQUEST_URL))
            .redirects().follow(false)
            .post();

        response = buildRequestWithAuth()
            .contentType(ContentType.JSON)
            .when()
            .baseUri(getUri(LIST_AUDIO_REQUESTS))
            .param("user_id", USER_ID)
            .param("expired", false)
            .header("user_id", USER_ID)
            .redirects().follow(false)
            .get()
            .then()
            .extract().response();

        ObjectMapper objectMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .build();

        try {
            List<AudioRequestSummary> list = objectMapper.readValue(response.getBody().prettyPrint(), new TypeReference<List<AudioRequestSummary>>(){});

            //Just taking the last request at the moment, won't work properly in multi user environment
            AudioRequestSummary audioRequestSummary = list.get(list.size() - 1);
            mediaRequestId = audioRequestSummary.getMediaRequestId();

            log.info("response.getBody().prettyPrint()" + response.getBody().prettyPrint());

            buildRequestWithAuth()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .param("requestId", mediaRequestId)
                .when()
                .baseUri(getUri(PROCESS_AUDIO))
                .redirects().follow(false)
                .get();
        } catch (JsonProcessingException e) {
            log.error("Error in JSON");
        }
    }

    @AfterEach
    void cleanup() {
        log.info("deleting media request " + mediaRequestId);

        buildRequestWithAuth()
            .contentType(ContentType.JSON)
            .param("audio_request_id", mediaRequestId)
            .when()
            .baseUri(getUri(DELETE_AUDIO_REQUEST))
            .redirects().follow(false)
            .delete();

        //delete blobs
        buildRequestWithAuth()
            .contentType(ContentType.JSON)
            .param("containerName", "darts-unstructured")
            .param("uuid", unstructuredUuid)
            .when()
            .baseUri(getUri(DELETE_BLOB))
            .redirects().follow(false)
            .delete();

        buildRequestWithAuth()
            .contentType(ContentType.JSON)
            .param("containerName", "darts-outbound")
            .param("uuid", outboundUuid)
            .when()
            .baseUri(getUri(DELETE_BLOB))
            .redirects().follow(false)
            .delete();
    }

}
