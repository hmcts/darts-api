package uk.gov.hmcts.darts.common.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.darts.common.enums.SecurityGroupEnum.MEDIA_IN_PERPETUITY;
import static uk.gov.hmcts.darts.common.enums.SecurityGroupEnum.SUPER_USER;
import static uk.gov.hmcts.darts.test.common.data.SecurityGroupTestData.minimalSecurityGroup;
import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.minimalUserAccount;

class UserAccountEntityTest {

    @Test
    void reportsCorrectlyIfUserIsInGroup() {
        var userAccountEntity = minimalUserAccount();
        var securityGroupEntity = minimalSecurityGroup();
        securityGroupEntity.setGroupName(SUPER_USER.getName());
        userAccountEntity.getSecurityGroupEntities().add(securityGroupEntity);

        assertTrue(userAccountEntity.isInGroup(SUPER_USER));
        assertFalse(userAccountEntity.isInGroup(MEDIA_IN_PERPETUITY));
    }

}