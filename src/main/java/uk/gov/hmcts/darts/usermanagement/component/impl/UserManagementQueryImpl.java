package uk.gov.hmcts.darts.usermanagement.component.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.usermanagement.component.UserManagementQuery;

import java.util.List;

import static org.springframework.data.domain.Sort.Direction.DESC;
import static org.springframework.data.jpa.domain.Specification.where;
import static uk.gov.hmcts.darts.usermanagement.component.impl.UserQuerySpecifications.hasEmailAddress;
import static uk.gov.hmcts.darts.usermanagement.component.impl.UserQuerySpecifications.isInIds;
import static uk.gov.hmcts.darts.usermanagement.component.impl.UserQuerySpecifications.notSystemUser;

@Component
@RequiredArgsConstructor
public class UserManagementQueryImpl implements UserManagementQuery {

    private final UserAccountRepository userAccountRepository;

    @Override
    public List<UserAccountEntity> getUsers(boolean includeSystemUsers, String emailAddress, List<Integer> userIds) {
        Specification<UserAccountEntity> spec = where(hasEmailAddress(emailAddress)).and(isInIds(userIds));
        if (!includeSystemUsers) {
            spec = spec.and(notSystemUser());
        }
        return userAccountRepository.findAll(spec, Sort.by(DESC, "id"));
    }
}
