package uk.gov.hmcts.darts.usermanagement.service.impl.usermanagement.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audit.api.AuditApi;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SecurityGroupEnum;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.repository.SecurityGroupRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.common.util.SecurityRoleMatcher;
import uk.gov.hmcts.darts.transcriptions.service.TranscriptionService;
import uk.gov.hmcts.darts.usermanagement.component.UserManagementQuery;
import uk.gov.hmcts.darts.usermanagement.component.UserSearchQuery;
import uk.gov.hmcts.darts.usermanagement.mapper.impl.SecurityGroupIdMapper;
import uk.gov.hmcts.darts.usermanagement.mapper.impl.UserAccountMapper;
import uk.gov.hmcts.darts.usermanagement.mapper.impl.UserAccountMapperImpl;
import uk.gov.hmcts.darts.usermanagement.model.UserPatch;
import uk.gov.hmcts.darts.usermanagement.model.UserWithIdAndTimestamps;
import uk.gov.hmcts.darts.usermanagement.service.impl.UserManagementServiceImpl;
import uk.gov.hmcts.darts.usermanagement.service.validation.UserAccountExistsValidator;
import uk.gov.hmcts.darts.usermanagement.service.validation.UserEmailValidator;
import uk.gov.hmcts.darts.usermanagement.service.validation.UserTypeValidator;
import uk.gov.hmcts.darts.usermanagement.validator.AuthorisedUserPermissionsValidator;
import uk.gov.hmcts.darts.usermanagement.validator.NotSameUserValidator;
import uk.gov.hmcts.darts.usermanagement.validator.UserActivateValidator;
import uk.gov.hmcts.darts.usermanagement.validator.UserDeactivateNotLastInSuperAdminGroupValidator;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceImplTest {

    private static final String EXISTING_EMAIL_ADDRESS = "existing-email@hmcts.net";
    private UserManagementServiceImpl service;
    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private SecurityGroupRepository securityGroupRepository;

    @Mock
    private AuthorisationApi authorisationApi;

    @Mock
    private UserSearchQuery userSearchQuery;

    @Mock
    private UserManagementQuery userManagementQuery;

    @Mock
    private SecurityGroupIdMapper securityGroupIdMapper;

    @Mock
    private TranscriptionService transcriptionService;

    @Mock
    private UserIdentity userIdentity;

    @Mock
    private AuditApi auditApi;

    @Mock
    private UserActivateValidator userAuthoriseValidator;

    @Mock
    private NotSameUserValidator notSameUserValidator;

    @BeforeEach
    void setUp() {
        UserAccountMapper mapper = new UserAccountMapperImpl();
        UserEmailValidator userEmailValidator = new UserEmailValidator(userAccountRepository);
        UserAccountExistsValidator userAccountExistsValidator = new UserAccountExistsValidator(userAccountRepository);

        AuthorisedUserPermissionsValidator enablementValidator = new AuthorisedUserPermissionsValidator(userIdentity);
        UserDeactivateNotLastInSuperAdminGroupValidator deactivateNotLastSuperAdminValidator
            = new UserDeactivateNotLastInSuperAdminGroupValidator(securityGroupRepository);

        UserTypeValidator userTypeValidator = new UserTypeValidator(userAccountRepository);

        service = new UserManagementServiceImpl(
            mapper,
            securityGroupIdMapper,
            userAccountRepository,
            securityGroupRepository,
            authorisationApi,
            userSearchQuery,
            userManagementQuery,
            userEmailValidator,
            userAccountExistsValidator,
            userTypeValidator,
            enablementValidator,
            deactivateNotLastSuperAdminValidator,
            transcriptionService,
            auditApi,
            userAuthoriseValidator,
            notSameUserValidator
        );
    }

    @Test
    void getUsers_ShouldReturnsUsers() {
        List<UserAccountEntity> userAccountEntities = Collections.singletonList(createUserAccount(1, EXISTING_EMAIL_ADDRESS));

        when(userManagementQuery.getUsers(false, EXISTING_EMAIL_ADDRESS, null)).thenReturn(userAccountEntities);

        List<UserWithIdAndTimestamps> resultList = service.getUsers(false, EXISTING_EMAIL_ADDRESS, null);

        assertEquals(userAccountEntities.getFirst().getUserFullName(), resultList.getFirst().getFullName());
        assertEquals(userAccountEntities.getFirst().getEmailAddress(), resultList.getFirst().getEmailAddress());
        assertEquals(userAccountEntities.getFirst().getLastLoginTime(), resultList.getFirst().getLastLoginAt());
        assertEquals(userAccountEntities.getFirst().getLastModifiedDateTime(), resultList.getFirst().getLastModifiedAt());
        assertEquals(userAccountEntities.getFirst().getCreatedDateTime(), resultList.getFirst().getCreatedAt());
    }

    @Test
    void modifyUser_ReturnsUpdatedUser_WithActivateFalse() {
        List<UserAccountEntity> userAccountEntities = new ArrayList<>();
        userAccountEntities.add(createUserAccount(1, EXISTING_EMAIL_ADDRESS));

        userAccountEntities.getFirst().setActive(false);
        userAccountEntities.getFirst().setIsSystemUser(false);

        userAccountEntities.add(createUserAccount(2, "another-user-email@hmcts.net"));

        Integer userId = 1001;
        UserPatch patch = new UserPatch();
        patch.setActive(false);

        Set<UserAccountEntity> userAccountEntitySet = new HashSet<>(userAccountEntities);
        SecurityGroupEntity securityGroupEntity = Mockito.mock(SecurityGroupEntity.class);
        when(securityGroupEntity.getUsers()).thenReturn(userAccountEntitySet);

        Long transcriptionId = 1001L;

        when(userIdentity.userHasGlobalAccess(Mockito.notNull())).thenReturn(true);
        when(securityGroupRepository.findByGroupNameIgnoreCase(SecurityGroupEnum.SUPER_ADMIN.getName())).thenReturn(Optional.of(securityGroupEntity));
        when(userAccountRepository.existsById(userId)).thenReturn(true);
        when(userAccountRepository.findById(userId)).thenReturn(Optional.of(userAccountEntities.getFirst()));
        when(transcriptionService.rollbackUserTranscriptions(userAccountEntities.getFirst())).thenReturn(Arrays.asList(transcriptionId));
        when(securityGroupRepository.findByGroupNameIgnoreCase(SecurityGroupEnum.SUPER_ADMIN.getName())).thenReturn(Optional.of(securityGroupEntity));

        UserWithIdAndTimestamps resultList = service.modifyUser(userId, patch);

        assertEquals(transcriptionId, resultList.getRolledBackTranscriptRequests().getFirst());
        verify(transcriptionService, times(1)).rollbackUserTranscriptions(Mockito.any());
    }

    @Test
    void modifyUser_ReturnsUpdatedUser_WithoutActiveSet() {
        List<UserAccountEntity> userAccountEntities = Collections.singletonList(createUserAccount(1, EXISTING_EMAIL_ADDRESS));
        userAccountEntities.getFirst().setIsSystemUser(false);

        UserPatch patch = new UserPatch();
        String description = "description";
        String fullName = "this is a full name";
        String emailAddress = "test@hmcts.net";
        Integer secGroupId = 1;

        patch.setDescription(description);
        patch.setFullName(fullName);
        patch.setEmailAddress(emailAddress);
        patch.addSecurityGroupIdsItem(secGroupId);

        SecurityGroupEntity securityGroupEntity = new SecurityGroupEntity();
        securityGroupEntity.setId(secGroupId);
        securityGroupEntity.setDisplayState(true);

        Set<SecurityGroupEntity> securityGroupEntitySet = new HashSet<>();
        securityGroupEntitySet.add(securityGroupEntity);
        userAccountEntities.getFirst().setSecurityGroupEntities(securityGroupEntitySet);

        Integer userId = 1001;
        when(userIdentity.userHasGlobalAccess(argThat(new SecurityRoleMatcher(SecurityRoleEnum.SUPER_ADMIN)))).thenReturn(true, false);
        when(userAccountRepository.existsById(userId)).thenReturn(true);
        when(userAccountRepository.findById(userId)).thenReturn(Optional.of(userAccountEntities.getFirst()));
        when(securityGroupRepository.findById(secGroupId)).thenReturn(Optional.of(securityGroupEntity));
        when(securityGroupIdMapper.mapSecurityGroupEntitiesToIds(Mockito.notNull())).thenReturn(Arrays.asList(secGroupId));

        UserWithIdAndTimestamps resultList = service.modifyUser(userId, patch);

        assertNull(resultList.getRolledBackTranscriptRequests());
        assertEquals(description, resultList.getDescription());
        assertEquals(fullName, resultList.getFullName());
        assertEquals(Arrays.asList(secGroupId), resultList.getSecurityGroupIds());
        assertEquals(emailAddress, resultList.getEmailAddress());

        verify(transcriptionService, times(0)).rollbackUserTranscriptions(Mockito.any());
    }

    @Test
    void modifyUser_ReturnsUpdatedUser_WithActiveTrue() {
        List<UserAccountEntity> userAccountEntities = Collections.singletonList(createUserAccount(1, EXISTING_EMAIL_ADDRESS));
        userAccountEntities.getFirst().setIsSystemUser(false);

        UserPatch patch = new UserPatch();
        patch.setActive(true);
        String description = "description";
        String fullName = "this is a full name";
        String emailAddress = "test@hmcts.net";
        Integer secGroupId = 1;

        patch.setDescription(description);
        patch.setFullName(fullName);
        patch.setEmailAddress(emailAddress);
        patch.addSecurityGroupIdsItem(secGroupId);

        SecurityGroupEntity securityGroupEntity = new SecurityGroupEntity();
        securityGroupEntity.setId(secGroupId);
        securityGroupEntity.setDisplayState(true);

        Set<SecurityGroupEntity> securityGroupEntitySet = new HashSet<>();
        securityGroupEntitySet.add(securityGroupEntity);
        userAccountEntities.getFirst().setSecurityGroupEntities(securityGroupEntitySet);

        Integer userId = 1001;

        when(userIdentity.userHasGlobalAccess(Mockito.notNull())).thenReturn(true);
        when(userAccountRepository.existsById(userId)).thenReturn(true);
        when(userAccountRepository.findById(userId)).thenReturn(Optional.of(userAccountEntities.getFirst()));
        when(securityGroupRepository.findById(secGroupId)).thenReturn(Optional.of(securityGroupEntity));
        when(securityGroupIdMapper.mapSecurityGroupEntitiesToIds(Mockito.notNull())).thenReturn(Arrays.asList(secGroupId));

        UserWithIdAndTimestamps resultList = service.modifyUser(userId, patch);

        assertNull(resultList.getRolledBackTranscriptRequests());
        assertEquals(description, resultList.getDescription());
        assertEquals(fullName, resultList.getFullName());
        assertEquals(Arrays.asList(secGroupId), resultList.getSecurityGroupIds());
        assertEquals(emailAddress, resultList.getEmailAddress());

        verify(transcriptionService, times(0)).rollbackUserTranscriptions(Mockito.any());
    }

    private static UserAccountEntity createUserAccount(int id, String emailAddress) {
        UserAccountEntity userAccount = new UserAccountEntity();
        userAccount.setId(id);
        userAccount.setUserFullName("James Smith");
        userAccount.setIsSystemUser(true);
        userAccount.setActive(true);
        userAccount.setCreatedDateTime(OffsetDateTime.of(2023, 10, 27, 22, 0, 0, 0, ZoneOffset.UTC));
        userAccount.setLastLoginTime(OffsetDateTime.of(2023, 10, 27, 22, 0, 0, 0, ZoneOffset.UTC));
        userAccount.setLastModifiedDateTime(OffsetDateTime.of(2023, 10, 27, 22, 0, 0, 0, ZoneOffset.UTC));
        userAccount.setEmailAddress(emailAddress);
        return userAccount;
    }
}