package uk.gov.hmcts.darts.event.handler;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.darts.common.entity.CaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;


@Data
@Builder
public class CourtroomCourthouseCourtcase {
    private final CourthouseEntity courthouseEntity;
    private final CourtroomEntity courtroomEntity;
    private final CaseEntity caseEntity;

    private boolean isHearingNew;
    private boolean isCourtroomDifferentFromHearing;
}
