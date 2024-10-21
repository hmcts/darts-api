package uk.gov.hmcts.darts.authorisation.service;

import uk.gov.hmcts.darts.authorisation.model.UserState;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface AuthorisationService {

    UserState getAuthorisation(int userId);

    Optional<UserState> getAuthorisation(String emailAddress);

    void checkCourthouseAuthorisation(List<CourthouseEntity> courthouses, Set<SecurityRoleEnum> securityRoles);

    List<UserAccountEntity> getUsersWithRoleAtCourthouse(SecurityRoleEnum securityRole, CourthouseEntity courthouse, UserAccountEntity... excludingUsers);

}
