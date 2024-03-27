package uk.gov.hmcts.darts.usermanagement.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.darts.authorisation.annotation.Authorisation;
import uk.gov.hmcts.darts.usermanagement.http.api.SecurityGroupApi;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroup;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroupPatch;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroupWithIdAndRole;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroupWithIdAndRoleAndUsers;
import uk.gov.hmcts.darts.usermanagement.service.SecurityGroupService;

import java.util.List;

import static uk.gov.hmcts.darts.authorisation.constants.AuthorisationConstants.SECURITY_SCHEMES_BEARER_AUTH;
import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.ANY_ENTITY_ID;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;

@RestController
@RequiredArgsConstructor
public class SecurityGroupController implements SecurityGroupApi {

    private final SecurityGroupService securityGroupService;

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID, globalAccessSecurityRoles = SUPER_ADMIN)
    public ResponseEntity<SecurityGroupWithIdAndRoleAndUsers> adminGetSecurityGroup(Integer securityGroupId) {
        return ResponseEntity.ok(securityGroupService.getSecurityGroup(securityGroupId));
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID, globalAccessSecurityRoles = SUPER_ADMIN)
    public ResponseEntity<List<SecurityGroupWithIdAndRole>> adminSecurityGroupsGet(List<Integer> roleIds, Integer courthouseId,
                                                                                   Integer userId, Boolean singletonUser) {
        return ResponseEntity.status(HttpStatus.OK)
            .body(securityGroupService.getSecurityGroups(roleIds, courthouseId, userId, singletonUser));
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID, globalAccessSecurityRoles = SUPER_ADMIN)
    public ResponseEntity<SecurityGroupWithIdAndRole> adminSecurityGroupsPost(SecurityGroup securityGroup) {
        SecurityGroupWithIdAndRole response = securityGroupService.createSecurityGroup(securityGroup);

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(response);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID, globalAccessSecurityRoles = SUPER_ADMIN)
    public ResponseEntity<SecurityGroupWithIdAndRoleAndUsers> modifySecurityGroup(Integer securityGroupId, SecurityGroupPatch securityGroupPatch) {
        SecurityGroupWithIdAndRoleAndUsers response = securityGroupService.modifySecurityGroup(securityGroupId, securityGroupPatch);

        return ResponseEntity.status(HttpStatus.OK)
            .body(response);
    }
}
