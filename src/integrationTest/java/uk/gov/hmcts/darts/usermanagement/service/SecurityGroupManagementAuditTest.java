package uk.gov.hmcts.darts.usermanagement.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.testutils.GivenBuilder;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroupPatch;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroupPostRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSCRIBER;
import static uk.gov.hmcts.darts.test.common.data.SecurityGroupTestData.buildGroupForRole;
import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.minimalUserAccount;

class SecurityGroupManagementAuditTest extends IntegrationBase {

    @Autowired
    private SecurityGroupService securityGroupService;

    @Autowired
    private GivenBuilder given;

    @Test
    void auditsWhenSecurityGroupsAreCreated() {
        var userAccountEntity = given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);

        securityGroupService.createSecurityGroup(
            new SecurityGroupPostRequest()
                .name("some-security-group-name")
                .displayName("some-security-group-display-name")
                .securityRoleId(4));

        var audits = dartsDatabase.findAudits();
        assertThat(audits).extracting("user.id").containsExactly(userAccountEntity.getId());
        assertThat(audits).extracting("auditActivity.name").containsExactly("Create Group");
    }

    @Test
    void auditsWhenSecurityGroupsAreUpdated() {
        var userAccountEntity = given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);

        var securityGroup = dartsDatabase.save(buildGroupForRole(TRANSCRIBER));

        securityGroupService.modifySecurityGroup(
            securityGroup.getId(),
            new SecurityGroupPatch().name("some-new-security-group-name"));

        var audits = dartsDatabase.findAudits();
        assertThat(audits).extracting("user.id").containsExactly(userAccountEntity.getId());
        assertThat(audits).extracting("auditActivity.name").containsExactly("Update Group");
    }

    @Test
    void auditsWhenSecurityGroupsMembersAreUpdated() {
        var userAccountEntity = given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);

        var securityGroup = dartsDatabase.save(buildGroupForRole(TRANSCRIBER));
        var userAccount = dartsDatabase.save(minimalUserAccount());

        securityGroupService.modifySecurityGroup(
            securityGroup.getId(),
            new SecurityGroupPatch().userIds(List.of(userAccount.getId())));

        var audits = dartsDatabase.findAudits();
        assertThat(audits).extracting("user.id").containsExactly(userAccountEntity.getId());
        assertThat(audits).extracting("auditActivity.name").containsExactly("Update Users Group");
    }

}
