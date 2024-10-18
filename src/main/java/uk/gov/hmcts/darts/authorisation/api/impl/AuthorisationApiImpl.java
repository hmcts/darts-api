package uk.gov.hmcts.darts.authorisation.api.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.authorisation.model.UserState;
import uk.gov.hmcts.darts.authorisation.service.AuthorisationService;
import uk.gov.hmcts.darts.authorisation.util.SecurityGroupUtil;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthorisationApiImpl implements AuthorisationApi {

    private final AuthorisationService authorisationService;
    private final UserIdentity userIdentity;

    @Override
    public UserState getAuthorisation(int userId) {
        return authorisationService.getAuthorisation(userId);
    }

    @Override
    public Optional<UserState> getAuthorisation(String emailAddress) {
        return authorisationService.getAuthorisation(emailAddress);
    }

    @Override
    public void checkCourthouseAuthorisation(List<CourthouseEntity> courthouses, Set<SecurityRoleEnum> securityRoles) {
        authorisationService.checkCourthouseAuthorisation(
            courthouses,
            securityRoles
        );
    }

    @Override
    public List<UserAccountEntity> getUsersWithRoleAtCourthouse(SecurityRoleEnum securityRole, CourthouseEntity courthouse,
                                                                UserAccountEntity... excludingUsers) {
        return authorisationService.getUsersWithRoleAtCourthouse(securityRole, courthouse, excludingUsers);
    }

    @Override
    public UserAccountEntity getCurrentUser() {
        return userIdentity.getUserAccount();
    }

    /*
    Confirms whether the user has at least 1 of the supplied security roles.
     */
    @Override
    @Transactional
    public boolean userHasOneOfRoles(List<SecurityRoleEnum> securityRoles) {
        Set<SecurityGroupEntity> securityGroupEntities = userIdentity.getUserAccount().getSecurityGroupEntities();
        return SecurityGroupUtil.matchesAtLeastOneSecurityGroup(securityGroupEntities, securityRoles);
    }

    @Override
    public List<Integer> getListOfCourthouseIdsUserHasAccessTo() {
        return userIdentity.getListOfCourthouseIdsUserHasAccessTo();
    }

}
