package uk.gov.hmcts.darts.casedocument.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
public class EventCaseDocument extends CreatedModifiedCaseDocument {

    private Long id;
    private String legacyObjectId;
    private EventHandlerCaseDocument eventType;
    private Integer eventId;
    private String eventText;
    private OffsetDateTime timestamp;
    private String legacyVersionLabel;
    private String messageId;
    private boolean isLogEntry;
    private String chronicleId;
    private String antecedentId;
}
