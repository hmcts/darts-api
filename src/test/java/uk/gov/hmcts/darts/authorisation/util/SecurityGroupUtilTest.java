package uk.gov.hmcts.darts.authorisation.util;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.SecurityRoleEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityGroupUtilTest {

    @Test
    void matchesAtLeastOneSecurityGroup_false() {

        Set<SecurityGroupEntity> securityGroupEntities = getSecurityGroupEntities();


        List<SecurityRoleEnum> securityRoles = new ArrayList<>();
        securityRoles.add(SecurityRoleEnum.SUPER_ADMIN);

        assertFalse(SecurityGroupUtil.matchesAtLeastOneSecurityGroup(securityGroupEntities, securityRoles));
    }

    @Test
    void matchesAtLeastOneSecurityGroup_true_single() {

        Set<SecurityGroupEntity> securityGroupEntities = getSecurityGroupEntities();


        List<SecurityRoleEnum> securityRoles = new ArrayList<>();
        securityRoles.add(SecurityRoleEnum.JUDICIARY);

        assertTrue(SecurityGroupUtil.matchesAtLeastOneSecurityGroup(securityGroupEntities, securityRoles));
    }

    @Test
    void matchesAtLeastOneSecurityGroup_true_multiple() {

        Set<SecurityGroupEntity> securityGroupEntities = getSecurityGroupEntities();


        List<SecurityRoleEnum> securityRoles = new ArrayList<>();
        securityRoles.add(SecurityRoleEnum.JUDICIARY);
        securityRoles.add(SecurityRoleEnum.SUPER_ADMIN);

        assertTrue(SecurityGroupUtil.matchesAtLeastOneSecurityGroup(securityGroupEntities, securityRoles));
    }

    private static Set<SecurityGroupEntity> getSecurityGroupEntities() {
        SecurityRoleEntity securityRoleEntity1 = new SecurityRoleEntity();
        securityRoleEntity1.setId(1);
        securityRoleEntity1.setDisplayState(true);

        SecurityRoleEntity securityRoleEntity2 = new SecurityRoleEntity();
        securityRoleEntity2.setId(2);
        securityRoleEntity2.setDisplayState(true);

        SecurityRoleEntity securityRoleEntity3 = new SecurityRoleEntity();
        securityRoleEntity3.setId(3);
        securityRoleEntity3.setDisplayState(true);

        SecurityGroupEntity securityGroupEntity1 = new SecurityGroupEntity();
        securityGroupEntity1.setSecurityRoleEntity(securityRoleEntity1);
        securityGroupEntity1.setDisplayState(true);

        SecurityGroupEntity securityGroupEntity2 = new SecurityGroupEntity();
        securityGroupEntity2.setSecurityRoleEntity(securityRoleEntity2);
        securityGroupEntity2.setDisplayState(true);

        SecurityGroupEntity securityGroupEntity3 = new SecurityGroupEntity();
        securityGroupEntity3.setSecurityRoleEntity(securityRoleEntity3);
        securityGroupEntity3.setDisplayState(true);

        Set<SecurityGroupEntity> securityGroupEntities = new HashSet<>();
        securityGroupEntities.add(securityGroupEntity1);
        securityGroupEntities.add(securityGroupEntity2);
        securityGroupEntities.add(securityGroupEntity3);
        return securityGroupEntities;
    }

}
