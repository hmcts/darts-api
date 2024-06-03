package uk.gov.hmcts.darts.usermanagement.auditing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.darts.test.common.ParameterisedTestDataGenerator;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroupPatch;

import java.util.List;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.darts.audit.api.AuditActivity.UPDATE_COURTHOUSE_GROUP;
import static uk.gov.hmcts.darts.audit.api.AuditActivity.UPDATE_GROUP;
import static uk.gov.hmcts.darts.audit.api.AuditActivity.UPDATE_USERS_GROUP;
import static uk.gov.hmcts.darts.test.common.data.SecurityGroupTestData.minimalSecurityGroup;
import static uk.gov.hmcts.darts.usermanagement.auditing.SecurityGroupUpdateAuditActivityProvider.auditActivitiesFor;

class SecurityGroupUpdateAuditActivityProviderTest {

    @ParameterizedTest
    @MethodSource("patchCombinationsExceptAllNull")
    void identifiesUpdatesToBasicFields(String description, String displayName, String name) {
        var prePatchedEntity = minimalSecurityGroup();
        var patch = new SecurityGroupPatch()
            .description(description)
            .displayName(displayName)
            .name(name);

        var activityProvider = auditActivitiesFor(prePatchedEntity, patch);

        assertThat(activityProvider.getAuditActivities()).containsExactly(UPDATE_GROUP);
    }

    @Test
    void reportsNoChangesWhenAllBasicPatchValuesAreNull() {
        var prePatchedEntity = minimalSecurityGroup();
        var patch = new SecurityGroupPatch()
            .description(null)
            .displayName(null)
            .name(null);

        var activityProvider = auditActivitiesFor(prePatchedEntity, patch);

        assertThat(activityProvider.getAuditActivities()).isEmpty();
    }

    @Test
    void identifiesUpdatesToUsersGroup() {
        var prePatchedEntity = minimalSecurityGroup();
        var patch = new SecurityGroupPatch()
            .userIds(asList(1, 2, 3));

        var activityProvider = auditActivitiesFor(prePatchedEntity, patch);

        assertThat(activityProvider.getAuditActivities()).containsExactly(UPDATE_USERS_GROUP);
    }

    @Test
    void identifiesUpdatesToCourthouseGroup() {
        var prePatchedEntity = minimalSecurityGroup();
        var patch = new SecurityGroupPatch()
            .courthouseIds(asList(1, 2, 3));

        var activityProvider = auditActivitiesFor(prePatchedEntity, patch);

        assertThat(activityProvider.getAuditActivities()).containsExactly(UPDATE_COURTHOUSE_GROUP);
    }

    static Stream<Arguments> patchCombinationsExceptAllNull() {
        List<List<String>> params = asList(
            asList("some-description", null),
            asList("some-display-name", null),
            asList("some-name", null)
        );
        return ParameterisedTestDataGenerator.generateCombinationsExcludingAllNull(params);
    }

}