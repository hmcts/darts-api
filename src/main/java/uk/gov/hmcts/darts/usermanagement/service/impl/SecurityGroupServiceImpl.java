package uk.gov.hmcts.darts.usermanagement.service.impl;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.audit.api.AuditApi;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity_;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.Objects.isNull;
import static uk.gov.hmcts.darts.audit.api.AuditActivity.CREATE_GROUP;
import static uk.gov.hmcts.darts.usermanagement.auditing.SecurityGroupUpdateAuditActivityProvider.auditActivitiesFor;
import static uk.gov.hmcts.darts.usermanagement.exception.UserManagementError.SECURITY_GROUP_NOT_ALLOWED;
import static uk.gov.hmcts.darts.usermanagement.exception.UserManagementError.SECURITY_GROUP_NOT_FOUND;

@Service
@RequiredArgsConstructor
@SuppressWarnings({
    "PMD.CouplingBetweenObjects",//TODO - refactor to reduce coupling when this class is next edited
    "PMD.TooManyMethods"//TODO - refactor to reduce methods when this class is next edited
})
public class SecurityGroupServiceImpl implements SecurityGroupService {

    private final SecurityGroupRepository securityGroupRepository;
    private final SecurityRoleRepository securityRoleRepository;
    private final CourthouseRepository courthouseRepository;
    private final UserAccountRepository userAccountRepository;
    private final SecurityGroupMapper securityGroupMapper;
    private final SecurityGroupCourthouseMapper securityGroupCourthouseMapper;
    private final SecurityGroupWithIdAndRoleAndUsersMapper securityGroupWithIdAndRoleAndUsersMapper;
    private final SecurityGroupCreationValidation securityGroupCreationValidation;
    private final AuditApi auditApi;
    private final AuthorisationApi authorisationApi;

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
            List<String> listOfAllowedRoleNames = securityRolesAllowedToBeCreatedInGroup.stream()
                .map(SecurityRoleEnum::name)
                .toList();
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


        var role = securityRoleRepository.getReferenceById(securityGroupModel.getRoleId());
        securityGroupEntity.setSecurityRoleEntity(role);

        var currentUser = authorisationApi.getCurrentUser();
        securityGroupEntity.setCreatedBy(currentUser);
        securityGroupEntity.setLastModifiedBy(currentUser);

        auditApi.record(CREATE_GROUP);
        return securityGroupRepository.saveAndFlush(securityGroupEntity);
    }

    @Override
    public List<SecurityGroupWithIdAndRoleAndUsers> getSecurityGroups(List<Integer> roleIds, Integer courthouseId, Integer userId, Boolean singletonUser) {

        List<SecurityGroupEntity> securityGroupEntities = securityGroupRepository.findAll(Sort.by(SecurityGroupEntity_.GROUP_NAME).ascending());

        securityGroupEntities = filterSecurityGroupEntitiesByRoleIds(securityGroupEntities, roleIds);
        securityGroupEntities = filterSecurityGroupEntitiesByCourthouseId(securityGroupEntities, courthouseId);
        securityGroupEntities = filterSecurityGroupEntitiesByUserId(securityGroupEntities, userId);
        securityGroupEntities = filterSecurityGroupEntitiesBySingleUser(securityGroupEntities, singletonUser);

        if (isNull(courthouseId)) {
            securityGroupEntities.forEach(entity -> entity.getUsers().clear());
        }

        return securityGroupEntities.stream()
            .map(securityGroupWithIdAndRoleAndUsersMapper::mapToSecurityGroupWithIdAndRoleAndUsers)
            .toList();
    }

    @Transactional
    @Override
    public SecurityGroupWithIdAndRoleAndUsers modifySecurityGroup(Integer securityGroupId, SecurityGroupPatch securityGroupPatch) {
        var securityGroupEntity = securityGroupRepository.findById(securityGroupId)
            .orElseThrow(() -> new DartsApiException(SECURITY_GROUP_NOT_FOUND));

        var auditableActivities = auditActivitiesFor(securityGroupEntity, securityGroupPatch);

        updateSecurityGroupEntity(securityGroupPatch, securityGroupEntity);

        var currentUser = authorisationApi.getCurrentUser();
        securityGroupEntity.setLastModifiedById(currentUser.getId());
        if (securityGroupEntity.getCreatedById() == null) {
            securityGroupEntity.setCreatedById(currentUser.getId());
        }
        var updatedGroup = securityGroupRepository.saveAndFlush(securityGroupEntity);

        auditApi.recordAll(auditableActivities);

        return securityGroupCourthouseMapper.mapToSecurityGroupWithCourthousesAndUsers(updatedGroup);
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
        if (securityGroupPatch.getCourthouseIds() != null) {
            // The PATCH contains the totality of courthouses we wish to have assigned to our group. So first we must remove any existing courthouses, and then
            // assign whatever courthouses are provided in the request.
            securityGroupEntity.setCourthouseEntities(new LinkedHashSet<>());

            securityGroupPatch.getCourthouseIds()
                .forEach(courthouseId -> addToSecurityGroup(courthouseId, securityGroupEntity));
        }

        patchSecurityGroupUsers(securityGroupPatch, securityGroupEntity);
    }

    private void addToSecurityGroup(Integer courthouseId, SecurityGroupEntity securityGroupEntity) {
        var courthouseEntity = courthouseRepository.findById(courthouseId)
            .orElseThrow(() -> new DartsApiException(
                UserManagementError.COURTHOUSE_NOT_FOUND,
                String.format("Courthouse id %d not found", courthouseId)));

        securityGroupEntity.getCourthouseEntities().add(courthouseEntity);
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
            .filter(user -> Boolean.TRUE.equals(user.getIsSystemUser()))
            .map(UserAccountEntity::getId)
            .toList();

        // check that users exist
        List<UserAccountEntity> patchUsers = userAccountRepository
            .findByIdInAndActive(securityGroupPatch.getUserIds(), true);

        if (!userIds.isEmpty() && patchUsers.isEmpty()) {
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
            .toList();

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