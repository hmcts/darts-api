package uk.gov.hmcts.darts.usermanagement.mapper.impl;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.ReportingPolicy;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.usermanagement.model.User;
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
        @Mapping(source = "active", target = "active"),
    })
    UserAccountEntity mapToUserEntity(User user);

    @Mappings({
        @Mapping(source = "id", target = "id"),
        @Mapping(source = "userName", target = "fullName"),
        @Mapping(source = "emailAddress", target = "emailAddress"),
        @Mapping(source = "userDescription", target = "description"),
        @Mapping(source = "active", target = "active")
    })
    UserWithId mapToUserWithIdModel(UserAccountEntity userAccountEntity);

    @Mappings({
        @Mapping(source = "id", target = "id"),
        @Mapping(source = "userName", target = "fullName"),
        @Mapping(source = "emailAddress", target = "emailAddress"),
        @Mapping(source = "userDescription", target = "description"),
        @Mapping(source = "active", target = "active"),
        @Mapping(source = "lastLoginTime", target = "lastLogin")
    })
    UserWithIdAndLastLogin mapToUserWithIdAndLastLoginModel(UserAccountEntity userAccountEntity);
}
