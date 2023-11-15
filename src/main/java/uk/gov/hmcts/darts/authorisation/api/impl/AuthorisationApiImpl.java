package uk.gov.hmcts.darts.authorisation.api.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.authorisation.model.UserState;
import uk.gov.hmcts.darts.authorisation.service.AuthorisationService;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
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
    public List<UserAccountEntity> getUsersWithRoleAtCourthouse(SecurityRoleEnum securityRole, CourthouseEntity courthouse) {
        return authorisationService.getUsersWithRoleAtCourthouse(securityRole, courthouse);
    }

    @Override
    public UserAccountEntity getCurrentUser() {
        return userIdentity.getUserAccount();
    }

}
