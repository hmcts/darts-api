package uk.gov.hmcts.darts.arm.mapper.template;

import lombok.AllArgsConstructor;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;

import java.time.format.DateTimeFormatter;
import java.util.List;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.darts.arm.util.TemplateConstants.CLIENT_IDENTIFIER_KEY;
import static uk.gov.hmcts.darts.arm.util.TemplateConstants.EVENT_DATE_KEY;
import static uk.gov.hmcts.darts.arm.util.TemplateConstants.RECORD_DATE_KEY;
import static uk.gov.hmcts.darts.arm.util.TemplateConstants.RELATION_ID_KEY;

@AllArgsConstructor
public class BaseTemplate {
    protected static final String CASE_NUMBER_DELIMITER = "|";
    protected final ArmDataManagementConfiguration armDataManagementConfiguration;
    protected final CurrentTimeHelper currentTimeHelper;

    protected String mapTemplateContents(ExternalObjectDirectoryEntity externalObjectDirectory, String templateFileContents) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(armDataManagementConfiguration.getDateTimeFormat());
        String eventDate = "";
        if (nonNull(externalObjectDirectory.getEventDateTs())) {
            eventDate = externalObjectDirectory.getEventDateTs().format(formatter);
        }
        return templateFileContents.replaceAll(RELATION_ID_KEY, externalObjectDirectory.getId().toString())
            .replaceAll(RECORD_DATE_KEY, currentTimeHelper.currentOffsetDateTime().format(formatter))
            .replaceAll(EVENT_DATE_KEY, eventDate)
            .replaceAll(CLIENT_IDENTIFIER_KEY, externalObjectDirectory.getId().toString());
    }

    protected String caseListToString(List<String> caseIdList) {
        return String.join(CASE_NUMBER_DELIMITER, caseIdList);
    }

}
