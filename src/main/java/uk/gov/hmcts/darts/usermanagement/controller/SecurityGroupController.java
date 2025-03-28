package uk.gov.hmcts.darts.usermanagement.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.darts.authorisation.annotation.Authorisation;
import uk.gov.hmcts.darts.usermanagement.http.api.SecurityGroupApi;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroupPatch;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroupPostRequest;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroupWithIdAndRole;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroupWithIdAndRoleAndUsers;
import uk.gov.hmcts.darts.usermanagement.service.SecurityGroupService;

import java.util.List;

import static uk.gov.hmcts.darts.authorisation.constants.AuthorisationConstants.SECURITY_SCHEMES_BEARER_AUTH;
import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.ANY_ENTITY_ID;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_USER;

@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "darts", name = "api-pod", havingValue = "true")
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
    @Authorisation(contextId = ANY_ENTITY_ID, globalAccessSecurityRoles = {SUPER_ADMIN, SUPER_USER})
    public ResponseEntity<List<SecurityGroupWithIdAndRoleAndUsers>> adminSecurityGroupsGet(List<Integer> roleIds, Integer courthouseId,
                                                                                   Integer userId, Boolean singletonUser) {
        return ResponseEntity.status(HttpStatus.OK)
            .body(securityGroupService.getSecurityGroups(roleIds, courthouseId, userId, singletonUser));
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID, globalAccessSecurityRoles = SUPER_ADMIN)
    public ResponseEntity<SecurityGroupWithIdAndRole> adminSecurityGroupsPost(SecurityGroupPostRequest securityGroupPostRequest) {
        SecurityGroupWithIdAndRole response = securityGroupService.createSecurityGroup(securityGroupPostRequest);

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