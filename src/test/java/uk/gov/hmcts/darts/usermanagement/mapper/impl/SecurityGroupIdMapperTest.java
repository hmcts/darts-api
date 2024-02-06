package uk.gov.hmcts.darts.usermanagement.mapper.impl;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.usermanagement.model.UserWithIdAndTimestamps;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SecurityGroupIdMapperTest {

    @Test
    void givenUserAccountEntityReturnUserWithIdAndTimestamps() {
        SecurityGroupEntity securityGroupEntity1 = new SecurityGroupEntity();
        securityGroupEntity1.setId(1);
        SecurityGroupEntity securityGroupEntity2 = new SecurityGroupEntity();
        securityGroupEntity2.setId(2);

        UserAccountEntity userAccountEntity = new UserAccountEntity();
        userAccountEntity.setSecurityGroupEntities(Set.of(securityGroupEntity1, securityGroupEntity2));

        UserAccountMapper userAccountMapper = new UserAccountMapperImpl();
        SecurityGroupIdMapper securityGroupIdMapper = new SecurityGroupIdMapper(userAccountMapper);

        UserWithIdAndTimestamps userWithIdAndTimestamps =
              securityGroupIdMapper.mapToUserWithSecurityGroups(userAccountEntity);
        assertEquals(2, userWithIdAndTimestamps.getSecurityGroupIds().size());


    }
}
