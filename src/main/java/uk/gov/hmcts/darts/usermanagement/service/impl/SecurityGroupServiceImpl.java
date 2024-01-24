package uk.gov.hmcts.darts.usermanagement.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.component.validation.Validator;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.repository.SecurityGroupRepository;
import uk.gov.hmcts.darts.common.repository.SecurityRoleRepository;
import uk.gov.hmcts.darts.usermanagement.mapper.impl.SecurityGroupCourthouseMapper;
import uk.gov.hmcts.darts.usermanagement.mapper.impl.SecurityGroupMapper;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroup;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroupWithIdAndRole;
import uk.gov.hmcts.darts.usermanagement.service.SecurityGroupService;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SecurityGroupServiceImpl implements SecurityGroupService {

    private final SecurityGroupRepository securityGroupRepository;
    private final SecurityRoleRepository securityRoleRepository;
    private final SecurityGroupMapper securityGroupMapper;
    private final SecurityGroupCourthouseMapper securityGroupCourthouseMapper;

    private final Validator<SecurityGroup> securityGroupCreationValidation;

    @Override
    @Transactional
    public SecurityGroupWithIdAndRole createSecurityGroup(SecurityGroup securityGroup) {
        securityGroupCreationValidation.validate(securityGroup);

        var securityGroupEntity = securityGroupMapper.mapToSecurityGroupEntity(securityGroup);
        securityGroupEntity.setGlobalAccess(false);
        securityGroupEntity.setDisplayState(true);

        var transcriberRoleEntity = securityRoleRepository.getReferenceById(SecurityRoleEnum.TRANSCRIBER.getId());
        securityGroupEntity.setSecurityRoleEntity(transcriberRoleEntity);

        var createdSecurityGroupEntity = securityGroupRepository.save(securityGroupEntity);

        var securityGroupWithIdAndRole = securityGroupMapper.mapToSecurityGroupWithIdAndRole(createdSecurityGroupEntity);
        securityGroupWithIdAndRole.setSecurityRoleId(createdSecurityGroupEntity.getSecurityRoleEntity().getId());

        return securityGroupWithIdAndRole;
    }

    public List<SecurityGroupWithIdAndRole> getSecurityGroups() {
        List<SecurityGroupEntity> securityGroupEntities = securityGroupRepository.findAll();
        List<SecurityGroupWithIdAndRole> securityGroupWithIdAndRoles = new ArrayList<>();
        for (SecurityGroupEntity securityGroupEntity: securityGroupEntities) {
            securityGroupWithIdAndRoles
                .add(securityGroupCourthouseMapper.mapToSecurityGroupWithIdAndRoleWithCourthouse(securityGroupEntity));
        }
        return securityGroupWithIdAndRoles;
    }

}
