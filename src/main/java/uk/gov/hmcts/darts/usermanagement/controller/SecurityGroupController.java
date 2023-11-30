package uk.gov.hmcts.darts.usermanagement.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.darts.usermanagement.http.api.SecurityGroupApi;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroup;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroupWithIdAndRole;

import java.util.List;

@RestController
public class SecurityGroupController implements SecurityGroupApi {

    @Override
    public ResponseEntity<List<SecurityGroupWithIdAndRole>> securityGroupsGet(Integer courthouse) {
        return SecurityGroupApi.super.securityGroupsGet(courthouse);
    }

    @Override
    public ResponseEntity<SecurityGroupWithIdAndRole> securityGroupsPost(SecurityGroup securityGroup) {
        return SecurityGroupApi.super.securityGroupsPost(securityGroup);
    }

}
