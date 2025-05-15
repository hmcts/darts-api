package uk.gov.hmcts.darts.retention.service;

import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.event.model.CreatedHearingAndEvent;
import uk.gov.hmcts.darts.event.model.DartsEvent;

@FunctionalInterface
public interface CloseCaseWithRetentionService {

    void closeCaseAndSetRetention(DartsEvent dartsEvent, CreatedHearingAndEvent hearingAndEvent, CourtCaseEntity courtCase);
}
