package uk.gov.hmcts.darts.casedocument.template;

import lombok.Data;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.JudgeEntity;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class CourtCaseDocument {

    private Integer id;
    private OffsetDateTime createdDateTime;
    private Integer createdBy;
    private OffsetDateTime lastModifiedDateTime;
    private Integer lastModifiedBy;
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
//    private List<CaseRetentionCaseDocument> caseRetentionEntities = new ArrayList<>();
    private List<HearingEntity> hearings = new ArrayList<>();
    private List<JudgeEntity> judges = new ArrayList<>();
    private boolean isDataAnonymised;
    private Integer dataAnonymisedBy;
    private OffsetDateTime dataAnonymisedTs;


}
