package uk.gov.hmcts.darts.notification.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.notification.NotificationConstants;
import uk.gov.hmcts.darts.notification.dto.GovNotifyRequest;
import uk.gov.hmcts.darts.notification.entity.NotificationEntity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class GovNotifyRequestHelper {

    @Value("${darts.portal.url}")
    private String portalUrl;

    private static final String NOT_AVAILABLE = "not available";

    public GovNotifyRequest map(NotificationEntity notification, String templateId) throws JsonProcessingException {
        GovNotifyRequest request = new GovNotifyRequest();
        request.setTemplateId(templateId);
        request.setEmailAddress(notification.getEmailAddress());
        request.setParameterMap(createParameterMap(notification));
        request.setReference(String.valueOf(notification.getId()));
        return request;
    }

    private Map<String, String> createParameterMap(NotificationEntity notification) throws JsonProcessingException {
        Map<String, String> parameterMap = new ConcurrentHashMap<>();
        addCommonParameters(notification, parameterMap);
        String templateValuesStr = notification.getTemplateValues();
        if (StringUtils.isNotBlank(templateValuesStr)) {
            Map<String, String> keyValues;
            try {
                ObjectReader objectReader = new ObjectMapper().readerForMapOf(String.class);
                keyValues = objectReader.readValue(templateValuesStr);
            } catch (JsonProcessingException e) {
                log.error("Unable to parse TemplateValues for notificationId {}", notification.getId());
                throw e;
            }
            parameterMap.putAll(keyValues);
        }
        return parameterMap;
    }

    private void addCommonParameters(NotificationEntity notification, Map<String, String> parameterMap) {
        parameterMap.put(
            NotificationConstants.ParameterMapValues.CASE_NUMBER,
            String.valueOf(notification.getCourtCase().getCaseNumber())
        );
        parameterMap.put(
            NotificationConstants.ParameterMapValues.PORTAL_URL,
            portalUrl
        );

        String courthouseName = notification.getCourtCase().getCourthouse().getDisplayName() != null
            ? notification.getCourtCase().getCourthouse().getDisplayName() : NOT_AVAILABLE;

        parameterMap.put(
            NotificationConstants.ParameterMapValues.COURTHOUSE,
            courthouseName
        );
    }
}
