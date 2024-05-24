package uk.gov.hmcts.darts.courthouse.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.darts.courthouse.model.CourthousePatch;
import uk.gov.hmcts.darts.test.common.ParameterisedTestDataGenerator;

import java.util.List;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.darts.audit.api.AuditActivity.UPDATE_COURTHOUSE;
import static uk.gov.hmcts.darts.audit.api.AuditActivity.UPDATE_COURTHOUSE_GROUP;
import static uk.gov.hmcts.darts.courthouse.service.CourthouseUpdateAuditActivityProvider.auditActivitiesFor;
import static uk.gov.hmcts.darts.test.common.data.CourthouseTestData.someMinimalCourthouse;


class CourthouseUpdateAuditActivityProviderTest {

    @ParameterizedTest
    @MethodSource("patchCombinationsExceptAllNull")
    void identifiesUpdatesToBasicFields(String courthouseName, String displayName, String regionId) {
        var prePatchedEntity = someMinimalCourthouse();
        var patch = new CourthousePatch()
            .courthouseName(courthouseName)
            .displayName(displayName)
            .regionId(regionId == null ? null : Integer.valueOf(regionId));

        var activityProvider = auditActivitiesFor(patch, prePatchedEntity);

        assertThat(activityProvider.getAuditActivities()).containsExactly(UPDATE_COURTHOUSE);
    }

    @Test
    void reportsNoChangesWhenAllBasicPatchValuesAreNull() {
        var prePatchedEntity = someMinimalCourthouse();
        var patch = new CourthousePatch()
            .courthouseName(null)
            .displayName(null)
            .regionId(null);

        var activityProvider = auditActivitiesFor(patch, prePatchedEntity);

        assertThat(activityProvider.getAuditActivities()).isEmpty();
    }

    @Test
    void identifiesUpdatesToCourthousesGroups() {
        var prePatchedEntity = someMinimalCourthouse();
        var patch = new CourthousePatch()
            .securityGroupIds(asList(1, 2, 3));

        var activityProvider = auditActivitiesFor(patch, prePatchedEntity);

        assertThat(activityProvider.getAuditActivities()).containsExactly(UPDATE_COURTHOUSE_GROUP);
    }

    static Stream<Arguments> patchCombinationsExceptAllNull() {
        List<List<String>> params = asList(
            asList("some-description", null),
            asList("some-display-name", null),
            asList("1", null)
        );
        return ParameterisedTestDataGenerator.generateCombinationsExcludingAllNull(params);
    }


}