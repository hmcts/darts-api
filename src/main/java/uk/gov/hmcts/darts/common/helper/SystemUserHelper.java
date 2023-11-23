package uk.gov.hmcts.darts.common.helper;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;


@Component
@RequiredArgsConstructor
@Slf4j
@ConfigurationProperties(prefix = "darts.automated.task")
@EnableConfigurationProperties
@Setter
public class SystemUserHelper {

    private Map<String, String> systemUserGuidMap;

    public String findSystemUserGuid(String systemUserName) {
        return systemUserGuidMap.get(systemUserName);
    }

}
