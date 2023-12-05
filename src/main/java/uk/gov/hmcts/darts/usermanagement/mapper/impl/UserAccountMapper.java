package uk.gov.hmcts.darts.usermanagement.mapper.impl;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.ReportingPolicy;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.usermanagement.model.User;
import uk.gov.hmcts.darts.usermanagement.model.UserState;
import uk.gov.hmcts.darts.usermanagement.model.UserWithId;
import uk.gov.hmcts.darts.usermanagement.model.UserWithIdAndLastLogin;

@Mapper(componentModel = "spring",
    unmappedSourcePolicy = ReportingPolicy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserAccountMapper {

    @Mappings({
        @Mapping(source = "fullName", target = "userName"),
        @Mapping(source = "emailAddress", target = "emailAddress"),
        @Mapping(source = "description", target = "userDescription"),
        @Mapping(source = "state", target = "active"),
    })
    UserAccountEntity mapToUserEntity(User user);

    @Mappings({
        @Mapping(source = "id", target = "id"),
        @Mapping(source = "userName", target = "fullName"),
        @Mapping(source = "emailAddress", target = "emailAddress"),
        @Mapping(source = "userDescription", target = "description"),
        @Mapping(source = "active", target = "state")
    })
    UserWithId mapToUserWithIdModel(UserAccountEntity userAccountEntity);

    @Mappings({
        @Mapping(source = "id", target = "id"),
        @Mapping(source = "userName", target = "fullName"),
        @Mapping(source = "emailAddress", target = "emailAddress"),
        @Mapping(source = "userDescription", target = "description"),
        @Mapping(source = "active", target = "state"),
        @Mapping(source = "lastLoginTime", target = "lastLogin")
    })
    UserWithIdAndLastLogin mapToUserWithIdAndLastLoginModel(UserAccountEntity userAccountEntity);

    default UserState mapToUserState(boolean stateValue) {
        return stateValue ? UserState.ENABLED : UserState.DISABLED;
    }

    default boolean mapToUserStateValue(UserState userState) {
        return UserState.ENABLED.equals(userState);
    }

}
