package uk.gov.hmcts.darts.audio.controller;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import static java.util.Arrays.stream;
import static java.util.UUID.randomUUID;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.UNSTRUCTURED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;
import static uk.gov.hmcts.darts.test.common.data.CourtroomTestData.someMinimalCourtRoom;
import static uk.gov.hmcts.darts.test.common.data.ExternalLocationTypeTestData.locationTypeOf;
import static uk.gov.hmcts.darts.test.common.data.ExternalObjectDirectoryTestData.minimalExternalObjectDirectory;
import static uk.gov.hmcts.darts.test.common.data.HearingTestData.createSomeMinimalHearing;
import static uk.gov.hmcts.darts.test.common.data.MediaTestData.createMediaWith;
import static uk.gov.hmcts.darts.test.common.data.ObjectRecordStatusTestData.statusOf;

@AutoConfigureMockMvc
@SuppressWarnings("VariableDeclarationUsageDistance")
class AudioControllerGetMetadataIntTest extends IntegrationBase {

    private static final String ENDPOINT_URL = "/audio/hearings/{hearing_id}/audios";
    private static final OffsetDateTime MEDIA_START_TIME = OffsetDateTime.parse("2023-01-01T12:00:00Z");
    private static final OffsetDateTime MEDIA_END_TIME = MEDIA_START_TIME.plusHours(1);

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserIdentity mockUserIdentity;

    // Will remove this in next PR
    @Disabled
    @Test
    void old() throws Exception {
        var mediaChannel1 = dartsDatabase.createMediaEntity("testCourthouse", "testCourtroom", MEDIA_START_TIME, MEDIA_END_TIME, 1);
        var mediaChannel2 = dartsDatabase.createMediaEntity("testCourthouse", "testCourtroom", MEDIA_START_TIME, MEDIA_END_TIME, 2);
        var mediaChannel3 = dartsDatabase.createMediaEntity("testCourthouse", "testCourtroom", MEDIA_START_TIME, MEDIA_END_TIME, 3);
        var mediaChannel4 = dartsDatabase.createMediaEntity("testCourthouse", "testCourtroom", MEDIA_START_TIME, MEDIA_END_TIME, 4);
        var mediaChannel5NotCurrent = dartsDatabase.createMediaEntity("testCourthouse", "testCourtroom", MEDIA_START_TIME, MEDIA_END_TIME, 5);
        mediaChannel5NotCurrent.setIsCurrent(false);

        var hearingEntity = dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            "999",
            "test",
            "test",
            LocalDateTime.now()
        );
        hearingEntity.addMedia(mediaChannel1);
        hearingEntity.addMedia(mediaChannel2);
        hearingEntity.addMedia(mediaChannel3);
        hearingEntity.addMedia(mediaChannel4);
        hearingEntity.addMedia(mediaChannel5NotCurrent);
        dartsDatabase.save(hearingEntity);

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub()
            .createAuthorisedIntegrationTestUser(hearingEntity.getCourtroom().getCourthouse());
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);

        var requestBuilder = get(ENDPOINT_URL, hearingEntity.getId());

        MvcResult mvcResult = mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();
        String expectedJson = """
            [
              {
                "id": %d,
                "media_start_timestamp": "2023-01-01T12:00:00Z",
                "media_end_timestamp": "2023-01-01T13:00:00Z",
                "is_archived": false,
                "is_available": true
              }
            ]
            """.formatted(mediaChannel1.getId());
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.NON_EXTENSIBLE);
    }


    @Disabled
    @Test
    void getAudioMetadataGetShouldReturnMediaChannel1MetadataAssociatedWithProvidedHearing() throws Exception {
        var courtroomEntity = someMinimalCourtRoom();
        var mediaChannel1 = createMediaWith(courtroomEntity, MEDIA_START_TIME, MEDIA_END_TIME, 1);
        var mediaChannel2 = createMediaWith(courtroomEntity, MEDIA_START_TIME, MEDIA_END_TIME, 2);
        var mediaChannel3 = createMediaWith(courtroomEntity, MEDIA_START_TIME, MEDIA_END_TIME, 3);
        var mediaChannel4 = createMediaWith(courtroomEntity, MEDIA_START_TIME, MEDIA_END_TIME, 4);
        var mediaChannel5NotCurrent = createMediaWith(courtroomEntity, MEDIA_START_TIME, MEDIA_END_TIME, 5);
        mediaChannel5NotCurrent.setIsCurrent(false);

        //        dartsPersistence.save(eodStoredInUnstructuredLocationForMedia(mediaChannel1));

        var hearingEntity = dartsPersistence.save(
            hearingWithMedias(mediaChannel1, mediaChannel2, mediaChannel3, mediaChannel4, mediaChannel5NotCurrent));

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub()
            .createAuthorisedIntegrationTestUser(hearingEntity.getCourtroom().getCourthouse());
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);

        var requestBuilder = get(ENDPOINT_URL, hearingEntity.getId());

        MvcResult mvcResult = mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();
        String expectedJson = """
            [
              {
                "id": %d,
                "media_start_timestamp": "2023-01-01T12:00:00Z",
                "media_end_timestamp": "2023-01-01T13:00:00Z",
                "is_archived": false,
                "is_available": true
              }
            ]
            """.formatted(mediaChannel1.getId());
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void getAudioMetadataGetShouldReturnEmptyListWhenNoMediaIsAssociatedWithHearing() throws Exception {
        var hearingEntity = dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            "999",
            "test",
            "test",
            LocalDateTime.now()
        );

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub()
            .createAuthorisedIntegrationTestUser(hearingEntity.getCourtroom().getCourthouse());
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);

        var requestBuilder = get(ENDPOINT_URL, hearingEntity.getId());

        MvcResult mvcResult = mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();
        JSONAssert.assertEquals("[]", actualJson, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void getAudioMetadataHearingNotFound() throws Exception {
        var requestBuilder = get(ENDPOINT_URL, 999);

        mockMvc.perform(requestBuilder).andExpect(status().isNotFound());
    }

    @Test
    void getAudioMetadataGetShouldNotReturnHiddenMediaChannel1() throws Exception {
        var courtroomEntity = someMinimalCourtRoom();
        var mediaChannel1 = createMediaWith(courtroomEntity, MEDIA_START_TIME, MEDIA_END_TIME, 1);
        var mediaChannel2 = createMediaWith(courtroomEntity, MEDIA_START_TIME, MEDIA_END_TIME, 2);
        var mediaChannel3 = createMediaWith(courtroomEntity, MEDIA_START_TIME, MEDIA_END_TIME, 3);
        var mediaChannel4 = createMediaWith(courtroomEntity, MEDIA_START_TIME, MEDIA_END_TIME, 4);

        dartsPersistence.save(eodStoredInUnstructuredLocationForMedia(mediaChannel1));

        var hearingEntity = dartsPersistence.save(hearingWithMedias(mediaChannel1, mediaChannel2, mediaChannel3, mediaChannel4));

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub()
            .createAuthorisedIntegrationTestUser(hearingEntity.getCourtroom().getCourthouse());
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);

        var requestBuilder = get(ENDPOINT_URL, hearingEntity.getId());

        MvcResult mvcResult = mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();
        JSONAssert.assertEquals("[]", actualJson, JSONCompareMode.NON_EXTENSIBLE);
    }

    private HearingEntity hearingWithMedias(MediaEntity... mediaEntities) {
        var hearingEntity = createSomeMinimalHearing();
        stream(mediaEntities).forEach(hearingEntity::addMedia);
        return hearingEntity;
    }

    private ExternalObjectDirectoryEntity eodStoredInUnstructuredLocationForMedia(MediaEntity media) {
        var eod = minimalExternalObjectDirectory();
        eod.setMedia(media);
        eod.setStatus(statusOf(STORED));
        eod.setExternalLocationType(locationTypeOf(UNSTRUCTURED));
        eod.setExternalLocation(randomUUID());
        return eod;
    }

}
