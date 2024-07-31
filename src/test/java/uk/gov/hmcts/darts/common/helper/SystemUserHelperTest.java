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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
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

    private SystemUserHelper systemUserHelper;

    @BeforeEach
    void setUp() {
        systemUserHelper = new SystemUserHelper(userAccountRepository);

        Map<String, String> guidMap = new HashMap<>();
        guidMap.put("housekeeping", HOUSEKEEPING_GUID);
        guidMap.put("dailylist-processor", DAILYLIST_PROCESSOR_GUID);
        systemUserHelper.setSystemUserGuidMap(guidMap);
    }

    /**
     * check if cache is used and populated correctly by trying to get a user.
     * The first time the repository will be hit the second time the same user will exist in the cache.
     */
    @Test
    void getSystemUser() {
        UserAccountEntity housekeepingUser = new UserAccountEntity();
        housekeepingUser.setAccountGuid(HOUSEKEEPING_GUID);

        UserAccountEntity dailylistProcessorUser = new UserAccountEntity();
        dailylistProcessorUser.setAccountGuid(DAILYLIST_PROCESSOR_GUID);

        when(userAccountRepository.findSystemUser(HOUSEKEEPING_GUID)).thenReturn(housekeepingUser);
        when(userAccountRepository.findSystemUser(DAILYLIST_PROCESSOR_GUID)).thenReturn(dailylistProcessorUser);

        UserAccountEntity user = systemUserHelper.getHousekeepingUser();
        verify(userAccountRepository, times(1)).findSystemUser(anyString());
        assertEquals(HOUSEKEEPING_GUID, user.getAccountGuid());

        user = systemUserHelper.getHousekeepingUser();
        verify(userAccountRepository, times(1)).findSystemUser(anyString());
        assertEquals(HOUSEKEEPING_GUID, user.getAccountGuid());

        user = systemUserHelper.getDailyListProcessorUser();
        verify(userAccountRepository, times(2)).findSystemUser(anyString());
        assertEquals(DAILYLIST_PROCESSOR_GUID, user.getAccountGuid());

        user = systemUserHelper.getDailyListProcessorUser();
        verify(userAccountRepository, times(2)).findSystemUser(anyString());
        assertEquals(DAILYLIST_PROCESSOR_GUID, user.getAccountGuid());
    }

    @Test
    void getSystemUserByEmail() {
        UserAccountEntity coreSystemUser = new UserAccountEntity();
        coreSystemUser.setAccountGuid(CORE_SYSTEM_USER);

        when(userAccountRepository.findByEmailAddressIgnoreCase(SystemUserHelper.SYSTEM_EMAIL_ADDRESS)).thenReturn(List.of(coreSystemUser));

        UserAccountEntity user = systemUserHelper.getSystemUser();
        verify(userAccountRepository, times(1)).findByEmailAddressIgnoreCase(SystemUserHelper.SYSTEM_EMAIL_ADDRESS);
        assertEquals(CORE_SYSTEM_USER, user.getAccountGuid());
    }

    @Test
    void getSystemUserByEmailThrows() {
        UserAccountEntity coreSystemUser = new UserAccountEntity();
        coreSystemUser.setAccountGuid(CORE_SYSTEM_USER);

        when(userAccountRepository.findByEmailAddressIgnoreCase(SystemUserHelper.SYSTEM_EMAIL_ADDRESS)).thenReturn(List.of());

        DartsApiException exception = Assertions.assertThrows(DartsApiException.class, () -> systemUserHelper.getSystemUser());
        assertEquals(AudioApiError.MISSING_SYSTEM_USER, exception.getError());
    }
}