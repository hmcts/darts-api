package uk.gov.hmcts.darts.event.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.event.model.CourtroomCourthouseCourtcase;
import uk.gov.hmcts.darts.event.model.DartsEvent;

@Slf4j
@Service
public class SetReportingRestrictionEventHandler extends EventHandlerBase {

    @Transactional
    @Override
    public void handle(DartsEvent dartsEvent) {
        CourtroomCourthouseCourtcase courtroomCourthouseCourtcase = getOrCreateCourtroomCourtHouseAndCases(dartsEvent);
        CourtCaseEntity courtCaseEntity = courtroomCourthouseCourtcase.getCourtCaseEntity();
        EventHandlerEntity eventHandlerEntity = eventTypeReference(dartsEvent);
        courtCaseEntity.setReportingRestrictions(eventHandlerEntity);
        getCaseRepository().saveAndFlush(courtCaseEntity);
    }
}
