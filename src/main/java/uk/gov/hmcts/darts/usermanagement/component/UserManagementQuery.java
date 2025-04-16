package uk.gov.hmcts.darts.usermanagement.component;

import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.util.List;

@FunctionalInterface
public interface UserManagementQuery {

    List<UserAccountEntity> getUsers(String emailAddress, List<Integer> userIds);

}
