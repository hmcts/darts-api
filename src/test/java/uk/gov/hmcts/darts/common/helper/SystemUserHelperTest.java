package uk.gov.hmcts.darts.common.helper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;

import java.util.HashMap;
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

        UserAccountEntity housekeepingUser = new UserAccountEntity();
        housekeepingUser.setAccountGuid(HOUSEKEEPING_GUID);

        UserAccountEntity dailylistProcessorUser = new UserAccountEntity();
        dailylistProcessorUser.setAccountGuid(DAILYLIST_PROCESSOR_GUID);

        when(userAccountRepository.findSystemUser(HOUSEKEEPING_GUID)).thenReturn(housekeepingUser);
        when(userAccountRepository.findSystemUser(DAILYLIST_PROCESSOR_GUID)).thenReturn(dailylistProcessorUser);
    }

    /**
     * check if cache is used and populated correctly by trying to get a user.
     * The first time the repository will be hit the second time the same user will exist in the cache.
     */
    @Test
    void getSystemUser() {
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
}