package uk.gov.hmcts.darts.usermanagement.mapper.impl;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.model.SecurityGroupModel;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroupPostRequest;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroupWithIdAndRole;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroupWithIdAndRoleAndUsers;

import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSLATION_QA;

@Mapper(componentModel = "spring",
    unmappedSourcePolicy = ReportingPolicy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SecurityGroupMapper {

    @Mappings({
        @Mapping(source = "name", target = "groupName"),
    })
    SecurityGroupEntity mapToSecurityGroupEntity(SecurityGroupModel securityGroupModel);

    @Mappings({
        @Mapping(source = "securityRoleId", target = "roleId"),
        @Mapping(source = "securityRoleId", target = "useInterpreter", qualifiedByName = "toUseInterpreterFlag")
    })
    SecurityGroupModel mapToSecurityGroupModel(SecurityGroupPostRequest securityGroupPostRequest);

    @Mappings({
        @Mapping(source = "groupName", target = "name"),
    })
    SecurityGroupWithIdAndRole mapToSecurityGroupWithIdAndRole(SecurityGroupEntity securityGroupEntity);

    @Mappings({
        @Mapping(source = "groupName", target = "name"),
    })
    SecurityGroupWithIdAndRoleAndUsers mapToSecurityGroupWithIdAndRoleAndUsers(SecurityGroupEntity securityGroupEntity);

    @Named("toUseInterpreterFlag")
    default boolean toUseInterpreterFlag(int roleId) {
        return TRANSLATION_QA.equals(SecurityRoleEnum.valueOfId(roleId));
    }

}
