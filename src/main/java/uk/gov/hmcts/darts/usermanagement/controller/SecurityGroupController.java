package uk.gov.hmcts.darts.usermanagement.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.darts.usermanagement.http.api.SecurityGroupApi;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroup;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroupWithIdAndRole;
import uk.gov.hmcts.darts.usermanagement.service.SecurityGroupService;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SecurityGroupController implements SecurityGroupApi {

    private final SecurityGroupService securityGroupService;

    @Override
    public ResponseEntity<List<SecurityGroupWithIdAndRole>> securityGroupsGet(Integer courthouse) {
        return SecurityGroupApi.super.securityGroupsGet(courthouse);
    }

    @Override
    public ResponseEntity<SecurityGroupWithIdAndRole> securityGroupsPost(SecurityGroup securityGroup) {
        SecurityGroupWithIdAndRole response = securityGroupService.createSecurityGroup(securityGroup);

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(response);
    }

}
