package uk.gov.hmcts.darts.cases.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.cases.helper.FindCurrentEntitiesHelper;
import uk.gov.hmcts.darts.cases.service.CaseService;
import uk.gov.hmcts.darts.cases.service.CloseOldCasesProcessor;
import uk.gov.hmcts.darts.common.entity.CaseRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.CaseRetentionRepository;
import uk.gov.hmcts.darts.retention.api.RetentionApi;
import uk.gov.hmcts.darts.retention.enums.CaseRetentionStatus;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceCategoryEnum;
import uk.gov.hmcts.darts.retention.enums.RetentionPolicyEnum;
import uk.gov.hmcts.darts.retention.helper.RetentionDateHelper;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static java.lang.Boolean.TRUE;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloseOldCasesProcessorImpl implements CloseOldCasesProcessor {


    private final CloseOldCasesProcessorImpl.CloseCaseProcessor caseProcessor;
    private final CaseRepository caseRepository;

    private final AuthorisationApi authorisationApi;

    @Value("${darts.retention.close-open-cases-older-than-years}")
    long years;

    @Override
    public void closeCases(int batchSize) {
        log.info("Starting to close old cases...");

        List<Integer> courtCaseEntityIdList = caseRepository.findOpenCasesToClose(OffsetDateTime.now().minusYears(years),
                                                                                  Limit.of(batchSize));
        int totalCasesToClose = courtCaseEntityIdList.size();
        log.info("Found {} cases to close out of a batch size {}", totalCasesToClose, batchSize);

        UserAccountEntity userAccount = authorisationApi.getCurrentUser();

        int closedCaseCount = 0;
        for (Integer id : courtCaseEntityIdList) {
            caseProcessor.closeCase(id, userAccount);
            closedCaseCount++;
            log.info("Closed {} out of {} cases.", closedCaseCount, totalCasesToClose);
        }
        log.info("Completed closing old cases.");
    }


    @Service
    @RequiredArgsConstructor(onConstructor = @__(@Autowired))
    static class CloseCaseProcessor {
        private static final String CLOSE_CASE_RETENTION_COMMENT = "CloseOldCases Automated job setting retention period to Default";
        private final CaseService caseService;
        private final CaseRetentionRepository caseRetentionRepository;
        private final RetentionApi retentionApi;
        private final RetentionDateHelper retentionDateHelper;
        private final FindCurrentEntitiesHelper findCurrentEntitiesHelper;

        @Value("#{'${darts.retention.close-events}'.split(',')}")
        private List<String> closeEvents;


        @Transactional
        public void closeCase(Integer courtCaseId, UserAccountEntity userAccount) {
            CourtCaseEntity courtCase = caseService.getCourtCaseById(courtCaseId);


            log.info("About to close court case id {}", courtCase.getId());
            List<EventEntity> eventList = findCurrentEntitiesHelper.getCurrentEvents(courtCase);
            if (CollectionUtils.isNotEmpty(eventList)) {
                eventList.sort(Comparator.comparing(EventEntity::getCreatedDateTime).reversed());
                //find latest closed event
                Optional<EventEntity> closedEvent =
                    eventList.stream().filter(eventEntity -> closeEvents.contains(eventEntity.getEventType().getEventName())).findFirst();

                if (closedEvent.isPresent()) {
                    closeCaseInDbAndAddRetention(courtCase, closedEvent.get().getCreatedDateTime(),
                                                 RetentionConfidenceCategoryEnum.AGED_CASE_CASE_CLOSED, userAccount);
                } else {
                    //look for the last event and use that date
                    closeCaseInDbAndAddRetention(courtCase, eventList.getFirst().getCreatedDateTime(),
                                                 RetentionConfidenceCategoryEnum.AGED_CASE_MAX_EVENT_CLOSED, userAccount);
                }
            } else if (courtCase.getHearings().isEmpty()) {
                //set to created date
                closeCaseInDbAndAddRetention(courtCase, courtCase.getCreatedDateTime(), RetentionConfidenceCategoryEnum.AGED_CASE_CASE_CREATION_CLOSED,
                                             userAccount);
            } else {
                //look for the last audio and use its recorded date
                List<MediaEntity> mediaList = findCurrentEntitiesHelper.getCurrentMedia(courtCase);
                if (mediaList.isEmpty()) {
                    //look for the last hearing date and use that
                    courtCase.getHearings().sort(Comparator.comparing(HearingEntity::getHearingDate).reversed());
                    HearingEntity lastHearingEntity = courtCase.getHearings().getFirst();
                    closeCaseInDbAndAddRetention(courtCase, OffsetDateTime.of(lastHearingEntity.getHearingDate().atStartOfDay(), ZoneOffset.UTC),
                                                 RetentionConfidenceCategoryEnum.AGED_CASE_MAX_HEARING_CLOSED, userAccount);
                } else {
                    mediaList.sort(Comparator.comparing(MediaEntity::getCreatedDateTime).reversed());
                    closeCaseInDbAndAddRetention(courtCase, mediaList.getFirst().getCreatedDateTime(),
                                                 RetentionConfidenceCategoryEnum.AGED_CASE_MAX_MEDIA_CLOSED, userAccount);
                }
            }
            log.info("Successfully closed case with ID: {}", courtCase.getId());
        }

        private void closeCaseInDbAndAddRetention(CourtCaseEntity courtCase, OffsetDateTime caseClosedDate,
                                                  RetentionConfidenceCategoryEnum retentionConfidenceCategory,
                                                  UserAccountEntity userAccount) {
            courtCase.setClosed(TRUE);
            courtCase.setCaseClosedTimestamp(caseClosedDate);
            retentionApi.updateCourtCaseConfidenceAttributesForRetention(courtCase, retentionConfidenceCategory);
            caseService.saveCase(courtCase);
            log.info("Closed court case id {}", courtCase.getId());

            LocalDate retentionDate = retentionDateHelper.getRetentionDateForPolicy(courtCase, RetentionPolicyEnum.DEFAULT);
            CaseRetentionEntity retentionEntity = retentionApi.createRetention(
                RetentionPolicyEnum.DEFAULT, CLOSE_CASE_RETENTION_COMMENT, courtCase, retentionDate, userAccount,
                CaseRetentionStatus.PENDING, retentionConfidenceCategory);
            caseRetentionRepository.save(retentionEntity);
            log.info("Set retention date {} and confidence category {} for case id {}", retentionDate, retentionConfidenceCategory,
                     courtCase.getId());
        }
    }
}