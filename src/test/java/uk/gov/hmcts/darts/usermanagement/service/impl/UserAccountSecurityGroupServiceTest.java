package uk.gov.hmcts.darts.usermanagement.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.SecurityGroupRepository;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserAccountSecurityGroupServiceTest {

    @Mock
    private SecurityGroupRepository securityGroupRepository;

    private UserAccountSecurityGroupService service;

    @BeforeEach
    void setUp() {
        service = new UserAccountSecurityGroupService(securityGroupRepository);
    }

    @Test
    void unassignUserFromGroupsTheyArePartOf_shouldRemoveUserFromUserGroupsAndSecurityGroupUsers_whenUserAssignedToGroups() {
        UserAccountEntity userAccount = new UserAccountEntity();
        SecurityGroupEntity securityGroup1 = securityGroup(userAccount);
        SecurityGroupEntity securityGroup2 = securityGroup(userAccount);
        userAccount.setSecurityGroupEntities(new LinkedHashSet<>(Set.of(securityGroup1, securityGroup2)));

        service.unassignUserFromGroupsTheyArePartOf(userAccount);

        assertThat(userAccount.getSecurityGroupEntities()).isEmpty();
        assertThat(securityGroup1.getUsers()).doesNotContain(userAccount);
        assertThat(securityGroup2.getUsers()).doesNotContain(userAccount);
        verify(securityGroupRepository).save(securityGroup1);
        verify(securityGroupRepository).save(securityGroup2);
    }

    private static SecurityGroupEntity securityGroup(UserAccountEntity userAccount) {
        SecurityGroupEntity securityGroup = new SecurityGroupEntity();
        securityGroup.getUsers().add(userAccount);
        return securityGroup;
    }
}
