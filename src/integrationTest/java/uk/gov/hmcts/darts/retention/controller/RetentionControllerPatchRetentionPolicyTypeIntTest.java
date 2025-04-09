package uk.gov.hmcts.darts.retention.controller;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.domain.Example;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.RetentionPolicyTypeEntity;
import uk.gov.hmcts.darts.common.repository.RetentionPolicyTypeRepository;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.SuperAdminUserStub;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class RetentionControllerPatchRetentionPolicyTypeIntTest extends PostgresIntegrationBase {

    private static final String POLICY_A_NAME = "Policy A";
    private static final String POLICY_B_NAME = "Policy B";
    private static final String SOME_POLICY_DESCRIPTION = "Policy description";
    private static final String SOME_POLICY_DURATION = "1Y0M0D";
    private static final String SOME_PAST_DATE_TIME = "2000-01-01T00:00:00Z";
    private static final String SOME_FUTURE_DATE_TIME = "2100-01-01T00:00:00Z";
    private static final String SOME_OTHER_FUTURE_DATE_TIME = "2110-01-01T00:00:00Z";

    @Autowired
    private RetentionPolicyTypeRepository retentionPolicyTypeRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SuperAdminUserStub superAdminUserStub;

    @MockitoBean
    private UserIdentity userIdentity;

    @AfterEach
    void tearDown() {
        clearCreatedRetentionPolicyTypes();
    }

    @Test
    void patchRetentionPolicyTypeShouldSucceedWhenStartDateIsChangedOnInactiveNewPolicy() throws Exception {
        // Given
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        final RetentionPolicyTypeEntity inactivePolicy = createAndSavePolicyType(POLICY_A_NAME,
                                                                                 SOME_FUTURE_DATE_TIME, null,
                                                                                 "100");

        // When
        ResultActions resultActions = mockMvc.perform(
            buildRequest(inactivePolicy.getId())
                .content("""
                             {
                               "policy_start_at": "%s"
                             }
                             """.formatted(SOME_OTHER_FUTURE_DATE_TIME)
                ));

        // Then
        resultActions
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.name").value(POLICY_A_NAME))
            .andExpect(jsonPath("$.display_name").value(POLICY_A_NAME))
            .andExpect(jsonPath("$.description").value(SOME_POLICY_DESCRIPTION))
            .andExpect(jsonPath("$.fixed_policy_key").value(100))
            .andExpect(jsonPath("$.duration").value(SOME_POLICY_DURATION))
            .andExpect(jsonPath("$.policy_start_at").value(SOME_OTHER_FUTURE_DATE_TIME));

        // And assert database state
        var updatedRetentionPolicy = retentionPolicyTypeRepository.findById(inactivePolicy.getId())
            .orElseThrow();
        assertEquals(OffsetDateTime.parse(SOME_OTHER_FUTURE_DATE_TIME), updatedRetentionPolicy.getPolicyStart());
        assertNull(inactivePolicy.getPolicyEnd());
        assertEquals("100", inactivePolicy.getFixedPolicyKey());
    }

    @Test
    void patchRetentionPolicyTypeShouldSucceedWhenStartDateIsChangedOnPendingRevision() throws Exception {
        // Given
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        final RetentionPolicyTypeEntity livePolicy = createAndSavePolicyType(POLICY_A_NAME,
                                                                             SOME_PAST_DATE_TIME, SOME_FUTURE_DATE_TIME,
                                                                             "100");
        final RetentionPolicyTypeEntity pendingRevision = createAndSavePolicyType(POLICY_A_NAME,
                                                                                  SOME_FUTURE_DATE_TIME, null,
                                                                                  "100");

        // When
        ResultActions resultActions = mockMvc.perform(
            buildRequest(pendingRevision.getId())
                .content("""
                             {
                               "policy_start_at": "%s"
                             }
                             """.formatted(SOME_OTHER_FUTURE_DATE_TIME)
                ));

        // Then
        resultActions
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.name").value(POLICY_A_NAME))
            .andExpect(jsonPath("$.display_name").value(POLICY_A_NAME))
            .andExpect(jsonPath("$.description").value(SOME_POLICY_DESCRIPTION))
            .andExpect(jsonPath("$.fixed_policy_key").value(100))
            .andExpect(jsonPath("$.duration").value(SOME_POLICY_DURATION))
            .andExpect(jsonPath("$.policy_start_at").value(SOME_OTHER_FUTURE_DATE_TIME));

        // And assert database state
        var updatedLivePolicy = retentionPolicyTypeRepository.findById(livePolicy.getId())
            .orElseThrow();
        assertEquals(OffsetDateTime.parse(SOME_PAST_DATE_TIME), updatedLivePolicy.getPolicyStart());
        assertEquals(OffsetDateTime.parse(SOME_OTHER_FUTURE_DATE_TIME), updatedLivePolicy.getPolicyEnd());

        var updatedPendingRevision = retentionPolicyTypeRepository.findById(pendingRevision.getId())
            .orElseThrow();
        assertEquals(OffsetDateTime.parse(SOME_OTHER_FUTURE_DATE_TIME), updatedPendingRevision.getPolicyStart());
        assertNull(updatedPendingRevision.getPolicyEnd());
    }

    @Test
    void patchRetentionPolicyTypeShouldSucceedWhenFixedPolicyKeyIsChanged_Scenario1() throws Exception {
        // Given
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        final RetentionPolicyTypeEntity pendingPolicy100 = createAndSavePolicyType(POLICY_A_NAME,
                                                                                   SOME_FUTURE_DATE_TIME, null,
                                                                                   "100");

        // When
        ResultActions resultActions = mockMvc.perform(
            buildRequest(pendingPolicy100.getId())
                .content("""
                             {
                               "fixed_policy_key": "%s"
                             }
                             """.formatted(200)
                ));

        // Then
        resultActions
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.name").value(POLICY_A_NAME))
            .andExpect(jsonPath("$.display_name").value(POLICY_A_NAME))
            .andExpect(jsonPath("$.description").value(SOME_POLICY_DESCRIPTION))
            .andExpect(jsonPath("$.fixed_policy_key").value(200))
            .andExpect(jsonPath("$.duration").value(SOME_POLICY_DURATION))
            .andExpect(jsonPath("$.policy_start_at").value(SOME_FUTURE_DATE_TIME));

        // And assert database state
        var updatedPendingPolicy = retentionPolicyTypeRepository.findById(pendingPolicy100.getId())
            .orElseThrow();
        assertEquals(OffsetDateTime.parse(SOME_FUTURE_DATE_TIME), updatedPendingPolicy.getPolicyStart());
        assertNull(updatedPendingPolicy.getPolicyEnd());
        assertEquals("200", updatedPendingPolicy.getFixedPolicyKey());
    }

    @Test
    void patchRetentionPolicyTypeShouldFailWhenFixedPolicyKeyIsChanged_Scenario2() throws Exception {
        // Given
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        createAndSavePolicyType(POLICY_A_NAME,
                                SOME_FUTURE_DATE_TIME, null,
                                "100");
        final RetentionPolicyTypeEntity pendingPolicy200 = createAndSavePolicyType(POLICY_B_NAME,
                                                                                   SOME_OTHER_FUTURE_DATE_TIME, null,
                                                                                   "200");

        // When
        ResultActions resultActions = mockMvc.perform(
            buildRequest(pendingPolicy200.getId())
                .content("""
                             {
                               "fixed_policy_key": "%s"
                             }
                             """.formatted(100)
                ));

        // Then
        resultActions
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.type").value("RETENTION_114"))
            .andExpect(jsonPath("$.title").value("Fixed policy key must be unique"));
    }

    @Test
    void patchRetentionPolicyTypeShouldSucceedWhenFixedPolicyKeyIsChanged_Scenario3() throws Exception {
        // Given
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        createAndSavePolicyType(POLICY_A_NAME,
                                SOME_PAST_DATE_TIME, null,
                                "100");
        createAndSavePolicyType(POLICY_B_NAME,
                                SOME_PAST_DATE_TIME, SOME_FUTURE_DATE_TIME,
                                "200");
        final RetentionPolicyTypeEntity pendingRevisionPolicy200 = createAndSavePolicyType(POLICY_B_NAME,
                                                                                           SOME_FUTURE_DATE_TIME, null,
                                                                                           "200");

        // When
        // Note names must be patched as part of this request to ensure they are unique amongst other fixed keys
        ResultActions resultActions = mockMvc.perform(
            buildRequest(pendingRevisionPolicy200.getId())
                .content("""
                             {
                                "name": "%s",
                                "display_name": "%s",
                                "fixed_policy_key": "%s"
                             }
                             """.formatted(POLICY_A_NAME,
                                           POLICY_A_NAME,
                                           100)
                ));

        // Then
        resultActions
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.type").value("RETENTION_114"))
            .andExpect(jsonPath("$.title").value("Fixed policy key must be unique"));
    }

    @Test
    void patchRetentionPolicyTypeShouldFailWhenFixedPolicyKeyAndStartDateIsChanged() throws Exception {
        // Given
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        createAndSavePolicyType(POLICY_A_NAME,
                                SOME_PAST_DATE_TIME, null,
                                "100");
        createAndSavePolicyType(POLICY_B_NAME,
                                SOME_PAST_DATE_TIME, SOME_FUTURE_DATE_TIME,
                                "200");
        final RetentionPolicyTypeEntity pendingRevisionPolicy200 = createAndSavePolicyType(POLICY_B_NAME,
                                                                                           SOME_FUTURE_DATE_TIME, null,
                                                                                           "200");

        // When
        // Note names must be patched as part of this request to ensure they are unique amongst other fixed keys
        ResultActions resultActions = mockMvc.perform(
            buildRequest(pendingRevisionPolicy200.getId())
                .content("""
                             {
                               "name": "%s",
                               "display_name": "%s",
                               "fixed_policy_key": "%s",
                               "policy_start_at": "%s"
                             }
                             """.formatted(POLICY_A_NAME,
                                           POLICY_A_NAME,
                                           100,
                                           SOME_OTHER_FUTURE_DATE_TIME)
                ));

        // Then
        resultActions
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.type").value("RETENTION_114"))
            .andExpect(jsonPath("$.title").value("Fixed policy key must be unique"));
    }

    @Test
    void patchRetentionPolicyTypeShouldSucceedWhenNameIsChanged() throws Exception {
        // Given
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        final RetentionPolicyTypeEntity inactivePolicy = createAndSavePolicyType(POLICY_A_NAME,
                                                                                 SOME_FUTURE_DATE_TIME, null,
                                                                                 "100");

        // When
        ResultActions resultActions = mockMvc.perform(
            buildRequest(inactivePolicy.getId())
                .content("""
                             {
                               "name": "%s"
                             }
                             """.formatted("Updated name")
                ));

        // Then
        resultActions
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.name").value("Updated name"))
            .andExpect(jsonPath("$.display_name").value(POLICY_A_NAME))
            .andExpect(jsonPath("$.description").value(SOME_POLICY_DESCRIPTION))
            .andExpect(jsonPath("$.fixed_policy_key").value(100))
            .andExpect(jsonPath("$.duration").value(SOME_POLICY_DURATION))
            .andExpect(jsonPath("$.policy_start_at").value(SOME_FUTURE_DATE_TIME));

        // And assert database state
        var updatedRetentionPolicy = retentionPolicyTypeRepository.findById(inactivePolicy.getId())
            .orElseThrow();
        assertEquals("Updated name", updatedRetentionPolicy.getPolicyName());
    }

    @Test
    void patchRetentionPolicyTypeShouldSucceedWhenDisplayNameIsChanged() throws Exception {
        // Given
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        final RetentionPolicyTypeEntity inactivePolicy = createAndSavePolicyType(POLICY_A_NAME,
                                                                                 SOME_FUTURE_DATE_TIME, null,
                                                                                 "100");

        // When
        ResultActions resultActions = mockMvc.perform(
            buildRequest(inactivePolicy.getId())
                .content("""
                             {
                               "display_name": "%s"
                             }
                             """.formatted("Updated display name")
                ));

        // Then
        resultActions
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.name").value(POLICY_A_NAME))
            .andExpect(jsonPath("$.display_name").value("Updated display name"))
            .andExpect(jsonPath("$.description").value(SOME_POLICY_DESCRIPTION))
            .andExpect(jsonPath("$.fixed_policy_key").value(100))
            .andExpect(jsonPath("$.duration").value(SOME_POLICY_DURATION))
            .andExpect(jsonPath("$.policy_start_at").value(SOME_FUTURE_DATE_TIME));

        // And assert database state
        var updatedRetentionPolicy = retentionPolicyTypeRepository.findById(inactivePolicy.getId())
            .orElseThrow();
        assertEquals("Updated display name", updatedRetentionPolicy.getDisplayName());
    }

    @Test
    void patchRetentionPolicyTypeShouldSucceedWhenDescriptionIsChanged() throws Exception {
        // Given
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        final RetentionPolicyTypeEntity inactivePolicy = createAndSavePolicyType(POLICY_A_NAME,
                                                                                 SOME_FUTURE_DATE_TIME, null,
                                                                                 "100");

        // When
        ResultActions resultActions = mockMvc.perform(
            buildRequest(inactivePolicy.getId())
                .content("""
                             {
                               "description": "%s"
                             }
                             """.formatted("Updated description")
                ));

        // Then
        resultActions
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.name").value(POLICY_A_NAME))
            .andExpect(jsonPath("$.display_name").value(POLICY_A_NAME))
            .andExpect(jsonPath("$.description").value("Updated description"))
            .andExpect(jsonPath("$.fixed_policy_key").value(100))
            .andExpect(jsonPath("$.duration").value(SOME_POLICY_DURATION))
            .andExpect(jsonPath("$.policy_start_at").value(SOME_FUTURE_DATE_TIME));

        // And assert database state
        var updatedRetentionPolicy = retentionPolicyTypeRepository.findById(inactivePolicy.getId())
            .orElseThrow();
        assertEquals("Updated description", updatedRetentionPolicy.getDescription());

        // Clean up
        retentionPolicyTypeRepository.delete(updatedRetentionPolicy);
    }

    @Test
    void patchRetentionPolicyTypeShouldSucceedWhenDescriptionIsChangedToEmptyString() throws Exception {
        // Given
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        final RetentionPolicyTypeEntity inactivePolicy = createAndSavePolicyType(POLICY_A_NAME,
                                                                                 SOME_FUTURE_DATE_TIME, null,
                                                                                 "100");

        // When
        ResultActions resultActions = mockMvc.perform(
            buildRequest(inactivePolicy.getId())
                .content("""
                             {
                               "description": ""
                             }
                             """
                ));

        // Then
        resultActions
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.name").value(POLICY_A_NAME))
            .andExpect(jsonPath("$.display_name").value(POLICY_A_NAME))
            .andExpect(jsonPath("$.description").value(""))
            .andExpect(jsonPath("$.fixed_policy_key").value(100))
            .andExpect(jsonPath("$.duration").value(SOME_POLICY_DURATION))
            .andExpect(jsonPath("$.policy_start_at").value(SOME_FUTURE_DATE_TIME));

        // And assert database state
        var updatedRetentionPolicy = retentionPolicyTypeRepository.findById(inactivePolicy.getId())
            .orElseThrow();
        assertEquals("", updatedRetentionPolicy.getDescription());

        // Clean up
        retentionPolicyTypeRepository.delete(updatedRetentionPolicy);
    }

    @Test
    void patchRetentionPolicyTypeShouldSucceedWhenDurationIsChanged() throws Exception {
        // Given
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        final RetentionPolicyTypeEntity inactivePolicy = createAndSavePolicyType(POLICY_A_NAME,
                                                                                 SOME_FUTURE_DATE_TIME, null,
                                                                                 "100");

        // When
        ResultActions resultActions = mockMvc.perform(
            buildRequest(inactivePolicy.getId())
                .content("""
                             {
                               "duration": "%s"
                             }
                             """.formatted("10Y0M0D")
                ));

        // Then
        resultActions
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.name").value(POLICY_A_NAME))
            .andExpect(jsonPath("$.display_name").value(POLICY_A_NAME))
            .andExpect(jsonPath("$.description").value(SOME_POLICY_DESCRIPTION))
            .andExpect(jsonPath("$.fixed_policy_key").value(100))
            .andExpect(jsonPath("$.duration").value("10Y0M0D"))
            .andExpect(jsonPath("$.policy_start_at").value(SOME_FUTURE_DATE_TIME));

        // And assert database state
        var updatedRetentionPolicy = retentionPolicyTypeRepository.findById(inactivePolicy.getId())
            .orElseThrow();
        assertEquals("10Y0M0D", updatedRetentionPolicy.getDuration());
    }

    @Test
    void patchRetentionPolicyTypeShouldFailIfAnyChangeIsAttemptedOnLivePolicy() throws Exception {
        // Given
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        final RetentionPolicyTypeEntity inactivePolicy = createAndSavePolicyType(POLICY_A_NAME,
                                                                                 SOME_PAST_DATE_TIME, null,
                                                                                 "100");

        // When
        ResultActions resultActions = mockMvc.perform(
            buildRequest(inactivePolicy.getId())
                .content("""
                             {
                               "name": "%s"
                             }
                             """.formatted("Updated name")
                ));

        // Then
        resultActions
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.type").value("RETENTION_116"))
            .andExpect(jsonPath("$.title").value("Live policies cannot be edited"));
    }

    @Test
    void patchRetentionPolicyTypeShouldFailWhenChangingToAFixedPolicyKeyThatExists() throws Exception {
        // Given
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        createAndSavePolicyType(POLICY_A_NAME,
                                SOME_PAST_DATE_TIME, null,
                                "100");
        createAndSavePolicyType(POLICY_B_NAME,
                                SOME_PAST_DATE_TIME, SOME_FUTURE_DATE_TIME,
                                "200");
        final RetentionPolicyTypeEntity pendingRevisionPolicy200 = createAndSavePolicyType(POLICY_B_NAME,
                                                                                           SOME_FUTURE_DATE_TIME, null,
                                                                                           "200");

        // When
        ResultActions resultActions = mockMvc.perform(
            buildRequest(pendingRevisionPolicy200.getId())
                .content("""
                             {
                                "fixed_policy_key": "%s"
                             }
                             """.formatted(100)
                ));

        // Then
        resultActions
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.type").value("RETENTION_114"))
            .andExpect(jsonPath("$.title").value("Fixed policy key must be unique"));
    }

    @Test
    void patchRetentionPolicyTypeShouldFailWhenStartDateIsChangedToDateInThePast() throws Exception {
        // Given
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        final RetentionPolicyTypeEntity inactivePolicy = createAndSavePolicyType(POLICY_A_NAME,
                                                                                 SOME_FUTURE_DATE_TIME, null,
                                                                                 "100");

        // When
        ResultActions resultActions = mockMvc.perform(
            buildRequest(inactivePolicy.getId())
                .content("""
                             {
                               "policy_start_at": "%s"
                             }
                             """.formatted(SOME_PAST_DATE_TIME)
                ));

        // Then
        resultActions
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.type").value("RETENTION_112"))
            .andExpect(jsonPath("$.title").value("The provided start date must be in the future"));
    }

    @Test
    void patchRetentionPolicyTypeShouldFailWhenChangingNameToNameThatExistsInOtherPolicies() throws Exception {
        // Given
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        createAndSavePolicyType(POLICY_A_NAME,
                                SOME_FUTURE_DATE_TIME, null,
                                "100");
        final RetentionPolicyTypeEntity inactivePolicy200 = createAndSavePolicyType(POLICY_B_NAME,
                                                                                    SOME_FUTURE_DATE_TIME, null,
                                                                                    "200");

        // When
        ResultActions resultActions = mockMvc.perform(
            buildRequest(inactivePolicy200.getId())
                .content("""
                             {
                                "name": "%s"
                             }
                             """.formatted(POLICY_A_NAME)
                ));

        // Then
        resultActions
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.type").value("RETENTION_109"))
            .andExpect(jsonPath("$.title").value("Policy name must be unique"));
    }

    @Test
    void patchRetentionPolicyTypeShouldFailWhenChangingDisplayNameToDisplayNameThatExistsInOtherPolicies() throws Exception {
        // Given
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        createAndSavePolicyType(POLICY_A_NAME,
                                SOME_FUTURE_DATE_TIME, null,
                                "100");
        final RetentionPolicyTypeEntity inactivePolicy200 = createAndSavePolicyType(POLICY_B_NAME,
                                                                                    SOME_FUTURE_DATE_TIME, null,
                                                                                    "200");

        // When
        ResultActions resultActions = mockMvc.perform(
            buildRequest(inactivePolicy200.getId())
                .content("""
                             {
                                "display_name": "%s"
                             }
                             """.formatted(POLICY_A_NAME)
                ));

        // Then
        resultActions
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.type").value("RETENTION_110"))
            .andExpect(jsonPath("$.title").value("Display name must be unique"));
    }

    @Test
    void patchRetentionPolicyTypeShouldFailWhenDurationIsChangedToInvalidValue() throws Exception {
        // Given
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        final RetentionPolicyTypeEntity inactivePolicy = createAndSavePolicyType(POLICY_A_NAME,
                                                                                 SOME_FUTURE_DATE_TIME, null,
                                                                                 "100");

        // When
        ResultActions resultActions = mockMvc.perform(
            buildRequest(inactivePolicy.getId())
                .content("""
                             {
                               "duration": "%s"
                             }
                             """.formatted("1Y")
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
    void patchRetentionPolicyTypeShouldFailWhenDurationIsTooShort() throws Exception {
        // Given
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        final RetentionPolicyTypeEntity inactivePolicy = createAndSavePolicyType(POLICY_A_NAME,
                                                                                 SOME_FUTURE_DATE_TIME, null,
                                                                                 "100");

        // When
        ResultActions resultActions = mockMvc.perform(
            buildRequest(inactivePolicy.getId())
                .content("""
                             {
                               "duration": "%s"
                             }
                             """.formatted("0Y0M0D")
                ));

        // Then
        resultActions
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.type").value("RETENTION_111"))
            .andExpect(jsonPath("$.title").value("Duration too short"))
            .andExpect(jsonPath("$.min_allowable_days").value(1));
    }

    @Test
    void patchRetentionPolicyTypeShouldFailWhenLongDescriptionIsProvided() throws Exception {
        // Given
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        final RetentionPolicyTypeEntity inactivePolicy = createAndSavePolicyType(POLICY_A_NAME,
                                                                                 SOME_FUTURE_DATE_TIME, null,
                                                                                 "100");

        int maxAllowableCharacters = 256;
        String longDescription = StringUtils.repeat("A", ++maxAllowableCharacters);

        // When
        ResultActions resultActions = mockMvc.perform(
            buildRequest(inactivePolicy.getId())
                .content("""
                             {
                               "description": "%s"
                             }
                             """.formatted(longDescription)
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
    void patchRetentionPolicyTypeShouldFailWhenFixedPolicyKeyIsChangedAndTargetKeyHasPendingRevision() throws Exception {
        // Given
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        final RetentionPolicyTypeEntity pendingPolicy100 = createAndSavePolicyType(POLICY_A_NAME,
                                                                                   SOME_OTHER_FUTURE_DATE_TIME, null,
                                                                                   "100");
        createAndSavePolicyType(POLICY_B_NAME,
                                SOME_PAST_DATE_TIME, SOME_FUTURE_DATE_TIME,
                                "200");
        final RetentionPolicyTypeEntity pendingPolicy200 = createAndSavePolicyType(POLICY_B_NAME,
                                                                                   SOME_FUTURE_DATE_TIME, null,
                                                                                   "200");

        // When
        // Note names must be patched as part of this request to ensure they are unique amongst other fixed keys
        ResultActions resultActions = mockMvc.perform(
            buildRequest(pendingPolicy100.getId())
                .content("""
                             {
                                "name": "%s",
                                "display_name": "%s",
                                "fixed_policy_key": "%s"
                             }
                             """.formatted(POLICY_B_NAME,
                                           POLICY_B_NAME,
                                           200)
                ));

        // Then
        resultActions
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.type").value("RETENTION_117"))
            .andExpect(jsonPath("$.title").value("Target policy has pending revision"))
            .andExpect(jsonPath("$.pending_revision_id").value(pendingPolicy200.getId()));
    }

    @Test
    void patchRetentionPolicyTypeShouldFailWhenUserIsNotSuperAdmin() throws Exception {
        // Given
        superAdminUserStub.givenUserIsNotAuthorised(userIdentity);

        final RetentionPolicyTypeEntity inactivePolicy = createAndSavePolicyType(POLICY_A_NAME,
                                                                                 SOME_FUTURE_DATE_TIME, null,
                                                                                 "100");

        // When
        ResultActions resultActions = mockMvc.perform(
            buildRequest(inactivePolicy.getId())
                .content("""
                             {
                               "name": "%s"
                             }
                             """.formatted("Updated name")
                ));

        // Then
        resultActions
            .andExpect(status().isForbidden());
    }

    private MockHttpServletRequestBuilder buildRequest(int policyId) {
        return patch("/admin/retention-policy-types/%d".formatted(policyId))
            .header("Content-Type", "application/json");
    }

    private RetentionPolicyTypeEntity createAndSavePolicyType(String name, String startDateTime, String endDateTime, String fixedPolicyKey) {
        RetentionPolicyTypeEntity retentionPolicyTypeEntity = new RetentionPolicyTypeEntity();
        retentionPolicyTypeEntity.setFixedPolicyKey(fixedPolicyKey);
        retentionPolicyTypeEntity.setPolicyName(name);
        retentionPolicyTypeEntity.setDisplayName(name);
        retentionPolicyTypeEntity.setDescription(SOME_POLICY_DESCRIPTION);
        retentionPolicyTypeEntity.setDuration(SOME_POLICY_DURATION);
        retentionPolicyTypeEntity.setPolicyStart(OffsetDateTime.parse(startDateTime));
        retentionPolicyTypeEntity.setPolicyEnd(endDateTime == null ? null : OffsetDateTime.parse(endDateTime));

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
