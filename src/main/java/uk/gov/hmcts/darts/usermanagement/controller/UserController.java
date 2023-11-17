package uk.gov.hmcts.darts.usermanagement.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.darts.authorisation.annotation.Authorisation;
import uk.gov.hmcts.darts.usermanagement.http.api.UserApi;
import uk.gov.hmcts.darts.usermanagement.model.UserWithIdAndLastLogin;

import java.util.List;

import static uk.gov.hmcts.darts.authorisation.constants.AuthorisationConstants.SECURITY_SCHEMES_BEARER_AUTH;
import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.ANY_ENTITY_ID;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.ADMIN;

@RestController
public class UserController implements UserApi {

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID, globalAccessSecurityRoles = ADMIN)
    public ResponseEntity<List<UserWithIdAndLastLogin>> usersGet(Integer courthouse) {
        return UserApi.super.usersGet(courthouse);
    }

}
