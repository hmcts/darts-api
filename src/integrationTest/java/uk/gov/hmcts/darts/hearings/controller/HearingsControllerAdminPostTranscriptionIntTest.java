package uk.gov.hmcts.darts.hearings.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.repository.ObjectAdminActionRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionDocumentRepository;
import uk.gov.hmcts.darts.hearings.exception.HearingApiError;
import uk.gov.hmcts.darts.hearings.model.HearingsSearchRequest;
import uk.gov.hmcts.darts.hearings.model.HearingsSearchResponse;
import uk.gov.hmcts.darts.hearings.model.Problem;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.HearingStub;
import uk.gov.hmcts.darts.testutils.stubs.SuperAdminUserStub;
import uk.gov.hmcts.darts.testutils.stubs.TransactionDocumentStub;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class HearingsControllerAdminPostTranscriptionIntTest extends IntegrationBase {

    private static final String ENDPOINT_URL = "/admin/hearings/search";

    @Autowired
    private SuperAdminUserStub superAdminUserStub;

    @Autowired
    private HearingStub hearingStub;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserIdentity userIdentity;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TransactionDocumentStub transactionDocumentStub;

    @Autowired
    private TranscriptionDocumentRepository transcriptionDocumentRepository;

    @Autowired
    private ObjectAdminActionRepository objectAdminActionRepository;


    @Test
    void testHearingSearchForAllResultsOnMaximumResultBoundary() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        List<HearingEntity> hearingEntityList = hearingStub.generateHearings(4);

        HearingsSearchRequest searchRequest = new HearingsSearchRequest();
        searchRequest.setCourthouseIds(List.of(hearingEntityList.get(1).getCourtroom().getCourthouse().getId()));
        searchRequest.setCaseNumber(hearingEntityList.get(1).getCourtCase().getCaseNumber());
        searchRequest.setHearingStartAt(hearingEntityList.get(1).getHearingDate());
        searchRequest.setCourtroomName(hearingEntityList.get(1).getCourtroom().getName());
        searchRequest.setHearingEndAt(hearingEntityList.get(1).getHearingDate());

        List<HearingEntity> expectedHearing = new ArrayList<>();
        expectedHearing.add(hearingEntityList.get(1));

        // run the test
        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT_URL)
                                                  .header("Content-Type", "application/json")
                                                  .content(objectMapper.writeValueAsString(searchRequest)))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        HearingsSearchResponse[] actualResponse
            = objectMapper.readValue(mvcResult.getResponse().getContentAsByteArray(), HearingsSearchResponse[].class);

        assertEquals(expectedHearing.size(), actualResponse.length);
        for (int i = 0; i < expectedHearing.size(); i++) {
            assertEquals(expectedHearing.get(i).getHearingDate(), actualResponse[i].getHearingDate());
            assertEquals(expectedHearing.get(i).getId(), actualResponse[i].getId());
            assertEquals(expectedHearing.get(i).getCourtroom().getCourthouse().getId(), actualResponse[i].getCourthouse().getId());
            assertEquals(expectedHearing.get(i).getCourtroom().getCourthouse().getDisplayName(), actualResponse[i].getCourthouse().getDisplayName());
            assertEquals(expectedHearing.get(i).getCourtroom().getId(), actualResponse[i].getCourtroom().getId());
            assertEquals(expectedHearing.get(i).getCourtroom().getName(), actualResponse[i].getCourtroom().getName());
            assertEquals(expectedHearing.get(i).getCourtCase().getId(), actualResponse[i].getCase().getId());
            assertEquals(expectedHearing.get(i).getCourtCase().getCaseNumber(), actualResponse[i].getCase().getCaseNumber());
        }
    }

    @Test
    void testHearingSearchForAllResultsNoCriteria() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        List<HearingEntity> hearingEntityList = hearingStub.generateHearings(4);
        hearingEntityList.sort((o1, o2) -> o2.getHearingDate().compareTo(o1.getHearingDate()));
        HearingsSearchRequest searchRequest = new HearingsSearchRequest();

        // run the test
        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT_URL)
                                                  .header("Content-Type", "application/json")
                                                  .content(objectMapper.writeValueAsString(searchRequest)))
            .andExpect(status().is4xxClientError())
            .andReturn();

        String actualResponse = mvcResult.getResponse().getContentAsString();
        String expectedResponse = """
            {
                "type": "HEARING_103",
                "title": "The search criteria is too broad. Please refine your search.",
                "status": 400
              }
            """;

        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void testHearingSearchForAllResultSWithCourthouseEmptyArray() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        List<HearingEntity> hearingEntityList = hearingStub.generateHearings(4);
        hearingEntityList.sort((o1, o2) -> o2.getHearingDate().compareTo(o1.getHearingDate()));

        HearingsSearchRequest searchRequest = new HearingsSearchRequest();
        searchRequest.setCourthouseIds(new ArrayList<>());

        // run the test
        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT_URL)
                                                  .header("Content-Type", "application/json")
                                                  .content(objectMapper.writeValueAsString(searchRequest)))
            .andExpect(status().is4xxClientError())
            .andReturn();

        String actualResponse = mvcResult.getResponse().getContentAsString();
        String expectedResponse = """
            {
                "type": "HEARING_103",
                "title": "The search criteria is too broad. Please refine your search.",
                "status": 400
              }
            """;

        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void testHearingSearchForResultWithCaseNumber() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        List<HearingEntity> hearingEntityList = hearingStub.generateHearings(4);

        HearingEntity expectedSearchResult = hearingEntityList.get(2);

        HearingsSearchRequest searchRequest = new HearingsSearchRequest();
        searchRequest.setCaseNumber(expectedSearchResult.getCourtCase().getCaseNumber());

        // run the test
        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT_URL)
                                                  .header("Content-Type", "application/json")
                                                  .content(objectMapper.writeValueAsString(searchRequest)))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        HearingsSearchResponse[] actualResponse
            = objectMapper.readValue(mvcResult.getResponse().getContentAsByteArray(), HearingsSearchResponse[].class);

        assertEquals(expectedSearchResult.getHearingDate(), actualResponse[0].getHearingDate());
        assertEquals(expectedSearchResult.getId(), actualResponse[0].getId());
        assertEquals(expectedSearchResult.getCourtroom().getCourthouse().getId(), actualResponse[0].getCourthouse().getId());
        assertEquals(expectedSearchResult.getCourtroom().getCourthouse().getDisplayName(), actualResponse[0].getCourthouse().getDisplayName());
        assertEquals(expectedSearchResult.getCourtroom().getId(), actualResponse[0].getCourtroom().getId());
        assertEquals(expectedSearchResult.getCourtroom().getName(), actualResponse[0].getCourtroom().getName());
        assertEquals(expectedSearchResult.getCourtCase().getId(), actualResponse[0].getCase().getId());
        assertEquals(expectedSearchResult.getCourtCase().getCaseNumber(), actualResponse[0].getCase().getCaseNumber());
    }

    @Test
    void testHearingSearchForResultWithCourtroomName() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        List<HearingEntity> hearingEntityList = hearingStub.generateHearings(4);

        HearingEntity expectedSearchResult = hearingEntityList.get(2);

        HearingsSearchRequest searchRequest = new HearingsSearchRequest();
        searchRequest.setCourtroomName(expectedSearchResult.getCourtroom().getName());

        // run the test
        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT_URL)
                                                  .header("Content-Type", "application/json")
                                                  .content(objectMapper.writeValueAsString(searchRequest)))
            .andExpect(status().is4xxClientError())
            .andReturn();

        String actualResponse = mvcResult.getResponse().getContentAsString();
        String expectedResponse = """
            {
                "type": "HEARING_103",
                "title": "The search criteria is too broad. Please refine your search.",
                "status": 400
              }
            """;

        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void testHearingSearchForResultWithDateRange() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        List<HearingEntity> hearingEntityList = hearingStub.generateHearings(4);

        HearingEntity expectedSearchResult = hearingEntityList.get(2);

        HearingsSearchRequest searchRequest = new HearingsSearchRequest();
        searchRequest.setHearingStartAt(expectedSearchResult.getHearingDate());
        searchRequest.setHearingEndAt(expectedSearchResult.getHearingDate());

        // run the test
        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT_URL)
                                                  .header("Content-Type", "application/json")
                                                  .content(objectMapper.writeValueAsString(searchRequest)))
            .andExpect(status().is4xxClientError())
            .andReturn();

        String actualResponse = mvcResult.getResponse().getContentAsString();
        String expectedResponse = """
            {
                "type": "HEARING_103",
                "title": "The search criteria is too broad. Please refine your search.",
                "status": 400
              }
            """;

        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void testHearingSearchForResultsWithStartDate() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        List<HearingEntity> hearingEntityList = hearingStub.generateHearings(4);

        HearingsSearchRequest searchRequest = new HearingsSearchRequest();
        searchRequest.setHearingStartAt(hearingEntityList.get(1).getHearingDate());

        List<HearingEntity> expectedHearing = new ArrayList<>();
        expectedHearing.add(hearingEntityList.get(3));
        expectedHearing.add(hearingEntityList.get(2));
        expectedHearing.add(hearingEntityList.get(1));

        // run the test
        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT_URL)
                                                  .header("Content-Type", "application/json")
                                                  .content(objectMapper.writeValueAsString(searchRequest)))
            .andExpect(status().is4xxClientError())
            .andReturn();

        String actualResponse = mvcResult.getResponse().getContentAsString();
        String expectedResponse = """
            {
                "type": "HEARING_103",
                "title": "The search criteria is too broad. Please refine your search.",
                "status": 400
              }
            """;

        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void testHearingSearchForResultsWithEndDate() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        List<HearingEntity> hearingEntityList = hearingStub.generateHearings(4);

        HearingsSearchRequest searchRequest = new HearingsSearchRequest();
        searchRequest.setHearingEndAt(hearingEntityList.get(1).getHearingDate());

        List<HearingEntity> expectedHearing = new ArrayList<>();
        expectedHearing.add(hearingEntityList.get(1));
        expectedHearing.add(hearingEntityList.getFirst());

        // run the test
        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT_URL)
                                                  .header("Content-Type", "application/json")
                                                  .content(objectMapper.writeValueAsString(searchRequest)))
            .andExpect(status().is4xxClientError())
            .andReturn();

        String actualResponse = mvcResult.getResponse().getContentAsString();
        String expectedResponse = """
            {
                "type": "HEARING_103",
                "title": "The search criteria is too broad. Please refine your search.",
                "status": 400
              }
            """;

        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void testHearingSearchForResultWithCourthouseIds() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        List<HearingEntity> hearingEntityList = hearingStub.generateHearings(4);

        HearingsSearchRequest searchRequest = new HearingsSearchRequest();
        searchRequest.setCourthouseIds(List.of(hearingEntityList.get(1)
                                                   .getCourtroom().getCourthouse().getId(), hearingEntityList.get(2).getCourtroom().getCourthouse().getId()));

        List<HearingEntity> expectedHearing = new ArrayList<>();
        expectedHearing.add(hearingEntityList.get(2));
        expectedHearing.add(hearingEntityList.get(1));

        // run the test
        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT_URL)
                                                  .header("Content-Type", "application/json")
                                                  .content(objectMapper.writeValueAsString(searchRequest)))
            .andExpect(status().is4xxClientError())
            .andReturn();

        String actualResponse = mvcResult.getResponse().getContentAsString();
        String expectedResponse = """
            {
                "type": "HEARING_103",
                "title": "The search criteria is too broad. Please refine your search.",
                "status": 400
              }
            """;

        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void testHearingSearchResultsExceedMaximumResults() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        List<HearingEntity> hearingEntityList = hearingStub.generateHearings(10);

        HearingsSearchRequest searchRequest = new HearingsSearchRequest();
        searchRequest.setCourthouseIds(List.of(hearingEntityList.getFirst().getCourtroom().getCourthouse().getId(),
                                               hearingEntityList.get(1).getCourtroom().getCourthouse().getId(),
                                               hearingEntityList.get(2).getCourtroom().getCourthouse().getId(),
                                               hearingEntityList.get(3).getCourtroom().getCourthouse().getId(),
                                               hearingEntityList.get(4).getCourtroom().getCourthouse().getId(),
                                               hearingEntityList.get(5).getCourtroom().getCourthouse().getId()));
        searchRequest.setHearingStartAt(hearingEntityList.getFirst().getHearingDate());
        searchRequest.setHearingEndAt(hearingEntityList.getLast().getHearingDate());

        // run the test
        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT_URL)
                                                  .header("Content-Type", "application/json")
                                                  .content(objectMapper.writeValueAsString(searchRequest)))
            .andExpect(status().isBadRequest())
            .andReturn();

        Problem actualResponse
            = objectMapper.readValue(mvcResult.getResponse().getContentAsByteArray(), Problem.class);

        assertEquals(actualResponse.getType().toString(), HearingApiError.TOO_MANY_RESULTS.getErrorTypeNumeric());
    }

    @Test
    void testSearchForHearingAuthorisationProblem() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity, SecurityRoleEnum.DAR_PC);

        HearingsSearchRequest searchRequest = new HearingsSearchRequest();

        mockMvc.perform(post(ENDPOINT_URL)
                            .header("Content-Type", "application/json")
                            .content(objectMapper.writeValueAsString(searchRequest)))
            .andExpect(status().isForbidden())
            .andReturn();
    }
}