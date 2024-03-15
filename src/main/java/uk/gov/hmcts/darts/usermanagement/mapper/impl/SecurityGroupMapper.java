package uk.gov.hmcts.darts.usermanagement.mapper.impl;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.ReportingPolicy;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.model.SecurityGroupModel;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroup;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroupWithIdAndRole;

@Mapper(componentModel = "spring",
    unmappedSourcePolicy = ReportingPolicy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SecurityGroupMapper {

    @Mappings({
        @Mapping(source = "name", target = "groupName"),
    })
    SecurityGroupEntity mapToSecurityGroupEntity(SecurityGroupModel securityGroupModel);

    SecurityGroupModel mapToSecurityGroupModel(SecurityGroup securityGroup);

    @Mappings({
        @Mapping(source = "groupName", target = "name"),
    })
    SecurityGroupWithIdAndRole mapToSecurityGroupWithIdAndRole(SecurityGroupEntity securityGroupEntity);

}
