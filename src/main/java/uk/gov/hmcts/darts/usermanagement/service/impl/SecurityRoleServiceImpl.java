package uk.gov.hmcts.darts.usermanagement.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.SecurityRoleEntity;
import uk.gov.hmcts.darts.common.repository.SecurityRoleRepository;
import uk.gov.hmcts.darts.usermanagement.mapper.impl.SecurityRoleMapper;
import uk.gov.hmcts.darts.usermanagement.model.Role;
import uk.gov.hmcts.darts.usermanagement.service.SecurityRoleService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SecurityRoleServiceImpl implements SecurityRoleService {

    private final SecurityRoleRepository securityRoleRepository;
    private final SecurityRoleMapper securityRoleMapper;

    @Override
    public List<Role> getAllRoles() {
        List<SecurityRoleEntity> allSecurityRoleEntities = securityRoleRepository.findAllByOrderById();
        return securityRoleMapper.mapToSecurityRoles(allSecurityRoleEntities);
    }
}
