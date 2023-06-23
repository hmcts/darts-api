package uk.gov.hmcts.darts.event.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.Case;
import uk.gov.hmcts.darts.event.CaseRepository;
import uk.gov.hmcts.darts.event.model.DartsEvent;

@Service
@RequiredArgsConstructor
public class DefaultEventHandler extends EventsHandlerBase  {

    private final CaseRepository caseRepository;

    @Override
    public void handle(DartsEvent dartsEvent) {
        var caseNumber = dartsEvent.getCaseNumbers().get(0);

        if (caseRepository.findByCaseId(caseNumber).isPresent()) {

        } else {
            Case caze = new Case();
            caze.setCaseId(caseNumber);
            caseRepository.saveAndFlush(caze);
        }
    }
}
