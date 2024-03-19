package uk.gov.hmcts.darts.usermanagement.api.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.model.SecurityGroupModel;
import uk.gov.hmcts.darts.usermanagement.api.UserManagementApi;
import uk.gov.hmcts.darts.usermanagement.service.SecurityGroupService;

@Component
@RequiredArgsConstructor
public class UserManagementApiImpl implements UserManagementApi {

    private final SecurityGroupService securityGroupService;

    @Override
    public SecurityGroupEntity createAndSaveSecurityGroup(SecurityGroupModel securityGroupModel) {
        return securityGroupService.createAndSaveSecurityGroup(securityGroupModel);
    }

}
