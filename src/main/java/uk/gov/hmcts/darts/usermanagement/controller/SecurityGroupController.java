package uk.gov.hmcts.darts.usermanagement.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.darts.authorisation.annotation.Authorisation;
import uk.gov.hmcts.darts.usermanagement.http.api.SecurityGroupApi;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroup;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroupWithIdAndRole;
import uk.gov.hmcts.darts.usermanagement.service.SecurityGroupService;

import java.util.List;

import static uk.gov.hmcts.darts.authorisation.constants.AuthorisationConstants.SECURITY_SCHEMES_BEARER_AUTH;
import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.ANY_ENTITY_ID;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.ADMIN;

@RestController
@RequiredArgsConstructor
public class SecurityGroupController implements SecurityGroupApi {

    private final SecurityGroupService securityGroupService;

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID, globalAccessSecurityRoles = ADMIN)
    public ResponseEntity<List<SecurityGroupWithIdAndRole>> securityGroupsGet(Integer courthouse) {
        return SecurityGroupApi.super.securityGroupsGet(courthouse);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID, globalAccessSecurityRoles = ADMIN)
    public ResponseEntity<SecurityGroupWithIdAndRole> securityGroupsPost(SecurityGroup securityGroup) {
        SecurityGroupWithIdAndRole response = securityGroupService.createSecurityGroup(securityGroup);

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(response);
    }

}
