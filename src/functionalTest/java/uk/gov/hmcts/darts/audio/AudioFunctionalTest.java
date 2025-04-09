package uk.gov.hmcts.darts.audio;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.models.BlobStorageException;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.darts.FunctionalTest;
import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequestWithStorageGUID;
import uk.gov.hmcts.darts.datamanagement.service.DataManagementService;
import uk.gov.hmcts.darts.testutil.TestUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles({"dev", "h2db", "functionalTest"})
@Slf4j
class AudioFunctionalTest extends FunctionalTest {

    private static final String TEST_BINARY_STRING = "Test String to be converted to binary!";
    private static final String ADD_AUDIO_METADATA_URI = "/audios/metadata";

    @Value("${darts.storage.blob.container-name.inbound}")
    private String inboundStorageContainerName;

    private final List<String> inboundAudioBlobsToBeDeleted = new ArrayList<>();

    @Autowired
    private DataManagementService dataManagementService;

    @AfterEach
    void cleanupBlobData() {
        if (CollectionUtils.isNotEmpty(inboundAudioBlobsToBeDeleted)) {
            for (String blobId : inboundAudioBlobsToBeDeleted) {
                try {
                    dataManagementService.deleteBlobData(inboundStorageContainerName, blobId);
                } catch (Exception e) {
                    log.error("Failed to delete blob with ID: {}", blobId, e);
                }
            }
        }
        clean();
    }

    @Test
    void addAudioMetadata_ShouldFail_WhenCourthouseInvalidAndBlobGetsCleanedUp() throws IOException {
        byte[] testStringInBytes = TEST_BINARY_STRING.getBytes(StandardCharsets.UTF_8);
        BinaryData originalData = BinaryData.fromBytes(testStringInBytes);
        String blobId = dataManagementService.saveBlobData(inboundStorageContainerName, originalData);
        inboundAudioBlobsToBeDeleted.add(blobId);

        String checksum = "123";
        var savedBlobData = dataManagementService.getBlobData(inboundStorageContainerName, blobId);
        assertNotNull(savedBlobData);

        AddAudioMetadataRequestWithStorageGUID request = createAddAudioRequest(
            OffsetDateTime.parse("2023-10-01T10:00:00Z"),
            OffsetDateTime.parse("2023-10-01T11:00:00Z"),
            "InvalidCourthouseName",
            "Room 1",
            "mp3",
            blobId,
            checksum,
            "case1", "case2");
        String requestBody = TestUtils.createObjectMapper().writeValueAsString(request);
        Response response = buildRequestWithExternalGlobalAccessAuth()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
            .baseUri(getUri(ADD_AUDIO_METADATA_URI))
            .redirects().follow(false)
            .post().then().extract().response();

        String actualJson = response.asPrettyString();
        String expectedJson = """
            {"type":"COMMON_100","title":"Provided courthouse does not exist","status":404,"detail":"Courthouse 'INVALIDCOURTHOUSENAME' not found."}""";

        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.NON_EXTENSIBLE);

        var exception = assertThrows(
            BlobStorageException.class, () -> dataManagementService.getBlobData(inboundStorageContainerName, blobId));

        assertThat(exception.getMessage()).contains("The specified blob does not exist");

    }

    private AddAudioMetadataRequestWithStorageGUID createAddAudioRequest(OffsetDateTime startedAt, OffsetDateTime endedAt,
                                                                         String courthouse, String courtroom, String filetype,
                                                                         String guid, String checksum, String... casesList) {

        AddAudioMetadataRequestWithStorageGUID addAudioMetadataRequest = new AddAudioMetadataRequestWithStorageGUID();
        addAudioMetadataRequest.startedAt(startedAt);
        addAudioMetadataRequest.endedAt(endedAt);
        addAudioMetadataRequest.setChannel(1);
        addAudioMetadataRequest.totalChannels(2);
        addAudioMetadataRequest.format(filetype);
        addAudioMetadataRequest.filename("functional_test");
        addAudioMetadataRequest.courthouse(courthouse);
        addAudioMetadataRequest.courtroom(courtroom);
        addAudioMetadataRequest.cases(List.of(casesList));
        addAudioMetadataRequest.setMediaFile("media file");
        addAudioMetadataRequest.setFileSize(123L);
        addAudioMetadataRequest.setChecksum(checksum);
        addAudioMetadataRequest.storageGuid(UUID.fromString(guid));
        return addAudioMetadataRequest;
    }
}
