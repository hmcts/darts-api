package uk.gov.hmcts.darts.usermanagement.auditing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.hmcts.darts.test.common.ParameterisedTestDataGenerator;
import uk.gov.hmcts.darts.usermanagement.model.UserPatch;

import java.util.List;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.darts.audit.api.AuditActivity.DEACTIVATE_USER;
import static uk.gov.hmcts.darts.audit.api.AuditActivity.REACTIVATE_USER;
import static uk.gov.hmcts.darts.audit.api.AuditActivity.UPDATE_USER;
import static uk.gov.hmcts.darts.audit.api.AuditActivity.UPDATE_USERS_GROUP;
import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.minimalUserAccount;
import static uk.gov.hmcts.darts.usermanagement.auditing.UserAccountUpdateAuditActivityProvider.auditActivitiesFor;

class UserAccountUpdateAuditActivityProviderTest {

    @ParameterizedTest
    @MethodSource("patchCombinationsExceptAllNull")
    void identifiesUpdatesToBasicFields(String fullName, String description, String emailAddress) {
        var prePatchedEntity = minimalUserAccount();
        var patch = new UserPatch()
            .fullName(fullName)
            .description(description)
            .emailAddress(emailAddress);

        var activityProvider = auditActivitiesFor(prePatchedEntity, patch);

        assertThat(activityProvider.getAuditActivities()).containsExactly(UPDATE_USER);
    }

    @Test
    void reportsNoChangesWhenAllBasicPatchValuesAreNull() {
        var prePatchedEntity = minimalUserAccount();
        var patch = new UserPatch()
            .fullName(null)
            .description(null)
            .emailAddress(null);

        var activityProvider = auditActivitiesFor(prePatchedEntity, patch);

        assertThat(activityProvider.getAuditActivities()).isEmpty();
    }

    @Test
    void identifiesUserReactivation() {
        var prePatchedEntity = minimalUserAccount();
        prePatchedEntity.setActive(false);
        var patch = new UserPatch()
            .active(true);

        var activityProvider = auditActivitiesFor(prePatchedEntity, patch);

        assertThat(activityProvider.getAuditActivities()).containsExactly(REACTIVATE_USER);
    }

    @Test
    void identifiesUserDeactivation() {
        var prePatchedEntity = minimalUserAccount();
        prePatchedEntity.setActive(true);
        var patch = new UserPatch()
            .active(false);

        var activityProvider = auditActivitiesFor(prePatchedEntity, patch);

        assertThat(activityProvider.getAuditActivities()).containsExactly(DEACTIVATE_USER);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void doesntReportActivityWhenActivationStatusUnchanged(boolean activeStatus) {
        var prePatchedEntity = minimalUserAccount();
        prePatchedEntity.setActive(activeStatus);
        var patch = new UserPatch()
            .active(activeStatus);

        var activityProvider = auditActivitiesFor(prePatchedEntity, patch);

        assertThat(activityProvider.getAuditActivities()).isEmpty();
    }

    @Test
    void identifiesUpdatesUsersGroups() {
        var prePatchedEntity = minimalUserAccount();
        var patch = new UserPatch()
            .securityGroupIds(asList(1, 2, 3));

        var activityProvider = auditActivitiesFor(prePatchedEntity, patch);

        assertThat(activityProvider.getAuditActivities()).containsExactly(UPDATE_USERS_GROUP);
    }

    static Stream<Arguments> patchCombinationsExceptAllNull() {
        List<List<String>> params = asList(
            asList("some-name", null),
            asList("some-description", null),
            asList("some-email-address", null)
        );
        return ParameterisedTestDataGenerator.generateCombinationsExcludingAllNull(params);
    }

}