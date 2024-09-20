package uk.gov.hmcts.darts.testutils.stubs;

import jakarta.annotation.PostConstruct;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.SecurityRoleEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SecurityGroupEnum;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.repository.SecurityGroupRepository;
import uk.gov.hmcts.darts.common.repository.SecurityRoleRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;

@RequiredArgsConstructor
@Component
@Deprecated
public class SecurityGroupStub {
    private final SecurityGroupRepository securityGroupRepository;
    private final UserAccountRepository userAccountRepository;
    private final SecurityRoleRepository securityRoleRepository;
    private final DartsDatabaseSaveStub dartsDatabaseSaveStub;

    private static SecurityRoleEntity defaultRole;

    @PostConstruct
    public void init() {
        defaultRole = securityRoleRepository.findAll().stream()
            .filter(securityRoleEntity -> securityRoleEntity.getId().equals(SecurityRoleEnum.REQUESTER.getId()))
            .findFirst()
            .orElseThrow();
    }

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
            dartsDatabaseSaveStub.save(accountEntity);
        }

        entity.get().getUsers().clear();

        dartsDatabaseSaveStub.save(entity.get());
    }

    public SecurityGroupEntity createAndSave(SecurityGroupEntitySpec spec, UserAccountEntity user) {
        SecurityGroupEntity securityGroup = new SecurityGroupEntity();
        securityGroup.setSecurityRoleEntity(spec.securityRoleEntity);
        securityGroup.setLegacyObjectId(spec.legacyObjectId);
        securityGroup.setGroupName(spec.groupName);
        securityGroup.setIsPrivate(spec.isPrivate);
        securityGroup.setDescription(spec.description);
        securityGroup.setGroupGlobalUniqueId(securityGroup.getGroupGlobalUniqueId());
        securityGroup.setGlobalAccess(spec.globalAccess);
        securityGroup.setDisplayState(spec.displayState);
        securityGroup.setUseInterpreter(spec.useInterpreter);
        securityGroup.setCourthouseEntities(spec.courthouseEntities);
        securityGroup.setUsers(spec.users);
        securityGroup.setDisplayName(spec.displayName);
        securityGroup.setCreatedBy(user);
        securityGroup.setLastModifiedBy(user);
        securityGroup.setCreatedDateTime(OffsetDateTime.now());
        securityGroup.setLastModifiedDateTime(OffsetDateTime.now());

        return dartsDatabaseSaveStub.save(securityGroup);
    }

    @Builder
    public static class SecurityGroupEntitySpec {
        @Builder.Default
        private SecurityRoleEntity securityRoleEntity = defaultRole;
        private String legacyObjectId;
        @Builder.Default
        private String groupName = "Some security group name";
        private Boolean isPrivate;
        @Builder.Default
        private String description = "Some description";
        private String groupGlobalUniqueId;
        @Builder.Default
        private Boolean globalAccess = false;
        @Builder.Default
        private Boolean displayState = true;
        @Builder.Default
        private Boolean useInterpreter = false;
        private Set<CourthouseEntity> courthouseEntities;
        private Set<UserAccountEntity> users;
        @Builder.Default
        private String displayName = "Some security group display name";
    }
}