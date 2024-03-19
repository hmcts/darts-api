package uk.gov.hmcts.darts.usermanagement.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.component.validation.Validator;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.model.SecurityGroupModel;
import uk.gov.hmcts.darts.common.repository.SecurityGroupRepository;
import uk.gov.hmcts.darts.common.repository.SecurityRoleRepository;
import uk.gov.hmcts.darts.usermanagement.mapper.impl.SecurityGroupCourthouseMapper;
import uk.gov.hmcts.darts.usermanagement.mapper.impl.SecurityGroupMapper;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroup;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroupWithIdAndRole;
import uk.gov.hmcts.darts.usermanagement.service.SecurityGroupService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SecurityGroupServiceImpl implements SecurityGroupService {

    private final SecurityGroupRepository securityGroupRepository;
    private final SecurityRoleRepository securityRoleRepository;
    private final SecurityGroupMapper securityGroupMapper;
    private final SecurityGroupCourthouseMapper securityGroupCourthouseMapper;

    private final Validator<SecurityGroupModel> securityGroupCreationValidation;

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

    public List<SecurityGroupWithIdAndRole> getSecurityGroups(List<Integer> roleIds, Integer courthouseId, Integer userId, Boolean singletonUser) {
        List<SecurityGroupEntity> securityGroupEntities = securityGroupRepository.findAll();

        securityGroupEntities = filterSecurityGroupEntitiesByRoleIds(securityGroupEntities, roleIds);
        securityGroupEntities = filterSecurityGroupEntitiesByCourthouseId(securityGroupEntities, courthouseId);
        securityGroupEntities = filterSecurityGroupEntitiesByUserId(securityGroupEntities, userId);
        securityGroupEntities = filterSecurityGroupEntitiesBySingleUser(securityGroupEntities, singletonUser);

        return securityGroupEntities.stream()
            .map(securityGroupCourthouseMapper::mapToSecurityGroupWithIdAndRoleWithCourthouse).toList();
    }

    private List<SecurityGroupEntity> filterSecurityGroupEntitiesByRoleIds(
        List<SecurityGroupEntity> securityGroupEntities, List<Integer> roleIds) {

        if (roleIds != null) {
            return securityGroupEntities.stream()
                .filter(securityGroup -> roleIds.contains(securityGroup.getSecurityRoleEntity().getId()))
                .toList();
        }
        return securityGroupEntities;
    }

    private List<SecurityGroupEntity> filterSecurityGroupEntitiesByCourthouseId(
        List<SecurityGroupEntity> securityGroupEntities, Integer courthouseId) {

        if (courthouseId != null) {
            return securityGroupEntities.stream()
                .filter(securityGroupEntity -> securityGroupEntity.getCourthouseEntities().stream()
                    .anyMatch(courthouseEntity -> courthouseEntity.getId().equals(courthouseId)))
                .toList();
        }
        return securityGroupEntities;
    }

    private List<SecurityGroupEntity> filterSecurityGroupEntitiesByUserId(
        List<SecurityGroupEntity> securityGroupEntities, Integer userId) {

        if (userId != null) {
            return securityGroupEntities.stream()
                .filter(securityGroupEntity -> securityGroupEntity.getUsers().stream()
                    .anyMatch(userAccountEntity -> userAccountEntity.getId().equals(userId)))
                .toList();
        }
        return securityGroupEntities;
    }

    private List<SecurityGroupEntity> filterSecurityGroupEntitiesBySingleUser(
        List<SecurityGroupEntity> securityGroupEntities, Boolean singletonUser) {

        if (singletonUser != null) {
            return securityGroupEntities.stream()
                .filter(securityGroupEntity -> ((!securityGroupEntity.getUsers().isEmpty())
                    && (securityGroupEntity.getUsers().size() == 1) == singletonUser))
                .toList();
        }
        return securityGroupEntities;
    }

}
