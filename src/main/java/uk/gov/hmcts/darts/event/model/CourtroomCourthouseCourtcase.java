package uk.gov.hmcts.darts.events.model;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;


@Data
@Builder
public class CourtroomCourthouseCourtcase {
    private final CourthouseEntity courthouseEntity;
    private final CourtroomEntity courtroomEntity;
    private final CourtCaseEntity courtCaseEntity;

    private boolean isHearingNew;
    private boolean isCourtroomDifferentFromHearing;
}
