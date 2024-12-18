package uk.gov.hmcts.darts.retention.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CaseRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.retention.enums.CaseRetentionStatus;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceCategoryEnum;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceReasonEnum;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceScoreEnum;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.test.common.data.RetentionConfidenceCategoryMapperTestData;
import uk.gov.hmcts.darts.test.common.data.builder.TestRetentionConfidenceCategoryMapperEntity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static uk.gov.hmcts.darts.retention.enums.RetentionConfidenceCategoryEnum.MANUAL_OVERRIDE;
import static uk.gov.hmcts.darts.retention.enums.RetentionConfidenceScoreEnum.CASE_PERFECTLY_CLOSED;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true") // To override Clock bean
@AutoConfigureMockMvc
class RetentionControllerPostRetentionIntTest extends IntegrationBase {
    @Autowired
    private transient MockMvc mockMvc;

    @MockBean
    private UserIdentity mockUserIdentity;

    @MockBean
    private CurrentTimeHelper currentTimeHelper;

    public static final String ENDPOINT_URL = "/retentions";

    private static final String SOME_COURTHOUSE = "SOME-COURTHOUSE";
    private static final String SOME_CASE_NUMBER = "12345";
    private static final OffsetDateTime CURRENT_DATE_TIME = OffsetDateTime.of(2024, 10, 1, 10, 0, 0, 0, ZoneOffset.UTC);

    @TestConfiguration
    public static class ClockConfig {
        @Bean
        public Clock clock() {
            return Clock.fixed(CURRENT_DATE_TIME.toInstant(), ZoneOffset.UTC);
        }
    }

    @BeforeEach
    void setUp() {
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(CURRENT_DATE_TIME);
    }

    private void createAndSaveRetentionConfidenceCategoryMappings() {
        RetentionConfidenceCategoryMapperTestData testData = PersistableFactory.getRetentionConfidenceCategoryMapperTestData();

        TestRetentionConfidenceCategoryMapperEntity manualOverrideMappingEntity = testData.someMinimalBuilder()
            .confidenceCategory(RetentionConfidenceCategoryEnum.MANUAL_OVERRIDE)
            .confidenceReason(RetentionConfidenceReasonEnum.MANUAL_OVERRIDE)
            .confidenceScore(RetentionConfidenceScoreEnum.CASE_PERFECTLY_CLOSED)
            .build();
        dartsPersistence.save(manualOverrideMappingEntity.getEntity());
    }

    @Test
    void happyPath() throws Exception {
        createAndSaveRetentionConfidenceCategoryMappings();

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub()
            .createAuthorisedIntegrationTestUser(SOME_COURTHOUSE);
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);

        CourtCaseEntity courtCase = dartsDatabase.createCase(
            SOME_COURTHOUSE,
            SOME_CASE_NUMBER
        );
        courtCase.setCaseClosedTimestamp(OffsetDateTime.of(2020, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC));
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
        String actualResponse = mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn().getResponse().getContentAsString();
        Optional<CaseRetentionEntity> latestCompletedRetention = dartsDatabase.getCaseRetentionRepository().findLatestCompletedRetention(courtCase);
        assertEquals(OffsetDateTime.parse("2024-05-20T00:00Z"), latestCompletedRetention.get().getRetainUntil());
        assertThat(latestCompletedRetention.get().getConfidenceCategory()).isEqualTo(MANUAL_OVERRIDE);

        String expectedResponse = """
            {
              "retention_date": "2024-05-20"
            }
            """;
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
        CourtCaseEntity actualCourtCase = dartsDatabase.getCaseRepository().findById(courtCase.getId()).get();
        assertThat(actualCourtCase.getRetConfScore()).isEqualTo(CASE_PERFECTLY_CLOSED);
        assertThat(actualCourtCase.getRetConfReason()).isEqualTo(RetentionConfidenceReasonEnum.MANUAL_OVERRIDE);
        assertThat(actualCourtCase.getRetConfUpdatedTs()).isEqualTo(CURRENT_DATE_TIME);
    }

    @Test
    void postRetentions_shouldSucceed_andSetNullConfidenceScoreAndReason_whenNoConfidenceMappingExists() throws Exception {
        // Given createAndSaveRetentionConfidenceCategoryMappings() is not invoked, so no confidence mappings exist in the DB

        // And
        UserAccountEntity testUser = dartsDatabase.getUserAccountStub()
            .createAuthorisedIntegrationTestUser(SOME_COURTHOUSE);
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);

        CourtCaseEntity courtCase = dartsDatabase.createCase(
            SOME_COURTHOUSE,
            SOME_CASE_NUMBER
        );
        courtCase.setCaseClosedTimestamp(OffsetDateTime.of(2020, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC));
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

        // When
        String actualResponse = mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn().getResponse().getContentAsString();

        // Then
        Optional<CaseRetentionEntity> latestCompletedRetention = dartsDatabase.getCaseRetentionRepository().findLatestCompletedRetention(courtCase);
        assertEquals(OffsetDateTime.parse("2024-05-20T00:00Z"), latestCompletedRetention.get().getRetainUntil());
        assertThat(latestCompletedRetention.get().getConfidenceCategory()).isEqualTo(MANUAL_OVERRIDE);

        String expectedResponse = """
            {
              "retention_date": "2024-05-20"
            }
            """;
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
        CourtCaseEntity actualCourtCase = dartsDatabase.getCaseRepository().findById(courtCase.getId()).get();
        assertNull(actualCourtCase.getRetConfScore());
        assertNull(actualCourtCase.getRetConfReason());
        assertThat(actualCourtCase.getRetConfUpdatedTs()).isEqualTo(CURRENT_DATE_TIME);
    }

    @Test
    void happyPath_validateOnly() throws Exception {
        createAndSaveRetentionConfidenceCategoryMappings();

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub()
            .createAuthorisedIntegrationTestUser(SOME_COURTHOUSE);
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);

        CourtCaseEntity courtCase = dartsDatabase.createCase(
            SOME_COURTHOUSE,
            SOME_CASE_NUMBER
        );
        courtCase.setCaseClosedTimestamp(OffsetDateTime.of(2020, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC));
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
            .queryParam("validate_only", "true")
            .content(requestBody);
        String actualResponse = mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn().getResponse().getContentAsString();
        Optional<CaseRetentionEntity> latestCompletedRetention = dartsDatabase.getCaseRetentionRepository().findLatestCompletedRetention(courtCase);
        assertEquals(retainUntilDate, latestCompletedRetention.get().getRetainUntil());

        String expectedResponse = """
            {
              "retention_date": "2024-05-20"
            }
            """;
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void happyPath_judgeReducingRetention() throws Exception {
        createAndSaveRetentionConfidenceCategoryMappings();

        CourtCaseEntity courtCase = dartsDatabase.createCase(
            SOME_COURTHOUSE,
            SOME_CASE_NUMBER
        );
        courtCase.setCaseClosedTimestamp(OffsetDateTime.of(2020, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC));
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
        String actualResponse = mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn().getResponse().getContentAsString();
        Optional<CaseRetentionEntity> latestCompletedRetention = dartsDatabase.getCaseRetentionRepository().findLatestCompletedRetention(courtCase);
        assertEquals(OffsetDateTime.parse("2023-05-20T00:00Z"), latestCompletedRetention.get().getRetainUntil());

        String expectedResponse = """
            {
              "retention_date": "2023-05-20"
            }
            """;
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);

    }

    @Test
    void givenARetentionDateEarlierThanLastAutomatedThenThrow422() throws Exception {
        createAndSaveRetentionConfidenceCategoryMappings();

        CourtCaseEntity courtCase = dartsDatabase.createCase(
            SOME_COURTHOUSE,
            SOME_CASE_NUMBER
        );
        courtCase.setCaseClosedTimestamp(OffsetDateTime.of(2020, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC));
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
            .andExpect(jsonPath("type", is("RETENTION_101")))
            .andExpect(jsonPath("title", is("The retention date being applied is too early.")))
            .andExpect(jsonPath("status", is(422)))
            .andExpect(jsonPath(
                "detail",
                is("caseId '" + courtCase.getId().toString()
                       + "' must have a retention date after the last completed automated retention date '2024-01-01'.")
            ))
            .andExpect(jsonPath(
                "latest_automated_retention_date",
                is("2024-01-01")
            ));
    }

    @Test
    void givenARetentionDateLaterThanMaxRetentionThenThrow422() throws Exception {
        createAndSaveRetentionConfidenceCategoryMappings();

        CourtCaseEntity courtCase = dartsDatabase.createCase(
            SOME_COURTHOUSE,
            SOME_CASE_NUMBER
        );
        courtCase.setClosed(true);
        courtCase.setCaseClosedTimestamp(OffsetDateTime.of(2020, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC));
        dartsDatabase.save(courtCase);

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub()
            .createJudgeUser(courtCase.getCourthouse());
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);

        dartsDatabase.createCaseRetentionObject(courtCase, CaseRetentionStatus.COMPLETE, OffsetDateTime.parse("2024-01-01T12:00Z"), false);

        String requestBody = """
            {
              "case_id": <<caseId>>,
              "retention_date": "2223-05-20",
              "is_permanent_retention": false,
              "comments": "string"
            }""";

        requestBody = requestBody.replace("<<caseId>>", courtCase.getId().toString());
        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT_URL)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(requestBody);
        mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().isUnprocessableEntity())
            .andExpect(jsonPath("type", is("RETENTION_107")))
            .andExpect(jsonPath("title", is("The retention date being applied is too late.")))
            .andExpect(jsonPath("status", is(422)))
            .andExpect(jsonPath(
                "detail",
                is("caseId '" + courtCase.getId().toString()
                       + "' must have a retention date before the maximum retention date '2119-10-10'.")
            ))
            .andExpect(jsonPath(
                "max_duration",
                is("99Y0M0D")
            ));
    }

}