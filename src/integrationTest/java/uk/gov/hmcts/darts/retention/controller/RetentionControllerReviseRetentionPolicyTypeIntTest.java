package uk.gov.hmcts.darts.retention.controller;

import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.domain.Example;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class RetentionControllerReviseRetentionPolicyTypeIntTest extends IntegrationBase {

    private static final String SOME_FIXED_POLICY_KEY = "99999";
    private static final String SOME_OTHER_FIXED_POLICY_KEY = "12345";

    private static final String SOME_POLICY_NAME = "Policy name";
    private static final String SOME_OTHER_POLICY_NAME = "Another policy name";

    private static final String SOME_POLICY_DISPLAY_NAME = "Policy display name";
    private static final String SOME_OTHER_POLICY_DISPLAY_NAME = "Another policy display name";

    private static final String SOME_POLICY_DESCRIPTION = "Policy description";
    private static final String SOME_POLICY_DURATION = "1Y0M0D";
    private static final String SOME_POLICY_START_DATE_IN_FUTURE = "2124-01-01T00:00:00Z";
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

    @AfterEach
    void tearDown() {
        clearCreatedRetentionPolicyTypes();
    }

    @Test
    void reviseRetentionPolicyTypeShouldSucceedAndCreatePolicyWhenPriorPolicyExistsWithSameFixedPolicyKey() throws Exception {
        // Given
        UserAccountEntity userAccountEntity = superAdminUserStub.givenUserIsAuthorised(userIdentity);

        RetentionPolicyTypeEntity priorPolicy = createAndSavePolicyType(SOME_FIXED_POLICY_KEY, SOME_POLICY_NAME, SOME_POLICY_DISPLAY_NAME,
                                                                        SOME_POLICY_START_DATE_IN_PAST);

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
                                           SOME_POLICY_START_DATE_IN_FUTURE)
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
            .andExpect(jsonPath("$.policy_start_at").value(SOME_POLICY_START_DATE_IN_FUTURE))
            .andReturn();

        // And assert database state
        var id = new JSONObject(result.getResponse().getContentAsString())
            .getInt("id");
        transactionTemplate.executeWithoutResult(status -> {
            var createdPolicyTypeEntity = retentionPolicyTypeRepository.findById(id)
                .orElseThrow();

            // Verify state of new policy
            assertEquals(SOME_POLICY_NAME, createdPolicyTypeEntity.getPolicyName());
            assertEquals(SOME_POLICY_DISPLAY_NAME, createdPolicyTypeEntity.getDisplayName());
            assertEquals(SOME_POLICY_DESCRIPTION, createdPolicyTypeEntity.getDescription());
            assertEquals(SOME_FIXED_POLICY_KEY, createdPolicyTypeEntity.getFixedPolicyKey());
            assertEquals(SOME_POLICY_DURATION, createdPolicyTypeEntity.getDuration());
            assertEquals(OffsetDateTime.parse(SOME_POLICY_START_DATE_IN_FUTURE), createdPolicyTypeEntity.getPolicyStart());
            assertNull(createdPolicyTypeEntity.getPolicyEnd());

            Integer userId = userAccountEntity.getId();
            assertEquals(userId, createdPolicyTypeEntity.getLastModifiedById());
            assertEquals(userId, createdPolicyTypeEntity.getCreatedById());

            assertNotNull(createdPolicyTypeEntity.getLastModifiedDateTime());
            assertNotNull(createdPolicyTypeEntity.getCreatedById());

            // Verify state of prior policy
            Optional<RetentionPolicyTypeEntity> updatedPriorPolicy = dartsDatabase.getRetentionPolicyTypeRepository().findById(priorPolicy.getId());
            assertEquals(OffsetDateTime.parse(SOME_POLICY_START_DATE_IN_FUTURE), updatedPriorPolicy.orElseThrow().getPolicyEnd());
        });
    }

    @Test
    void reviseRetentionPolicyTypeShouldFailWhenFixedPolicyKeyIsUnique() throws Exception {
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
                                           SOME_POLICY_START_DATE_IN_FUTURE)
                ));

        // Then
        resultActions
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.type").value("RETENTION_115"))
            .andExpect(jsonPath("$.title").value("Fixed policy key not recognised"));
    }

    @Test
    void reviseRetentionPolicyTypeShouldFailWhenPolicyNameExistsForOtherFixedPolicyKey() throws Exception {
        // Given
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        createAndSavePolicyType(SOME_FIXED_POLICY_KEY, SOME_POLICY_NAME, SOME_POLICY_DISPLAY_NAME, SOME_POLICY_START_DATE_IN_PAST);
        createAndSavePolicyType(SOME_OTHER_FIXED_POLICY_KEY, SOME_OTHER_POLICY_NAME, SOME_OTHER_POLICY_DISPLAY_NAME, SOME_POLICY_START_DATE_IN_PAST);

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
                                           SOME_OTHER_POLICY_DISPLAY_NAME,
                                           SOME_POLICY_DESCRIPTION,
                                           SOME_OTHER_FIXED_POLICY_KEY,
                                           SOME_POLICY_DURATION,
                                           SOME_POLICY_START_DATE_IN_FUTURE)
                ));

        // Then
        resultActions
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.type").value("RETENTION_109"))
            .andExpect(jsonPath("$.title").value("Policy name must be unique"));
    }

    @Test
    void reviseRetentionPolicyTypeShouldFailWhenDisplayNameExistsForOtherFixedPolicyKey() throws Exception {
        // Given
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        createAndSavePolicyType(SOME_FIXED_POLICY_KEY, SOME_POLICY_NAME, SOME_POLICY_DISPLAY_NAME, SOME_POLICY_START_DATE_IN_PAST);
        createAndSavePolicyType(SOME_OTHER_FIXED_POLICY_KEY, SOME_OTHER_POLICY_NAME, SOME_OTHER_POLICY_DISPLAY_NAME, SOME_POLICY_START_DATE_IN_PAST);

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
                             """.formatted(SOME_OTHER_POLICY_NAME,
                                           SOME_POLICY_DISPLAY_NAME,
                                           SOME_POLICY_DESCRIPTION,
                                           SOME_OTHER_FIXED_POLICY_KEY,
                                           SOME_POLICY_DURATION,
                                           SOME_POLICY_START_DATE_IN_FUTURE)
                ));

        // Then
        resultActions
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.type").value("RETENTION_110"))
            .andExpect(jsonPath("$.title").value("Display name must be unique"));
    }

    @Test
    void reviseRetentionPolicyTypeShouldFailWhenPolicyStartDateIsInThePast() throws Exception {
        // Given
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        createAndSavePolicyType(SOME_FIXED_POLICY_KEY, SOME_POLICY_NAME, SOME_POLICY_DISPLAY_NAME, SOME_POLICY_START_DATE_IN_PAST);

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

    @Test
    void reviseRetentionPolicyTypeShouldFailWhenPriorPolicyStartDateIsInTheFuture() throws Exception {
        // Given
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        createAndSavePolicyType(SOME_FIXED_POLICY_KEY, SOME_POLICY_NAME, SOME_POLICY_DISPLAY_NAME, SOME_POLICY_START_DATE_IN_FUTURE);

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
                                           SOME_POLICY_START_DATE_IN_FUTURE)
                ));

        // Then
        resultActions
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.type").value("RETENTION_113"))
            .andExpect(jsonPath("$.title").value("To revise a policy, the start date of the prior revision must be in the past"));
    }

    private MockHttpServletRequestBuilder buildRequest() {
        return post("/admin/retention-policy-types")
            .header("Content-Type", "application/json")
            .queryParam("is_revision", "true");
    }

    private RetentionPolicyTypeEntity createAndSavePolicyType(String fixedPolicyKey, String name, String displayName, String startDateTime) {
        RetentionPolicyTypeEntity retentionPolicyTypeEntity = new RetentionPolicyTypeEntity();
        retentionPolicyTypeEntity.setFixedPolicyKey(fixedPolicyKey);
        retentionPolicyTypeEntity.setPolicyName(name);
        retentionPolicyTypeEntity.setDisplayName(displayName);
        retentionPolicyTypeEntity.setDescription(SOME_POLICY_DESCRIPTION);
        retentionPolicyTypeEntity.setDuration(SOME_POLICY_DURATION);
        retentionPolicyTypeEntity.setPolicyStart(OffsetDateTime.parse(startDateTime));
        retentionPolicyTypeEntity.setPolicyEnd(null);

        retentionPolicyTypeEntity.setCreatedBy(userIdentity.getUserAccount());
        retentionPolicyTypeEntity.setLastModifiedBy(userIdentity.getUserAccount());

        return dartsDatabase.getRetentionPolicyTypeRepository()
            .save(retentionPolicyTypeEntity);
    }

    private void clearCreatedRetentionPolicyTypes() {
        RetentionPolicyTypeEntity exampleEntity = new RetentionPolicyTypeEntity();
        exampleEntity.setDescription(SOME_POLICY_DESCRIPTION);

        List<RetentionPolicyTypeEntity> entitiesForDeletion = dartsDatabase.getRetentionPolicyTypeRepository()
            .findAll(Example.of(exampleEntity));

        dartsDatabase.getRetentionPolicyTypeRepository()
            .deleteAllById(entitiesForDeletion.stream()
                               .map(RetentionPolicyTypeEntity::getId)
                               .toList());
    }

}
