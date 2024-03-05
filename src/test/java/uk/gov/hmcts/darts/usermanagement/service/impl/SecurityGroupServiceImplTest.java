package uk.gov.hmcts.darts.usermanagement.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.component.validation.Validator;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.SecurityRoleEntity;
import uk.gov.hmcts.darts.common.repository.SecurityGroupRepository;
import uk.gov.hmcts.darts.common.repository.SecurityRoleRepository;
import uk.gov.hmcts.darts.usermanagement.mapper.impl.SecurityGroupCourthouseMapper;
import uk.gov.hmcts.darts.usermanagement.mapper.impl.SecurityGroupMapper;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroup;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityGroupServiceImplTest {

    SecurityGroupServiceImpl securityGroupService;
    @Mock
    SecurityGroupRepository securityGroupRepository;
    @Mock
    SecurityRoleRepository securityRoleRepository;
    @Mock
    SecurityGroupMapper securityGroupMapper;
    @Mock
    SecurityGroupCourthouseMapper securityGroupCourthouseMapper;
    @Mock
    Validator<SecurityGroup> securityGroupCreationValidation;

    @BeforeEach
    void setUp() {
        securityGroupService = new SecurityGroupServiceImpl(
            securityGroupRepository,
            securityRoleRepository,
            securityGroupMapper,
            securityGroupCourthouseMapper,
            securityGroupCreationValidation
        );
    }

    @Test
    void testGetAllSecurityGroups() {

        List<SecurityGroupEntity> securityGroupEntities = List.of(
            createSecurityGroupEntity(1,10,20),
            createSecurityGroupEntity(2,11,21),
            createSecurityGroupEntity(3,12,22)
        );

        when(securityGroupRepository.findAll()).thenReturn(securityGroupEntities);

        var filteredGroups = securityGroupService.getSecurityGroups(null, null);

        assertEquals(3, filteredGroups.size());
    }

    @Test
    void testGetSecurityGroupsFilteredByRoleId() {

        List<Integer> listOfRoleIds = List.of(10);

        List<SecurityGroupEntity> securityGroupEntities = List.of(
            createSecurityGroupEntity(1,10,20),
            createSecurityGroupEntity(2,11,21),
            createSecurityGroupEntity(3,12,22)
        );

        when(securityGroupRepository.findAll()).thenReturn(securityGroupEntities);

        var filteredGroups = securityGroupService.getSecurityGroups(listOfRoleIds, null);

        assertEquals(1, filteredGroups.size());
    }

    @Test
    void testGetSecurityGroupsFilteredByMultipleRoleIds() {

        List<Integer> listOfRoleIds = List.of(10,11);

        List<SecurityGroupEntity> securityGroupEntities = List.of(
            createSecurityGroupEntity(1,10,20),
            createSecurityGroupEntity(2,11,21),
            createSecurityGroupEntity(3,12,22)
        );

        when(securityGroupRepository.findAll()).thenReturn(securityGroupEntities);

        var filteredGroups = securityGroupService.getSecurityGroups(listOfRoleIds, null);

        assertEquals(2, filteredGroups.size());
    }

    @Test
    void testGetSecurityGroupsFilterByRoleIdReturningNoMatch() {

        List<Integer> listOfRoleIds = List.of(30);

        List<SecurityGroupEntity> securityGroupEntities = List.of(
            createSecurityGroupEntity(1,10,20),
            createSecurityGroupEntity(2,11,21),
            createSecurityGroupEntity(3,12,22)
        );

        when(securityGroupRepository.findAll()).thenReturn(securityGroupEntities);

        var filteredGroups = securityGroupService.getSecurityGroups(listOfRoleIds, null);

        assertEquals(0, filteredGroups.size());
    }

    @Test
    void testGetSecurityGroupsFilteredByCourthouseId() {

        Integer courthouseId = 20;

        List<SecurityGroupEntity> securityGroupEntities = List.of(
            createSecurityGroupEntity(1,10,20),
            createSecurityGroupEntity(2,11,21),
            createSecurityGroupEntity(3,12,22)
        );

        when(securityGroupRepository.findAll()).thenReturn(securityGroupEntities);

        var filteredGroups = securityGroupService.getSecurityGroups(null, courthouseId);

        assertEquals(1, filteredGroups.size());
    }

    @Test
    void testGetSecurityGroupsFilteredByCourthouseIdReturningNoMatch() {

        Integer courthouseId = 50;

        List<SecurityGroupEntity> securityGroupEntities = List.of(
            createSecurityGroupEntity(1,10,20),
            createSecurityGroupEntity(2,11,21),
            createSecurityGroupEntity(3,12,22)
        );

        when(securityGroupRepository.findAll()).thenReturn(securityGroupEntities);

        var filteredGroups = securityGroupService.getSecurityGroups(null, courthouseId);

        assertEquals(0, filteredGroups.size());
    }

    @Test
    void testGetSecurityGroupsFilteredByRoleIdsAndCourthouseId() {

        Integer courthouseId = 20;
        List<Integer> listOfRoleIds = List.of(10,11);

        List<SecurityGroupEntity> securityGroupEntities = List.of(
            createSecurityGroupEntity(1,10,20),
            createSecurityGroupEntity(2,11,21),
            createSecurityGroupEntity(3,12,22)
        );

        when(securityGroupRepository.findAll()).thenReturn(securityGroupEntities);

        var filteredGroups = securityGroupService.getSecurityGroups(listOfRoleIds, courthouseId);

        assertEquals(1, filteredGroups.size());
    }

    private SecurityGroupEntity createSecurityGroupEntity(Integer securityGroupId, Integer roleId, Integer courthouseId) {
        SecurityRoleEntity securityRoleEntity = new SecurityRoleEntity();
        securityRoleEntity.setId(roleId);
        securityRoleEntity.setRoleName("Test Role " + roleId);

        CourthouseEntity courthouse = new CourthouseEntity();
        courthouse.setId(courthouseId);
        courthouse.setCourthouseName("Test Courthouse " + courthouseId);

        Set<CourthouseEntity> courthouseEntitySet = new HashSet<>();
        courthouseEntitySet.add(courthouse);

        SecurityGroupEntity securityGroupEntity = new SecurityGroupEntity();
        securityGroupEntity.setId(securityGroupId);
        securityGroupEntity.setGroupName("Test Group " + securityGroupId);
        securityGroupEntity.setSecurityRoleEntity(securityRoleEntity);
        securityGroupEntity.setCourthouseEntities(courthouseEntitySet);

        return securityGroupEntity;
    }
}
