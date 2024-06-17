package uk.gov.hmcts.darts.casedocument.template;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
public class EventCaseDocument extends CreatedModifiedCaseDocument {

    private final Integer id;
    private final String legacyObjectId;
    private final EventHandlerCaseDocument eventType;
    private final Integer legacyEventId;
    private final String eventText;
    private final OffsetDateTime timestamp;
    private final String legacyVersionLabel;
    private final String messageId;
    private final Boolean isLogEntry;
    private final String chronicleId;
    private final String antecedentId;
}
