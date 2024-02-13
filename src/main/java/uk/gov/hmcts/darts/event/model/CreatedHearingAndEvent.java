package uk.gov.hmcts.darts.event.model;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;


@Data
@Builder
public class CreatedHearingAndEvent {
    private final HearingEntity hearingEntity;

    private boolean isHearingNew;
    private boolean isCourtroomDifferentFromHearing;
    private EventEntity eventEntity;
}
