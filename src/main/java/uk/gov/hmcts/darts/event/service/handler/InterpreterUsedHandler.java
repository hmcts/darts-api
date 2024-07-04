package uk.gov.hmcts.darts.event.service.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.event.service.EventPersistenceService;
import uk.gov.hmcts.darts.event.service.handler.base.EventHandlerBase;
import uk.gov.hmcts.darts.log.api.LogApi;

@Slf4j
@Service
@SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
public class InterpreterUsedHandler extends EventHandlerBase {

    public InterpreterUsedHandler(RetrieveCoreObjectService retrieveCoreObjectService,
                                  EventRepository eventRepository,
                                  HearingRepository hearingRepository,
                                  CaseRepository caseRepository,
                                  ApplicationEventPublisher eventPublisher,
                                  AuthorisationApi authorisationApi,
                                  LogApi logApi,
                                  EventPersistenceService eventPersistenceService) {
        super(retrieveCoreObjectService, eventRepository, hearingRepository, caseRepository, eventPublisher, logApi, eventPersistenceService, authorisationApi);
    }

    @Override
    @Transactional
    public void handle(final DartsEvent dartsEvent, EventHandlerEntity eventHandler) {
        var createdHearingAndEvent = createHearingAndSaveEvent(dartsEvent, eventHandler);

        var courtCase = createdHearingAndEvent.getHearingEntity().getCourtCase();
        courtCase.setInterpreterUsed(true);
        caseRepository.saveAndFlush(courtCase);
    }
}
