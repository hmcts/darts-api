package uk.gov.hmcts.darts.audio.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.darts.audio.model.PostAdminMediasSearchRequest;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.ObjectAdminActionRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.test.common.TestUtils;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.MediaStub;
import uk.gov.hmcts.darts.testutils.stubs.SuperAdminUserStub;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.test.common.TestUtils.getContentsFromFile;

@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "darts.audio.admin-search.max-results=20"
})
class MediaControllerPostAdminMediasSearchIntTest extends IntegrationBase {

    private static final String ENDPOINT_URL = "/admin/medias/search";

    @Autowired
    private SuperAdminUserStub superAdminUserStub;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserIdentity userIdentity;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MediaStub mediaStub;

    @Autowired
    private MediaRepository mediaRepository;

    @Autowired
    private ObjectAdminActionRepository objectAdminActionRepository;

    private CourthouseEntity courthouse1;
    private CourthouseEntity courthouse2;

    private static final List<String> TAGS_TO_IGNORE = List.of("id");

    @BeforeEach
    @SuppressWarnings({"PMD.NcssCount"})
    void setupDate() {
        List<MediaEntity> mediaToSave = new ArrayList<>();
        OffsetDateTime tenth10am = OffsetDateTime.of(2020, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime startTime = tenth10am.plusSeconds(1);

        courthouse1 = dartsDatabase.createCourthouseUnlessExists("Courthouse1");
        courthouse2 = dartsDatabase.createCourthouseUnlessExists("Courthouse2");
        mediaToSave.add(mediaStub.createMediaEntity(courthouse1, "courtroom1", startTime, startTime.plusHours(1), 1));
        startTime = startTime.plusSeconds(1);
        mediaToSave.add(mediaStub.createMediaEntity(courthouse1, "courtroom1", startTime, startTime.plusHours(1), 1));
        startTime = startTime.plusSeconds(1);
        mediaToSave.add(mediaStub.createMediaEntity(courthouse1, "courtroom1", startTime, startTime.plusHours(1), 1));

        mediaStub.linkToCase(mediaToSave.get(0), "caseNumber1");
        mediaStub.linkToCase(mediaToSave.get(1), "caseNumber1");
        mediaStub.linkToCase(mediaToSave.get(2), "caseNumber1");

        startTime = tenth10am.plusHours(1).plusSeconds(1);
        mediaToSave.add(mediaStub.createMediaEntity(courthouse1, "courtroom1", startTime, startTime.plusHours(1), 1));
        startTime = startTime.plusSeconds(1);
        mediaToSave.add(mediaStub.createMediaEntity(courthouse1, "courtroom1", startTime, startTime.plusHours(1), 1));
        startTime = startTime.plusSeconds(1);
        mediaToSave.add(mediaStub.createMediaEntity(courthouse1, "courtroom1", startTime, startTime.plusHours(1), 1));

        mediaStub.linkToCase(mediaToSave.get(3), "caseNumber2");
        mediaStub.linkToCase(mediaToSave.get(4), "caseNumber2");
        mediaStub.linkToCase(mediaToSave.get(5), "caseNumber2");

        startTime = tenth10am.plusHours(2).plusSeconds(1);
        mediaToSave.add(mediaStub.createMediaEntity(courthouse1, "courtroom1", startTime, startTime.plusHours(1), 1));
        startTime = startTime.plusSeconds(1);
        mediaToSave.add(mediaStub.createMediaEntity(courthouse1, "courtroom1", startTime, startTime.plusHours(1), 1));
        startTime = startTime.plusSeconds(1);
        mediaToSave.add(mediaStub.createMediaEntity(courthouse1, "courtroom1", startTime, startTime.plusHours(1), 1));

        mediaStub.linkToCase(mediaToSave.get(6), "caseNumber3");
        mediaStub.linkToCase(mediaToSave.get(7), "caseNumber3");
        mediaStub.linkToCase(mediaToSave.get(8), "caseNumber3");


        startTime = tenth10am.plusSeconds(1);
        mediaToSave.add(mediaStub.createMediaEntity(courthouse1, "courtroom2", startTime, startTime.plusHours(1), 1));
        startTime = startTime.plusSeconds(1);
        mediaToSave.add(mediaStub.createMediaEntity(courthouse1, "courtroom2", startTime, startTime.plusHours(1), 1));
        startTime = startTime.plusSeconds(1);
        mediaToSave.add(mediaStub.createMediaEntity(courthouse1, "courtroom2", startTime, startTime.plusHours(1), 1));

        mediaStub.linkToCase(mediaToSave.get(9), "caseNumber4");
        mediaStub.linkToCase(mediaToSave.get(10), "caseNumber4");
        mediaStub.linkToCase(mediaToSave.get(11), "caseNumber4");


        startTime = tenth10am.plusHours(1).plusSeconds(1);
        mediaToSave.add(mediaStub.createMediaEntity(courthouse1, "courtroom2", startTime, startTime.plusHours(1), 1));
        startTime = startTime.plusSeconds(1);
        mediaToSave.add(mediaStub.createMediaEntity(courthouse1, "courtroom2", startTime, startTime.plusHours(1), 1));
        startTime = startTime.plusSeconds(1);
        mediaToSave.add(mediaStub.createMediaEntity(courthouse1, "courtroom2", startTime, startTime.plusHours(1), 1));

        mediaStub.linkToCase(mediaToSave.get(12), "caseNumber4");
        mediaStub.linkToCase(mediaToSave.get(13), "caseNumber4");
        mediaStub.linkToCase(mediaToSave.get(14), "caseNumber4");


        OffsetDateTime eleventh10am = OffsetDateTime.of(2020, 10, 11, 10, 0, 0, 0, ZoneOffset.UTC);
        startTime = eleventh10am.plusHours(2).plusSeconds(1);
        mediaToSave.add(mediaStub.createMediaEntity(courthouse1, "courtroom2", startTime, startTime.plusHours(1), 1));
        startTime = startTime.plusSeconds(1);
        mediaToSave.add(mediaStub.createMediaEntity(courthouse1, "courtroom2", startTime, startTime.plusHours(1), 1));
        startTime = startTime.plusSeconds(1);
        mediaToSave.add(mediaStub.createMediaEntity(courthouse1, "courtroom2", startTime, startTime.plusHours(1), 1));

        mediaStub.linkToCase(mediaToSave.get(15), "caseNumber4");
        mediaStub.linkToCase(mediaToSave.get(16), "caseNumber4");
        mediaStub.linkToCase(mediaToSave.get(17), "caseNumber4");

        startTime = eleventh10am.plusSeconds(1);
        mediaToSave.add(mediaStub.createMediaEntity(courthouse2, "courtroom2", startTime, startTime.plusHours(1), 1));
        startTime = startTime.plusSeconds(1);
        mediaToSave.add(mediaStub.createMediaEntity(courthouse2, "courtroom2", startTime, startTime.plusHours(1), 1));
        startTime = startTime.plusSeconds(1);
        mediaToSave.add(mediaStub.createMediaEntity(courthouse2, "courtroom2", startTime, startTime.plusHours(1), 1));

        mediaStub.linkToCase(mediaToSave.get(18), "caseNumber1");
        mediaStub.linkToCase(mediaToSave.get(19), "caseNumber1");
        mediaStub.linkToCase(mediaToSave.get(20), "caseNumber1");

        OffsetDateTime twelfth10am = OffsetDateTime.of(2020, 10, 12, 10, 0, 0, 0, ZoneOffset.UTC);
        startTime = twelfth10am.plusHours(1).plusSeconds(1);
        mediaToSave.add(mediaStub.createMediaEntity(courthouse2, "courtroom2", startTime, startTime.plusHours(1), 1));
        startTime = startTime.plusSeconds(1);
        mediaToSave.add(mediaStub.createMediaEntity(courthouse2, "courtroom2", startTime, startTime.plusHours(1), 1));
        startTime = startTime.plusSeconds(1);
        mediaToSave.add(mediaStub.createMediaEntity(courthouse2, "courtroom2", startTime, startTime.plusHours(1), 1));

        mediaStub.linkToCase(mediaToSave.get(21), "caseNumber2");
        mediaStub.linkToCase(mediaToSave.get(22), "caseNumber2");
        mediaStub.linkToCase(mediaToSave.get(23), "caseNumber2");

        startTime = twelfth10am.plusHours(2).plusSeconds(1);
        mediaToSave.add(mediaStub.createMediaEntity(courthouse2, "courtroom2", startTime, startTime.plusHours(1), 1));
        startTime = startTime.plusSeconds(1);
        mediaToSave.add(mediaStub.createMediaEntity(courthouse2, "courtroom2", startTime, startTime.plusHours(1), 1));
        startTime = startTime.plusSeconds(1);
        mediaToSave.add(mediaStub.createMediaEntity(courthouse2, "courtroom2", startTime, startTime.plusHours(1), 1));

        mediaStub.linkToCase(mediaToSave.get(24), "caseNumber3");
        mediaStub.linkToCase(mediaToSave.get(25), "caseNumber3");
        mediaStub.linkToCase(mediaToSave.get(26), "caseNumber3");

        dartsDatabase.getMediaRepository().saveAllAndFlush(mediaToSave);
    }

    @BeforeEach
    void setupOpenInView() {
        openInViewUtil.openEntityManager();
    }

    @AfterEach
    void closeOpenInView() {
        openInViewUtil.closeEntityManager();
    }

    @Test
    void multipleFields() throws Exception {
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

        String actualResponse = TestUtils.removeTags(TAGS_TO_IGNORE, mvcResult.getResponse().getContentAsString());
        String expectedResponse = getContentsFromFile("tests/audio/MediaControllerPostAdminMediasSearchIntTest/multipleFields.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);

    }

    @Test
    void onlyHearingStart() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        PostAdminMediasSearchRequest request = new PostAdminMediasSearchRequest();
        request.setHearingStartAt(LocalDate.of(2020, 10, 11));


        // run the test
        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT_URL)
                                                  .header("Content-Type", "application/json")
                                                  .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        String actualResponse = TestUtils.removeTags(TAGS_TO_IGNORE, mvcResult.getResponse().getContentAsString());
        String expectedResponse = getContentsFromFile("tests/audio/MediaControllerPostAdminMediasSearchIntTest/onlyHearingStart.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);

    }

    @Test
    void onlyHearingEnd() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        PostAdminMediasSearchRequest request = new PostAdminMediasSearchRequest();
        request.setHearingEndAt(LocalDate.of(2020, 10, 10));


        // run the test
        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT_URL)
                                                  .header("Content-Type", "application/json")
                                                  .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        String actualResponse = TestUtils.removeTags(TAGS_TO_IGNORE, mvcResult.getResponse().getContentAsString());
        String expectedResponse = getContentsFromFile("tests/audio/MediaControllerPostAdminMediasSearchIntTest/onlyHearingEnd.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);

    }

    @Test
    void multipleAndHearingStartAndEnd() throws Exception {
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

        String actualResponse = TestUtils.removeTags(TAGS_TO_IGNORE, mvcResult.getResponse().getContentAsString());
        String expectedResponse = getContentsFromFile("tests/audio/MediaControllerPostAdminMediasSearchIntTest/multipleAndHearingStartAndEnd.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);

    }

    @Test
    void noMatch() throws Exception {
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
        String expectedResponse = getContentsFromFile("tests/audio/MediaControllerPostAdminMediasSearchIntTest/noMatch.json");

        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);

    }

    @Test
    void emptyCourthouses() throws Exception {
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

        String actualResponse = TestUtils.removeTags(TAGS_TO_IGNORE, mvcResult.getResponse().getContentAsString());
        String expectedResponse = getContentsFromFile("tests/audio/MediaControllerPostAdminMediasSearchIntTest/emptyCourthouses.json");

        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);

    }

    @Test
    void tooManyResults() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        PostAdminMediasSearchRequest request = new PostAdminMediasSearchRequest();

        // run the test
        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT_URL)
                                                  .header("Content-Type", "application/json")
                                                  .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().is4xxClientError())
            .andReturn();

        String actualResponse = TestUtils.removeTags(TAGS_TO_IGNORE, mvcResult.getResponse().getContentAsString());
        String expectedResponse = getContentsFromFile("tests/audio/MediaControllerPostAdminMediasSearchIntTest/tooManyResults.json");

        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);

    }

    @Test
    void noPermissions() throws Exception {
        PostAdminMediasSearchRequest request = new PostAdminMediasSearchRequest();
        request.setCourthouseIds(new ArrayList<>());

        // run the test
        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT_URL)
                                                  .header("Content-Type", "application/json")
                                                  .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden())
            .andReturn();

        String actualResponse = TestUtils.removeTags(TAGS_TO_IGNORE, mvcResult.getResponse().getContentAsString());
        String expectedResponse = getContentsFromFile("tests/audio/MediaControllerPostAdminMediasSearchIntTest/noPermissions.json");

        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);

    }

}