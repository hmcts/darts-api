package uk.gov.hmcts.darts.audio.model;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.darts.common.entity.EventEntity;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
public class ViqAnnotationData {

    private OffsetDateTime annotationsStartTime;
    private List<EventEntity> events;
}
