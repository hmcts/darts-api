package uk.gov.hmcts.darts.usermanagement.service;

import uk.gov.hmcts.darts.usermanagement.model.Role;

import java.util.List;

@FunctionalInterface
public interface SecurityRoleService {

    List<Role> getAllRoles();

}
