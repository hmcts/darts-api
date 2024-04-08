package uk.gov.hmcts.darts.usermanagement.service;

import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.model.SecurityGroupModel;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroupPatch;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroupPostRequest;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroupWithIdAndRole;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroupWithIdAndRoleAndUsers;

import java.util.List;

public interface SecurityGroupService {

    SecurityGroupWithIdAndRoleAndUsers getSecurityGroup(Integer securityGroupId);

    SecurityGroupWithIdAndRole createSecurityGroup(SecurityGroupPostRequest securityGroup);

    SecurityGroupEntity createAndSaveSecurityGroup(SecurityGroupModel securityGroupModel);

    List<SecurityGroupWithIdAndRoleAndUsers> getSecurityGroups(List<Integer> roleIds, Integer courthouseId, Integer userId, Boolean singletonUser);

    SecurityGroupWithIdAndRoleAndUsers modifySecurityGroup(Integer securityGroupId, SecurityGroupPatch securityGroupPatch);
}
