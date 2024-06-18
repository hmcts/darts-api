package uk.gov.hmcts.darts.retention.auditing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.darts.retentions.model.AdminPatchRetentionRequest;
import uk.gov.hmcts.darts.test.common.ParameterisedTestDataGenerator;

import java.util.List;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.darts.audit.api.AuditActivity.EDIT_RETENTION_POLICY;
import static uk.gov.hmcts.darts.retention.auditing.RetentionPolicyUpdateAuditActivityProvider.auditActivitiesFor;
import static uk.gov.hmcts.darts.test.common.data.RetentionPolicyTestData.minimalRetentionPolicy;

class RetentionPolicyUpdateAuditActivityProviderTest {

    @ParameterizedTest
    @MethodSource("patchCombinationsExceptAllNull")
    void identifiesUpdatesToBasicFields(String name, String displayName, String duration, String description, String fixedPolicyKeyIsUpdated) {
        var prePatchedEntity = minimalRetentionPolicy();
        var patch = new AdminPatchRetentionRequest()
            .name(name)
            .displayName(displayName)
            .duration(duration)
            .description(description)
            .fixedPolicyKey(fixedPolicyKeyIsUpdated);

        var activityProvider = auditActivitiesFor(prePatchedEntity, patch);

        assertThat(activityProvider.getAuditActivities()).containsExactly(EDIT_RETENTION_POLICY);
    }

    @Test
    void reportsNoChangesWhenAllBasicPatchValuesAreNull() {
        var prePatchedEntity = minimalRetentionPolicy();
        var patch = new AdminPatchRetentionRequest()
            .name(null)
            .displayName(null)
            .duration(null)
            .description(null)
            .fixedPolicyKey(null);

        var activityProvider = auditActivitiesFor(prePatchedEntity, patch);

        assertThat(activityProvider.getAuditActivities()).isEmpty();
    }

    static Stream<Arguments> patchCombinationsExceptAllNull() {
        List<List<String>> params = asList(
            asList("some-new-name", null),
            asList("some-new-display-name", null),
            asList("some-new-duration", null),
            asList("some-new-description", null),
            asList("some-new-fixed-policy-key", null)
        );
        return ParameterisedTestDataGenerator.generateCombinationsExcludingAllNull(params);
    }

}