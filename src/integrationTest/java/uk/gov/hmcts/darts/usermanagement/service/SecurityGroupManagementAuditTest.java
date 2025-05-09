package uk.gov.hmcts.darts.usermanagement.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.AuditEntity;
import uk.gov.hmcts.darts.testutils.GivenBuilder;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroupPatch;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroupPostRequest;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroupWithIdAndRole;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.history.RevisionMetadata.RevisionType.INSERT;
import static org.springframework.data.history.RevisionMetadata.RevisionType.UPDATE;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSCRIBER;
import static uk.gov.hmcts.darts.test.common.data.SecurityGroupTestData.createGroupForRole;
import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.minimalUserAccount;

class SecurityGroupManagementAuditTest extends IntegrationBase {

    @Autowired
    private SecurityGroupService securityGroupService;

    @Autowired
    private GivenBuilder given;

    @Test
    void auditsWhenSecurityGroupsAreCreated() {
        var userAccountEntity = given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);
        var securityGroup = createSecurityGroup();
        transactionalUtil.executeInTransaction(() -> {
            var createGroupActivity = findAuditActivity("Create Group", dartsDatabase.findAudits());
            assertThat(createGroupActivity.getUser().getId()).isEqualTo(userAccountEntity.getId());

            var userAccountRevisions = dartsDatabase.findSecurityGroupRevisionsFor(securityGroup.getId());
            assertThat(userAccountRevisions.getLatestRevision().getMetadata().getRevisionType()).isEqualTo(INSERT);
        });
    }

    @Test
    void auditsWhenSecurityGroupsAreUpdated() {
        var userAccountEntity = given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);
        var securityGroup = createSecurityGroup();

        securityGroupService.modifySecurityGroup(
            securityGroup.getId(),
            new SecurityGroupPatch().name("some-new-security-group-name"));
        transactionalUtil.executeInTransaction(() -> {
            var updateGroupActivity = findAuditActivity("Update Group", dartsDatabase.findAudits());
            assertThat(updateGroupActivity.getUser().getId()).isEqualTo(userAccountEntity.getId());

            var securityGroupRevisions = dartsDatabase.findSecurityGroupRevisionsFor(securityGroup.getId());
            assertThat(securityGroupRevisions.getLatestRevision().getMetadata().getRevisionType()).isEqualTo(UPDATE);
        });
    }

    @Test
    void auditsWhenSecurityGroupsMembersAreUpdated() {
        var userAccountEntity = given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);
        var securityGroup = dartsDatabase.save(createGroupForRole(TRANSCRIBER));
        var userAccount = dartsDatabase.save(minimalUserAccount());

        securityGroupService.modifySecurityGroup(
            securityGroup.getId(),
            new SecurityGroupPatch().userIds(List.of(userAccount.getId())));

        transactionalUtil.executeInTransaction(() -> {
            var audits = dartsDatabase.findAudits();
            assertThat(audits).extracting("user.id").containsExactly(userAccountEntity.getId());
            assertThat(audits).extracting("auditActivity.name").containsExactly("Update Users Group");
        });
    }

    private SecurityGroupWithIdAndRole createSecurityGroup() {
        return securityGroupService.createSecurityGroup(
            new SecurityGroupPostRequest()
                .name("some-security-group-name")
                .displayName("some-security-group-display-name")
                .securityRoleId(4));
    }

    private AuditEntity findAuditActivity(String activity, List<AuditEntity> audits) {
        return audits.stream()
            .filter(audit -> activity.equals(audit.getAuditActivity().getName()))
            .findFirst().orElseThrow();
    }

}
