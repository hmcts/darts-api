package uk.gov.hmcts.darts.event.service.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.common.entity.CaseManagementRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CaseRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.entity.RetentionPolicyTypeEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.CaseManagementRetentionRepository;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.CaseRetentionRepository;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.repository.RetentionPolicyTypeRepository;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;
import uk.gov.hmcts.darts.event.model.CreatedHearingAndEvent;
import uk.gov.hmcts.darts.event.model.DarNotifyApplicationEvent;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.event.model.DartsEventRetentionPolicy;
import uk.gov.hmcts.darts.event.model.stopandclosehandler.PendingRetention;
import uk.gov.hmcts.darts.event.service.handler.base.EventHandlerBase;
import uk.gov.hmcts.darts.event.service.impl.DarNotifyServiceImpl;
import uk.gov.hmcts.darts.retention.api.RetentionApi;
import uk.gov.hmcts.darts.retention.enums.CaseRetentionStatus;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static java.lang.Boolean.TRUE;
import static uk.gov.hmcts.darts.event.enums.DarNotifyType.STOP_RECORDING;
import static uk.gov.hmcts.darts.event.exception.EventError.EVENT_DATA_NOT_FOUND;

@Service
@Slf4j
public class StopAndCloseHandler extends EventHandlerBase {

    private final DarNotifyServiceImpl darNotifyService;
    private final CaseRetentionRepository caseRetentionRepository;
    private final CaseManagementRetentionRepository caseManagementRetentionRepository;
    private final RetentionPolicyTypeRepository retentionPolicyTypeRepository;
    private final CurrentTimeHelper currentTimeHelper;
    private final RetentionApi retentionApi;

    public StopAndCloseHandler(RetrieveCoreObjectService retrieveCoreObjectService,
                               EventRepository eventRepository,
                               HearingRepository hearingRepository,
                               CaseRepository caseRepository,
                               ApplicationEventPublisher eventPublisher,
                               DarNotifyServiceImpl darNotifyService,
                               CaseRetentionRepository caseRetentionRepository,
                               CaseManagementRetentionRepository caseManagementRetentionRepository,
                               RetentionPolicyTypeRepository retentionPolicyTypeRepository,
                               CurrentTimeHelper currentTimeHelper,
                               RetentionApi retentionApi,
                               AuthorisationApi authorisationApi) {
        super(retrieveCoreObjectService, eventRepository, hearingRepository, caseRepository, eventPublisher, authorisationApi);
        this.darNotifyService = darNotifyService;
        this.caseRetentionRepository = caseRetentionRepository;
        this.caseManagementRetentionRepository = caseManagementRetentionRepository;
        this.retentionPolicyTypeRepository = retentionPolicyTypeRepository;
        this.currentTimeHelper = currentTimeHelper;
        this.retentionApi = retentionApi;
    }


    @Override
    @Transactional
    public void handle(DartsEvent dartsEvent, EventHandlerEntity eventHandler) {
        var hearingAndEvent = createHearingAndSaveEvent(dartsEvent, eventHandler); // saveEvent
        var courtCase = hearingAndEvent.getHearingEntity().getCourtCase();

        var notifyEvent = new DarNotifyApplicationEvent(this, dartsEvent, STOP_RECORDING, hearingAndEvent.getCourtroomId());
        darNotifyService.notifyDarPc(notifyEvent);

        CaseManagementRetentionEntity caseManagementRetentionEntity = createCaseManagementRetentionEntity(hearingAndEvent.getEventEntity(),
                                                                                                          hearingAndEvent.getHearingEntity().getCourtCase(),
                                                                                                          dartsEvent.getRetentionPolicy());

        Optional<CaseRetentionEntity> latestCompletedManualRetention = caseRetentionRepository.findLatestCompletedManualRetention(courtCase);
        if (latestCompletedManualRetention.isPresent()) {
            log.info("Ignoring event with id {} because there is a manual retention for caseId {}.", dartsEvent.getEventId(), courtCase.getId());
            return;
        }

        Optional<PendingRetention> latestPendingRetentionOpt = caseRetentionRepository.findLatestPendingRetention(courtCase);
        if (latestPendingRetentionOpt.isEmpty()) {
            closeCase(dartsEvent, courtCase);
            createRetention(caseManagementRetentionEntity, hearingAndEvent, dartsEvent);
        } else {
            PendingRetention latestPendingRetention = latestPendingRetentionOpt.get();
            if (dartsEvent.getDateTime().isAfter(latestPendingRetention.getEventTimestamp())) {
                closeCase(dartsEvent, courtCase);
                updateExistingRetention(caseManagementRetentionEntity, latestPendingRetention.getCaseRetention(), dartsEvent);
            } else {
                log.info("Ignoring event with id {} because its event time {} is not after the latest pending entry {} for caseId {}.", dartsEvent.getEventId(),
                         dartsEvent.getDateTime(), latestPendingRetention.getEventTimestamp(), courtCase.getId());
            }
        }
    }

    private static void closeCase(DartsEvent dartsEvent, CourtCaseEntity courtCase) {
        //setting the case to closed after notifying DAR Pc to ensure notification is sent.
        courtCase.setClosed(TRUE);
        courtCase.setCaseClosedTimestamp(dartsEvent.getDateTime());
    }

    private void updateExistingRetention(CaseManagementRetentionEntity caseManagementRetentionEntity, CaseRetentionEntity existingCaseRetention,
                                         DartsEvent dartsEvent) {
        DartsEventRetentionPolicy dartsEventRetentionPolicy = dartsEvent.getRetentionPolicy();
        existingCaseRetention.setRetentionPolicyType(caseManagementRetentionEntity.getRetentionPolicyTypeEntity());
        existingCaseRetention.setTotalSentence(dartsEventRetentionPolicy.getCaseTotalSentence());

        OffsetDateTime currentTimestamp = currentTimeHelper.currentOffsetDateTime();
        LocalDate currentDate = currentTimestamp.toLocalDate();
        LocalDate retentionDate = retentionApi.applyPolicyStringToDate(currentDate,
                                                                       dartsEventRetentionPolicy.getCaseTotalSentence(),
                                                                       caseManagementRetentionEntity.getRetentionPolicyTypeEntity());

        existingCaseRetention.setRetainUntil(retentionDate.atStartOfDay().atOffset(ZoneOffset.UTC));
        existingCaseRetention.setCaseManagementRetention(caseManagementRetentionEntity);
        UserAccountEntity currentUser = authorisationApi.getCurrentUser();
        existingCaseRetention.setSubmittedBy(currentUser);
        existingCaseRetention.setCreatedBy(currentUser);
        existingCaseRetention.setLastModifiedBy(currentUser);
        caseRetentionRepository.save(existingCaseRetention);
    }

    private void createRetention(CaseManagementRetentionEntity caseManagementRetentionEntity, CreatedHearingAndEvent hearingAndEvent, DartsEvent dartsEvent) {
        DartsEventRetentionPolicy dartsEventRetentionPolicy = dartsEvent.getRetentionPolicy();
        CourtCaseEntity courtCase = hearingAndEvent.getHearingEntity().getCourtCase();

        CaseRetentionEntity caseRetentionEntity = new CaseRetentionEntity();
        caseRetentionEntity.setCourtCase(courtCase);
        caseRetentionEntity.setRetentionPolicyType(caseManagementRetentionEntity.getRetentionPolicyTypeEntity());
        caseRetentionEntity.setCaseManagementRetention(caseManagementRetentionEntity);
        caseRetentionEntity.setTotalSentence(dartsEventRetentionPolicy.getCaseTotalSentence());
        OffsetDateTime currentTimestamp = currentTimeHelper.currentOffsetDateTime();
        LocalDate currentDate = currentTimestamp.toLocalDate();
        LocalDate retentionDate = retentionApi.applyPolicyStringToDate(currentDate,
                                                                       dartsEventRetentionPolicy.getCaseTotalSentence(),
                                                                       caseManagementRetentionEntity.getRetentionPolicyTypeEntity());

        caseRetentionEntity.setRetainUntil(retentionDate.atStartOfDay().atOffset(ZoneOffset.UTC));
        caseRetentionEntity.setCurrentState(CaseRetentionStatus.PENDING.name());
        UserAccountEntity currentUser = authorisationApi.getCurrentUser();
        caseRetentionEntity.setSubmittedBy(currentUser);
        caseRetentionEntity.setCreatedBy(currentUser);
        caseRetentionEntity.setLastModifiedBy(currentUser);
        caseRetentionRepository.save(caseRetentionEntity);
    }

    private CaseManagementRetentionEntity createCaseManagementRetentionEntity(EventEntity eventEntity, CourtCaseEntity courtCase,
                                                                              DartsEventRetentionPolicy dartsEventRetentionPolicy) {
        CaseManagementRetentionEntity caseManagementRetentionEntity = new CaseManagementRetentionEntity();
        caseManagementRetentionEntity.setCourtCase(courtCase);
        caseManagementRetentionEntity.setEventEntity(eventEntity);


        caseManagementRetentionEntity.setRetentionPolicyTypeEntity(getRetentionPolicy(dartsEventRetentionPolicy.getCaseRetentionFixedPolicy()));
        caseManagementRetentionEntity.setTotalSentence(dartsEventRetentionPolicy.getCaseTotalSentence());
        caseManagementRetentionEntity = caseManagementRetentionRepository.save(caseManagementRetentionEntity);
        return caseManagementRetentionEntity;
    }

    private RetentionPolicyTypeEntity getRetentionPolicy(String fixedPolicyKey) {
        Optional<RetentionPolicyTypeEntity> retentionPolicyOpt = retentionPolicyTypeRepository.findCurrentWithFixedPolicyKey(
            fixedPolicyKey, currentTimeHelper.currentOffsetDateTime());
        if (retentionPolicyOpt.isEmpty()) {
            throw new DartsApiException(EVENT_DATA_NOT_FOUND,
                                        MessageFormat.format("Could not find a retention policy for fixedPolicyKey ''{0}''", fixedPolicyKey));
        }
        return retentionPolicyOpt.get();
    }

}
