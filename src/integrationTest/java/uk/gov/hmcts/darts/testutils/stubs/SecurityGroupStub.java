package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SecurityGroupEnum;
import uk.gov.hmcts.darts.common.repository.SecurityGroupRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;

import java.util.Optional;
import java.util.Set;

@RequiredArgsConstructor
@Component
public class SecurityGroupStub {
    private final SecurityGroupRepository securityGroupRepository;
    private final UserAccountRepository userAccountRepository;


    @Transactional
    public void addCourthouse(SecurityGroupEntity securityGroupEntity, CourthouseEntity courthouseEntity) {
        Optional<SecurityGroupEntity> securityGroup = securityGroupRepository.findById(securityGroupEntity.getId());
        securityGroup.get().getCourthouseEntities().add(courthouseEntity);
    }

    @Transactional
    public Set<SecurityGroupEntity> getGroupsForUser(Integer userId) {
        Optional<UserAccountEntity> fndUserIdentity = userAccountRepository.findById(userId);
        return fndUserIdentity.get().getSecurityGroupEntities();
    }

    @Transactional
    public boolean isPartOfAnySecurityGroup(Integer userId) {
        return !getGroupsForUser(userId).isEmpty();
    }

    @Transactional
    public void clearUsers(SecurityGroupEnum groupEnum) {
        Optional<SecurityGroupEntity> entity = securityGroupRepository.findByGroupNameIgnoreCase(groupEnum.getName());
        for (UserAccountEntity accountEntity : entity.get().getUsers()) {
            accountEntity.getSecurityGroupEntities().remove(entity.get());
            userAccountRepository.save(accountEntity);
        }

        entity.get().getUsers().clear();

        securityGroupRepository.saveAndFlush(entity.get());
    }
}