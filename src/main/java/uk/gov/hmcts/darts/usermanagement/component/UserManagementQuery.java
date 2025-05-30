package uk.gov.hmcts.darts.usermanagement.component;

import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.util.List;

@FunctionalInterface
public interface UserManagementQuery {

    List<UserAccountEntity> getUsers(boolean includeSystemUSers, String emailAddress, List<Integer> userIds);

}
