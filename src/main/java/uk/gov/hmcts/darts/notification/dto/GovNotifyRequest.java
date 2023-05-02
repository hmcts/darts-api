package uk.gov.hmcts.darts.notification.dto;

import lombok.Data;

import java.util.Map;

@Data
public class GovNotifyRequest {
    String templateId;
    String emailAddress;
    Map<String, String> parameterMap;
    String reference;

}
