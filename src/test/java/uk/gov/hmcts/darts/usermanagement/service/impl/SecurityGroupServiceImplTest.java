package uk.gov.hmcts.darts.usermanagement.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audit.api.AuditApi;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.SecurityRoleEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.CourthouseRepository;
import uk.gov.hmcts.darts.common.repository.SecurityGroupRepository;
import uk.gov.hmcts.darts.common.repository.SecurityRoleRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.usermanagement.component.validation.impl.SecurityGroupCreationValidation;
import uk.gov.hmcts.darts.usermanagement.mapper.impl.SecurityGroupCourthouseMapper;
import uk.gov.hmcts.darts.usermanagement.mapper.impl.SecurityGroupMapper;
import uk.gov.hmcts.darts.usermanagement.mapper.impl.SecurityGroupWithIdAndRoleAndUsersMapper;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroupPatch;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityGroupServiceImplTest {

    SecurityGroupServiceImpl securityGroupService;
    @Mock
    SecurityGroupRepository securityGroupRepository;
    @Mock
    SecurityRoleRepository securityRoleRepository;
    @Mock
    CourthouseRepository courthouseRepository;
    @Mock
    UserAccountRepository userAccountRepository;
    @Mock
    SecurityGroupMapper securityGroupMapper;
    @Mock
    SecurityGroupCourthouseMapper securityGroupCourthouseMapper;
    @Mock
    SecurityGroupWithIdAndRoleAndUsersMapper securityGroupWithIdAndRoleAndUsersMapper;
    @Mock
    SecurityGroupCreationValidation securityGroupCreationValidation;
    @Mock
    AuditApi auditApi;
    @Mock
    AuthorisationApi authorisationApi;

    @BeforeEach
    void setUp() {
        securityGroupService = new SecurityGroupServiceImpl(
            securityGroupRepository,
            securityRoleRepository,
            courthouseRepository,
            userAccountRepository,
            securityGroupMapper,
            securityGroupCourthouseMapper,
            securityGroupWithIdAndRoleAndUsersMapper,
            securityGroupCreationValidation,
            auditApi,
            authorisationApi
        );
    }

    @Test
    void testGetAllSecurityGroups() {

        List<SecurityGroupEntity> securityGroupEntities = List.of(
            createSecurityGroupEntity(1, 10, 20),
            createSecurityGroupEntity(2, 11, 21),
            createSecurityGroupEntity(3, 12, 22)
        );

        when(securityGroupRepository.findAll()).thenReturn(securityGroupEntities);

        var filteredGroups = securityGroupService.getSecurityGroups(null, null, null, null);

        assertEquals(3, filteredGroups.size());
    }

    @Test
    void testGetSecurityGroupsFilteredByRoleId() {

        List<Integer> listOfRoleIds = List.of(10);

        List<SecurityGroupEntity> securityGroupEntities = List.of(
            createSecurityGroupEntity(1, 10, 20),
            createSecurityGroupEntity(2, 11, 21),
            createSecurityGroupEntity(3, 12, 22)
        );

        when(securityGroupRepository.findAll()).thenReturn(securityGroupEntities);

        var filteredGroups = securityGroupService.getSecurityGroups(listOfRoleIds, null, null, null);

        assertEquals(1, filteredGroups.size());
    }

    @Test
    void testGetSecurityGroupsFilteredByMultipleRoleIds() {

        List<Integer> listOfRoleIds = List.of(10, 11);

        List<SecurityGroupEntity> securityGroupEntities = List.of(
            createSecurityGroupEntity(1, 10, 20),
            createSecurityGroupEntity(2, 11, 21),
            createSecurityGroupEntity(3, 12, 22)
        );

        when(securityGroupRepository.findAll()).thenReturn(securityGroupEntities);

        var filteredGroups = securityGroupService.getSecurityGroups(listOfRoleIds, null, null, null);

        assertEquals(2, filteredGroups.size());
    }

    @Test
    void testGetSecurityGroupsFilterByRoleIdReturningNoMatch() {

        List<Integer> listOfRoleIds = List.of(30);

        List<SecurityGroupEntity> securityGroupEntities = List.of(
            createSecurityGroupEntity(1, 10, 20),
            createSecurityGroupEntity(2, 11, 21),
            createSecurityGroupEntity(3, 12, 22)
        );

        when(securityGroupRepository.findAll()).thenReturn(securityGroupEntities);

        var filteredGroups = securityGroupService.getSecurityGroups(listOfRoleIds, null, null, null);

        assertEquals(0, filteredGroups.size());
    }

    @Test
    void testGetSecurityGroupsFilteredByCourthouseId() {

        Integer courthouseId = 20;

        List<SecurityGroupEntity> securityGroupEntities = List.of(
            createSecurityGroupEntity(1, 10, 20),
            createSecurityGroupEntity(2, 11, 21),
            createSecurityGroupEntity(3, 12, 22)
        );

        when(securityGroupRepository.findAll()).thenReturn(securityGroupEntities);

        var filteredGroups = securityGroupService.getSecurityGroups(null, courthouseId, null, null);

        assertEquals(1, filteredGroups.size());
    }

    @Test
    void testGetSecurityGroupsFilteredByCourthouseIdReturningNoMatch() {

        Integer courthouseId = 50;

        List<SecurityGroupEntity> securityGroupEntities = List.of(
            createSecurityGroupEntity(1, 10, 20),
            createSecurityGroupEntity(2, 11, 21),
            createSecurityGroupEntity(3, 12, 22)
        );

        when(securityGroupRepository.findAll()).thenReturn(securityGroupEntities);

        var filteredGroups = securityGroupService.getSecurityGroups(null, courthouseId, null, null);

        assertEquals(0, filteredGroups.size());
    }

    @Test
    void testGetSecurityGroupsFilteredByRoleIdsAndCourthouseId() {

        Integer courthouseId = 20;
        List<Integer> listOfRoleIds = List.of(10, 11);

        List<SecurityGroupEntity> securityGroupEntities = List.of(
            createSecurityGroupEntity(1, 10, 20),
            createSecurityGroupEntity(2, 11, 21),
            createSecurityGroupEntity(3, 12, 22)
        );

        when(securityGroupRepository.findAll()).thenReturn(securityGroupEntities);

        var filteredGroups = securityGroupService.getSecurityGroups(listOfRoleIds, courthouseId, null, null);

        assertEquals(1, filteredGroups.size());
    }

    @Test
    void testGetSecurityGroupsFilteredByUserId() {

        Integer userId = 2;

        List<SecurityGroupEntity> securityGroupEntities = List.of(
            createSecurityGroupEntity(1, 10, 20),
            createSecurityGroupEntity(2, 11, 21),
            createSecurityGroupEntity(3, 12, 22)
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
            createSecurityGroupEntity(1, 10, 20),
            createSecurityGroupEntity(2, 11, 21),
            createSecurityGroupEntity(3, 12, 22)
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
            createSecurityGroupEntity(1, 10, 20),
            createSecurityGroupEntity(2, 11, 21),
            createSecurityGroupEntity(3, 12, 22)
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
            createSecurityGroupEntity(1, 10, 20),
            createSecurityGroupEntity(2, 11, 21),
            createSecurityGroupEntity(3, 12, 22)
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
        securityRoleEntity.setDisplayState(true);

        CourthouseEntity courthouse = new CourthouseEntity();
        courthouse.setId(courthouseId);
        courthouse.setCourthouseName("Test Courthouse " + courthouseId);

        Set<CourthouseEntity> courthouseEntitySet = new HashSet<>();
        courthouseEntitySet.add(courthouse);

        UserAccountEntity userAccountEntity = new UserAccountEntity();
        userAccountEntity.setId(1);
        userAccountEntity.setIsSystemUser(false);

        Set<UserAccountEntity> userAccountEntitySet = new HashSet<>();
        userAccountEntitySet.add(userAccountEntity);

        SecurityGroupEntity securityGroupEntity = new SecurityGroupEntity();
        securityGroupEntity.setId(securityGroupId);
        securityGroupEntity.setGroupName("Test Group " + securityGroupId);
        securityGroupEntity.setDisplayName("Display Test Group " + securityGroupId);
        securityGroupEntity.setDescription("Description Test Group " + securityGroupId);
        securityGroupEntity.setSecurityRoleEntity(securityRoleEntity);
        securityGroupEntity.setCourthouseEntities(courthouseEntitySet);
        securityGroupEntity.setUsers(userAccountEntitySet);
        securityGroupEntity.setDisplayState(true);

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
                userAccountEntity.setUserFullName("TestUser" + userIdCounter);
                userAccountEntities.add(userAccountEntity);

                userIdCounter++;
            }
            securityGroupEntity.setUsers(userAccountEntities);
        }

        return securityGroupEntities;
    }

    @Test
    void testUpdateNameOnly() {
        SecurityGroupEntity securityGroupEntity = createSecurityGroupEntity(1, 1, 1);

        SecurityGroupPatch securityGroupPatch = new SecurityGroupPatch();
        String newName = "new name";
        securityGroupPatch.setName(newName);
        securityGroupService.updateSecurityGroupEntity(securityGroupPatch, securityGroupEntity);

        assertEquals(newName, securityGroupEntity.getGroupName());
        assertEquals("Display Test Group 1", securityGroupEntity.getDisplayName());
        assertEquals("Description Test Group 1", securityGroupEntity.getDescription());
        assertEquals(1, securityGroupEntity.getCourthouseEntities().iterator().next().getId());
        assertEquals(1, securityGroupEntity.getUsers().iterator().next().getId());
    }

    @Test
    void testUpdateDisplayNameAndDescriptionOnly() {
        SecurityGroupEntity securityGroupEntity = createSecurityGroupEntity(1, 1, 1);

        SecurityGroupPatch securityGroupPatch = new SecurityGroupPatch();
        String newDisplayName = "new name";
        securityGroupPatch.setDisplayName(newDisplayName);
        String newDescription = "new description";
        securityGroupPatch.setDescription(newDescription);
        securityGroupService.updateSecurityGroupEntity(securityGroupPatch, securityGroupEntity);

        assertEquals("Test Group 1", securityGroupEntity.getGroupName());
        assertEquals(newDisplayName, securityGroupEntity.getDisplayName());
        assertEquals(newDescription, securityGroupEntity.getDescription());
        assertEquals(1, securityGroupEntity.getCourthouseEntities().iterator().next().getId());
        assertEquals(1, securityGroupEntity.getUsers().iterator().next().getId());
    }

    @Test
    void testUpdateUsersExcludesSystemUsers() {

        SecurityGroupEntity securityGroupEntity = createSecurityGroupEntity(1, 1, 1);

        // two system users added to security group
        Set<UserAccountEntity> userAccountEntitySet = securityGroupEntity.getUsers();
        UserAccountEntity userAccountEntity2 = new UserAccountEntity();
        userAccountEntity2.setId(2);
        userAccountEntity2.setIsSystemUser(true);
        userAccountEntitySet.add(userAccountEntity2);
        UserAccountEntity userAccountEntity3 = new UserAccountEntity();
        userAccountEntity3.setId(3);
        userAccountEntity3.setIsSystemUser(true);
        userAccountEntitySet.add(userAccountEntity3);
        securityGroupEntity.setUsers(userAccountEntitySet);

        UserAccountEntity userAccountEntity10 = new UserAccountEntity();
        userAccountEntity10.setId(10);
        userAccountEntity10.setIsSystemUser(false);
        UserAccountEntity userAccountEntity11 = new UserAccountEntity();
        userAccountEntity11.setId(11);
        userAccountEntity11.setIsSystemUser(true);
        UserAccountEntity userAccountEntity12 = new UserAccountEntity();
        userAccountEntity12.setId(12);
        userAccountEntity12.setIsSystemUser(false);

        SecurityGroupPatch securityGroupPatch = new SecurityGroupPatch();

        // three user ids added to patch - 11 is a system user
        securityGroupPatch.setUserIds(Arrays.asList(10, 11, 12));

        when(userAccountRepository.findByIdInAndActive(any(), Mockito.eq(true)))
            .thenReturn(Arrays.asList(userAccountEntity10, userAccountEntity11, userAccountEntity12));
        when(userAccountRepository.findById(2)).thenReturn(Optional.of(userAccountEntity2));
        when(userAccountRepository.findById(3)).thenReturn(Optional.of(userAccountEntity3));
        when(userAccountRepository.findById(10)).thenReturn(Optional.of(userAccountEntity10));
        when(userAccountRepository.findById(12)).thenReturn(Optional.of(userAccountEntity12));

        securityGroupService.updateSecurityGroupEntity(securityGroupPatch, securityGroupEntity);

        // resulting security group should contain
        // - the 2 system users from the original security group
        // - plus 2 new (non-system) users from the patch
        assertEquals(4, securityGroupEntity.getUsers().size());
    }

}