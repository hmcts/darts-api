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

import java.util.Map;


@Component
@RequiredArgsConstructor
@Slf4j
@ConfigurationProperties(prefix = "darts.automated.task")
@EnableConfigurationProperties
@Setter
public class SystemUserHelper {

    private Map<String, String> systemUserGuidMap;

    private final UserAccountRepository userAccountRepository;

    public String findSystemUserGuid(String systemUserName) {
        return systemUserGuidMap.get(systemUserName);
    }

    public UserAccountEntity getSystemUser() {
        UserAccountEntity user = userAccountRepository.findSystemUser(findSystemUserGuid("housekeeping"));
        if (user == null) {
            throw new DartsApiException(AudioApiError.MISSING_SYSTEM_USER);
        }
        return user;
    }

}
