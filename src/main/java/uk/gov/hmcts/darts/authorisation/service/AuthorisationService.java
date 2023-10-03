package uk.gov.hmcts.darts.authorisation.service;

import uk.gov.hmcts.darts.authorisation.model.UserState;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface AuthorisationService {

    Optional<UserState> getAuthorisation(String emailAddress);

    void checkAuthorisation(List<CourthouseEntity> courthouses, Set<SecurityRoleEnum> securityRoles);

    List<UserAccountEntity> getUsersWithRoleAtCourthouses(SecurityRoleEnum securityRole, List<CourthouseEntity> courthouses);

    List<UserAccountEntity> getUsersWithRoleAtCourthouse(SecurityRoleEnum securityRole, CourthouseEntity courthouses);
}
