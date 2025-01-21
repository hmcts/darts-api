package uk.gov.hmcts.darts.common.helper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audio.exception.AudioApiError;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemUserHelperTest {

    public static final String HOUSEKEEPING_GUID = "ecfd1f14-c9b6-4f15-94c7-cc60e53f2c7a";
    public static final String DAILYLIST_PROCESSOR_GUID = "f6f71122-ff85-4ebe-93d9-1706460dbea5";
    public static final String CORE_SYSTEM_USER = "f6f71122-ff85-4ebe-93d9-1706460dbea9";
    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties;

    private SystemUserHelper systemUserHelper;

    @BeforeEach
    void setUp() {
        systemUserHelper = new SystemUserHelper(userAccountRepository, automatedTaskConfigurationProperties);
    }

    @Test
    void getSystemUserByEmail() {
        UserAccountEntity coreSystemUser = new UserAccountEntity();
        coreSystemUser.setAccountGuid(CORE_SYSTEM_USER);

        String email = "test@email.com";
        when(automatedTaskConfigurationProperties.getSystemUserEmail()).thenReturn(email);

        when(userAccountRepository.findFirstByEmailAddressIgnoreCase(email)).thenReturn(Optional.of(coreSystemUser));

        UserAccountEntity user = systemUserHelper.getSystemUser();
        verify(userAccountRepository, times(1)).findFirstByEmailAddressIgnoreCase(email);
        assertEquals(CORE_SYSTEM_USER, user.getAccountGuid());
    }

    @Test
    void getSystemUserByEmailThrows() {
        UserAccountEntity coreSystemUser = new UserAccountEntity();
        coreSystemUser.setAccountGuid(CORE_SYSTEM_USER);

        String email = "test@email.com";
        when(automatedTaskConfigurationProperties.getSystemUserEmail()).thenReturn(email);

        when(userAccountRepository.findFirstByEmailAddressIgnoreCase(email)).thenReturn(Optional.empty());

        DartsApiException exception = Assertions.assertThrows(DartsApiException.class, () -> systemUserHelper.getSystemUser());
        assertEquals(AudioApiError.MISSING_SYSTEM_USER, exception.getError());
    }
}