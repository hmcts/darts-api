package uk.gov.hmcts.darts.authorisation.component.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.helper.SystemUserHelper;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.common.repository.UserRolesCourthousesRepository;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

@AllArgsConstructor
@Slf4j
public class AtsUserIdentityImpl implements UserIdentity {

    private final SystemUserHelper systemUserHelper;

    private final UserAccountRepository userAccountRepository;

    private final UserRolesCourthousesRepository userRolesCourthousesRepository;

    @Override
    public UserAccountEntity getUserAccount() {
        return systemUserHelper.getSystemUser();
    }

    @Override
    public boolean userHasGlobalAccess(Set<SecurityRoleEnum> globalAccessRoles) {
        boolean userHasGlobalAccess = false;
        String emailAddress = null;

        emailAddress = getUserAccount().getEmailAddress();


        if (nonNull(emailAddress)) {
            List<UserAccountEntity> userAccountEntities =
                userAccountRepository.findByEmailAddressOrAccountGuidForRolesAndGlobalAccessIsTrue(
                    emailAddress, null,
                    globalAccessRoles.stream().map(SecurityRoleEnum::getId).collect(Collectors.toUnmodifiableSet())
                );
            if (!userAccountEntities.isEmpty()) {
                userHasGlobalAccess = true;
            }
        }
        return userHasGlobalAccess;
    }

    @Override
    public List<Integer> getListOfCourthouseIdsUserHasAccessTo() {
        UserAccountEntity userAccount = getUserAccount();
        return userRolesCourthousesRepository.findAllCourthouseIdsByUserAccount(userAccount);
    }
}