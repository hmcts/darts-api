package uk.gov.hmcts.darts.notification.helper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.notification.exception.TemplateNotFoundException;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;


@Component
@RequiredArgsConstructor
@Slf4j
public class TemplateIdHelper {

    @Value("${darts.notification.gov_notify.template_ids}")
    private String templateIdsString;

    private Map<String, String> templateIdMap;

    public String findTemplateId(String templateName) throws TemplateNotFoundException {
        getTemplateIdMap();
        String templateId = templateIdMap.get(templateName);
        if(StringUtils.isBlank(templateId)){
            String errorMessage = MessageFormat.format("Unable to find template with name ''{0}''.", templateName);
            log.error(errorMessage);
            throw new TemplateNotFoundException(errorMessage);
        } else{
            return templateId;
        }
    }

    private void getTemplateIdMap(){
        if(templateIdMap==null){
            templateIdMap = new HashMap<>();
            String[] templateMappings = templateIdsString.split(",");
            for(String templateMap: templateMappings){
                String[] nameId = templateMap.split("=");
                templateIdMap.put(nameId[0], nameId[1]);
            }
        }
    }

}
