package uk.gov.hmcts.darts.usermanagement.component.impl;

import org.springframework.data.jpa.domain.Specification;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity_;

import java.util.List;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class UserQuerySpecifications {

    private UserQuerySpecifications() {
    }

    public static Specification<UserAccountEntity> hasEmailAddress(String emailAddress) {
        if (isBlank(emailAddress)) {
            return null;
        }
        return (root, query, cb) -> cb.equal(
            cb.lower(root.get(UserAccountEntity_.emailAddress)),
            emailAddress.toLowerCase());
    }

    public static Specification<UserAccountEntity> isInIds(List<Integer> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return null;
        }
        return (root, query, builder) -> root.get(UserAccountEntity_.id).in(userIds);
    }

    public static Specification<UserAccountEntity> notSystemUser() {
        return (root, query, builder) -> builder.equal(root.get(UserAccountEntity_.isSystemUser), false);
    }

}
