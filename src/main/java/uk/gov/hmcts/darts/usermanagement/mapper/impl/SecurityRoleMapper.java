package uk.gov.hmcts.darts.usermanagement.mapper.impl;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import uk.gov.hmcts.darts.common.entity.SecurityRoleEntity;
import uk.gov.hmcts.darts.usermanagement.model.Role;

import java.util.List;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = "spring")
@FunctionalInterface
public interface SecurityRoleMapper {

    List<Role> mapToSecurityRoles(List<SecurityRoleEntity> securityRoles);

}
