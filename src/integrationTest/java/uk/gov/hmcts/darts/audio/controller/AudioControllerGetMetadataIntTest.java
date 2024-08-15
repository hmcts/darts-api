package uk.gov.hmcts.darts.audio.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.TransactionalUtil;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    @BeforeEach
    void openSessionInView() {
        openInViewUtil.openEntityManager();
    }

    @AfterEach
    void closeSessionInView() {
        openInViewUtil.closeEntityManager();
    }

    @Test
    void getAudioMetadataGetShouldReturnMediaChannel1MetadataAssociatedWithProvidedHearing() throws Exception {
        var mediaChannel1 = dartsDatabase.createMediaEntity("testCourthouse", "testCourtroom", MEDIA_START_TIME, MEDIA_END_TIME, 1);
        var mediaChannel2 = dartsDatabase.createMediaEntity("testCourthouse", "testCourtroom", MEDIA_START_TIME, MEDIA_END_TIME, 2);
        var mediaChannel3 = dartsDatabase.createMediaEntity("testCourthouse", "testCourtroom", MEDIA_START_TIME, MEDIA_END_TIME, 3);
        var mediaChannel4 = dartsDatabase.createMediaEntity("testCourthouse", "testCourtroom", MEDIA_START_TIME, MEDIA_END_TIME, 4);
        var mediaChannel5NotCurrent = dartsDatabase.createMediaEntity("testCourthouse", "testCourtroom", MEDIA_START_TIME, MEDIA_END_TIME, 5);
        mediaChannel5NotCurrent.setIsCurrent(false);
        dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(mediaChannel1,
                                                                                     ObjectRecordStatusEnum.STORED,
                                                                                     ExternalLocationTypeEnum.UNSTRUCTURED,
                                                                                     UUID.randomUUID());

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
        var mediaChannel1 = dartsDatabase.createHiddenMediaEntity("testCourthouse", "testCourtroom", MEDIA_START_TIME, MEDIA_END_TIME, 1);
        var mediaChannel2 = dartsDatabase.createMediaEntity("testCourthouse", "testCourtroom", MEDIA_START_TIME, MEDIA_END_TIME, 2);
        var mediaChannel3 = dartsDatabase.createMediaEntity("testCourthouse", "testCourtroom", MEDIA_START_TIME, MEDIA_END_TIME, 3);
        var mediaChannel4 = dartsDatabase.createMediaEntity("testCourthouse", "testCourtroom", MEDIA_START_TIME, MEDIA_END_TIME, 4);
        dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(mediaChannel1,
                                                                                     ObjectRecordStatusEnum.STORED,
                                                                                     ExternalLocationTypeEnum.UNSTRUCTURED,
                                                                                     UUID.randomUUID());

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
        dartsDatabase.save(hearingEntity);

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

}
