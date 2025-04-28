package uk.gov.hmcts.darts.common.entity.compositeid;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor
@EqualsAndHashCode
public class HearingReportingRestrictionsId implements Serializable {

    private Integer hearingId;

    private Long eveId;
}
