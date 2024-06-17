package uk.gov.hmcts.darts.casedocument.template;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class CourtCaseDocument extends CreatedModifiedCaseDocument {

    private final Integer id;
    private final EventHandlerCaseDocument reportingRestrictions;
    private final String legacyCaseObjectId;
    private final String caseNumber;
    private final CourthouseCaseDocument courthouse;
    private final Boolean closed;
    private final Boolean interpreterUsed;
    private final OffsetDateTime caseClosedTimestamp;
    private final boolean retentionUpdated;
    private final List<DefenceCourtCase> defendants;
    private final List<DefenceCourtCase> prosecutors;
    private final List<DefenceCourtCase> defences;
    private final boolean deleted;
    private final Integer deletedBy;
    private final OffsetDateTime deletedTimestamp;
    private final List<CaseRetentionCaseDocument> caseRetentions;
//    private final List<HearingEntity> hearings;
    private final List<JudgeCourtCaseDocument> judges;
    private final boolean dataAnonymised;
    private final Integer dataAnonymisedBy;
    private final OffsetDateTime dataAnonymisedTs;


}
