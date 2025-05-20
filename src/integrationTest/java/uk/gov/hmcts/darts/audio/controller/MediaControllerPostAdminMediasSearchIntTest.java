package uk.gov.hmcts.darts.audio.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.darts.audio.model.PostAdminMediasSearchRequest;
import uk.gov.hmcts.darts.audio.model.PostAdminMediasSearchResponseItem;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.test.common.TestUtils;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.MediaStub;
import uk.gov.hmcts.darts.testutils.stubs.SuperAdminUserStub;

import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.test.common.TestUtils.getContentsFromFile;

@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "darts.audio.admin-search.max-results=5"
})
@Slf4j
class MediaControllerPostAdminMediasSearchIntTest extends IntegrationBase {

    private static final String ENDPOINT_URL = "/admin/medias/search";

    @Autowired
    private SuperAdminUserStub superAdminUserStub;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserIdentity userIdentity;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MediaStub mediaStub;

    @Autowired
    private MediaRepository mediaRepository;

    private CourthouseEntity courthouse1;
    private CourthouseEntity courthouse2;

    private MediaEntity mediaEntity1a;
    private MediaEntity mediaEntity1b;
    private MediaEntity mediaEntity1c;
    private MediaEntity mediaEntity1d;
    private MediaEntity mediaEntity1e;
    private MediaEntity mediaEntity1f;
    private MediaEntity mediaEntity1g;
    private MediaEntity mediaEntity2a;
    private MediaEntity mediaEntity2b;
    private MediaEntity mediaEntity2c;
    private MediaEntity mediaEntity2d;
    private MediaEntity mediaEntity2e;
    private MediaEntity mediaEntity2f;
    private MediaEntity mediaEntity3a;
    private MediaEntity mediaEntity3b;
    private MediaEntity mediaEntity3c;
    private MediaEntity mediaEntity3d;
    private MediaEntity mediaEntity3e;
    private MediaEntity mediaEntity3f;
    private MediaEntity mediaEntity4a;
    private MediaEntity mediaEntity4b;
    private MediaEntity mediaEntity4c;
    private MediaEntity mediaEntity4d;
    private MediaEntity mediaEntity4e;
    private MediaEntity mediaEntity4f;

    private MediaEntity mediaEntity4g;
    private MediaEntity mediaEntity4h;
    private MediaEntity mediaEntity4i;

    private static final List<String> TAGS_TO_IGNORE = List.of("id");

    @BeforeEach
    void setupOpenInView() {
        openInViewUtil.openEntityManager();
    }

    @AfterEach
    void closeOpenInView() {
        openInViewUtil.closeEntityManager();
    }

    @BeforeEach
    @SuppressWarnings({"PMD.NcssCount"})
    void setupDate() {
        OffsetDateTime tenth10am = OffsetDateTime.of(2020, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime startTime = tenth10am.plusSeconds(1);

        courthouse1 = dartsDatabase.createCourthouseUnlessExists("Courthouse1");
        courthouse2 = dartsDatabase.createCourthouseUnlessExists("Courthouse2");
        mediaEntity1a = createMedia(courthouse1, "courtroom1", startTime, "caseNumber1");
        mediaEntity1b = createMedia(courthouse1, "courtroom1", startTime.plusSeconds(1), "caseNumber1");
        mediaEntity1c = createMedia(courthouse1, "courtroom1", startTime.plusSeconds(2), "caseNumber1");
        mediaEntity1g = createMedia(courthouse1, "courtroom1", startTime.plusSeconds(3), "caseNumber1", false);

        startTime = tenth10am.plusHours(1).plusSeconds(1);
        mediaEntity2a = createMedia(courthouse1, "courtroom1", startTime, "caseNumber2");
        mediaEntity2b = createMedia(courthouse1, "courtroom1", startTime.plusSeconds(1), "caseNumber2");
        mediaEntity2c = createMedia(courthouse1, "courtroom1", startTime.plusSeconds(2), "caseNumber2");

        startTime = tenth10am.plusHours(2).plusSeconds(1);
        mediaEntity3a = createMedia(courthouse1, "courtroom1", startTime, "caseNumber3");
        mediaEntity3b = createMedia(courthouse1, "courtroom1", startTime.plusSeconds(1), "caseNumber3");
        mediaEntity3c = createMedia(courthouse1, "courtroom1", startTime.plusSeconds(2), "caseNumber3");


        startTime = tenth10am.plusSeconds(1);
        mediaEntity4a = createMedia(courthouse1, "courtroom2", startTime, "caseNumber4");
        mediaEntity4b = createMedia(courthouse1, "courtroom2", startTime.plusSeconds(1), "caseNumber4");
        mediaEntity4c = createMedia(courthouse1, "courtroom2", startTime.plusSeconds(2), "caseNumber4");


        startTime = tenth10am.plusHours(1).plusSeconds(1);
        mediaEntity4d = createMedia(courthouse1, "courtroom2", startTime, "caseNumber4");
        mediaEntity4e = createMedia(courthouse1, "courtroom2", startTime.plusSeconds(1), "caseNumber4");
        mediaEntity4f = createMedia(courthouse1, "courtroom2", startTime.plusSeconds(2), "caseNumber4");


        OffsetDateTime eleventh10am = OffsetDateTime.of(2020, 10, 11, 10, 0, 0, 0, ZoneOffset.UTC);
        startTime = eleventh10am.plusHours(2).plusSeconds(1);
        mediaEntity4g = createMedia(courthouse1, "courtroom2", startTime, "caseNumber4");
        mediaEntity4h = createMedia(courthouse1, "courtroom2", startTime.plusSeconds(1), "caseNumber4");
        mediaEntity4i = createMedia(courthouse1, "courtroom2", startTime.plusSeconds(2), "caseNumber4");


        startTime = eleventh10am.plusSeconds(1);
        mediaEntity1d = createMedia(courthouse2, "courtroom2", startTime, "caseNumber1");
        mediaEntity1e = createMedia(courthouse2, "courtroom2", startTime.plusSeconds(1), "caseNumber1");
        mediaEntity1f = createMedia(courthouse2, "courtroom2", startTime.plusSeconds(2), "caseNumber1");

        OffsetDateTime twelfth10am = OffsetDateTime.of(2020, 10, 12, 10, 0, 0, 0, ZoneOffset.UTC);
        startTime = twelfth10am.plusHours(1).plusSeconds(1);
        mediaEntity2d = createMedia(courthouse2, "courtroom2", startTime, "caseNumber2");
        mediaEntity2d.setChannel(1);
        mediaEntity2e = createMedia(courthouse2, "courtroom2", startTime.plusSeconds(1), "caseNumber2");
        mediaEntity2e.setChannel(2);
        mediaEntity2f = createMedia(courthouse2, "courtroom2", startTime.plusSeconds(2), "caseNumber2");
        mediaEntity2f.setChannel(3);
        dartsDatabase.saveAll(mediaEntity2d, mediaEntity2e, mediaEntity2f);
        startTime = twelfth10am.plusHours(2).plusSeconds(1);
        mediaEntity3d = createMedia(courthouse2, "courtroom2", startTime, "caseNumber3");
        mediaEntity3e = createMedia(courthouse2, "courtroom2", startTime.plusSeconds(1), "caseNumber3");
        mediaEntity3f = createMedia(courthouse2, "courtroom2", startTime.plusSeconds(2), "caseNumber3");

    }

    @Test
    void adminSearchForMedia_ShouldOnlyReturnCurrentMedia_whenMediaMatchesSearchCriteria() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        PostAdminMediasSearchRequest request = new PostAdminMediasSearchRequest();
        request.setCourthouseIds(List.of(courthouse1.getId(), courthouse2.getId()));
        request.setCaseNumber("caseNumber1");
        request.setCourtroomName("courtroom1");

        // run the test
        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT_URL)
                                                  .header("Content-Type", "application/json")
                                                  .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        List<MediaEntity> expectedEntities = List.of(mediaEntity1b, mediaEntity1a, mediaEntity1c);
        assertResponseItems(expectedEntities, mvcResult);

        String actualResponse = mvcResult.getResponse().getContentAsString();
        String expectedResponse = """
            [
               {
                 "id": 3,
                 "courthouse": {
                   "id": 1,
                   "display_name": "COURTHOUSE1"
                 },
                 "courtroom": {
                   "id": 1,
                   "name": "COURTROOM1"
                 },
                 "start_at": "2020-10-10T10:00:03Z",
                 "end_at": "2020-10-10T11:00:03Z",
                 "channel": 1,
                 "is_hidden": false
               },
               {
                 "id": 2,
                 "courthouse": {
                   "id": 1,
                   "display_name": "COURTHOUSE1"
                 },
                 "courtroom": {
                   "id": 1,
                   "name": "COURTROOM1"
                 },
                 "start_at": "2020-10-10T10:00:02Z",
                 "end_at": "2020-10-10T11:00:02Z",
                 "channel": 1,
                 "is_hidden": false
               },
               {
                 "id": 1,
                 "courthouse": {
                   "id": 1,
                   "display_name": "COURTHOUSE1"
                 },
                 "courtroom": {
                   "id": 1,
                   "name": "COURTROOM1"
                 },
                 "start_at": "2020-10-10T10:00:01Z",
                 "end_at": "2020-10-10T11:00:01Z",
                 "channel": 1,
                 "is_hidden": false
               }
             ]""";
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void adminSearchForMedia_ShouldReturnResults_WhenDatesAreStartAndEndHearingDatesExactlyOneYearOverLeapYear() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        PostAdminMediasSearchRequest request = new PostAdminMediasSearchRequest();
        request.setCourthouseIds(List.of(courthouse2.getId()));
        request.setCaseNumber("caseNumber3");
        request.setHearingStartAt(LocalDate.of(2020, 01, 11));
        request.setHearingEndAt(LocalDate.of(2021, 01, 11));

        // run the test
        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT_URL)
                                                  .header("Content-Type", "application/json")
                                                  .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn();

        String actualResponse = TestUtils.removeTags(TAGS_TO_IGNORE, mvcResult.getResponse().getContentAsString());
        log.info("actualResponse: {}", actualResponse);
        String expectedResponse = TestUtils.removeTags(TAGS_TO_IGNORE, getContentsFromFile(
            "tests/media/MediaControllerPostAdminMediasSearchIntTest/expectedResultOneYearLeapYear.json"));
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void adminSearchForMedia_ShouldThrowException_WhenDatesAreStartAndEndHearingDatesGreaterThanOneYear() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        PostAdminMediasSearchRequest request = new PostAdminMediasSearchRequest();
        request.setCourthouseIds(List.of(courthouse1.getId(), courthouse2.getId()));
        request.setCaseNumber("caseNumber1");
        request.setHearingStartAt(LocalDate.of(2020, 01, 11));
        request.setHearingEndAt(LocalDate.of(2021, 01, 12));

        // run the test
        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT_URL)
                                                  .header("Content-Type", "application/json")
                                                  .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().is4xxClientError())
            .andReturn();

        String actualResponse = mvcResult.getResponse().getContentAsString();
        String expectedResponse = """
            {
               "type": "COMMON_104",
               "title": "Invalid request",
               "status": 422,
               "detail": "The time between the start and end date cannot be more than 12 months"
             }
            """;
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void adminSearchForMedia_ShouldThrowSearchCriteriaTooBroad_withOnlyHearingStart() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        PostAdminMediasSearchRequest request = new PostAdminMediasSearchRequest();
        request.setHearingStartAt(LocalDate.of(2020, 10, 11));

        // run the test
        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT_URL)
                                                  .header("Content-Type", "application/json")
                                                  .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().is4xxClientError())
            .andReturn();

        String actualResponse = TestUtils.removeTags(TAGS_TO_IGNORE, mvcResult.getResponse().getContentAsString());
        String expectedResponse = """
            {
              "type": "COMMON_105",
              "title": "The search criteria is too broad",
              "status": 422
            }""";

        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void adminSearchForMedia_ShouldThrowSearchCriteriaTooBroad_WithOnlyHearingEnd() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        PostAdminMediasSearchRequest request = new PostAdminMediasSearchRequest();
        request.setHearingEndAt(LocalDate.of(2020, 10, 10));

        // run the test
        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT_URL)
                                                  .header("Content-Type", "application/json")
                                                  .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().is4xxClientError())
            .andReturn();

        String actualResponse = TestUtils.removeTags(TAGS_TO_IGNORE, mvcResult.getResponse().getContentAsString());
        String expectedResponse = """
                        {
              "type": "COMMON_105",
              "title": "The search criteria is too broad",
              "status": 422
            }""";

        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void adminSearchForMedia_ShouldReturnMultipleMedias_WithCaseNumberAndCourtroonAndHearingStartAndEnd() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        PostAdminMediasSearchRequest request = new PostAdminMediasSearchRequest();
        request.setCaseNumber("1");
        request.setCourtroomName("1");
        request.setHearingStartAt(LocalDate.of(2020, 10, 10));
        request.setHearingEndAt(LocalDate.of(2020, 10, 11));

        // run the test
        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT_URL)
                                                  .header("Content-Type", "application/json")
                                                  .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        List<MediaEntity> expectedEntities = List.of(mediaEntity1b, mediaEntity1a, mediaEntity1c);
        assertResponseItems(expectedEntities, mvcResult);
    }

    @Test
    void adminSearchForMedia_ShouldReturnNoMatches_WhenNoDataFound() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        PostAdminMediasSearchRequest request = new PostAdminMediasSearchRequest();
        request.setCaseNumber("10");
        request.setCourtroomName("1");
        request.setHearingStartAt(LocalDate.of(2020, 10, 10));
        request.setHearingEndAt(LocalDate.of(2020, 10, 11));

        // run the test
        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT_URL)
                                                  .header("Content-Type", "application/json")
                                                  .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        String actualResponse = TestUtils.removeTags(TAGS_TO_IGNORE, mvcResult.getResponse().getContentAsString());
        String expectedResponse = """
            []
            """;

        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);

    }

    @Test
    void adminSearchForMedia_ShouldReturnEmptyCourthouses() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        PostAdminMediasSearchRequest request = new PostAdminMediasSearchRequest();
        request.setCaseNumber("1");
        request.setCourtroomName("1");
        request.setCourthouseIds(new ArrayList<>());

        // run the test
        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT_URL)
                                                  .header("Content-Type", "application/json")
                                                  .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().is2xxSuccessful())
            .andReturn();


        List<MediaEntity> expectedEntities = List.of(mediaEntity1b, mediaEntity1a, mediaEntity1c);
        assertResponseItems(expectedEntities, mvcResult);

    }

    @Test
    void adminSearchForMedia_ShouldReturnTooManyResultsException() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        PostAdminMediasSearchRequest request = new PostAdminMediasSearchRequest();
        request.setCaseNumber("caseNumber1");

        // run the test
        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT_URL)
                                                  .header("Content-Type", "application/json")
                                                  .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().is4xxClientError())
            .andReturn();

        String actualResponse = TestUtils.removeTags(TAGS_TO_IGNORE, mvcResult.getResponse().getContentAsString());
        String expectedResponse = """
            {
              "type": "AUDIO_116",
              "title": "Too many results",
              "status": 422
            }""";

        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);

    }

    @Test
    void adminSearchForMedia_ShouldReturnNoPermissionsException() throws Exception {
        PostAdminMediasSearchRequest request = new PostAdminMediasSearchRequest();
        request.setCourthouseIds(new ArrayList<>());

        // run the test
        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT_URL)
                                                  .header("Content-Type", "application/json")
                                                  .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden())
            .andReturn();

        String actualResponse = TestUtils.removeTags(TAGS_TO_IGNORE, mvcResult.getResponse().getContentAsString());
        String expectedResponse = """
            {
              "type": "AUTHORISATION_109",
              "title": "User is not authorised for this endpoint",
              "status": 403
            }""";

        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);

    }

    @Test
    void adminSearchForMedia_ShouldOnlyReturnCurrentMedia_whenMediaMatchesCaseNumberAndCourtroom() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        PostAdminMediasSearchRequest request = new PostAdminMediasSearchRequest();
        request.setCaseNumber("caseNumber2");
        request.setCourtroomName("courtroom2");

        // run the test
        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT_URL)
                                                  .header("Content-Type", "application/json")
                                                  .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        List<MediaEntity> expectedEntities = List.of(mediaEntity2d, mediaEntity2e, mediaEntity2f);
        assertResponseItems(expectedEntities, mvcResult);

        String actualResponse = mvcResult.getResponse().getContentAsString();
        String expectedResponse = """
            [
                  {
                    "id": 23,
                    "courthouse": {
                      "id": 2,
                      "display_name": "COURTHOUSE2"
                    },
                    "courtroom": {
                      "id": 3,
                      "name": "COURTROOM2"
                    },
                    "start_at": "2020-10-12T11:00:01Z",
                    "end_at": "2020-10-12T12:00:01Z",
                    "channel": 1,
                    "is_hidden": false
                  },
                  {
                    "id": 24,
                    "courthouse": {
                      "id": 2,
                      "display_name": "COURTHOUSE2"
                    },
                    "courtroom": {
                      "id": 3,
                      "name": "COURTROOM2"
                    },
                    "start_at": "2020-10-12T11:00:02Z",
                    "end_at": "2020-10-12T12:00:02Z",
                    "channel": 2,
                    "is_hidden": false
                  },
                  {
                    "id": 25,
                    "courthouse": {
                      "id": 2,
                      "display_name": "COURTHOUSE2"
                    },
                    "courtroom": {
                      "id": 3,
                      "name": "COURTROOM2"
                    },
                    "start_at": "2020-10-12T11:00:03Z",
                    "end_at": "2020-10-12T12:00:03Z",
                    "channel": 3,
                    "is_hidden": false
                  }
                ]
            """;
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    private void assertResponseItems(List<MediaEntity> expectedEntities, MvcResult mvcResult) throws JsonProcessingException, UnsupportedEncodingException {
        String contentAsString = mvcResult.getResponse().getContentAsString();

        List<Long> expectedIds = expectedEntities.stream().map(MediaEntity::getId).sorted().toList();
        List<PostAdminMediasSearchResponseItem> actualResponseItems =
            objectMapper.readValue(contentAsString, new TypeReference<>() {
            });
        List<Long> actualIds = actualResponseItems.stream().map(PostAdminMediasSearchResponseItem::getId).sorted().toList();
        assertThat(actualIds, is(expectedIds));
    }

    private MediaEntity createMedia(CourthouseEntity courthouse, String courtroomName, OffsetDateTime startTime, String caseNumber) {
        return createMedia(courthouse, courtroomName, startTime, caseNumber, true);
    }

    private MediaEntity createMedia(CourthouseEntity courthouse, String courtroomName, OffsetDateTime startTime, String caseNumber, boolean isCurrent) {
        MediaEntity newMediaEntity = mediaStub.createMediaEntity(courthouse, courtroomName, startTime, startTime.plusHours(1), 1, isCurrent);
        mediaRepository.save(newMediaEntity);

        mediaStub.linkToCase(newMediaEntity, caseNumber);
        return newMediaEntity;
    }
}