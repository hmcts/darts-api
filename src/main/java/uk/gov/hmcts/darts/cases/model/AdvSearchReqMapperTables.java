package uk.gov.hmcts.darts.cases.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdvSearchReqMapperTables {
    private boolean joinHearing = false;
    private boolean joinCourtroom = false;
    private boolean joinCourthouse = false;
    private boolean joinJudges = false;
    private boolean joinDefendants = false;
    private boolean joinEvents = false;
}
