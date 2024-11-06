package uk.gov.hmcts.darts.common.helper;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.audio.exception.AudioApiError;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.dailylist.exception.DailyListError;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@RequiredArgsConstructor
@Slf4j
@ConfigurationProperties(prefix = "darts.automated.task.common-config")
@EnableConfigurationProperties
@Setter
public class SystemUserHelper {

    public static final String HOUSEKEEPING = "housekeeping";
    public static final String DAILYLIST_PROCESSOR = "dailylist-processor";
    private final UserAccountRepository userAccountRepository;
    private Map<String, String> systemUserGuidMap;
    private ConcurrentMap<String, UserAccountEntity> systemUserNameToEntityMap = new ConcurrentHashMap<>();
    private final AutomatedTaskConfigurationProperties properties;

    public String findSystemUserGuid(String configKey) {
        return systemUserGuidMap.get(configKey);
    }

    public UserAccountEntity getDailyListProcessorUser() {
        return systemUserNameToEntityMap.computeIfAbsent(DAILYLIST_PROCESSOR, k -> {
            UserAccountEntity user = userAccountRepository.findSystemUser(findSystemUserGuid(DAILYLIST_PROCESSOR));
            if (user == null) {
                throw new DartsApiException(DailyListError.MISSING_DAILY_LIST_USER);
            }
            return user;
        });
    }

    public UserAccountEntity getSystemUser() {
        List<UserAccountEntity> userList = userAccountRepository.findByEmailAddressIgnoreCase(properties.getSystemUserEmail());

        if (userList.isEmpty()) {
            throw new DartsApiException(AudioApiError.MISSING_SYSTEM_USER);
        }

        return userList.getFirst();
    }
}