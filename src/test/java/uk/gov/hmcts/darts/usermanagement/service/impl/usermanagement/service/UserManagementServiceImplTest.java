package uk.gov.hmcts.darts.usermanagement.service.impl.usermanagement.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SecurityGroupEnum;
import uk.gov.hmcts.darts.common.repository.SecurityGroupRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
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
import uk.gov.hmcts.darts.usermanagement.validator.UserDeactivateNotLastInSuperAdminGroupValidator;
import uk.gov.hmcts.darts.usermanagement.validator.UserSuperAdminDeactivateValidator;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceImplTest {
    private static final String EXISTING_EMAIL_ADDRESS = "existing-email@hmcts.net";
    UserManagementServiceImpl service;
    @Mock
    UserAccountRepository userAccountRepository;

    @Mock
    SecurityGroupRepository securityGroupRepository;

    @Mock
    AuthorisationApi authorisationApi;

    @Mock
    UserSearchQuery userSearchQuery;

    @Mock
    UserManagementQuery userManagementQuery;

    @Mock
    SecurityGroupIdMapper securityGroupIdMapper;

    @Mock
    TranscriptionService transcriptionService;

    @Mock
    UserIdentity userIdentity;

    @BeforeEach
    void setUp() {
        UserAccountMapper mapper = new UserAccountMapperImpl();
        UserEmailValidator userEmailValidator = new UserEmailValidator(userAccountRepository);
        UserAccountExistsValidator userAccountExistsValidator = new UserAccountExistsValidator(userAccountRepository);

        UserSuperAdminDeactivateValidator enablementValidator = new UserSuperAdminDeactivateValidator(userIdentity);
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
            transcriptionService
        );
    }


    @Test
    void testGetUser() throws IOException {
        List<UserAccountEntity> userAccountEntities = Collections.singletonList(createUserAccount(1, EXISTING_EMAIL_ADDRESS));

        when(userManagementQuery.getUsers(eq(EXISTING_EMAIL_ADDRESS), eq(null))).thenReturn(userAccountEntities);

        List<UserWithIdAndTimestamps> resultList = service.getUsers(EXISTING_EMAIL_ADDRESS, null);

        assertEquals(userAccountEntities.get(0).getUserName(), resultList.get(0).getFullName());
        assertEquals(userAccountEntities.get(0).getEmailAddress(), resultList.get(0).getEmailAddress());
        assertEquals(userAccountEntities.get(0).getLastLoginTime(), resultList.get(0).getLastLoginAt());
        assertEquals(userAccountEntities.get(0).getLastModifiedDateTime(), resultList.get(0).getLastModifiedAt());
        assertEquals(userAccountEntities.get(0).getCreatedDateTime(), resultList.get(0).getCreatedAt());
    }

    @Test
    void testModifyUserWithDeactivate() throws IOException {
        List<UserAccountEntity> userAccountEntities = Collections.singletonList(createUserAccount(1, EXISTING_EMAIL_ADDRESS));
        userAccountEntities.get(0).setActive(false);
        userAccountEntities.get(0).setIsSystemUser(false);

        Integer userId = 1001;
        Integer transcriptionId = 1001;
        UserPatch patch = new UserPatch();
        patch.setActive(false);

        SecurityGroupEntity securityGroupEntity = Mockito.mock(SecurityGroupEntity.class);
        when(securityGroupRepository.findByGroupNameIgnoreCase(SecurityGroupEnum.SUPER_ADMIN.getName())).thenReturn(Optional.of(securityGroupEntity));
        when(userAccountRepository.existsById(Mockito.eq(userId))).thenReturn(true);
        when(userAccountRepository.findById(userId)).thenReturn(Optional.of(userAccountEntities.get(0)));
        when(transcriptionService.rollbackUserTransactions(userAccountEntities.get(0))).thenReturn(Arrays.asList(transcriptionId));

        UserWithIdAndTimestamps resultList = service.modifyUser(userId, patch);

        assertEquals(transcriptionId, resultList.getRolledBackTranscriptRequests().get(0));
    }

    @Test
    void testModifyUserWithoutDeactivate() throws IOException {
        List<UserAccountEntity> userAccountEntities = Collections.singletonList(createUserAccount(1, EXISTING_EMAIL_ADDRESS));
        userAccountEntities.get(0).setIsSystemUser(false);

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

        Set<SecurityGroupEntity> securityGroupEntitySet = new HashSet<>();
        securityGroupEntitySet.add(securityGroupEntity);
        userAccountEntities.get(0).setSecurityGroupEntities(securityGroupEntitySet);

        Integer userId = 1001;
        when(userAccountRepository.existsById(Mockito.eq(userId))).thenReturn(true);
        when(userAccountRepository.findById(userId)).thenReturn(Optional.of(userAccountEntities.get(0)));
        when(securityGroupRepository.findById(Mockito.eq(secGroupId))).thenReturn(Optional.of(securityGroupEntity));
        when(securityGroupIdMapper.mapSecurityGroupEntitiesToIds(Mockito.notNull())).thenReturn(Arrays.asList(secGroupId));

        UserWithIdAndTimestamps resultList = service.modifyUser(userId, patch);

        assertNull(resultList.getRolledBackTranscriptRequests());
        assertEquals(description, resultList.getDescription());
        assertEquals(fullName, resultList.getFullName());
        assertEquals(Arrays.asList(secGroupId), resultList.getSecurityGroupIds());
        assertEquals(emailAddress, resultList.getEmailAddress());
    }

    private static UserAccountEntity createUserAccount(int id, String emailAddress) {
        UserAccountEntity userAccount = new UserAccountEntity();
        userAccount.setId(id);
        userAccount.setUserName("James Smith");
        userAccount.setIsSystemUser(true);
        userAccount.setActive(true);
        userAccount.setCreatedDateTime(OffsetDateTime.of(2023, 10, 27, 22, 0, 0, 0, ZoneOffset.UTC));
        userAccount.setLastLoginTime(OffsetDateTime.of(2023, 10, 27, 22, 0, 0, 0, ZoneOffset.UTC));
        userAccount.setLastModifiedDateTime(OffsetDateTime.of(2023, 10, 27, 22, 0, 0, 0, ZoneOffset.UTC));
        userAccount.setEmailAddress(emailAddress);
        return userAccount;
    }
}