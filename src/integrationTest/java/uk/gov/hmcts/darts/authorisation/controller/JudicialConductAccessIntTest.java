package uk.gov.hmcts.darts.authorisation.controller;

import com.azure.storage.blob.BlobAsyncClient;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.enums.MediaRequestStatus;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestDetails;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestType;
import uk.gov.hmcts.darts.common.datamanagement.component.DataManagementAzureClientFactory;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.util.DateConverterUtil;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.AnnotationStub;
import uk.gov.hmcts.darts.testutils.stubs.TransientObjectDirectoryStub;

import java.io.IOException;
import java.io.OutputStream;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.JUDICIAL_CONDUCT;

@AutoConfigureMockMvc
@ActiveProfiles("blobTest")
class JudicialConductAccessIntTest extends IntegrationBase {

    private static final OffsetDateTime HEARING_DATE_TIME = OffsetDateTime.parse("2023-01-01T12:00:00Z");
    private static final OffsetDateTime MEDIA_START_TIME = OffsetDateTime.parse("2023-01-01T12:00:00Z");
    private static final OffsetDateTime MEDIA_END_TIME = MEDIA_START_TIME.plusHours(1);
    private static final String DOWNLOADED_BLOB_CONTENTS = "downloaded audio export";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AnnotationStub annotationStub;

    @Autowired
    private TransientObjectDirectoryStub transientObjectDirectoryStub;

    @MockitoBean
    private DataManagementAzureClientFactory dataManagementAzureClientFactory;

    private HearingEntity hearing;
    private UserAccountEntity judicialConductUser;
    private MediaRequestEntity openDownloadMediaRequest;
    private Integer transformedMediaId;

    @BeforeEach
    void setUp() {
        stubOutboundBlobDownload();

        hearing = dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            "JCO-CASE-1",
            "JCO-COURTHOUSE",
            "jco-courtroom",
            DateConverterUtil.toLocalDateTime(HEARING_DATE_TIME)
        );
        dartsDatabase.createEvent(hearing);
        addMediaToHearing();

        UserAccountEntity requestor = dartsDatabase.getUserAccountStub()
            .createAuthorisedIntegrationTestUser(hearing.getCourtroom().getCourthouse());
        dartsDatabase.getTranscriptionStub().createAndSaveCompletedTranscriptionWithDocument(
            requestor,
            hearing.getCourtCase(),
            hearing,
            HEARING_DATE_TIME,
            false
        );
        annotationStub.createAndSaveAnnotationEntityWith(requestor, "JCO should not see this annotation", hearing);

        judicialConductUser = givenBuilder.anAuthenticatedUserWithGlobalAccessAndRole(JUDICIAL_CONDUCT);
        openDownloadMediaRequest = dartsDatabase.getMediaRequestStub().createAndLoadMediaRequestEntity(
            judicialConductUser,
            hearing,
            AudioRequestType.DOWNLOAD,
            MediaRequestStatus.OPEN
        );
        var completedDownloadMediaRequest = dartsDatabase.getMediaRequestStub().createAndLoadMediaRequestEntity(
            judicialConductUser,
            hearing,
            AudioRequestType.DOWNLOAD,
            MediaRequestStatus.COMPLETED
        );
        var transientObjectDirectoryEntity = transientObjectDirectoryStub.createTransientObjectDirectoryEntity(
            completedDownloadMediaRequest,
            dartsDatabase.getObjectRecordStatusEntity(STORED),
            UUID.randomUUID().toString()
        );
        transformedMediaId = transientObjectDirectoryEntity.getTransformedMedia().getId();
    }

    @Test
    void judicialConductUserCanAccessCaseFileTabsAcrossAllCourts() throws Exception {
        Integer caseId = hearing.getCourtCase().getId();

        mockMvc.perform(get("/cases/{case_id}", caseId))
            .andExpect(status().isOk());

        mockMvc.perform(get("/cases/{case_id}/hearings", caseId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()", is(1)))
            .andExpect(jsonPath("$[0].id", is(hearing.getId())));

        mockMvc.perform(get("/cases/{case_id}/events", caseId)
                            .queryParam("page_number", "1")
                            .queryParam("page_size", "25"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()", is(1)));

        mockMvc.perform(get("/cases/{case_id}/transcripts", caseId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()", is(1)));
    }

    @Test
    void judicialConductUserCanAccessHearingDetailsEventsAndAudioAcrossAllCourts() throws Exception {
        Integer hearingId = hearing.getId();

        mockMvc.perform(get("/hearings/{hearingId}", hearingId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.hearing_id", is(hearingId)));

        mockMvc.perform(get("/hearings/{hearingId}/events", hearingId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()", is(1)));

        mockMvc.perform(get("/audio/hearings/{hearing_id}/audios", hearingId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()", is(1)));
    }

    @Test
    void judicialConductUserCannotAccessAnnotations() throws Exception {
        mockMvc.perform(get("/cases/{case_id}/annotations", hearing.getCourtCase().getId()))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/hearings/{hearingId}/annotations", hearing.getId()))
            .andExpect(status().isForbidden());
    }

    @Test
    void judicialConductUserCanViewAndRequestDownloadAudioAcrossAllCourts() throws Exception {
        mockMvc.perform(get("/audio-requests/v2")
                            .header("user_id", judicialConductUser.getId())
                            .queryParam("expired", "false"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.media_request_details.length()", is(1)))
            .andExpect(jsonPath("$.media_request_details[0].request_type", is("DOWNLOAD")))
            .andExpect(jsonPath("$.transformed_media_details.length()", is(1)))
            .andExpect(jsonPath("$.transformed_media_details[0].request_type", is("DOWNLOAD")));

        mockMvc.perform(post("/audio-requests/download")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createAudioRequestDetails())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.request_id").isNumber())
            .andExpect(jsonPath("$.case_id", is(hearing.getCourtCase().getId())))
            .andExpect(jsonPath("$.case_number", is(hearing.getCourtCase().getCaseNumber())))
            .andExpect(jsonPath("$.courthouse_name", is(hearing.getCourtroom().getCourthouse().getCourthouseName())));
    }

    @Test
    void judicialConductUserCanDownloadAudioExportAcrossAllCourts() throws Exception {
        var result = mockMvc.perform(get("/audio-requests/download")
                                         .queryParam("transformed_media_id", String.valueOf(transformedMediaId)))
            .andExpect(status().isOk())
            .andReturn();

        assertEquals(DOWNLOADED_BLOB_CONTENTS, result.getResponse().getContentAsString());

        mockMvc.perform(patch("/audio-requests/transformed_media/{transformed_media_id}", transformedMediaId))
            .andExpect(status().isNoContent());
    }

    @Test
    void judicialConductUserCannotDeleteDownloadAudio() throws Exception {
        mockMvc.perform(delete("/audio-requests/{media_request_id}", openDownloadMediaRequest.getId()))
            .andExpect(status().isForbidden());

        mockMvc.perform(delete("/audio-requests/transformed_media/{transformed_media_id}", transformedMediaId))
            .andExpect(status().isForbidden());
    }

    private void addMediaToHearing() {
        var media = PersistableFactory.getMediaTestData()
            .createMediaWith(hearing.getCourtroom(), MEDIA_START_TIME, MEDIA_END_TIME, 1);
        hearing.addMedia(media);
        dartsPersistence.save(hearing);
        dartsPersistence.save(PersistableFactory.getExternalObjectDirectoryTestData()
                                  .eodStoredInUnstructuredLocationForMedia(media));
    }

    private AudioRequestDetails createAudioRequestDetails() {
        var audioRequestDetails = new AudioRequestDetails();
        audioRequestDetails.setHearingId(hearing.getId());
        audioRequestDetails.setRequestor(judicialConductUser.getId());
        audioRequestDetails.setStartTime(MEDIA_START_TIME.plusMinutes(10));
        audioRequestDetails.setEndTime(MEDIA_START_TIME.plusMinutes(20));
        return audioRequestDetails;
    }

    private void stubOutboundBlobDownload() {
        BlobServiceClient client = Mockito.mock(BlobServiceClient.class);
        BlobContainerClient containerClient = Mockito.mock(BlobContainerClient.class);
        BlobAsyncClient blobAsyncClient = Mockito.mock(BlobAsyncClient.class);
        when(blobAsyncClient.getServiceVersion()).thenReturn(Mockito.mock(BlobServiceVersion.class));
        BlobClient blobClient = new DownloadableBlobClient(blobAsyncClient);

        when(dataManagementAzureClientFactory.getBlobServiceClient(notNull())).thenReturn(client);
        when(dataManagementAzureClientFactory.getBlobContainerClient(notNull(), eq(client))).thenReturn(containerClient);
        when(dataManagementAzureClientFactory.getBlobClient(eq(containerClient), notNull())).thenReturn(blobClient);
    }

    private static final class DownloadableBlobClient extends BlobClient {
        private DownloadableBlobClient(BlobAsyncClient client) {
            super(client);
        }

        @Override
        public void downloadStream(OutputStream stream) {
            try {
                stream.write(DOWNLOADED_BLOB_CONTENTS.getBytes());
            } catch (IOException e) {
                throw new UnsupportedOperationException("Download error", e);
            }
        }

        @Override
        public Boolean exists() {
            return true;
        }
    }
}
