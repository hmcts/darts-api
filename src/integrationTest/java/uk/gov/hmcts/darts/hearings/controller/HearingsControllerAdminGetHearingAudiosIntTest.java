package uk.gov.hmcts.darts.hearings.controller;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.authorisation.exception.AuthorisationError;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.hearings.exception.HearingApiError;
import uk.gov.hmcts.darts.testutils.GivenBuilder;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.SuperAdminUserStub;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import static java.time.ZoneOffset.UTC;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@AutoConfigureMockMvc
@Slf4j
class HearingsControllerAdminGetHearingAudiosIntTest extends IntegrationBase {

    @Autowired
    private transient MockMvc mockMvc;

    public static final String ENDPOINT_URL = "/admin/hearings/{hearingId}/audios";

    @Autowired
    private SuperAdminUserStub superAdminUserStub;
    @Autowired
    private GivenBuilder givenBuilder;


    private HearingEntity setupStandardData() {
        OffsetDateTime baseTime = OffsetDateTime.of(2020, 6, 20, 10, 0, 0, 0, UTC);
        //Create media entities that should be returned
        MediaEntity mediaEntityToReturn1 = dartsDatabase.createMediaEntity("courthosue", "1",
                                                                           baseTime,
                                                                           baseTime.plusMinutes(20), 1);
        mediaEntityToReturn1.setIsCurrent(true);
        mediaEntityToReturn1.setTotalChannels(2);
        mediaEntityToReturn1.setMediaFile("a-media-file-1");
        MediaEntity mediaEntityToReturn2 = dartsDatabase.createMediaEntity("courthosue", "1",
                                                                           baseTime.plusMinutes(21),
                                                                           baseTime.plusMinutes(40), 2);
        mediaEntityToReturn2.setIsCurrent(true);
        mediaEntityToReturn2.setTotalChannels(2);
        mediaEntityToReturn2.setMediaFile("a-media-file-2");
        MediaEntity mediaEntityToReturn3 = dartsDatabase.createMediaEntity("courthosue", "1",
                                                                           baseTime.plusMinutes(21),
                                                                           baseTime.plusMinutes(25), 3);
        mediaEntityToReturn3.setIsCurrent(true);
        mediaEntityToReturn3.setTotalChannels(3);
        mediaEntityToReturn3.setMediaFile("a-media-file-3");

        //Create media entities that should not be returned
        MediaEntity mediaEntityShouldNotReturnNotCurrent = dartsDatabase.createMediaEntity("courthosue", "1",
                                                                                           baseTime,
                                                                                           baseTime.plusMinutes(20), 1);
        mediaEntityShouldNotReturnNotCurrent.setIsCurrent(false);
        MediaEntity mediaEntityShouldNotReturnNotLinkedToHearing = dartsDatabase.createMediaEntity("courthosue", "1",
                                                                                                   baseTime,
                                                                                                   baseTime.plusMinutes(20), 1);
        mediaEntityShouldNotReturnNotLinkedToHearing.setIsCurrent(true);


        dartsDatabase.saveAll(mediaEntityToReturn1, mediaEntityToReturn2, mediaEntityToReturn3, mediaEntityShouldNotReturnNotCurrent,
                              mediaEntityShouldNotReturnNotLinkedToHearing);


        HearingEntity hearing = dartsDatabase.createHearing(
            "testCourthouse",
            "testCourtroom",
            "testCaseNumber",
            LocalDateTime.of(2020, 6, 20, 10, 0, 0)
        );

        hearing.addMedia(mediaEntityToReturn1);
        hearing.addMedia(mediaEntityToReturn2);
        hearing.addMedia(mediaEntityToReturn3);
        hearing.addMedia(mediaEntityShouldNotReturnNotCurrent);
        return dartsDatabase.save(hearing);
    }

    @ParameterizedTest
    @EnumSource(value = SecurityRoleEnum.class, names = {"SUPER_ADMIN", "SUPER_USER"}, mode = EnumSource.Mode.INCLUDE)
    void adminHearingsIdAudiosGet_shouldReutrnCorrectData_whenAValidHearingIsUsed(SecurityRoleEnum role) throws Exception {
        givenBuilder.anAuthenticatedUserWithGlobalAccessAndRole(role);

        HearingEntity hearing = setupStandardData();
        MvcResult mvcResult = mockMvc.perform(
                get(ENDPOINT_URL, hearing.getId()))
            .andExpect(status().isOk())
            .andReturn();

        String expectedJson = """
            [
                {
                  "id": 1,
                  "start_at": "2020-06-20T10:00:00Z",
                  "end_at": "2020-06-20T10:20:00Z",
                  "filename": "a-media-file-1",
                  "channel": 1,
                  "total_channels": 2
                },
                {
                  "id": 2,
                  "start_at": "2020-06-20T10:21:00Z",
                  "end_at": "2020-06-20T10:40:00Z",
                  "filename": "a-media-file-2",
                  "channel": 2,
                  "total_channels": 2
                },
                {
                  "id": 3,
                  "start_at": "2020-06-20T10:21:00Z",
                  "end_at": "2020-06-20T10:25:00Z",
                  "filename": "a-media-file-3",
                  "channel": 3,
                  "total_channels": 3
                }
              ]
            """;
        String actualJson = mvcResult.getResponse().getContentAsString();
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.NON_EXTENSIBLE);
    }


    @Test
    void adminGetHearing_usingAHearingIdThatDoesNotExist_shouldReturnNotFound() throws Exception {
        givenBuilder.anAuthenticatedUserWithGlobalAccessAndRole(SecurityRoleEnum.SUPER_ADMIN);
        int hearingId = -1;
        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL, hearingId);
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(status().isNotFound()).andReturn();
        assertStandardErrorJsonResponse(mvcResult, HearingApiError.HEARING_NOT_FOUND);
    }

    @ParameterizedTest
    @EnumSource(value = SecurityRoleEnum.class, names = {"SUPER_ADMIN", "SUPER_USER"}, mode = EnumSource.Mode.EXCLUDE)
    void adminHearingsIdAudiosGet_shouldThrowError_whenUserIsNotAuthenticated(SecurityRoleEnum role) throws Exception {
        givenBuilder.anAuthenticatedUserWithGlobalAccessAndRole(role);
        HearingEntity hearing = setupStandardData();

        MvcResult mvcResult = mockMvc.perform(
                get(ENDPOINT_URL, hearing.getId()))
            .andExpect(status().isForbidden())
            .andReturn();
        assertStandardErrorJsonResponse(mvcResult, AuthorisationError.USER_NOT_AUTHORISED_FOR_ENDPOINT);
    }
}