package uk.gov.hmcts.darts.retention.controller;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.RetentionPolicyTypeEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.RetentionPolicyTypeRepository;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.SuperAdminUserStub;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class RetentionControllerCreateRetentionPolicyTypeIntTest extends IntegrationBase {

    private static final String SOME_FIXED_POLICY_KEY = "99999";
    private static final String SOME_POLICY_NAME = "Policy name";
    private static final String SOME_POLICY_DISPLAY_NAME = "Policy display name";
    private static final String SOME_POLICY_DESCRIPTION = "Policy description";
    private static final String SOME_POLICY_DURATION = "1Y0M0D";
    private static final String SOME_POLICY_START_DATE_IN_THE_FUTURE = "2124-01-01T00:00:00Z";
    private static final String SOME_POLICY_START_DATE_IN_PAST = "2000-01-01T00:00:00Z";

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private RetentionPolicyTypeRepository retentionPolicyTypeRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SuperAdminUserStub superAdminUserStub;

    @MockitoBean
    private UserIdentity userIdentity;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Test
    void createRetentionPolicyTypeShouldSucceedAndCreatePolicyWhenFixedPolicyKeyIsUnique() throws Exception {
        // Given
        UserAccountEntity userAccountEntity = superAdminUserStub.givenUserIsAuthorised(userIdentity);

        // When
        ResultActions resultActions = mockMvc.perform(
            buildRequest()
                .content("""
                             {
                               "name": "%s",
                               "display_name": "%s",
                               "description": "%s",
                               "fixed_policy_key": "%s",
                               "duration": "%s",
                               "policy_start_at": "%s"
                             }
                             """.formatted(SOME_POLICY_NAME,
                                           SOME_POLICY_DISPLAY_NAME,
                                           SOME_POLICY_DESCRIPTION,
                                           SOME_FIXED_POLICY_KEY,
                                           SOME_POLICY_DURATION,
                                           SOME_POLICY_START_DATE_IN_THE_FUTURE)
                ));

        // Then assert response
        MvcResult result = resultActions
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.name").value(SOME_POLICY_NAME))
            .andExpect(jsonPath("$.display_name").value(SOME_POLICY_DISPLAY_NAME))
            .andExpect(jsonPath("$.description").value(SOME_POLICY_DESCRIPTION))
            .andExpect(jsonPath("$.fixed_policy_key").value(SOME_FIXED_POLICY_KEY))
            .andExpect(jsonPath("$.duration").value(SOME_POLICY_DURATION))
            .andExpect(jsonPath("$.policy_start_at").value(SOME_POLICY_START_DATE_IN_THE_FUTURE))
            .andReturn();

        // And assert database state
        var id = new JSONObject(result.getResponse().getContentAsString())
            .getInt("id");
        transactionTemplate.executeWithoutResult(status -> {
            var createdPolicyTypeEntity = retentionPolicyTypeRepository.findById(id)
                .orElseThrow();

            assertEquals(SOME_POLICY_NAME, createdPolicyTypeEntity.getPolicyName());
            assertEquals(SOME_POLICY_DISPLAY_NAME, createdPolicyTypeEntity.getDisplayName());
            assertEquals(SOME_POLICY_DESCRIPTION, createdPolicyTypeEntity.getDescription());
            assertEquals(SOME_FIXED_POLICY_KEY, createdPolicyTypeEntity.getFixedPolicyKey());
            assertEquals(SOME_POLICY_DURATION, createdPolicyTypeEntity.getDuration());
            assertEquals(OffsetDateTime.parse(SOME_POLICY_START_DATE_IN_THE_FUTURE), createdPolicyTypeEntity.getPolicyStart());
            assertNull(createdPolicyTypeEntity.getPolicyEnd());

            Integer userId = userAccountEntity.getId();
            assertEquals(userId, createdPolicyTypeEntity.getLastModifiedBy().getId());
            assertEquals(userId, createdPolicyTypeEntity.getCreatedBy().getId());

            assertNotNull(createdPolicyTypeEntity.getLastModifiedDateTime());
            assertNotNull(createdPolicyTypeEntity.getCreatedBy());
        });
    }

    @Test
    void createRetentionPolicyTypeShouldFailWhenFixedPolicyKeyIsNotUnique() throws Exception {
        // Given
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        createAndSavePolicyTypeWithStartDate(SOME_POLICY_START_DATE_IN_PAST);

        // When
        ResultActions resultActions = mockMvc.perform(
            buildRequest()
                .content("""
                             {
                               "name": "%s",
                               "display_name": "%s",
                               "description": "%s",
                               "fixed_policy_key": "%s",
                               "duration": "%s",
                               "policy_start_at": "%s"
                             }
                             """.formatted(SOME_POLICY_NAME,
                                           SOME_POLICY_DISPLAY_NAME,
                                           SOME_POLICY_DESCRIPTION,
                                           SOME_FIXED_POLICY_KEY,
                                           SOME_POLICY_DURATION,
                                           SOME_POLICY_START_DATE_IN_THE_FUTURE)
                ));

        // Then
        resultActions
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.type").value("RETENTION_114"))
            .andExpect(jsonPath("$.title").value("Fixed policy key must be unique"));
    }

    @Test
    void createRetentionPolicyTypeShouldFailWhenPolicyNameIsNotUnique() throws Exception {
        // Given
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        createAndSavePolicyTypeWithStartDate(SOME_POLICY_START_DATE_IN_PAST);

        // When
        ResultActions resultActions = mockMvc.perform(
            buildRequest()
                .content("""
                             {
                               "name": "%s",
                               "display_name": "%s",
                               "description": "%s",
                               "fixed_policy_key": "%s",
                               "duration": "%s",
                               "policy_start_at": "%s"
                             }
                             """.formatted(SOME_POLICY_NAME,
                                           SOME_POLICY_DISPLAY_NAME,
                                           SOME_POLICY_DESCRIPTION,
                                           "A unique key",
                                           SOME_POLICY_DURATION,
                                           SOME_POLICY_START_DATE_IN_THE_FUTURE)
                ));

        // Then
        resultActions
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.type").value("RETENTION_109"))
            .andExpect(jsonPath("$.title").value("Policy name must be unique"));
    }

    @Test
    void createRetentionPolicyTypeShouldFailWhenDisplayNameIsNotUnique() throws Exception {
        // Given
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        createAndSavePolicyTypeWithStartDate(SOME_POLICY_START_DATE_IN_PAST);

        // When
        ResultActions resultActions = mockMvc.perform(
            buildRequest()
                .content("""
                             {
                               "name": "%s",
                               "display_name": "%s",
                               "description": "%s",
                               "fixed_policy_key": "%s",
                               "duration": "%s",
                               "policy_start_at": "%s"
                             }
                             """.formatted("A unique name",
                                           SOME_POLICY_DISPLAY_NAME,
                                           SOME_POLICY_DESCRIPTION,
                                           "A unique key",
                                           SOME_POLICY_DURATION,
                                           SOME_POLICY_START_DATE_IN_THE_FUTURE)
                ));

        // Then
        resultActions
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.type").value("RETENTION_110"))
            .andExpect(jsonPath("$.title").value("Display name must be unique"));
    }

    @Test
    void createRetentionPolicyTypeShouldFailWhenLongDescriptionIsProvided() throws Exception {
        // Given
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        int maxAllowableCharacters = 256;
        String longDescription = StringUtils.repeat("A", ++maxAllowableCharacters);

        // When
        ResultActions resultActions = mockMvc.perform(
            buildRequest()
                .content("""
                             {
                               "name": "%s",
                               "display_name": "%s",
                               "description": "%s",
                               "fixed_policy_key": "%s",
                               "duration": "%s",
                               "policy_start_at": "%s"
                             }
                             """.formatted(SOME_POLICY_NAME,
                                           SOME_POLICY_DISPLAY_NAME,
                                           longDescription,
                                           SOME_FIXED_POLICY_KEY,
                                           SOME_POLICY_DURATION,
                                           SOME_POLICY_START_DATE_IN_THE_FUTURE)
                ));

        // Then
        resultActions
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.type").value("https://zalando.github.io/problem/constraint-violation"))
            .andExpect(jsonPath("$.title").value("Constraint Violation"))
            .andExpect(jsonPath("$.violations.*.field").value("description"))
            .andExpect(jsonPath("$.violations.*.message").value("size must be between 0 and 256"));
    }

    @Test
    void createRetentionPolicyTypeShouldFailWhenDurationFormatIsIncorrect() throws Exception {
        // Given
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        // When
        ResultActions resultActions = mockMvc.perform(
            buildRequest()
                .content("""
                             {
                               "name": "%s",
                               "display_name": "%s",
                               "description": "%s",
                               "fixed_policy_key": "%s",
                               "duration": "%s",
                               "policy_start_at": "%s"
                             }
                             """.formatted(SOME_POLICY_NAME,
                                           SOME_POLICY_DISPLAY_NAME,
                                           SOME_POLICY_DESCRIPTION,
                                           SOME_FIXED_POLICY_KEY,
                                           "1Y",
                                           SOME_POLICY_START_DATE_IN_THE_FUTURE)
                ));

        // Then
        resultActions
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.type").value("https://zalando.github.io/problem/constraint-violation"))
            .andExpect(jsonPath("$.title").value("Constraint Violation"))
            .andExpect(jsonPath("$.violations.*.field").value("duration"))
            .andExpect(jsonPath("$.violations.*.message").value("must match \"^\\d{1,2}Y\\d{1,2}M\\d{1,2}D$\""));
    }

    @Test
    void createRetentionPolicyTypeShouldFailWhenDurationIsTooShort() throws Exception {
        // Given
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        // When
        ResultActions resultActions = mockMvc.perform(
            buildRequest()
                .content("""
                             {
                               "name": "%s",
                               "display_name": "%s",
                               "description": "%s",
                               "fixed_policy_key": "%s",
                               "duration": "%s",
                               "policy_start_at": "%s"
                             }
                             """.formatted(SOME_POLICY_NAME,
                                           SOME_POLICY_DISPLAY_NAME,
                                           SOME_POLICY_DESCRIPTION,
                                           SOME_FIXED_POLICY_KEY,
                                           "0Y0M0D",
                                           SOME_POLICY_START_DATE_IN_THE_FUTURE)
                ));

        // Then
        resultActions
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.type").value("RETENTION_111"))
            .andExpect(jsonPath("$.title").value("Duration too short"))
            .andExpect(jsonPath("$.min_allowable_days").value(1));
    }

    @Test
    void createRetentionPolicyTypeShouldFailWhenPolicyStartDateIsInThePast() throws Exception {
        // Given
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        // When
        ResultActions resultActions = mockMvc.perform(
            buildRequest()
                .content("""
                             {
                               "name": "%s",
                               "display_name": "%s",
                               "description": "%s",
                               "fixed_policy_key": "%s",
                               "duration": "%s",
                               "policy_start_at": "%s"
                             }
                             """.formatted(SOME_POLICY_NAME,
                                           SOME_POLICY_DISPLAY_NAME,
                                           SOME_POLICY_DESCRIPTION,
                                           SOME_FIXED_POLICY_KEY,
                                           SOME_POLICY_DURATION,
                                           SOME_POLICY_START_DATE_IN_PAST)
                ));

        // Then
        resultActions
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.type").value("RETENTION_112"))
            .andExpect(jsonPath("$.title").value("The provided start date must be in the future"));
    }

    private MockHttpServletRequestBuilder buildRequest() {
        return post("/admin/retention-policy-types")
            .header("Content-Type", "application/json");
    }

    private RetentionPolicyTypeEntity createAndSavePolicyTypeWithStartDate(String startDateTime) {
        RetentionPolicyTypeEntity retentionPolicyTypeEntity = new RetentionPolicyTypeEntity();
        retentionPolicyTypeEntity.setFixedPolicyKey(SOME_FIXED_POLICY_KEY);
        retentionPolicyTypeEntity.setPolicyName(SOME_POLICY_NAME);
        retentionPolicyTypeEntity.setDisplayName(SOME_POLICY_DISPLAY_NAME);
        retentionPolicyTypeEntity.setDescription(SOME_POLICY_DESCRIPTION);
        retentionPolicyTypeEntity.setDuration(SOME_POLICY_DURATION);
        retentionPolicyTypeEntity.setPolicyStart(OffsetDateTime.parse(startDateTime));
        retentionPolicyTypeEntity.setPolicyEnd(null);

        retentionPolicyTypeEntity.setCreatedBy(userIdentity.getUserAccount());
        retentionPolicyTypeEntity.setLastModifiedBy(userIdentity.getUserAccount());

        return dartsDatabase.getRetentionPolicyTypeRepository()
            .save(retentionPolicyTypeEntity);
    }
}
