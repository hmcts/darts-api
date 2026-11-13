package uk.gov.hmcts.darts.usermanagement.service.impl;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserAccountSecurityGroupServiceTest {

    @Test
    void unassignUserFromGroupsTheyArePartOf_shouldRemoveUserFromLoadedSecurityGroupUsers_whenUserAssignedToGroups() {
        UserAccountSecurityGroupService service = new UserAccountSecurityGroupService();
        UserAccountEntity userAccount = new UserAccountEntity();
        SecurityGroupEntity securityGroup1 = securityGroup(userAccount);
        SecurityGroupEntity securityGroup2 = securityGroup(userAccount);
        userAccount.setSecurityGroupEntities(new LinkedHashSet<>(Set.of(securityGroup1, securityGroup2)));

        service.unassignUserFromGroupsTheyArePartOf(userAccount);

        assertThat(userAccount.getSecurityGroupEntities()).isEmpty();
        assertThat(securityGroup1.getUsers()).doesNotContain(userAccount);
        assertThat(securityGroup2.getUsers()).doesNotContain(userAccount);
    }

    @Test
    void unassignUserFromGroupsTheyArePartOf_shouldNotUpdateSecurityGroupUsers_whenGroupUsersAreUninitialized() {
        UserAccountSecurityGroupService service = new UserAccountSecurityGroupService();
        UserAccountEntity userAccount = new UserAccountEntity();
        SecurityGroupEntity securityGroup = mock(SecurityGroupEntity.class);
        Set<UserAccountEntity> groupUsers = spy(new LinkedHashSet<>());
        when(securityGroup.getUsers()).thenReturn(groupUsers);
        userAccount.setSecurityGroupEntities(new LinkedHashSet<>(Set.of(securityGroup)));

        try (MockedStatic<Hibernate> hibernate = mockStatic(Hibernate.class)) {
            hibernate.when(() -> Hibernate.isInitialized(groupUsers)).thenReturn(false);

            service.unassignUserFromGroupsTheyArePartOf(userAccount);
        }

        assertThat(userAccount.getSecurityGroupEntities()).isEmpty();
        verify(groupUsers, never()).remove(userAccount);
    }

    private static SecurityGroupEntity securityGroup(UserAccountEntity userAccount) {
        SecurityGroupEntity securityGroup = new SecurityGroupEntity();
        securityGroup.getUsers().add(userAccount);
        return securityGroup;
    }
}
