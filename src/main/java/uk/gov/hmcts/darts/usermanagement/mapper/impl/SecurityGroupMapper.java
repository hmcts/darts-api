package uk.gov.hmcts.darts.usermanagement.mapper.impl;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.ReportingPolicy;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroup;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroupWithIdAndRole;

@Mapper(componentModel = "spring",
    unmappedSourcePolicy = ReportingPolicy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SecurityGroupMapper {

    @Mappings({
        @Mapping(source = "name", target = "groupName"),
        @Mapping(source = "displayName", target = "displayName"),
        @Mapping(source = "description", target = "description"),
        @Mapping(target = "useInterpreter", constant = "false")
    })
    SecurityGroupEntity mapToSecurityGroupEntity(SecurityGroup securityGroup);

    @Mappings({
        @Mapping(source = "id", target = "id"),
        @Mapping(source = "groupName", target = "name"),
        @Mapping(source = "displayName", target = "displayName"),
        @Mapping(source = "description", target = "description"),
        @Mapping(source = "globalAccess", target = "globalAccess"),
        @Mapping(source = "displayState", target = "displayState")
    })
    SecurityGroupWithIdAndRole mapToSecurityGroupWithIdAndRole(SecurityGroupEntity securityGroupEntity);

}
