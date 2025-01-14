package uk.gov.hmcts.darts.audio.controller;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.darts.audio.enums.MediaRequestStatus;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestType;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.test.common.TestUtils;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.MediaRequestStub;
import uk.gov.hmcts.darts.testutils.stubs.TransformedMediaStub;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@TestInstance(Lifecycle.PER_CLASS)
class AudioRequestsControllerGetYourAudioV2IntTest extends IntegrationBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountRepository userAccountRepository;

    private static final List<String> TAGS_TO_IGNORE = List.of("media_request_id", "transformed_media_id", "case_id", "hearing_id");

    private void setupTestData() {
        MediaRequestStub mediaRequestStub = dartsDatabase.getMediaRequestStub();

        var thisOwner = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        var differentOwner = dartsDatabase.getUserAccountStub().getSystemUserAccountEntity();

        TransformedMediaStub transformedMediaStub = dartsDatabase.getTransformedMediaStub();

        {
            //create some OPEN media requests
            var openMediaRequest = mediaRequestStub.createAndLoadMediaRequestEntity(thisOwner, AudioRequestType.DOWNLOAD, MediaRequestStatus.OPEN);
            var someoneElsesOpenMediaRequest = mediaRequestStub.createAndLoadMediaRequestEntity(
                differentOwner,
                AudioRequestType.DOWNLOAD,
                MediaRequestStatus.OPEN
            );

            transformedMediaStub.createTransformedMediaEntity(openMediaRequest);
            transformedMediaStub.createTransformedMediaEntity(someoneElsesOpenMediaRequest);
        }

        {
            //create some COMPLETED media requests
            var completedMediaRequest = mediaRequestStub.createAndLoadMediaRequestEntity(thisOwner, AudioRequestType.DOWNLOAD, MediaRequestStatus.COMPLETED);
            var someoneElsesCompletedMediaRequest = mediaRequestStub.createAndLoadMediaRequestEntity(
                differentOwner,
                AudioRequestType.DOWNLOAD,
                MediaRequestStatus.COMPLETED
            );

            OffsetDateTime time = OffsetDateTime.of(3020, 6, 20, 15, 30, 0, 0, ZoneOffset.UTC);
            transformedMediaStub.createTransformedMediaEntity(completedMediaRequest, "file1", null, time);
            transformedMediaStub.createTransformedMediaEntity(completedMediaRequest, "file2", time, time);
            transformedMediaStub.createTransformedMediaEntity(someoneElsesCompletedMediaRequest, "file20", time, time);
        }

        {
            //create some EXPIRED transformed_media with EXPIRED media requests
            var expiredMediaRequest = mediaRequestStub.createAndLoadMediaRequestEntity(thisOwner, AudioRequestType.DOWNLOAD, MediaRequestStatus.EXPIRED);
            var someoneElsesExpiredMediaRequest = mediaRequestStub.createAndLoadMediaRequestEntity(
                differentOwner,
                AudioRequestType.DOWNLOAD,
                MediaRequestStatus.EXPIRED
            );

            OffsetDateTime time = OffsetDateTime.of(2020, 6, 20, 15, 30, 0, 0, ZoneOffset.UTC);
            transformedMediaStub.createTransformedMediaEntity(expiredMediaRequest, "file11", time, time);
            transformedMediaStub.createTransformedMediaEntity(expiredMediaRequest, "file12", time, time);
            transformedMediaStub.createTransformedMediaEntity(someoneElsesExpiredMediaRequest, "file120", time, time);
        }

        {
            //create some EXPIRED transformed_media with COMPLETED media requests
            var expiredMediaRequest = mediaRequestStub.createAndLoadMediaRequestEntity(thisOwner, AudioRequestType.DOWNLOAD, MediaRequestStatus.COMPLETED);
            var someoneElsesExpiredMediaRequest = mediaRequestStub.createAndLoadMediaRequestEntity(
                differentOwner,
                AudioRequestType.DOWNLOAD,
                MediaRequestStatus.EXPIRED
            );

            OffsetDateTime time = OffsetDateTime.of(2020, 6, 20, 15, 30, 0, 0, ZoneOffset.UTC);
            transformedMediaStub.createTransformedMediaEntity(expiredMediaRequest, "file11", time, time);
            transformedMediaStub.createTransformedMediaEntity(expiredMediaRequest, "file12", time, time);
            transformedMediaStub.createTransformedMediaEntity(someoneElsesExpiredMediaRequest, "file120", time, time);
        }

        {
            //create some DELETED media requests
            mediaRequestStub.createAndLoadMediaRequestEntity(thisOwner, AudioRequestType.DOWNLOAD, MediaRequestStatus.DELETED);
            mediaRequestStub.createAndLoadMediaRequestEntity(
                differentOwner,
                AudioRequestType.DOWNLOAD,
                MediaRequestStatus.DELETED
            );

        }
    }

    @Test
    void getYourAudioCurrent() throws Exception {
        setupTestData();
        var thisOwner = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();

        var requestBuilder = get(URI.create(String.format("/audio-requests/v2?expired=%s", FALSE)))
            .header(
                "user_id",
                thisOwner.getId()
            );

        MvcResult mvcResult = mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();
        String expectedJson = """
            {
              "media_request_details": [
                {
                  "media_request_id": 1,
                  "case_id": 1,
                  "hearing_id": 1,
                  "request_type": "DOWNLOAD",
                  "case_number": "2",
                  "courthouse_name": "NEWCASTLE",
                  "hearing_date": "2023-06-10",
                  "start_ts": "2023-06-26T13:00:00Z",
                  "end_ts": "2023-06-26T13:45:00Z",
                  "media_request_status": "OPEN"
                }
              ],
              "transformed_media_details": [
                {
                  "media_request_id": 3,
                  "transformed_media_id": 4,
                  "case_id": 1,
                  "hearing_id": 1,
                  "request_type": "DOWNLOAD",
                  "case_number": "2",
                  "courthouse_name": "NEWCASTLE",
                  "hearing_date": "2023-06-10",
                  "start_ts": "2023-06-26T13:00:00Z",
                  "end_ts": "2023-06-26T13:45:00Z",
                  "transformed_media_expiry_ts": "3020-06-20T15:30:00Z",
                  "media_request_status": "COMPLETED",
                  "transformed_media_filename": "file2",
                  "transformed_media_format": "ZIP",
                  "last_accessed_ts": "3020-06-20T15:30:00Z"
                },
                {
                  "media_request_id": 3,
                  "transformed_media_id": 3,
                  "case_id": 1,
                  "hearing_id": 1,
                  "request_type": "DOWNLOAD",
                  "case_number": "2",
                  "courthouse_name": "NEWCASTLE",
                  "hearing_date": "2023-06-10",
                  "start_ts": "2023-06-26T13:00:00Z",
                  "end_ts": "2023-06-26T13:45:00Z",
                  "media_request_status": "COMPLETED",
                  "transformed_media_filename": "file1",
                  "transformed_media_format": "ZIP",
                  "last_accessed_ts": "3020-06-20T15:30:00Z"
                }
              ]
            }
            """;
        TestUtils.compareJson(expectedJson, actualJson, TAGS_TO_IGNORE, JSONCompareMode.STRICT);
    }

    @Test
    void getYourAudioExpired() throws Exception {
        setupTestData();
        var thisOwner = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();

        var requestBuilder = get(URI.create(String.format("/audio-requests/v2?expired=%s", TRUE)))
            .header(
                "user_id",
                thisOwner.getId()
            );

        MvcResult mvcResult = mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();
        String expectedJson = """
            {
              "transformed_media_details": [
                {
                  "media_request_id": 7,
                  "transformed_media_id": 10,
                  "case_id": 1,
                  "hearing_id": 1,
                  "request_type": "DOWNLOAD",
                  "case_number": "2",
                  "courthouse_name": "NEWCASTLE",
                  "hearing_date": "2023-06-10",
                  "start_ts": "2023-06-26T13:00:00Z",
                  "end_ts": "2023-06-26T13:45:00Z",
                  "transformed_media_expiry_ts": "2020-06-20T15:30:00Z",
                  "media_request_status": "COMPLETED",
                  "transformed_media_filename": "file12",
                  "transformed_media_format": "ZIP",
                  "last_accessed_ts": "2020-06-20T15:30:00Z"
                },
                {
                  "media_request_id": 7,
                  "transformed_media_id": 9,
                  "case_id": 1,
                  "hearing_id": 1,
                  "request_type": "DOWNLOAD",
                  "case_number": "2",
                  "courthouse_name": "NEWCASTLE",
                  "hearing_date": "2023-06-10",
                  "start_ts": "2023-06-26T13:00:00Z",
                  "end_ts": "2023-06-26T13:45:00Z",
                  "transformed_media_expiry_ts": "2020-06-20T15:30:00Z",
                  "media_request_status": "COMPLETED",
                  "transformed_media_filename": "file11",
                  "transformed_media_format": "ZIP",
                  "last_accessed_ts": "2020-06-20T15:30:00Z"
                },
                {
                  "media_request_id": 5,
                  "transformed_media_id": 7,
                  "case_id": 1,
                  "hearing_id": 1,
                  "request_type": "DOWNLOAD",
                  "case_number": "2",
                  "courthouse_name": "NEWCASTLE",
                  "hearing_date": "2023-06-10",
                  "start_ts": "2023-06-26T13:00:00Z",
                  "end_ts": "2023-06-26T13:45:00Z",
                  "transformed_media_expiry_ts": "2020-06-20T15:30:00Z",
                  "media_request_status": "EXPIRED",
                  "transformed_media_filename": "file12",
                  "transformed_media_format": "ZIP",
                  "last_accessed_ts": "2020-06-20T15:30:00Z"
                },
                {
                  "media_request_id": 5,
                  "transformed_media_id": 6,
                  "case_id": 1,
                  "hearing_id": 1,
                  "request_type": "DOWNLOAD",
                  "case_number": "2",
                  "courthouse_name": "NEWCASTLE",
                  "hearing_date": "2023-06-10",
                  "start_ts": "2023-06-26T13:00:00Z",
                  "end_ts": "2023-06-26T13:45:00Z",
                  "transformed_media_expiry_ts": "2020-06-20T15:30:00Z",
                  "media_request_status": "EXPIRED",
                  "transformed_media_filename": "file11",
                  "transformed_media_format": "ZIP",
                  "last_accessed_ts": "2020-06-20T15:30:00Z"
                }
              ]
            }
            """;
        TestUtils.compareJson(expectedJson, actualJson, TAGS_TO_IGNORE, JSONCompareMode.STRICT);
    }

    @Test
    void getYourAudioCurrentShouldReturnEmptyArrayInResponseBodyWhenNoCurrentMediaRequestExists() throws Exception {

        MediaRequestStub mediaRequestStub = dartsDatabase.getMediaRequestStub();

        var differentOwner = dartsDatabase.getUserAccountStub().getSystemUserAccountEntity();

        TransformedMediaStub transformedMediaStub = dartsDatabase.getTransformedMediaStub();

        {
            //create some OPEN media requests
            var someoneElsesOpenMediaRequest = mediaRequestStub.createAndLoadMediaRequestEntity(
                differentOwner,
                AudioRequestType.DOWNLOAD,
                MediaRequestStatus.OPEN
            );

            transformedMediaStub.createTransformedMediaEntity(someoneElsesOpenMediaRequest);
        }

        {
            //create some COMPLETED media requests
            var someoneElsesCompletedMediaRequest = mediaRequestStub.createAndLoadMediaRequestEntity(
                differentOwner,
                AudioRequestType.DOWNLOAD,
                MediaRequestStatus.COMPLETED
            );

            OffsetDateTime futureDate = OffsetDateTime.of(3020, 6, 20, 15, 30, 0, 0, ZoneOffset.UTC);
            transformedMediaStub.createTransformedMediaEntity(someoneElsesCompletedMediaRequest, "file20", futureDate, futureDate);
        }

        {
            //create some EXPIRED media requests
            var someoneElsesExpiredMediaRequest = mediaRequestStub.createAndLoadMediaRequestEntity(
                differentOwner,
                AudioRequestType.DOWNLOAD,
                MediaRequestStatus.EXPIRED
            );

            OffsetDateTime time = OffsetDateTime.of(2020, 6, 20, 15, 30, 0, 0, ZoneOffset.UTC);
            transformedMediaStub.createTransformedMediaEntity(someoneElsesExpiredMediaRequest, "file120", time, time);
        }

        {
            //create some EXPIRED transformed_media with COMPLETED media requests
            var someoneElsesExpiredMediaRequest = mediaRequestStub.createAndLoadMediaRequestEntity(
                differentOwner,
                AudioRequestType.DOWNLOAD,
                MediaRequestStatus.EXPIRED
            );

            OffsetDateTime time = OffsetDateTime.of(2020, 6, 20, 15, 30, 0, 0, ZoneOffset.UTC);
            transformedMediaStub.createTransformedMediaEntity(someoneElsesExpiredMediaRequest, "file120", time, time);
        }

        {
            //create some DELETED media requests
            mediaRequestStub.createAndLoadMediaRequestEntity(
                differentOwner,
                AudioRequestType.DOWNLOAD,
                MediaRequestStatus.DELETED
            );

        }

        var thisOwner = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();

        var requestBuilder = get(URI.create(String.format("/audio-requests/v2?expired=%s", FALSE)))
            .header(
                "user_id",
                thisOwner.getId()
            );

        MvcResult mvcResult = mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();
        String expectedJson = """
            {"media_request_details":[],"transformed_media_details":[]}
            """;
        TestUtils.compareJson(expectedJson, actualJson, TAGS_TO_IGNORE);
    }

    @Test
    void getYourAudioShouldReturnEmptyArrayInResponseBodyWhenNoUserAccountExists() throws Exception {

        var requestBuilder = get(URI.create(String.format("/audio-requests/v2?expired=%s", FALSE)))
            .header(
                "user_id",
                999
            );

        MvcResult mvcResult = mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();
        String expectedJson = """
            {"media_request_details":[],"transformed_media_details":[]}
            """;
        TestUtils.compareJson(expectedJson, actualJson, TAGS_TO_IGNORE);
    }

    @Test
    void getYourAudioCurrentInactive() throws Exception {
        setupTestData();
        var thisOwner = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        UserAccountEntity userAccountEntity = userAccountRepository.findById(thisOwner.getId()).get();
        userAccountEntity.setActive(false);
        userAccountRepository.save(userAccountEntity);

        var requestBuilder = get(URI.create(String.format("/audio-requests/v2?expired=%s", FALSE)))
            .header(
                "user_id",
                thisOwner.getId()
            );

        MvcResult mvcResult = mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();
        Assertions.assertEquals("{\"media_request_details\":[],\"transformed_media_details\":[]}", actualJson);
    }
}