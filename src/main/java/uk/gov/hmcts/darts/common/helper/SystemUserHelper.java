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

import java.util.HashMap;
import java.util.Map;


@Component
@RequiredArgsConstructor
@Slf4j
@ConfigurationProperties(prefix = "darts.automated.task")
@EnableConfigurationProperties
@Setter
public class SystemUserHelper {

    public static final String HOUSEKEEPING = "housekeeping";
    public static final String DAILYLIST_PROCESSOR = "dailylist-processor";
    private final UserAccountRepository userAccountRepository;
    private Map<String, String> systemUserGuidMap;
    private Map<String, UserAccountEntity> systemUserNameToEntityMap = new HashMap<>();

    public String findSystemUserGuid(String configKey) {
        return systemUserGuidMap.get(configKey);
    }

    public UserAccountEntity getHousekeepingUser() {
        return getSystemUser(HOUSEKEEPING);
    }

    public UserAccountEntity getDailyListProcessorUser() {
        return getSystemUser(DAILYLIST_PROCESSOR);
    }

    /**
     * This method does not search the database by username but by the key defined in the application yaml.
     */
    public UserAccountEntity getSystemUser(String configKey) {
        return systemUserNameToEntityMap.computeIfAbsent(configKey, k -> {
            UserAccountEntity user = userAccountRepository.findSystemUser(findSystemUserGuid(configKey));
            if (user == null) {
                throw new DartsApiException(AudioApiError.MISSING_SYSTEM_USER);
            }
            return user;
        });

    }

}
