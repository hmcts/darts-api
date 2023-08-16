package uk.gov.hmcts.darts.audio.model;

import lombok.Builder;
import lombok.Value;
import uk.gov.hmcts.darts.common.entity.EventEntity;

import java.time.ZonedDateTime;
import java.util.List;

@Value
@Builder
public class ViqAnnotationData {

    private ZonedDateTime annotationsStartTime;
    private List<EventEntity> events;

}
