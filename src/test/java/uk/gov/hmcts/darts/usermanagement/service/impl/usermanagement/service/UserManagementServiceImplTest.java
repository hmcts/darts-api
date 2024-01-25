package uk.gov.hmcts.darts.usermanagement.service.impl.usermanagement.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.SecurityGroupRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.usermanagement.component.UserManagementQuery;
import uk.gov.hmcts.darts.usermanagement.component.UserSearchQuery;
import uk.gov.hmcts.darts.usermanagement.mapper.impl.SecurityGroupIdMapper;
import uk.gov.hmcts.darts.usermanagement.mapper.impl.UserAccountMapper;
import uk.gov.hmcts.darts.usermanagement.mapper.impl.UserAccountMapperImpl;
import uk.gov.hmcts.darts.usermanagement.model.UserWithIdAndTimestamps;
import uk.gov.hmcts.darts.usermanagement.service.impl.UserManagementServiceImpl;
import uk.gov.hmcts.darts.usermanagement.service.validation.DuplicateEmailValidator;
import uk.gov.hmcts.darts.usermanagement.service.validation.UserAccountExistsValidator;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;

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

    @BeforeEach
    void setUp() {
        UserAccountMapper mapper = new UserAccountMapperImpl();
        DuplicateEmailValidator duplicateEmailValidator = new DuplicateEmailValidator(userAccountRepository);
        UserAccountExistsValidator userAccountExistsValidator = new UserAccountExistsValidator(userAccountRepository);

        service = new UserManagementServiceImpl(
            mapper,
            securityGroupIdMapper,
            userAccountRepository,
            securityGroupRepository,
            authorisationApi,
            userSearchQuery,
            userManagementQuery,
            duplicateEmailValidator,
            userAccountExistsValidator
        );
    }


    @Test
    void testGetUser() throws IOException {
        List<UserAccountEntity> userAccountEntities = Collections.singletonList(createUserAccount(1, EXISTING_EMAIL_ADDRESS));

        Mockito.when(userManagementQuery.getUsers(
            eq(EXISTING_EMAIL_ADDRESS)
        )).thenReturn(userAccountEntities);

        List<UserWithIdAndTimestamps> resultList = service.getUsers(EXISTING_EMAIL_ADDRESS);

        assertEquals(userAccountEntities.get(0).getUserName(), resultList.get(0).getFullName());
        assertEquals(userAccountEntities.get(0).getEmailAddress(), resultList.get(0).getEmailAddress());
        assertEquals(userAccountEntities.get(0).getLastLoginTime(), resultList.get(0).getLastLoginAt());
        assertEquals(userAccountEntities.get(0).getLastModifiedDateTime(), resultList.get(0).getLastModifiedAt());
        assertEquals(userAccountEntities.get(0).getCreatedDateTime(), resultList.get(0).getCreatedAt());
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

