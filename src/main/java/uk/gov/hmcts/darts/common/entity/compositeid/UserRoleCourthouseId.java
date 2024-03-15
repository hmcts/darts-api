package uk.gov.hmcts.darts.common.entity.compositeid;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.SecurityRoleEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.io.Serializable;

@NoArgsConstructor
@EqualsAndHashCode
public class UserRoleCourthouseId implements Serializable {

    private CourthouseEntity courthouse;
    private UserAccountEntity userAccount;
    private SecurityRoleEntity securityRole;
}
