package uk.gov.hmcts.darts.usermanagement.mapper.impl;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.ReportingPolicy;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.usermanagement.model.User;
import uk.gov.hmcts.darts.usermanagement.model.UserWithId;
import uk.gov.hmcts.darts.usermanagement.model.UserWithIdAndTimestamps;

@Mapper(componentModel = "spring",
    unmappedSourcePolicy = ReportingPolicy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserAccountMapper {

    String USER_FULL_NAME = "userFullName";
    String EMAIL_ADDRESS = "emailAddress";
    String USER_DESCRIPTION = "userDescription";
    String FULL_NAME = "fullName";
    String DESCRIPTION = "description";
    String ACTIVE = "active";
    String LAST_LOGIN_AT = "lastLoginAt";
    String LAST_LOGIN_TIME = "lastLoginTime";
    String LAST_MODIFIED_AT = "lastModifiedAt";
    String LAST_MODIFIED_DATE_TIME = "lastModifiedDateTime";
    String CREATED_AT = "createdAt";
    String CREATED_DATE_TIME = "createdDateTime";
    String ID = "id";
    String IS_SYSTEM_USER = "isSystemUser";

    @Mappings({
        @Mapping(source = FULL_NAME, target = USER_FULL_NAME),
        @Mapping(source = EMAIL_ADDRESS, target = EMAIL_ADDRESS),
        @Mapping(source = DESCRIPTION, target = USER_DESCRIPTION),
        @Mapping(source = ACTIVE, target = ACTIVE),
    })
    UserAccountEntity mapToUserEntity(User user);

    @Mappings({
        @Mapping(source = ID, target = ID),
        @Mapping(source = USER_FULL_NAME, target = FULL_NAME),
        @Mapping(source = EMAIL_ADDRESS, target = EMAIL_ADDRESS),
        @Mapping(source = USER_DESCRIPTION, target = DESCRIPTION),
        @Mapping(source = ACTIVE, target = ACTIVE)
    })
    UserWithId mapToUserWithIdModel(UserAccountEntity userAccountEntity);

    @Mappings({
        @Mapping(source = ID, target = ID),
        @Mapping(source = USER_FULL_NAME, target = FULL_NAME),
        @Mapping(source = EMAIL_ADDRESS, target = EMAIL_ADDRESS),
        @Mapping(source = USER_DESCRIPTION, target = DESCRIPTION),
        @Mapping(source = ACTIVE, target = ACTIVE),
        @Mapping(source = LAST_LOGIN_TIME, target = LAST_LOGIN_AT),
        @Mapping(source = LAST_MODIFIED_DATE_TIME, target = LAST_MODIFIED_AT),
        @Mapping(source = CREATED_DATE_TIME, target = CREATED_AT),
        @Mapping(source = IS_SYSTEM_USER, target = IS_SYSTEM_USER)
    })
    UserWithIdAndTimestamps mapToUserWithIdAndLastLoginModel(UserAccountEntity userAccountEntity);

    @Mappings({
        @Mapping(source = ID, target = ID),
        @Mapping(source = USER_FULL_NAME, target = FULL_NAME),
        @Mapping(source = EMAIL_ADDRESS, target = EMAIL_ADDRESS),
        @Mapping(source = USER_DESCRIPTION, target = DESCRIPTION),
        @Mapping(source = ACTIVE, target = ACTIVE),
        @Mapping(source = LAST_LOGIN_TIME, target = LAST_LOGIN_AT),
        @Mapping(source = LAST_MODIFIED_DATE_TIME, target = LAST_MODIFIED_AT),
        @Mapping(source = CREATED_DATE_TIME, target = CREATED_AT)
    })
    UserWithIdAndTimestamps mapToUserWithIdAndUserFullName(UserAccountEntity userAccountEntity);
}