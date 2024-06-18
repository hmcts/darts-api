package uk.gov.hmcts.darts.casedocument.template;

import lombok.Data;
import uk.gov.hmcts.darts.common.entity.ObjectHiddenReasonEntity;

import java.time.OffsetDateTime;

@Data
public class ObjectAdminActionCaseDocument {

    private Integer id;
    private Integer annotationDocument;
    private Integer caseDocument;
    private Integer media;
    private Integer transcriptionDocument;
    private ObjectHiddenReasonEntity objectHiddenReason;
    private Integer hiddenBy;
    private OffsetDateTime hiddenDateTime;
    private boolean markedForManualDeletion;
    private Integer markedForManualDelBy;
    private OffsetDateTime markedForManualDelDateTime;
    private String ticketReference;
    private String comments;
}
