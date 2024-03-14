package uk.gov.hmcts.darts.usermanagement.service;

import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.model.SecurityGroupModel;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroup;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroupWithIdAndRole;

import java.util.List;

public interface SecurityGroupService {

    SecurityGroupWithIdAndRole createSecurityGroup(SecurityGroup securityGroup);

    SecurityGroupEntity createAndSaveSecurityGroup(SecurityGroupModel securityGroupModel);

    List<SecurityGroupWithIdAndRole> getSecurityGroups(List<Integer> roleIds, Integer courthouseId);

}
