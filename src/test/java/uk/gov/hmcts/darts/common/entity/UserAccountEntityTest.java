package uk.gov.hmcts.darts.common.entity;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.darts.common.enums.SecurityGroupEnum.MEDIA_IN_PERPETUITY;
import static uk.gov.hmcts.darts.common.enums.SecurityGroupEnum.SUPER_ADMIN;
import static uk.gov.hmcts.darts.common.enums.SecurityGroupEnum.SUPER_USER;
import static uk.gov.hmcts.darts.test.common.data.SecurityGroupTestData.minimalSecurityGroup;
import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.minimalUserAccount;

class UserAccountEntityTest {

    @Test
    void reportsCorrectlyIfUserIsSuperUser() {
        var userAccountEntity = minimalUserAccount();
        var securityGroupEntity = minimalSecurityGroup();
        securityGroupEntity.setGroupName(SUPER_USER.getName());
        userAccountEntity.getSecurityGroupEntities().add(securityGroupEntity);

        assertTrue(userAccountEntity.isInGroup(List.of(SUPER_USER)));
        assertFalse(userAccountEntity.isInGroup(List.of(MEDIA_IN_PERPETUITY)));
    }

    @Test
    void reportsCorrectlyIfUserIsSuperAdmin() {
        var userAccountEntity = minimalUserAccount();
        var securityGroupEntity = minimalSecurityGroup();
        securityGroupEntity.setGroupName(SUPER_ADMIN.getName());
        userAccountEntity.getSecurityGroupEntities().add(securityGroupEntity);

        assertFalse(userAccountEntity.isInGroup(List.of(SUPER_USER)));
        assertFalse(userAccountEntity.isInGroup(List.of(MEDIA_IN_PERPETUITY)));
        assertTrue(userAccountEntity.isInGroup(List.of(SUPER_ADMIN)));
    }

    @Test
    void reportsCorrectlyIfUserIsMediaInPerpetuityGroup() {
        var userAccountEntity = minimalUserAccount();
        var securityGroupEntity = minimalSecurityGroup();
        securityGroupEntity.setGroupName(MEDIA_IN_PERPETUITY.getName());
        userAccountEntity.getSecurityGroupEntities().add(securityGroupEntity);

        assertFalse(userAccountEntity.isInGroup(List.of(SUPER_USER)));
        assertTrue(userAccountEntity.isInGroup(List.of(MEDIA_IN_PERPETUITY)));
        assertFalse(userAccountEntity.isInGroup(List.of(SUPER_ADMIN)));
    }

}