package uk.gov.hmcts.darts.casedocument.model;

import lombok.Data;
import uk.gov.hmcts.darts.common.entity.ObjectHiddenReasonEntity;

import java.time.OffsetDateTime;

@Data
public class ObjectAdminActionCaseDocument {

    private final Integer id;
    private final Long annotationDocument;
    private final Long caseDocument;
    private final Long media;
    private final Long transcriptionDocument;
    private final ObjectHiddenReasonEntity objectHiddenReason;
    private final Integer hiddenBy;
    private final OffsetDateTime hiddenDateTime;
    private final boolean markedForManualDeletion;
    private final Integer markedForManualDelBy;
    private final OffsetDateTime markedForManualDelDateTime;
    private final String ticketReference;
    private final String comments;
}
