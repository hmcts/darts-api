package uk.gov.hmcts.darts.event.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.event.model.CreatedHearing;
import uk.gov.hmcts.darts.event.model.DartsEvent;

@Slf4j
@Service
@RequiredArgsConstructor
public class SetReportingRestrictionEventHandler extends EventHandlerBase {

    @Transactional
    @Override
    public void handle(DartsEvent dartsEvent, EventHandlerEntity eventHandler) {
        CreatedHearing createdHearing = createHearingAndSaveEvent(dartsEvent, eventHandler);
        CourtCaseEntity courtCaseEntity = createdHearing.getHearingEntity().getCourtCase();
        courtCaseEntity.setReportingRestrictions(eventHandler);
        caseRepository.saveAndFlush(courtCaseEntity);
    }
}
