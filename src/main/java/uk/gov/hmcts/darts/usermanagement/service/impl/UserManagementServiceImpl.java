package uk.gov.hmcts.darts.usermanagement.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.SecurityGroupRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.usermanagement.component.UserSearchQuery;
import uk.gov.hmcts.darts.usermanagement.exception.UserManagementError;
import uk.gov.hmcts.darts.usermanagement.mapper.impl.UserAccountMapper;
import uk.gov.hmcts.darts.usermanagement.model.User;
import uk.gov.hmcts.darts.usermanagement.model.UserPatch;
import uk.gov.hmcts.darts.usermanagement.model.UserSearch;
import uk.gov.hmcts.darts.usermanagement.model.UserState;
import uk.gov.hmcts.darts.usermanagement.model.UserWithId;
import uk.gov.hmcts.darts.usermanagement.model.UserWithIdAndLastLogin;
import uk.gov.hmcts.darts.usermanagement.service.UserManagementService;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserManagementServiceImpl implements UserManagementService {

    private final UserAccountMapper userAccountMapper;
    private final UserAccountRepository userAccountRepository;
    private final SecurityGroupRepository securityGroupRepository;
    private final AuthorisationApi authorisationApi;
    private final UserSearchQuery userSearchQuery;

    @Override
    public UserWithId createUser(User user) {
        var userEntity = userAccountMapper.mapToUserEntity(user);
        userEntity.setIsSystemUser(false);
        mapSecurityGroupsToUserEntity(user.getSecurityGroups(), userEntity);

        var currentUser = authorisationApi.getCurrentUser();
        userEntity.setCreatedBy(currentUser);
        userEntity.setLastModifiedBy(currentUser);

        var now = OffsetDateTime.now();
        userEntity.setCreatedDateTime(now);
        userEntity.setLastModifiedDateTime(now);

        var createdUserEntity = userAccountRepository.save(userEntity);

        UserWithId userWithId = userAccountMapper.mapToUserWithIdModel(createdUserEntity);
        List<Integer> securityGroupIds = mapSecurityGroupEntitiesToIds(createdUserEntity.getSecurityGroupEntities());
        userWithId.setSecurityGroups(securityGroupIds);

        return userWithId;
    }

    @Override
    public UserWithIdAndLastLogin modifyUser(Integer userId, UserPatch userPatch) {
        var userEntity = userAccountRepository.findById(userId)
            .orElseThrow(() -> new DartsApiException(
                UserManagementError.USER_NOT_FOUND,
                String.format("User id %d not found", userId)
            ));
        updateEntity(userPatch, userEntity);

        UserAccountEntity updatedUserEntity = userAccountRepository.save(userEntity);

        UserWithIdAndLastLogin user = userAccountMapper.mapToUserWithIdAndLastLoginModel(updatedUserEntity);
        List<Integer> securityGroupIds = mapSecurityGroupEntitiesToIds(updatedUserEntity.getSecurityGroupEntities());
        user.setSecurityGroups(securityGroupIds);

        return user;
    }

    @Override
    public List<UserWithIdAndLastLogin> search(UserSearch userSearch) {
        List<UserWithIdAndLastLogin> userWithIdAndLastLoginList = new ArrayList<>();

        userSearchQuery.getUsers(userSearch.getFullName(), userSearch.getEmailAddress())
            .forEach(userAccountEntity -> {
                UserWithIdAndLastLogin userWithIdAndLastLogin = userAccountMapper.mapToUserWithIdAndLastLoginModel(userAccountEntity);
                userWithIdAndLastLogin.setSecurityGroups(mapSecurityGroupEntitiesToIds(userAccountEntity.getSecurityGroupEntities()));
                userWithIdAndLastLoginList.add(userWithIdAndLastLogin);
            });

        return userWithIdAndLastLoginList;
    }

    private void updateEntity(UserPatch user, UserAccountEntity userAccountEntity) {
        String name = user.getFullName();
        if (name != null) {
            userAccountEntity.setUserName(name);
        }

        String description = user.getDescription();
        if (description != null) {
            userAccountEntity.setUserDescription(description);
        }

        UserState state = user.getState();
        if (state != null) {
            userAccountEntity.setState(userAccountMapper.mapToUserStateValue(state));
        }

        if (UserState.DISABLED.equals(user.getState())) {
            userAccountEntity.setSecurityGroupEntities(Collections.emptySet());
        } else {
            mapSecurityGroupsToUserEntity(user.getSecurityGroups(), userAccountEntity);
        }

        userAccountEntity.setLastModifiedBy(authorisationApi.getCurrentUser());
        userAccountEntity.setLastModifiedDateTime(OffsetDateTime.now());
    }

    private void mapSecurityGroupsToUserEntity(List<Integer> securityGroups, UserAccountEntity userAccountEntity) {
        if (securityGroups != null) {
            Set<SecurityGroupEntity> securityGroupEntities = securityGroups.stream()
                .map(securityGroupRepository::findById)
                .map(Optional::orElseThrow)
                .collect(Collectors.toSet());
            userAccountEntity.setSecurityGroupEntities(securityGroupEntities);
        }
    }

    private List<Integer> mapSecurityGroupEntitiesToIds(Set<SecurityGroupEntity> securityGroupEntities) {
        return securityGroupEntities.stream()
            .map(SecurityGroupEntity::getId)
            .toList();
    }

}
