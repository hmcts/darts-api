package uk.gov.hmcts.darts.notification.helper;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.notification.exception.TemplateNotFoundException;

import java.text.MessageFormat;
import java.util.Map;


@Component
@RequiredArgsConstructor
@Slf4j
@ConfigurationProperties(prefix = "darts.notification.gov-notify")
@EnableConfigurationProperties
@Setter
public class TemplateIdHelper {

    private Map<String, String> templateMap;

    public String findTemplateId(String templateName) throws TemplateNotFoundException {
        String templateId = templateMap.get(templateName);
        if (StringUtils.isBlank(templateId)) {
            String errorMessage = MessageFormat.format("Unable to find template with name ''{0}''.", templateName);
            log.error(errorMessage);
            throw new TemplateNotFoundException(errorMessage);
        } else {
            return templateId;
        }
    }

}
