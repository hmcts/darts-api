package uk.gov.hmcts.darts.usermanagement.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.component.validation.Validator;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.model.SecurityGroupModel;
import uk.gov.hmcts.darts.common.repository.SecurityGroupRepository;
import uk.gov.hmcts.darts.common.repository.SecurityRoleRepository;
import uk.gov.hmcts.darts.usermanagement.mapper.impl.SecurityGroupCourthouseMapper;
import uk.gov.hmcts.darts.usermanagement.mapper.impl.SecurityGroupMapper;
import uk.gov.hmcts.darts.usermanagement.mapper.impl.SecurityGroupWithIdAndRoleAndUsersMapper;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroup;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroupWithIdAndRole;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroupWithIdAndRoleAndUsers;
import uk.gov.hmcts.darts.usermanagement.service.SecurityGroupService;

import java.util.List;
import java.util.stream.Collectors;

import static uk.gov.hmcts.darts.usermanagement.exception.UserManagementError.SECURITY_GROUP_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class SecurityGroupServiceImpl implements SecurityGroupService {

    private final SecurityGroupRepository securityGroupRepository;
    private final SecurityRoleRepository securityRoleRepository;
    private final SecurityGroupMapper securityGroupMapper;
    private final SecurityGroupCourthouseMapper securityGroupCourthouseMapper;
    private final SecurityGroupWithIdAndRoleAndUsersMapper securityGroupWithIdAndRoleAndUsersMapper;
    private final Validator<SecurityGroupModel> securityGroupCreationValidation;

    @Override
    public SecurityGroupWithIdAndRoleAndUsers getSecurityGroup(Integer securityGroupId) {
        SecurityGroupEntity securityGroup = securityGroupRepository.findById(securityGroupId)
            .orElseThrow(() -> new DartsApiException(SECURITY_GROUP_NOT_FOUND));

        return securityGroupWithIdAndRoleAndUsersMapper.mapToSecurityGroupWithIdAndRoleAndUsers(securityGroup);
    }

    @Override
    @Transactional
    public SecurityGroupWithIdAndRole createSecurityGroup(SecurityGroup securityGroupRequest) {
        SecurityGroupModel securityGroupModel = securityGroupMapper.mapToSecurityGroupModel(securityGroupRequest);
        securityGroupModel.setRoleId(SecurityRoleEnum.TRANSCRIBER.getId());

        SecurityGroupEntity createdSecurityGroupEntity = createAndSaveSecurityGroup(securityGroupModel);

        SecurityGroupWithIdAndRole securityGroupPostResponse = securityGroupMapper.mapToSecurityGroupWithIdAndRole(createdSecurityGroupEntity);
        securityGroupPostResponse.setSecurityRoleId(createdSecurityGroupEntity.getSecurityRoleEntity().getId());

        return securityGroupPostResponse;
    }

    @Override
    @Transactional
    public SecurityGroupEntity createAndSaveSecurityGroup(SecurityGroupModel securityGroupModel) {
        securityGroupCreationValidation.validate(securityGroupModel);

        var securityGroupEntity = securityGroupMapper.mapToSecurityGroupEntity(securityGroupModel);
        securityGroupEntity.setGlobalAccess(false);
        securityGroupEntity.setDisplayState(true);

        var transcriberRoleEntity = securityRoleRepository.getReferenceById(securityGroupModel.getRoleId());
        securityGroupEntity.setSecurityRoleEntity(transcriberRoleEntity);

        return securityGroupRepository.saveAndFlush(securityGroupEntity);
    }

    public List<SecurityGroupWithIdAndRole> getSecurityGroups(List<Integer> roleIds, Integer courthouseId) {
        List<SecurityGroupEntity> securityGroupEntities = securityGroupRepository.findAll();

        securityGroupEntities = filterSecurityGroupEntitiesByRoleIds(securityGroupEntities, roleIds);
        securityGroupEntities = filterSecurityGroupEntitiesByCourthouseId(securityGroupEntities, courthouseId);

        List<SecurityGroupWithIdAndRole> securityGroupWithIdAndRoles = securityGroupEntities.stream()
            .map(securityGroupCourthouseMapper::mapToSecurityGroupWithIdAndRoleWithCourthouse).toList();

        return securityGroupWithIdAndRoles;
    }

    private List<SecurityGroupEntity> filterSecurityGroupEntitiesByRoleIds(
        List<SecurityGroupEntity> securityGroupEntities, List<Integer> roleIds) {

        if (roleIds != null) {
            return securityGroupEntities.stream()
                .filter(securityGroup -> roleIds.contains(securityGroup.getSecurityRoleEntity().getId()))
                .collect(Collectors.toList());
        }
        return securityGroupEntities;
    }

    private List<SecurityGroupEntity> filterSecurityGroupEntitiesByCourthouseId(
        List<SecurityGroupEntity> securityGroupEntities, Integer courthouseId) {

        if (courthouseId != null) {
            return securityGroupEntities.stream()
                .filter(securityGroupEntity -> securityGroupEntity.getCourthouseEntities().stream()
                    .anyMatch(courthouseEntity -> courthouseEntity.getId().equals(courthouseId)))
                .collect(Collectors.toList());
        }
        return securityGroupEntities;
    }

}
