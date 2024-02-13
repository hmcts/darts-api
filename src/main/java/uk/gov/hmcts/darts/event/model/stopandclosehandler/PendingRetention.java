package uk.gov.hmcts.darts.event.model.stopandclosehandler;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import uk.gov.hmcts.darts.common.entity.CaseRetentionEntity;

import java.time.OffsetDateTime;

@Getter
@Setter
@ToString
@AllArgsConstructor
public class PendingRetention {
    CaseRetentionEntity caseRetention;
    OffsetDateTime eventTimestamp;
}
