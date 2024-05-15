package uk.gov.hmcts.darts.usermanagement.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.model.SecurityGroupModel;
import uk.gov.hmcts.darts.common.repository.CourthouseRepository;
import uk.gov.hmcts.darts.common.repository.SecurityGroupRepository;
import uk.gov.hmcts.darts.common.repository.SecurityRoleRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.usermanagement.component.validation.impl.SecurityGroupCreationValidation;
import uk.gov.hmcts.darts.usermanagement.exception.UserManagementError;
import uk.gov.hmcts.darts.usermanagement.mapper.impl.SecurityGroupCourthouseMapper;
import uk.gov.hmcts.darts.usermanagement.mapper.impl.SecurityGroupMapper;
import uk.gov.hmcts.darts.usermanagement.mapper.impl.SecurityGroupWithIdAndRoleAndUsersMapper;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroupPatch;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroupPostRequest;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroupWithIdAndRole;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroupWithIdAndRoleAndUsers;
import uk.gov.hmcts.darts.usermanagement.service.SecurityGroupService;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.isNull;
import static uk.gov.hmcts.darts.usermanagement.exception.UserManagementError.SECURITY_GROUP_NOT_ALLOWED;
import static uk.gov.hmcts.darts.usermanagement.exception.UserManagementError.SECURITY_GROUP_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class SecurityGroupServiceImpl implements SecurityGroupService {

    private final SecurityGroupRepository securityGroupRepository;
    private final SecurityRoleRepository securityRoleRepository;
    private final CourthouseRepository courthouseRepository;
    private final UserAccountRepository userAccountRepository;
    private final SecurityGroupMapper securityGroupMapper;
    private final SecurityGroupCourthouseMapper securityGroupCourthouseMapper;
    private final SecurityGroupWithIdAndRoleAndUsersMapper securityGroupWithIdAndRoleAndUsersMapper;
    private final SecurityGroupCreationValidation securityGroupCreationValidation;

    private final List<SecurityRoleEnum> securityRolesAllowedToBeCreatedInGroup = List.of(SecurityRoleEnum.TRANSCRIBER, SecurityRoleEnum.TRANSLATION_QA);

    @Override
    public SecurityGroupWithIdAndRoleAndUsers getSecurityGroup(Integer securityGroupId) {
        SecurityGroupEntity securityGroup = securityGroupRepository.findById(securityGroupId)
            .orElseThrow(() -> new DartsApiException(SECURITY_GROUP_NOT_FOUND));

        return securityGroupWithIdAndRoleAndUsersMapper.mapToSecurityGroupWithIdAndRoleAndUsers(securityGroup);
    }

    @Override
    @Transactional
    public SecurityGroupWithIdAndRole createSecurityGroup(SecurityGroupPostRequest securityGroupRequest) {
        validateCreateSecurityGroupRequest(securityGroupRequest);

        SecurityGroupModel securityGroupModel = securityGroupMapper.mapToSecurityGroupModel(securityGroupRequest);

        SecurityGroupEntity createdSecurityGroupEntity = createAndSaveSecurityGroup(securityGroupModel);

        SecurityGroupWithIdAndRole securityGroupPostResponse = securityGroupMapper.mapToSecurityGroupWithIdAndRole(createdSecurityGroupEntity);
        securityGroupPostResponse.setSecurityRoleId(createdSecurityGroupEntity.getSecurityRoleEntity().getId());

        return securityGroupPostResponse;
    }

    private void validateCreateSecurityGroupRequest(SecurityGroupPostRequest securityGroupRequest) {
        //check the roleType is allowed
        SecurityRoleEnum requestedSecurityRole = SecurityRoleEnum.valueOfId(securityGroupRequest.getSecurityRoleId());
        if (!securityRolesAllowedToBeCreatedInGroup.contains(requestedSecurityRole)) {
            List<String> listOfAllowedRoleNames = securityRolesAllowedToBeCreatedInGroup.stream().map(role -> role.name()).toList();
            String errorMessage = MessageFormat.format("A group with a role of type {0} has been requested, but only roles of type {1} are allowed.",
                                                       requestedSecurityRole.name(), listOfAllowedRoleNames);
            throw new DartsApiException(SECURITY_GROUP_NOT_ALLOWED, errorMessage);
        }
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

    @Override
    public List<SecurityGroupWithIdAndRoleAndUsers> getSecurityGroups(List<Integer> roleIds, Integer courthouseId, Integer userId, Boolean singletonUser) {

        List<SecurityGroupEntity> securityGroupEntities = securityGroupRepository.findAll();

        securityGroupEntities = filterSecurityGroupEntitiesByRoleIds(securityGroupEntities, roleIds);
        securityGroupEntities = filterSecurityGroupEntitiesByCourthouseId(securityGroupEntities, courthouseId);
        securityGroupEntities = filterSecurityGroupEntitiesByUserId(securityGroupEntities, userId);
        securityGroupEntities = filterSecurityGroupEntitiesBySingleUser(securityGroupEntities, singletonUser);

        if (isNull(courthouseId)) {
            securityGroupEntities.forEach(entity -> entity.getUsers().clear());
        }

        return securityGroupEntities.stream()
            .map(securityGroupWithIdAndRoleAndUsersMapper::mapToSecurityGroupWithIdAndRoleAndUsers).toList();


    }

    @Transactional
    @Override
    public SecurityGroupWithIdAndRoleAndUsers modifySecurityGroup(Integer securityGroupId, SecurityGroupPatch securityGroupPatch) {

        Optional<SecurityGroupEntity> securityGroupEntityOptional = securityGroupRepository.findById(securityGroupId);

        if (securityGroupEntityOptional.isPresent()) {
            SecurityGroupEntity securityGroupEntity = securityGroupEntityOptional.get();
            updateSecurityGroupEntity(securityGroupPatch, securityGroupEntity);
            var updatedGroup = securityGroupRepository.saveAndFlush(securityGroupEntity);
            return securityGroupCourthouseMapper.mapToSecurityGroupWithCourthousesAndUsers(updatedGroup);
        } else {
            //throw a 404 not found
            throw new DartsApiException(
                SECURITY_GROUP_NOT_FOUND,
                String.format("Security group id %d not found", securityGroupId));
        }

    }

    void updateSecurityGroupEntity(SecurityGroupPatch securityGroupPatch, SecurityGroupEntity securityGroupEntity) {
        validate(securityGroupPatch, securityGroupEntity);

        String name = securityGroupPatch.getName();
        String displayName = securityGroupPatch.getDisplayName();


        if (StringUtils.isNotBlank(name)) {
            securityGroupEntity.setGroupName(name);
        }
        if (StringUtils.isNotBlank(displayName)) {
            securityGroupEntity.setDisplayName(displayName);
        }
        String description = securityGroupPatch.getDescription();
        if (description != null) {
            securityGroupEntity.setDescription(description);
        }
        List<Integer> courthouseIds = securityGroupPatch.getCourthouseIds();
        if (courthouseIds != null) {
            Set<CourthouseEntity> courthouseEntities = new HashSet<>();
            for (Integer courthouseId : courthouseIds) {
                Optional<CourthouseEntity> courthouseEntity = courthouseRepository.findById(courthouseId);
                if (courthouseEntity.isPresent()) {
                    courthouseEntities.add(courthouseEntity.get());
                } else {
                    throw new DartsApiException(
                        UserManagementError.COURTHOUSE_NOT_FOUND,
                        String.format("Courthouse id %d not found", courthouseId));
                }
            }
            securityGroupEntity.setCourthouseEntities(courthouseEntities);
        }

        patchSecurityGroupUsers(securityGroupPatch, securityGroupEntity);
    }

    private void patchSecurityGroupUsers(SecurityGroupPatch securityGroupPatch, SecurityGroupEntity securityGroupEntity) {

        List<Integer> userIds = securityGroupPatch.getUserIds();
        if (userIds == null) {
            return;
        }

        // get a list of system users for security group
        List<Integer> systemUserIds = securityGroupEntity
            .getUsers()
            .stream()
            .filter(user -> user.getIsSystemUser())
            .map(UserAccountEntity::getId)
            .toList();

        // check that users exist
        List<UserAccountEntity> patchUsers = userAccountRepository
            .findByIdIn(securityGroupPatch.getUserIds());

        if (userIds.size() > 0 && patchUsers.isEmpty()) {
            throw new DartsApiException(
                UserManagementError.USER_NOT_FOUND,
                String.format("No User accounts found for patch user IDs %s", securityGroupPatch.getUserIds()));
        }

        // filter the incoming list to remove system users
        List<Integer> patchNonSystemUserIds = patchUsers
            .stream().filter(user -> !user.getIsSystemUser())
            .map(UserAccountEntity::getId)
            .toList();

        // join the 2 lists - distinct
        List<Integer> combinedUserIds = Stream.concat(systemUserIds.stream(), patchNonSystemUserIds.stream())
            .distinct()
            .collect(Collectors.toList());

        if (combinedUserIds != null) {
            securityGroupEntity.getUsers().forEach(userAccountEntity -> userAccountEntity.getSecurityGroupEntities().remove(securityGroupEntity));
            securityGroupEntity.getUsers().clear();
            for (Integer userId : combinedUserIds) {
                Optional<UserAccountEntity> userAccountEntity = userAccountRepository.findById(userId);
                if (userAccountEntity.isPresent()) {
                    userAccountEntity.get().getSecurityGroupEntities().add(securityGroupEntity);
                    securityGroupEntity.getUsers().add(userAccountEntity.get());
                } else {
                    throw new DartsApiException(
                        UserManagementError.USER_NOT_FOUND,
                        String.format("User account id %d not found", userId));
                }
            }
        }
    }

    private void validate(SecurityGroupPatch securityGroupPatch, SecurityGroupEntity securityGroupEntity) {
        Integer id = securityGroupEntity.getId();

        if (StringUtils.isNotBlank(securityGroupPatch.getName())) {
            securityGroupRepository.findByGroupNameIgnoreCaseAndIdNot(securityGroupPatch.getName(), id)
                .ifPresent(existingGroup -> {
                    throw new DartsApiException(
                        UserManagementError.DUPLICATE_SECURITY_GROUP_NAME_NOT_PERMITTED,
                        "Attempt to use name of an existing group",
                        Collections.singletonMap("existing_group_id", existingGroup.getId())
                    );
                });
        }
        if (StringUtils.isNotBlank(securityGroupPatch.getDisplayName())) {
            securityGroupRepository.findByDisplayNameIgnoreCaseAndIdNot(securityGroupPatch.getDisplayName(), id)
                .ifPresent(existingGroup -> {
                    throw new DartsApiException(
                        UserManagementError.DUPLICATE_SECURITY_GROUP_NAME_NOT_PERMITTED,
                        "Attempt to use display name of an existing group",
                        Collections.singletonMap("existing_group_id", existingGroup.getId())
                    );
                });
        }
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

    @SuppressWarnings({"PMD.UselessParentheses"})
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
