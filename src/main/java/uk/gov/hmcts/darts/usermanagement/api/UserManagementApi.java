package uk.gov.hmcts.darts.usermanagement.api;

import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.model.SecurityGroupModel;

@FunctionalInterface
public interface UserManagementApi {
    SecurityGroupEntity createAndSaveSecurityGroup(SecurityGroupModel securityGroupModel);
}
