package uk.gov.hmcts.darts.usermanagement.service.impl.usermanagement.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.SecurityGroupRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.common.util.TestUtils;
import uk.gov.hmcts.darts.usermanagement.component.UserManagementQuery;
import uk.gov.hmcts.darts.usermanagement.component.UserSearchQuery;
import uk.gov.hmcts.darts.usermanagement.mapper.impl.UserAccountMapper;
import uk.gov.hmcts.darts.usermanagement.mapper.impl.UserAccountMapperImpl;
import uk.gov.hmcts.darts.usermanagement.model.UserWithIdAndTimestamps;
import uk.gov.hmcts.darts.usermanagement.service.impl.UserManagementServiceImpl;
import uk.gov.hmcts.darts.usermanagement.service.validation.DuplicateEmailValidator;
import uk.gov.hmcts.darts.usermanagement.service.validation.UserAccountExistsValidator;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static uk.gov.hmcts.darts.common.util.TestUtils.getContentsFromFile;

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

    private ObjectMapper objectMapper;


    @BeforeEach
    void setUp() {
        UserAccountMapper mapper = new UserAccountMapperImpl();
        DuplicateEmailValidator duplicateEmailValidator = new DuplicateEmailValidator(userAccountRepository);
        UserAccountExistsValidator userAccountExistsValidator = new UserAccountExistsValidator(userAccountRepository);

        service = new UserManagementServiceImpl(
            mapper,
            userAccountRepository,
            securityGroupRepository,
            authorisationApi,
            userSearchQuery,
            userManagementQuery,
            duplicateEmailValidator,
            userAccountExistsValidator
        );
        this.objectMapper = TestUtils.getObjectMapper();
    }


    @Test
    void testGetUser() throws IOException {

        List<UserWithIdAndTimestamps> userWithIdAndTimestamps = new ArrayList<>();
        List<UserAccountEntity> userAccountEntities = new ArrayList<>();
        userWithIdAndTimestamps.add(createUserWithIdAndTimestamp(1, EXISTING_EMAIL_ADDRESS));
        userAccountEntities.add(createUserAccount(1, EXISTING_EMAIL_ADDRESS));

        Mockito.when(userManagementQuery.getUsers(
            eq(userWithIdAndTimestamps.get(0).getEmailAddress())
        )).thenReturn(userAccountEntities);

        List<UserWithIdAndTimestamps> resultList = service.getUsers(userWithIdAndTimestamps.get(0).getEmailAddress());

        String actualResponse = objectMapper.writeValueAsString(resultList);
        String expectedResponse = getContentsFromFile(
            "Tests/usermanagement/UserManagementTest/testGetUser/expectedResponse.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    private static UserWithIdAndTimestamps createUserWithIdAndTimestamp(int id, String emailAddress) {
        UserWithIdAndTimestamps userWithIdAndTimestamps = new UserWithIdAndTimestamps();
        userWithIdAndTimestamps.setId(id);
        userWithIdAndTimestamps.setFullName("Tom Smith");
        userWithIdAndTimestamps.setCreatedAt(OffsetDateTime.of(2023, 10, 27, 22, 0, 0, 0, ZoneOffset.UTC));
        userWithIdAndTimestamps.setLastLoginAt(OffsetDateTime.of(2023, 10, 27, 22, 0, 0, 0, ZoneOffset.UTC));
        userWithIdAndTimestamps.setLastModifiedAt(OffsetDateTime.of(2023, 10, 27, 22, 0, 0, 0, ZoneOffset.UTC));
        userWithIdAndTimestamps.setEmailAddress(emailAddress);
        return userWithIdAndTimestamps;
    }

    private static UserAccountEntity createUserAccount(int id, String emailAddress) {
        UserAccountEntity userAccount = new UserAccountEntity();
        userAccount.setId(id);
        userAccount.setUserName("Tom Smith");
        userAccount.setIsSystemUser(true);
        userAccount.setActive(true);
        userAccount.setCreatedDateTime(OffsetDateTime.of(2023, 10, 27, 22, 0, 0, 0, ZoneOffset.UTC));
        userAccount.setLastLoginTime(OffsetDateTime.of(2023, 10, 27, 22, 0, 0, 0, ZoneOffset.UTC));
        userAccount.setLastModifiedDateTime(OffsetDateTime.of(2023, 10, 27, 22, 0, 0, 0, ZoneOffset.UTC));
        userAccount.setEmailAddress(emailAddress);
        return userAccount;
    }
}

