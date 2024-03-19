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
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.model.SecurityGroupModel;
import uk.gov.hmcts.darts.common.repository.SecurityGroupRepository;
import uk.gov.hmcts.darts.common.repository.SecurityRoleRepository;
import uk.gov.hmcts.darts.usermanagement.mapper.impl.SecurityGroupCourthouseMapper;
import uk.gov.hmcts.darts.usermanagement.mapper.impl.SecurityGroupMapper;

import java.util.HashSet;
import java.util.LinkedHashSet;
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
    Validator<SecurityGroupModel> securityGroupCreationValidation;

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

        var filteredGroups = securityGroupService.getSecurityGroups(null, null, null, null);

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

        var filteredGroups = securityGroupService.getSecurityGroups(listOfRoleIds, null, null, null);

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

        var filteredGroups = securityGroupService.getSecurityGroups(listOfRoleIds, null, null, null);

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

        var filteredGroups = securityGroupService.getSecurityGroups(listOfRoleIds, null, null, null);

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

        var filteredGroups = securityGroupService.getSecurityGroups(null, courthouseId, null, null);

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

        var filteredGroups = securityGroupService.getSecurityGroups(null, courthouseId, null, null);

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

        var filteredGroups = securityGroupService.getSecurityGroups(listOfRoleIds, courthouseId, null, null);

        assertEquals(1, filteredGroups.size());
    }

    @Test
    void testGetSecurityGroupsFilteredByUserId() {

        Integer userId = 2;

        List<SecurityGroupEntity> securityGroupEntities = List.of(
            createSecurityGroupEntity(1,10,20),
            createSecurityGroupEntity(2,11,21),
            createSecurityGroupEntity(3,12,22)
        );

        addUserAccountsToSecurityGroups(securityGroupEntities);

        when(securityGroupRepository.findAll()).thenReturn(securityGroupEntities);

        var filteredGroups = securityGroupService.getSecurityGroups(null, null, userId, null);

        assertEquals(1, filteredGroups.size());
    }

    @Test
    void testGetSecurityGroupsFilteredByUserIdNoMatch() {

        Integer userId = 20;

        List<SecurityGroupEntity> securityGroupEntities = List.of(
            createSecurityGroupEntity(1,10,20),
            createSecurityGroupEntity(2,11,21),
            createSecurityGroupEntity(3,12,22)
        );

        addUserAccountsToSecurityGroups(securityGroupEntities);

        when(securityGroupRepository.findAll()).thenReturn(securityGroupEntities);

        var filteredGroups = securityGroupService.getSecurityGroups(null, null, userId, null);

        assertEquals(0, filteredGroups.size());
    }

    @Test
    void testGetSecurityGroupsFilteredBySingletonUser() {

        Boolean singletonUser = true;

        List<SecurityGroupEntity> securityGroupEntities = List.of(
            createSecurityGroupEntity(1,10,20),
            createSecurityGroupEntity(2,11,21),
            createSecurityGroupEntity(3,12,22)
        );

        addUserAccountsToSecurityGroups(securityGroupEntities);

        when(securityGroupRepository.findAll()).thenReturn(securityGroupEntities);

        var filteredGroups = securityGroupService.getSecurityGroups(null, null, null, singletonUser);

        assertEquals(1, filteredGroups.size());
    }

    @Test
    void testGetSecurityGroupsFilteredByMultiUser() {

        Boolean singletonUser = false;

        List<SecurityGroupEntity> securityGroupEntities = List.of(
            createSecurityGroupEntity(1,10,20),
            createSecurityGroupEntity(2,11,21),
            createSecurityGroupEntity(3,12,22)
        );

        addUserAccountsToSecurityGroups(securityGroupEntities);

        when(securityGroupRepository.findAll()).thenReturn(securityGroupEntities);

        var filteredGroups = securityGroupService.getSecurityGroups(null, null, null, singletonUser);

        assertEquals(2, filteredGroups.size());
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

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    private List<SecurityGroupEntity> addUserAccountsToSecurityGroups(List<SecurityGroupEntity> securityGroupEntities) {

        int userIdCounter = 1;

        for (SecurityGroupEntity securityGroupEntity : securityGroupEntities) {
            Set<UserAccountEntity> userAccountEntities = new LinkedHashSet<>();

            for (int i = 1; i <= securityGroupEntity.getId(); i++) {
                UserAccountEntity userAccountEntity = new UserAccountEntity();

                userAccountEntity.setId(userIdCounter);
                userAccountEntity.setUserName("TestUser" + userIdCounter);
                userAccountEntities.add(userAccountEntity);

                userIdCounter++;
            }
            securityGroupEntity.setUsers(userAccountEntities);
        }

        return securityGroupEntities;
    }
}
