package uk.gov.hmcts.darts.usermanagement.service;

import uk.gov.hmcts.darts.usermanagement.model.SecurityGroup;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroupWithIdAndRole;

public interface SecurityGroupService {

    SecurityGroupWithIdAndRole createSecurityGroup(SecurityGroup securityGroup);

}
