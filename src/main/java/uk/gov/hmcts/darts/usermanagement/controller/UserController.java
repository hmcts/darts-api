package uk.gov.hmcts.darts.usermanagement.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.darts.authorisation.annotation.Authorisation;
import uk.gov.hmcts.darts.usermanagement.http.api.UserApi;
import uk.gov.hmcts.darts.usermanagement.model.User;
import uk.gov.hmcts.darts.usermanagement.model.UserPatch;
import uk.gov.hmcts.darts.usermanagement.model.UserSearch;
import uk.gov.hmcts.darts.usermanagement.model.UserWithId;
import uk.gov.hmcts.darts.usermanagement.model.UserWithIdAndLastLogin;
import uk.gov.hmcts.darts.usermanagement.service.UserManagementService;

import java.util.List;

import static uk.gov.hmcts.darts.authorisation.constants.AuthorisationConstants.SECURITY_SCHEMES_BEARER_AUTH;
import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.ANY_ENTITY_ID;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.ADMIN;

@RestController
@RequiredArgsConstructor
public class UserController implements UserApi {

    private final UserManagementService userManagementService;

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID, globalAccessSecurityRoles = ADMIN)
    public ResponseEntity<List<UserWithIdAndLastLogin>> getUsers(Integer courthouseId, String emailAddress) {
        return UserApi.super.getUsers(courthouseId, emailAddress);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID, globalAccessSecurityRoles = ADMIN)
    public ResponseEntity<UserWithId> createUser(User user) {
        UserWithId createdUser = userManagementService.createUser(user);

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(createdUser);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID, globalAccessSecurityRoles = ADMIN)
    public ResponseEntity<UserWithIdAndLastLogin> modifyUser(Integer userId, UserPatch userPatch) {
        UserWithIdAndLastLogin updatedUser = userManagementService.modifyUser(userId, userPatch);

        return ResponseEntity.ok(updatedUser);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID, globalAccessSecurityRoles = ADMIN)
    public ResponseEntity<List<UserWithIdAndLastLogin>> search(UserSearch userSearch) {
        return ResponseEntity.ok(userManagementService.search(userSearch));
    }

}
