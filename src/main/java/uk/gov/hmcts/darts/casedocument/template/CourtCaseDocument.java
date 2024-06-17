package uk.gov.hmcts.darts.casedocument.template;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class CourtCaseDocument extends CreatedModifiedCaseDocument {

    private Integer id;
    private EventHandlerCaseDocument reportingRestrictions;
    private String legacyCaseObjectId;
    private String caseNumber;
    private CourthouseCaseDocument courthouse;
    private Boolean closed;
    private Boolean interpreterUsed;
    private OffsetDateTime caseClosedTimestamp;
    private boolean isRetentionUpdated;
    private List<DefenceCourtCase> defendants;
    private List<DefenceCourtCase> prosecutors;
    private List<DefenceCourtCase> defences;
    private boolean isDeleted;
    private Integer deletedBy;
    private OffsetDateTime deletedTimestamp;
    private List<CaseRetentionCaseDocument> caseRetentions;
//    private List<HearingEntity> hearings;
    private List<JudgeCourtCaseDocument> judges;
    private boolean isDataAnonymised;
    private Integer dataAnonymisedBy;
    private OffsetDateTime dataAnonymisedTs;


}
