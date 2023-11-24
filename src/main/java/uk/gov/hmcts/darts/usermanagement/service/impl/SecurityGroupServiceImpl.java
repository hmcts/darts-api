package uk.gov.hmcts.darts.usermanagement.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.SecurityGroupRepository;
import uk.gov.hmcts.darts.common.repository.SecurityRoleRepository;
import uk.gov.hmcts.darts.usermanagement.exception.UserManagementError;
import uk.gov.hmcts.darts.usermanagement.mapper.impl.SecurityGroupMapper;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroup;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroupWithIdAndRole;
import uk.gov.hmcts.darts.usermanagement.service.SecurityGroupService;

import java.util.Collections;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SecurityGroupServiceImpl implements SecurityGroupService {

    private final SecurityGroupRepository securityGroupRepository;
    private final SecurityRoleRepository roleRepository;
    private final SecurityGroupMapper securityGroupMapper;

    @Override
    public SecurityGroupWithIdAndRole createSecurityGroup(SecurityGroup securityGroup) {
        validateName(securityGroup.getName());

        var securityGroupEntity = securityGroupMapper.mapToUserEntity(securityGroup);
        securityGroupEntity.setGlobalAccess(false);
        securityGroupEntity.setDisplayState(true);

        var transcriberRoleEntity = roleRepository.findByRoleName("TRANSCRIBER")
            .orElseThrow();
        securityGroupEntity.setSecurityRoleEntity(transcriberRoleEntity);

        var createdSecurityGroupEntity = securityGroupRepository.save(securityGroupEntity);

        var securityGroupWithIdAndRole = securityGroupMapper.mapToSecurityGroupWithIdAndRole(createdSecurityGroupEntity);
        securityGroupWithIdAndRole.setRole(createdSecurityGroupEntity.getSecurityRoleEntity().getId());

        return securityGroupWithIdAndRole;
    }

    private void validateName(String name) {
        Optional<SecurityGroupEntity> existingGroup = securityGroupRepository.findByGroupName(name);
        if (existingGroup.isPresent()) {
            throw new DartsApiException(UserManagementError.DUPLICATE_SECURITY_GROUP_NAME_NOT_PERMITTED,
                                        "Attempt to create group that already exists",
                                        Collections.singletonMap("existing_group_id", existingGroup.get().getId()));
        }
    }

}
