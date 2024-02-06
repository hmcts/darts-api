package uk.gov.hmcts.darts.usermanagement.mapper.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.usermanagement.model.UserWithIdAndTimestamps;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class SecurityGroupIdMapper {

    private final UserAccountMapper userAccountMapper;

    public UserWithIdAndTimestamps mapToUserWithSecurityGroups(UserAccountEntity userAccountEntity) {
        UserWithIdAndTimestamps userWithIdAndTimestamps = userAccountMapper.mapToUserWithIdAndLastLoginModel(userAccountEntity);
        Set<SecurityGroupEntity> securityGroupEntities =  userAccountEntity.getSecurityGroupEntities();
        List<Integer> securityGroupIds =  mapSecurityGroupEntitiesToIds(securityGroupEntities);
        return userWithIdAndTimestamps.securityGroupIds(securityGroupIds);
    }

    public List<Integer> mapSecurityGroupEntitiesToIds(Set<SecurityGroupEntity> securityGroupEntities) {
        return securityGroupEntities.stream()
            .map(SecurityGroupEntity::getId)
            .toList();
    }


}
