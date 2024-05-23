package uk.gov.hmcts.darts.test.common;

import org.mockito.ArgumentMatcher;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;

import java.util.Set;

public class SecurityRoleMatcher implements ArgumentMatcher<Set<SecurityRoleEnum>> {

    private final SecurityRoleEnum assertRole;

    public SecurityRoleMatcher(SecurityRoleEnum assertRole) {
        this.assertRole = assertRole;
    }

    @Override
    public boolean matches(Set<SecurityRoleEnum> roleSet) {
        return roleSet != null && roleSet.stream().anyMatch(e -> e == assertRole);
    }
}