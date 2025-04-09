package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.EventLinkedCaseEntity;
import uk.gov.hmcts.darts.common.repository.EventLinkedCaseRepository;

@Component
@RequiredArgsConstructor
public class EventLinkedCaseStub {

    private final EventLinkedCaseRepository eventLinkedCaseRepository;

    public EventLinkedCaseEntity createCaseLinkedEvent(EventEntity event, CourtCaseEntity caseEntity) {
        EventLinkedCaseEntity eventLinkedCaseEntity = new EventLinkedCaseEntity();
        eventLinkedCaseEntity.setEvent(event);
        eventLinkedCaseEntity.setCourtCase(caseEntity);
        return eventLinkedCaseRepository.save(eventLinkedCaseEntity);
    }
}