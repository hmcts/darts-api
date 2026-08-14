package uk.gov.hmcts.darts.usermanagement.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_USER;

@ExtendWith(MockitoExtension.class)
class DisableInactiveUserAccountsServiceImplTest {

    private static final OffsetDateTime CURRENT_DATE_TIME = OffsetDateTime.of(2026, 8, 14, 10, 5, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime CUTOFF_DATE_TIME = CURRENT_DATE_TIME.minusMonths(6);
    private static final Set<Integer> EXCLUDED_ROLE_IDS = Set.of(SUPER_USER.getId(), SUPER_ADMIN.getId());

    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private CurrentTimeHelper currentTimeHelper;

    private DisableInactiveUserAccountsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DisableInactiveUserAccountsServiceImpl(userAccountRepository, currentTimeHelper);
    }

    @Test
    void process_shouldDisableInactiveUsersAndRemoveThemFromSecurityGroups() {
        UserAccountEntity inactiveUser = userAccount(123);
        SecurityGroupEntity securityGroup = securityGroup(inactiveUser);
        inactiveUser.getSecurityGroupEntities().add(securityGroup);

        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(CURRENT_DATE_TIME);
        when(userAccountRepository.findInactiveUsersExcludingRoles(CUTOFF_DATE_TIME, EXCLUDED_ROLE_IDS, PageRequest.of(0, 1000)))
            .thenReturn(List.of(inactiveUser));

        service.process(1000);

        assertThat(inactiveUser.isActive()).isFalse();
        assertThat(inactiveUser.getSecurityGroupEntities()).isEmpty();
        verify(userAccountRepository).saveAll(List.of(inactiveUser));
    }

    @Test
    void process_shouldUseMinimumBatchSize_WhenBatchSizeIsLessThanOne() {
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(CURRENT_DATE_TIME);
        when(userAccountRepository.findInactiveUsersExcludingRoles(CUTOFF_DATE_TIME, EXCLUDED_ROLE_IDS, PageRequest.of(0, 1)))
            .thenReturn(List.of());

        service.process(0);

        verify(userAccountRepository).findInactiveUsersExcludingRoles(CUTOFF_DATE_TIME, EXCLUDED_ROLE_IDS, PageRequest.of(0, 1));
        verify(userAccountRepository, never()).saveAll(List.of());
    }

    @Test
    void process_shouldNotSave_WhenNoInactiveUsersAreFound() {
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(CURRENT_DATE_TIME);
        when(userAccountRepository.findInactiveUsersExcludingRoles(CUTOFF_DATE_TIME, EXCLUDED_ROLE_IDS, PageRequest.of(0, 1000)))
            .thenReturn(List.of());

        service.process(1000);

        verify(userAccountRepository, never()).saveAll(List.of());
    }

    private static UserAccountEntity userAccount(Integer id) {
        UserAccountEntity userAccount = new UserAccountEntity();
        userAccount.setId(id);
        userAccount.setActive(true);
        userAccount.setIsSystemUser(false);
        return userAccount;
    }

    private static SecurityGroupEntity securityGroup(UserAccountEntity userAccount) {
        SecurityGroupEntity securityGroup = new SecurityGroupEntity();
        securityGroup.getUsers().add(userAccount);
        return securityGroup;
    }
}
