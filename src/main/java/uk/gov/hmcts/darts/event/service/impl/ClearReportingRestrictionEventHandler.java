package uk.gov.hmcts.darts.event.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.CaseEntity;
import uk.gov.hmcts.darts.event.model.CourtroomCourthouseCourtcase;
import uk.gov.hmcts.darts.event.model.DartsEvent;

@Slf4j
@Service
public class ClearReportingRestrictionEventHandler extends EventHandlerBase {

    @Transactional
    @Override
    public void handle(final DartsEvent dartsEvent) {
        CourtroomCourthouseCourtcase courtroomCourthouseCourtcase = getOrCreateCourtroomCourtHouseAndCases(dartsEvent);
        CaseEntity caseEntity = courtroomCourthouseCourtcase.getCaseEntity();
        caseEntity.setReportingRestrictions(null);
        getCaseRepository().save(caseEntity);
    }
}
