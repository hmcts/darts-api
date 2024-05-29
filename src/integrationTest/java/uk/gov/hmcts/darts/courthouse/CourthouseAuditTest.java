package uk.gov.hmcts.darts.courthouse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.courthouse.model.CourthousePatch;
import uk.gov.hmcts.darts.courthouse.model.CourthousePost;
import uk.gov.hmcts.darts.courthouse.service.CourthouseService;
import uk.gov.hmcts.darts.testutils.GivenBuilder;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.data.history.RevisionMetadata.RevisionType.INSERT;
import static org.springframework.data.history.RevisionMetadata.RevisionType.UPDATE;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;
import static uk.gov.hmcts.darts.test.common.data.CourthouseTestData.someMinimalCourthouse;

class CourthouseAuditTest extends IntegrationBase {

    @Autowired
    private CourthouseService courthouseService;

    @Autowired
    private GivenBuilder given;

    @Test
    void auditsWhenCourthousesAreCreated() {
        var userAccountEntity = given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);

        var courthouse = someMinimalCourthouse();
        var courthouseAndGroups = courthouseService.createCourthouseAndGroups(
            new CourthousePost()
                .courthouseName(courthouse.getCourthouseName())
                .displayName(courthouse.getDisplayName()));

        var audits = dartsDatabase.findAudits();
        assertThat(audits).extracting("user.id").containsExactly(userAccountEntity.getId());
        assertThat(audits).extracting("auditActivity.name").containsExactly("Create Courthouse");

        var courthouseRevisions = dartsDatabase.findCourthouseRevisionsFor(courthouseAndGroups.getId());

        assertThat(courthouseRevisions.getLatestRevision().getMetadata().getRevisionType()).isEqualTo(INSERT);
        assertThat(courthouseRevisions.getLatestRevision().getEntity().getId()).isEqualTo(courthouseAndGroups.getId());
    }

    @Test
    void auditsWhenCourthousesAreUpdated() {
        var userAccountEntity = given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);

        var courthouse = someMinimalCourthouse();
        var initialCourthouse = courthouseService.createCourthouseAndGroups(
            new CourthousePost()
                .courthouseName(courthouse.getCourthouseName())
                .displayName(courthouse.getDisplayName()));

        var courthouseAndGroups = courthouseService.updateCourthouse(
            initialCourthouse.getId(),
            new CourthousePatch().displayName("some-other-display-name"));

        var audits = dartsDatabase.findAudits();
        assertThat(audits).extracting("user.id").containsExactly(userAccountEntity.getId(), userAccountEntity.getId());
        assertThat(audits).extracting("auditActivity.name").contains("Update Courthouse");

        var courthouseRevisions = dartsDatabase.findCourthouseRevisionsFor(courthouseAndGroups.getId());

        assertThat(courthouseRevisions.getLatestRevision().getMetadata().getRevisionType()).isEqualTo(UPDATE);
        assertThat(courthouseRevisions.getLatestRevision().getEntity().getId()).isEqualTo(courthouseAndGroups.getId());
    }

    @Test
    void auditsWhenCourthousesGroupsAreUpdated() {
        var userAccountEntity = given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);

        var courthouse = someMinimalCourthouse();
        var initialCourthouse = courthouseService.createCourthouseAndGroups(
            new CourthousePost()
                .courthouseName(courthouse.getCourthouseName())
                .displayName(courthouse.getDisplayName()));

        var courthouseAndGroups = courthouseService.updateCourthouse(
            initialCourthouse.getId(),
            new CourthousePatch().securityGroupIds(List.of(2)));

        var audits = dartsDatabase.findAudits();
        assertThat(audits).extracting("user.id").containsExactly(userAccountEntity.getId(), userAccountEntity.getId());
        assertThat(audits).extracting("auditActivity.name").contains("Update Courthouse Group");

        var courthouseRevisions = dartsDatabase.findCourthouseRevisionsFor(courthouseAndGroups.getId());

        assertThat(courthouseRevisions.getLatestRevision().getMetadata().getRevisionType()).isEqualTo(UPDATE);
        assertThat(courthouseRevisions.getLatestRevision().getEntity().getId()).isEqualTo(courthouseAndGroups.getId());
    }

    @Test
    void doesntAuditWhenValidationFails() {
        given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);

        assertThatThrownBy(() -> courthouseService.createCourthouseAndGroups(
            new CourthousePost().courthouseName("some-courthouse").displayName("some-display-name").regionId(-1)));
        assertThat(dartsDatabase.findAudits()).isEmpty();
    }

}
