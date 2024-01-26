package uk.gov.hmcts.darts.retention.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CaseRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.retention.enums.CaseRetentionStatus;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@AutoConfigureMockMvc
class RetentionControllerPostRetentionTest extends IntegrationBase {
    @Autowired
    private transient MockMvc mockMvc;

    @MockBean
    private UserIdentity mockUserIdentity;

    public static final String ENDPOINT_URL = "/retentions";

    private static final String SOME_COURTHOUSE = "some-courthouse";
    private static final String SOME_CASE_NUMBER = "12345";

    @Test
    void happyPath() throws Exception {

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub()
            .createAuthorisedIntegrationTestUser(SOME_COURTHOUSE);
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);

        CourtCaseEntity courtCase = dartsDatabase.createCase(
            SOME_COURTHOUSE,
            SOME_CASE_NUMBER
        );
        courtCase.setClosed(true);
        dartsDatabase.save(courtCase);

        OffsetDateTime retainUntilDate = OffsetDateTime.parse("2023-01-01T12:00Z");

        dartsDatabase.createCaseRetentionObject(courtCase, CaseRetentionStatus.COMPLETE, retainUntilDate, false);

        String requestBody = """
            {
              "case_id": <<caseId>>,
              "retention_date": "2024-05-20",
              "is_permanent_retention": false,
              "comments": "string"
            }""";

        requestBody = requestBody.replace("<<caseId>>", courtCase.getId().toString());

        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT_URL)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(requestBody);
        mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().isOk());
        Optional<CaseRetentionEntity> latestCompletedRetention = dartsDatabase.getCaseRetentionRepository().findLatestCompletedRetention(courtCase);
        assertEquals(OffsetDateTime.parse("2024-05-20T00:00Z"), latestCompletedRetention.get().getRetainUntil());
    }

    @Test
    void happyPath_judgeReducingRetention() throws Exception {
        CourtCaseEntity courtCase = dartsDatabase.createCase(
            SOME_COURTHOUSE,
            SOME_CASE_NUMBER
        );
        courtCase.setClosed(true);
        dartsDatabase.save(courtCase);

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub()
            .createJudgeUser(courtCase.getCourthouse());
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);


        dartsDatabase.createCaseRetentionObject(courtCase, CaseRetentionStatus.COMPLETE, OffsetDateTime.parse("2023-01-01T12:00Z"), false);
        dartsDatabase.createCaseRetentionObject(courtCase, CaseRetentionStatus.COMPLETE, OffsetDateTime.parse("2024-01-01T12:00Z"), true);

        String requestBody = """
            {
              "case_id": <<caseId>>,
              "retention_date": "2023-05-20",
              "is_permanent_retention": false,
              "comments": "string"
            }""";

        requestBody = requestBody.replace("<<caseId>>", courtCase.getId().toString());
        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT_URL)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(requestBody);
        mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().isOk());
        Optional<CaseRetentionEntity> latestCompletedRetention = dartsDatabase.getCaseRetentionRepository().findLatestCompletedRetention(courtCase);
        assertEquals(OffsetDateTime.parse("2023-05-20T00:00Z"), latestCompletedRetention.get().getRetainUntil());
    }

    @Test
    void givenARetentionDateEarlierThanLastAutomatedThenThrow422() throws Exception {
        CourtCaseEntity courtCase = dartsDatabase.createCase(
            SOME_COURTHOUSE,
            SOME_CASE_NUMBER
        );
        courtCase.setClosed(true);
        dartsDatabase.save(courtCase);

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub()
            .createJudgeUser(courtCase.getCourthouse());
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);

        dartsDatabase.createCaseRetentionObject(courtCase, CaseRetentionStatus.COMPLETE, OffsetDateTime.parse("2024-01-01T12:00Z"), false);

        String requestBody = """
            {
              "case_id": <<caseId>>,
              "retention_date": "2023-05-20",
              "is_permanent_retention": false,
              "comments": "string"
            }""";

        requestBody = requestBody.replace("<<caseId>>", courtCase.getId().toString());
        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT_URL)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(requestBody);
        mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().isUnprocessableEntity())
            .andExpect(jsonPath("type", is("RETENTION_106")))
            .andExpect(jsonPath("title", is("The retention date being applied is too early.")))
            .andExpect(jsonPath("status", is(422)))
            .andExpect(jsonPath("detail",
                is("caseId '" + courtCase.getId().toString()
                       + "' must have a retention date after the last Completed Automated retention date '2024-01-01T12:00Z'.")));
    }

}
