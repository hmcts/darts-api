package uk.gov.hmcts.darts.common.helper;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.audio.exception.AudioApiError;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SystemUsersEnum;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
@ConfigurationProperties(prefix = "darts.automated.task.common-config")
@EnableConfigurationProperties
@Setter
public class SystemUserHelper {

    private final UserAccountRepository userAccountRepository;
    private final AutomatedTaskConfigurationProperties properties;

    public UserAccountEntity getSystemUser() {
        Optional<UserAccountEntity> userAccount = userAccountRepository.findFirstByEmailAddressIgnoreCase(properties.getSystemUserEmail());

        if (userAccount.isEmpty()) {
            throw new DartsApiException(AudioApiError.MISSING_SYSTEM_USER);
        }

        return userAccount.get();
    }

    public UserAccountEntity getReferenceTo(SystemUsersEnum systemUserEnum) {
        return userAccountRepository.getReferenceById(systemUserEnum.getId());
    }
}