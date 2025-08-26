package uk.gov.hmcts.darts.event.service.handler;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;
import uk.gov.hmcts.darts.event.config.EventConfig;
import uk.gov.hmcts.darts.event.model.CreatedHearingAndEvent;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.event.service.CaseManagementRetentionService;
import uk.gov.hmcts.darts.event.service.EventPersistenceService;
import uk.gov.hmcts.darts.event.service.handler.base.EventHandlerBase;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.util.DataUtil;

@Slf4j
@Service
public class StandardEventHandler extends EventHandlerBase {
    private final CaseManagementRetentionService caseManagementRetentionService;

    private final EventConfig eventConfig;

    public StandardEventHandler(RetrieveCoreObjectService retrieveCoreObjectService,
                                EventRepository eventRepository,
                                HearingRepository hearingRepository,
                                CaseRepository caseRepository,
                                ApplicationEventPublisher eventPublisher,
                                LogApi logApi,
                                EventPersistenceService eventPersistenceService,
                                CaseManagementRetentionService caseManagementRetentionService,
                                EventConfig eventConfig
    ) {
        super(retrieveCoreObjectService, eventRepository, hearingRepository, caseRepository, eventPublisher, logApi, eventPersistenceService);
        this.caseManagementRetentionService = caseManagementRetentionService;
        this.eventConfig = eventConfig;
    }

    @Override
    public void handle(final DartsEvent dartsEvent, EventHandlerEntity eventHandler) {
        DataUtil.preProcess(dartsEvent);
        var hearingAndEvent = createHearingAndSaveEvent(dartsEvent, eventHandler);

        saveRetentionForEvent(dartsEvent, hearingAndEvent, eventHandler);
    }

    private void saveRetentionForEvent(DartsEvent dartsEvent, CreatedHearingAndEvent hearingAndEvent, EventHandlerEntity eventHandler) {
        // store retention information for potential future use
        if (CollectionUtils.isNotEmpty(eventConfig.getStandardEventTypesWithRetention())
            && eventConfig.getStandardEventTypesWithRetention().contains(eventHandler.getType())
            && dartsEvent.getRetentionPolicy() != null) {

            caseManagementRetentionService.createCaseManagementRetention(
                hearingAndEvent.getEventEntity(),
                hearingAndEvent.getHearingEntity().getCourtCase(),
                dartsEvent.getRetentionPolicy());

        }
    }
}
