package uk.gov.hmcts.darts.usermanagement.service;

import uk.gov.hmcts.darts.usermanagement.model.Role;

import java.util.List;

public interface SecurityRoleService {

    List<Role> getAllRoles();

}
