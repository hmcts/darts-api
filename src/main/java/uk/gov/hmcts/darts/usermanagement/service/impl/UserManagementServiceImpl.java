package uk.gov.hmcts.darts.usermanagement.service.impl;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.audit.api.AuditActivity;
import uk.gov.hmcts.darts.audit.api.AuditApi;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.common.component.validation.Validator;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.SecurityGroupRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.common.validation.IdRequest;
import uk.gov.hmcts.darts.transcriptions.service.TranscriptionService;
import uk.gov.hmcts.darts.usermanagement.component.UserManagementQuery;
import uk.gov.hmcts.darts.usermanagement.component.UserSearchQuery;
import uk.gov.hmcts.darts.usermanagement.exception.UserManagementError;
import uk.gov.hmcts.darts.usermanagement.mapper.impl.SecurityGroupIdMapper;
import uk.gov.hmcts.darts.usermanagement.mapper.impl.UserAccountMapper;
import uk.gov.hmcts.darts.usermanagement.model.User;
import uk.gov.hmcts.darts.usermanagement.model.UserPatch;
import uk.gov.hmcts.darts.usermanagement.model.UserSearch;
import uk.gov.hmcts.darts.usermanagement.model.UserWithId;
import uk.gov.hmcts.darts.usermanagement.model.UserWithIdAndTimestamps;
import uk.gov.hmcts.darts.usermanagement.service.UserManagementService;
import uk.gov.hmcts.darts.usermanagement.service.validation.UserAccountExistsValidator;
import uk.gov.hmcts.darts.usermanagement.service.validation.UserTypeValidator;
import uk.gov.hmcts.darts.usermanagement.validator.AuthorisedUserPermissionsValidator;
import uk.gov.hmcts.darts.usermanagement.validator.UserActivateValidator;
import uk.gov.hmcts.darts.usermanagement.validator.UserDeactivateNotLastInSuperAdminGroupValidator;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toSet;
import static uk.gov.hmcts.darts.usermanagement.auditing.UserAccountUpdateAuditActivityProvider.auditActivitiesFor;

@Service
@RequiredArgsConstructor
@SuppressWarnings("PMD.CouplingBetweenObjects")//TODO - refactor to reduce coupling when this class is next edited
public class UserManagementServiceImpl implements UserManagementService {

    private final UserAccountMapper userAccountMapper;
    private final SecurityGroupIdMapper securityGroupIdMapper;
    private final UserAccountRepository userAccountRepository;
    private final SecurityGroupRepository securityGroupRepository;
    private final AuthorisationApi authorisationApi;
    private final UserSearchQuery userSearchQuery;
    private final UserManagementQuery userManagementQuery;
    private final Validator<User> userEmailValidator;
    private final UserAccountExistsValidator userAccountExistsValidator;
    private final UserTypeValidator userTypeValidator;
    private final AuthorisedUserPermissionsValidator userActivationPermissionsValidator;
    private final UserDeactivateNotLastInSuperAdminGroupValidator userNotLastSuperAdminValidator;
    private final TranscriptionService transcriptionService;
    private final AuditApi auditApi;
    private final UserActivateValidator authoriseValidator;

    @Override
    @Transactional
    public UserWithId createUser(User user) {
        userEmailValidator.validate(user);

        var userEntity = userAccountMapper.mapToUserEntity(user);
        if (isNull(userEntity.isActive())) {
            userEntity.setActive(true);
        }
        userEntity.setIsSystemUser(false);
        mapSecurityGroupsToUserEntity(user.getSecurityGroupIds(), userEntity);

        var currentUser = authorisationApi.getCurrentUser();
        userEntity.setCreatedBy(currentUser);
        userEntity.setLastModifiedBy(currentUser);

        var now = OffsetDateTime.now();
        userEntity.setCreatedDateTime(now);
        userEntity.setLastModifiedDateTime(now);

        var createdUserEntity = userAccountRepository.save(userEntity);
        auditApi.record(AuditActivity.CREATE_USER, authorisationApi.getCurrentUser());

        UserWithId userWithId = userAccountMapper.mapToUserWithIdModel(createdUserEntity);
        List<Integer> securityGroupIds = securityGroupIdMapper.mapSecurityGroupEntitiesToIds(createdUserEntity.getSecurityGroupEntities());
        userWithId.setSecurityGroupIds(securityGroupIds);

        return userWithId;
    }

    @Override
    @Transactional
    public UserWithIdAndTimestamps modifyUser(Integer userId, UserPatch userPatch) {
        userAccountExistsValidator.validate(userId);
        userTypeValidator.validate(userId);
        userActivationPermissionsValidator.validate(userPatch);
        userNotLastSuperAdminValidator.validate(new IdRequest<>(userPatch, userId));
        authoriseValidator.validate(new IdRequest<>(userPatch, userId));

        var userAccountEntity = userAccountRepository.findById(userId)
            .orElseThrow(() -> new NoSuchElementException("No value present"));

        var activitiesForAudit = auditActivitiesFor(userAccountEntity, userPatch);
        var rolledBackTranscriptionsIds = updatedUserAccount(userPatch, userAccountEntity);
        auditApi.recordAll(activitiesForAudit);

        UserWithIdAndTimestamps user = userAccountMapper.mapToUserWithIdAndLastLoginModel(userAccountEntity);

        // lets add the rolled back transcription ids
        if (!rolledBackTranscriptionsIds.isEmpty()) {
            user.setRolledBackTranscriptRequests(rolledBackTranscriptionsIds);
        }

        List<Integer> securityGroupIds = securityGroupIdMapper.mapSecurityGroupEntitiesToIds(userAccountEntity.getSecurityGroupEntities());
        user.setSecurityGroupIds(securityGroupIds);

        return user;
    }

    @Override
    public List<UserWithIdAndTimestamps> search(UserSearch userSearch) {
        List<UserWithIdAndTimestamps> userWithIdAndLastLoginList = new ArrayList<>();

        userSearchQuery.getUsers(userSearch.getFullName(), userSearch.getEmailAddress(), userSearch.getActive())
            .forEach(userAccountEntity -> {
                UserWithIdAndTimestamps userWithIdAndLastLogin = userAccountMapper.mapToUserWithIdAndUserFullName(userAccountEntity);
                userWithIdAndLastLogin.setSecurityGroupIds(securityGroupIdMapper.mapSecurityGroupEntitiesToIds(userAccountEntity.getSecurityGroupEntities()));
                userWithIdAndLastLoginList.add(userWithIdAndLastLogin);
            });

        return userWithIdAndLastLoginList;
    }

    @SuppressWarnings("Convert2MethodRef")
    @Override
    public List<UserWithIdAndTimestamps> getUsers(String emailAddress, List<Integer> userIds) {
        return userManagementQuery.getUsers(emailAddress, userIds).stream()
            .map(this::toUserWithIdAndTimestamps)
            .toList();
    }

    private UserWithIdAndTimestamps toUserWithIdAndTimestamps(UserAccountEntity userAccountEntity) {
        var userWithIdAndLastLogin = userAccountMapper.mapToUserWithIdAndLastLoginModel(userAccountEntity);
        userWithIdAndLastLogin.setSecurityGroupIds(securityGroupIdMapper.mapSecurityGroupEntitiesToIds(userAccountEntity.getSecurityGroupEntities()));
        return userWithIdAndLastLogin;
    }

    @Override
    public UserWithIdAndTimestamps getUserById(Integer userId) {
        Optional<UserAccountEntity> entity = userAccountRepository.findById(userId);
        if (entity.isPresent()) {
            userTypeValidator.validate(userId);
            return securityGroupIdMapper.mapToUserWithSecurityGroups(entity.get());
        }
        throw new DartsApiException(
            UserManagementError.USER_NOT_FOUND,
            String.format("User id %d not found", userId));
    }

    private List<Integer> updatedUserAccount(UserPatch userPatch, UserAccountEntity userEntity) {
        List<Integer> rolledBackTranscriptions = updateEntity(userPatch, userEntity);
        userAccountRepository.save(userEntity);
        return rolledBackTranscriptions;
    }

    private List<Integer> updateEntity(UserPatch userPatch, UserAccountEntity userAccountEntity) {
        List<Integer> rolledBackTranscriptionsList = new ArrayList<>();

        if (userPatch.getEmailAddress() != null && !userPatch.getEmailAddress().equals(userAccountEntity.getEmailAddress())) {
            userEmailValidator.validate(new User(userPatch.getFullName(), userPatch.getEmailAddress(), null, null, null));
            userAccountEntity.setEmailAddress(userPatch.getEmailAddress());
        }

        String name = userPatch.getFullName();
        if (name != null) {
            userAccountEntity.setUserFullName(name);
        }

        String description = userPatch.getDescription();
        if (description != null) {
            userAccountEntity.setUserDescription(description);
        }

        Boolean active = userPatch.getActive();
        if (active != null) {
            userAccountEntity.setActive(active);

            // if we are disabling the use then disable the transcriptions
            // and remove user from security groups
            if (active.equals(Boolean.FALSE)) {
                unassignUserFromGroupsTheyArePartOf(userAccountEntity);
                rolledBackTranscriptionsList = transcriptionService.rollbackUserTranscriptions(userAccountEntity);
            }
        }

        if (BooleanUtils.isTrue(userAccountEntity.isActive())) {
            mapSecurityGroupsToUserEntity(userPatch.getSecurityGroupIds(), userAccountEntity);
        } else {
            userAccountEntity.setSecurityGroupEntities(Collections.emptySet());
        }

        userAccountEntity.setLastModifiedBy(authorisationApi.getCurrentUser());
        userAccountEntity.setLastModifiedDateTime(OffsetDateTime.now());

        return rolledBackTranscriptionsList;
    }

    private void unassignUserFromGroupsTheyArePartOf(UserAccountEntity userAccount) {
        Set<SecurityGroupEntity> groupEntities = userAccount.getSecurityGroupEntities();
        userAccount.getSecurityGroupEntities().clear();
        for (SecurityGroupEntity groupEntity : groupEntities) {
            groupEntity.getUsers().remove(userAccount);
            securityGroupRepository.save(groupEntity);
        }
    }

    private void mapSecurityGroupsToUserEntity(List<Integer> securityGroups, UserAccountEntity userAccountEntity) {
        if (securityGroups != null) {
            Set<SecurityGroupEntity> securityGroupEntities = securityGroups.stream()
                .map(securityGroupRepository::findById)
                .map(Optional::orElseThrow)
                .collect(toSet());
            userAccountEntity.setSecurityGroupEntities(securityGroupEntities);
        }
    }

}