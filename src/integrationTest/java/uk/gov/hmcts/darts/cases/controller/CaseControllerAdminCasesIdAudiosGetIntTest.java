package uk.gov.hmcts.darts.cases.controller;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.SuperAdminUserStub;

import java.time.OffsetDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.test.common.TestUtils.getContentsFromFile;
import static uk.gov.hmcts.darts.test.common.data.PersistableFactory.getMediaTestData;

@Slf4j
@AutoConfigureMockMvc
class CaseControllerAdminCasesIdAudiosGetIntTest extends PostgresIntegrationBase {

    private static final String ENDPOINT_URL = "/admin/cases/{id}/audios";
    private static final String COURTROOM_NAME = "COURTROOM 1";

    private CourtCaseEntity courtCaseEntity1;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private SuperAdminUserStub superAdminUserStub;
    @MockitoBean
    private UserIdentity mockUserIdentity;

    @BeforeEach
    void setupData() {
        var minimalHearing1 = PersistableFactory.getHearingTestData().someMinimalHearing();
        var hearingEntity1 = dartsDatabase.save(minimalHearing1);

        var courtroomEntity = hearingEntity1.getCourtroom();
        courtroomEntity.setName(COURTROOM_NAME);
        dartsDatabase.save(courtroomEntity);

        courtCaseEntity1 = hearingEntity1.getCourtCase();

        var media1 = dartsPersistence.save(
            getMediaTestData().createMediaWith(
                hearingEntity1.getCourtroom(),
                OffsetDateTime.parse("2023-09-26T13:00:00Z"),
                OffsetDateTime.parse("2023-09-26T13:45:00Z"),
                1
            ));
        MediaEntity currentMediaEntity1 = dartsDatabase.save(media1);

        var media2 = dartsPersistence.save(
            getMediaTestData().createMediaWith(
                hearingEntity1.getCourtroom(),
                OffsetDateTime.parse("2023-09-26T13:00:00Z"),
                OffsetDateTime.parse("2023-09-26T13:45:00Z"),
                2
            ));
        MediaEntity currentMediaEntity2 = dartsDatabase.save(media2);

        var media3 = dartsPersistence.save(
            getMediaTestData().createMediaWith(
                hearingEntity1.getCourtroom(),
                OffsetDateTime.parse("2023-09-26T13:00:00Z"),
                OffsetDateTime.parse("2023-09-26T13:45:00Z"),
                3
            ));
        MediaEntity currentMediaEntity3 = dartsDatabase.save(media3);

        var media4 = dartsPersistence.save(
            getMediaTestData().createMediaWith(
                hearingEntity1.getCourtroom(),
                OffsetDateTime.parse("2023-09-26T13:00:00Z"),
                OffsetDateTime.parse("2023-09-26T13:45:00Z"),
                4
            ));
        MediaEntity currentMediaEntity4 = dartsDatabase.save(media4);

        var media5 = dartsPersistence.save(
            getMediaTestData().createMediaWith(
                hearingEntity1.getCourtroom(),
                OffsetDateTime.parse("2023-09-26T15:00:00Z"),
                OffsetDateTime.parse("2023-09-26T17:45:00Z"),
                1
            ));
        media5.setIsCurrent(false);
        MediaEntity mediaEntityNotCurrent1 = dartsDatabase.save(media5);

        hearingEntity1.addMedia(currentMediaEntity1);
        hearingEntity1.addMedia(currentMediaEntity2);
        hearingEntity1.addMedia(currentMediaEntity3);
        hearingEntity1.addMedia(currentMediaEntity4);
        hearingEntity1.addMedia(mediaEntityNotCurrent1);
        dartsDatabase.save(hearingEntity1);

        var minimalHearing2 = PersistableFactory.getHearingTestData().someMinimalHearing();
        var hearingEntity2 = dartsDatabase.save(minimalHearing2);

        var media6 = dartsPersistence.save(
            getMediaTestData().createMediaWith(
                hearingEntity2.getCourtroom(),
                OffsetDateTime.parse("2023-09-26T15:00:00Z"),
                OffsetDateTime.parse("2023-09-26T17:45:00Z"),
                1
            ));
        MediaEntity currentMediaEntityForHearing2 = dartsDatabase.save(media6);
        hearingEntity2.addMedia(currentMediaEntityForHearing2);
        dartsDatabase.save(hearingEntity2);
    }

    @Test
    void adminCasesIdAudiosGet_ShouldReturnPaginatedListWithMultiplePages() throws Exception {
        // given
        superAdminUserStub.givenUserIsAuthorised(mockUserIdentity);
        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL, courtCaseEntity1.getId())
            .queryParam("sort_by", "audioId")
            .queryParam("sort_order", "asc")
            .queryParam("page_number", "1")
            .queryParam("page_size", "3");

        // when
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();

        // then
        String actualResponse = mvcResult.getResponse().getContentAsString();
        String expectedResponse = getContentsFromFile(
            "tests/cases/CaseControllerAdminCasesIdAudiosGetTest/testPaginationDefault/expectedResponse.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void adminCasesIdAudiosGet_ShouldReturnPaginatedListByChannelDesc() throws Exception {
        // given
        superAdminUserStub.givenUserIsAuthorised(mockUserIdentity);
        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL, courtCaseEntity1.getId())
            .queryParam("sort_by", "channel")
            .queryParam("sort_order", "desc")
            .queryParam("page_number", "1")
            .queryParam("page_size", "3");

        // when
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();

        // then
        String actualResponse = mvcResult.getResponse().getContentAsString();
        String expectedResponse = getContentsFromFile(
            "tests/cases/CaseControllerAdminCasesIdAudiosGetTest/testPaginationByChannelDesc/expectedResponse.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);

    }

    @Test
    void adminCasesIdAudiosGet_ShouldReturnPaginatedListByAudioIdDesc() throws Exception {
        // given
        superAdminUserStub.givenUserIsAuthorised(mockUserIdentity);
        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL, courtCaseEntity1.getId())
            .queryParam("sort_by", "audioId")
            .queryParam("sort_order", "desc")
            .queryParam("page_number", "1")
            .queryParam("page_size", "3");

        // when
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();

        // then
        String actualResponse = mvcResult.getResponse().getContentAsString();
        String expectedResponse = getContentsFromFile(
            "tests/cases/CaseControllerAdminCasesIdAudiosGetTest/testPaginationByAudioIdDesc/expectedResponse.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);

    }

    @Test
    void adminCasesIdAudiosGet_ShouldReturnPaginatedListByCourtroomDesc() throws Exception {
        // given
        superAdminUserStub.givenUserIsAuthorised(mockUserIdentity);
        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL, courtCaseEntity1.getId())
            .queryParam("sort_by", "courtroom")
            .queryParam("sort_order", "desc")
            .queryParam("page_number", "1")
            .queryParam("page_size", "3");

        // when
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();

        // then
        String actualResponse = mvcResult.getResponse().getContentAsString();
        String expectedResponse = getContentsFromFile(
            "tests/cases/CaseControllerAdminCasesIdAudiosGetTest/testPaginationByCourtroomDesc/expectedResponse.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);

    }

    @Test
    void adminCasesIdAudiosGet_ShouldReturnPaginatedListByStartTimeDesc() throws Exception {
        // given
        superAdminUserStub.givenUserIsAuthorised(mockUserIdentity);
        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL, courtCaseEntity1.getId())
            .queryParam("sort_by", "audioId")
            .queryParam("sort_order", "desc")
            .queryParam("page_number", "1")
            .queryParam("page_size", "3");

        // when
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();

        // then
        String actualResponse = mvcResult.getResponse().getContentAsString();
        String expectedResponse = getContentsFromFile(
            "tests/cases/CaseControllerAdminCasesIdAudiosGetTest/testPaginationByStartTimeDesc/expectedResponse.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);

    }

    @Test
    void adminCasesIdAudiosGet_ShouldReturnPaginatedListByEndTimeDesc() throws Exception {
        // given
        superAdminUserStub.givenUserIsAuthorised(mockUserIdentity);
        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL, courtCaseEntity1.getId())
            .queryParam("sort_by", "audioId")
            .queryParam("sort_order", "desc")
            .queryParam("page_number", "1")
            .queryParam("page_size", "3");

        // when
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();

        // then
        String actualResponse = mvcResult.getResponse().getContentAsString();
        String expectedResponse = getContentsFromFile(
            "tests/cases/CaseControllerAdminCasesIdAudiosGetTest/testPaginationByEndTimeDesc/expectedResponse.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);

    }

    @Test
    void adminCasesIdAudiosGet_ShouldReturnForbiddenError_WhenUserIsNotAuthenticated() throws Exception {
        // given
        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL, courtCaseEntity1.getId())
            .queryParam("sort_by", "channel")
            .queryParam("sort_order", "desc")
            .queryParam("page_number", "1")
            .queryParam("page_size", "3");

        // when
        mockMvc.perform(requestBuilder).andExpect(status().isForbidden());
    }
}
