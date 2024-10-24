package uk.gov.hmcts.darts.event.service.handler;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.common.entity.CaseManagementRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CaseRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.CaseRetentionRepository;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;
import uk.gov.hmcts.darts.event.exception.DarNotifyError;
import uk.gov.hmcts.darts.event.model.CreatedHearingAndEvent;
import uk.gov.hmcts.darts.event.model.DarNotifyApplicationEvent;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.event.model.DartsEventRetentionPolicy;
import uk.gov.hmcts.darts.event.model.stopandclosehandler.PendingRetention;
import uk.gov.hmcts.darts.event.service.CaseManagementRetentionService;
import uk.gov.hmcts.darts.event.service.EventPersistenceService;
import uk.gov.hmcts.darts.event.service.handler.base.EventHandlerBase;
import uk.gov.hmcts.darts.event.service.impl.DarNotifyServiceImpl;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.retention.api.RetentionApi;
import uk.gov.hmcts.darts.retention.enums.CaseRetentionStatus;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceCategoryEnum;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceReasonEnum;
import uk.gov.hmcts.darts.retention.enums.RetentionPolicyEnum;
import uk.gov.hmcts.darts.util.DataUtil;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static java.lang.Boolean.TRUE;
import static uk.gov.hmcts.darts.event.enums.DarNotifyType.STOP_RECORDING;
import static uk.gov.hmcts.darts.retention.enums.RetentionConfidenceScoreEnum.CASE_PERFECTLY_CLOSED;

@Service
@Slf4j
public class StopAndCloseHandler extends EventHandlerBase {

    private final DarNotifyServiceImpl darNotifyService;
    private final CaseRetentionRepository caseRetentionRepository;
    private final RetentionApi retentionApi;
    private final CaseManagementRetentionService caseManagementRetentionService;
    private final AuthorisationApi authorisationApi;
    private final CurrentTimeHelper currentTimeHelper;


    @Value("${darts.retention.overridable-fixed-policy-keys}")
    List<String> overridableFixedPolicyKeys;

    @SuppressWarnings({"PMD.ExcessiveParameterList"})
    public StopAndCloseHandler(RetrieveCoreObjectService retrieveCoreObjectService,
                               EventRepository eventRepository,
                               HearingRepository hearingRepository,
                               CaseRepository caseRepository,
                               ApplicationEventPublisher eventPublisher,
                               DarNotifyServiceImpl darNotifyService,
                               CaseRetentionRepository caseRetentionRepository,
                               RetentionApi retentionApi,
                               AuthorisationApi authorisationApi,
                               LogApi logApi,
                               CaseManagementRetentionService caseManagementRetentionService,
                               EventPersistenceService eventPersistenceService,
                               CurrentTimeHelper currentTimeHelper) {
        super(retrieveCoreObjectService, eventRepository, hearingRepository, caseRepository, eventPublisher, logApi, eventPersistenceService);
        this.darNotifyService = darNotifyService;
        this.caseRetentionRepository = caseRetentionRepository;
        this.caseManagementRetentionService = caseManagementRetentionService;
        this.retentionApi = retentionApi;
        this.authorisationApi = authorisationApi;
        this.currentTimeHelper = currentTimeHelper;
    }


    @Override
    @Transactional
    @SuppressWarnings("PMD.EmptyCatchBlock")
    public void handle(DartsEvent dartsEvent, EventHandlerEntity eventHandler) {
        DataUtil.preProcess(dartsEvent);
        var hearingAndEvent = createHearingAndSaveEvent(dartsEvent, eventHandler); // saveEvent
        var courtCase = hearingAndEvent.getHearingEntity().getCourtCase();

        try {
            var notifyEvent = new DarNotifyApplicationEvent(this, dartsEvent, STOP_RECORDING, hearingAndEvent.getCourtroomId());
            darNotifyService.notifyDarPc(notifyEvent);
        } catch (DarNotifyError ignored) {
            // if DAR notify fails, continue processing the event
        }

        setDefaultPolicyIfNotDefined(dartsEvent);

        CaseManagementRetentionEntity caseManagementRetentionEntity = caseManagementRetentionService.createCaseManagementRetention(
            hearingAndEvent.getEventEntity(),
            hearingAndEvent.getHearingEntity().getCourtCase(),
            dartsEvent.getRetentionPolicy());

        Optional<CaseRetentionEntity> latestCompletedManualRetention = caseRetentionRepository.findLatestCompletedManualRetention(courtCase);
        if (latestCompletedManualRetention.isPresent()) {
            log.info("Ignoring event with id {} because there is a manual retention for caseId {}.", dartsEvent.getEventId(), courtCase.getId());
            return;
        }

        // ignore the caseTotalSentence if it's not an overridable policy
        if (dartsEvent.getRetentionPolicy().getCaseTotalSentence() != null && !overridableFixedPolicyKeys.contains(
            dartsEvent.getRetentionPolicy().getCaseRetentionFixedPolicy())) {
            dartsEvent.getRetentionPolicy().setCaseTotalSentence(null);
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

    private void setDefaultPolicyIfNotDefined(DartsEvent dartsEvent) {
        if (dartsEvent.getRetentionPolicy() == null) {
            DartsEventRetentionPolicy defaultRetentionPolicy = new DartsEventRetentionPolicy();
            defaultRetentionPolicy.setCaseRetentionFixedPolicy(RetentionPolicyEnum.DEFAULT.getPolicyKey());
            dartsEvent.setRetentionPolicy(defaultRetentionPolicy);
        }
    }

    private void closeCase(DartsEvent dartsEvent, CourtCaseEntity courtCase) {
        if (BooleanUtils.isNotTrue(courtCase.getClosed()) || courtCase.getCaseClosedTimestamp() == null) {
            //setting the case to closed after notifying DAR Pc to ensure notification is sent.
            courtCase.setClosed(TRUE);
            courtCase.setCaseClosedTimestamp(dartsEvent.getDateTime());
            courtCase.setLastModifiedBy(authorisationApi.getCurrentUser());
            courtCase.setRetConfScore(CASE_PERFECTLY_CLOSED);
            courtCase.setRetConfReason(RetentionConfidenceReasonEnum.CASE_CLOSED);
            courtCase.setRetConfUpdatedTs(currentTimeHelper.currentOffsetDateTime());
            caseRepository.saveAndFlush(courtCase);
        }
    }

    private void updateExistingRetention(CaseManagementRetentionEntity caseManagementRetentionEntity, CaseRetentionEntity existingCaseRetention,
                                         DartsEvent dartsEvent) {
        DartsEventRetentionPolicy dartsEventRetentionPolicy = dartsEvent.getRetentionPolicy();
        existingCaseRetention.setRetentionPolicyType(caseManagementRetentionEntity.getRetentionPolicyTypeEntity());
        existingCaseRetention.setTotalSentence(dartsEventRetentionPolicy.getCaseTotalSentence());

        OffsetDateTime eventTimestamp = dartsEvent.getDateTime();
        LocalDate eventDate = eventTimestamp.toLocalDate();
        LocalDate retentionDate = retentionApi.applyPolicyStringToDate(eventDate,
                                                                       dartsEventRetentionPolicy.getCaseTotalSentence(),
                                                                       caseManagementRetentionEntity.getRetentionPolicyTypeEntity());

        existingCaseRetention.setRetainUntil(retentionDate.atStartOfDay().atOffset(ZoneOffset.UTC));
        existingCaseRetention.setCaseManagementRetention(caseManagementRetentionEntity);
        UserAccountEntity currentUser = authorisationApi.getCurrentUser();
        existingCaseRetention.setSubmittedBy(currentUser);
        existingCaseRetention.setCreatedBy(currentUser);
        existingCaseRetention.setLastModifiedBy(currentUser);
        existingCaseRetention.setConfidenceCategory(RetentionConfidenceCategoryEnum.CASE_CLOSED);
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
        caseRetentionEntity.setConfidenceCategory(RetentionConfidenceCategoryEnum.CASE_CLOSED);
        OffsetDateTime eventTimestamp = dartsEvent.getDateTime();
        LocalDate eventDate = eventTimestamp.toLocalDate();
        LocalDate retentionDate = retentionApi.applyPolicyStringToDate(eventDate,
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

}