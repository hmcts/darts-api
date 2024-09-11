package uk.gov.hmcts.darts.cases.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
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
import uk.gov.hmcts.darts.retentions.model.PostRetentionRequest;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static java.lang.Boolean.TRUE;
import static uk.gov.hmcts.darts.retention.enums.RetentionConfidenceReasonEnum.AGED_CASE;
import static uk.gov.hmcts.darts.retention.enums.RetentionConfidenceScoreEnum.CASE_NOT_PERFECTLY_CLOSED;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloseOldCasesProcessorImpl implements CloseOldCasesProcessor {
    private static final String CLOSE_CASE_RETENTION_COMMENT = "CloseOldCases Automated job setting retention period to Default";
    private final CaseRepository caseRepository;
    private final CaseRetentionRepository caseRetentionRepository;
    private final RetentionApi retentionApi;
    private final RetentionDateHelper retentionDateHelper;
    private final AuthorisationApi authorisationApi;

    private UserAccountEntity userAccount;

    @Value("${darts.retention.close-open-cases-older-than-years}")
    long years;

    @Value("#{'${darts.retention.close-events}'.split(',')}")
    private List<String> closeEvents;

    @Transactional
    @Override
    public void closeCases(int batchSize) {
        log.info("Starting to close old cases...");

        List<CourtCaseEntity> courtCaseEntityList = caseRepository.findOpenCasesToClose(OffsetDateTime.now().minusYears(years),
                                                                                        Pageable.ofSize(batchSize));
        int totalCasesToClose = courtCaseEntityList.size();
        log.info("Found {} cases to close.", totalCasesToClose);

        userAccount = authorisationApi.getCurrentUser();

        int closedCaseCount = 0;
        for (CourtCaseEntity courtCase : courtCaseEntityList) {
            closeCase(courtCase);
            closedCaseCount++;
            log.info("Closed {} out of {} cases.", closedCaseCount, totalCasesToClose);
        }

        log.info("Completed closing old cases.");
    }

    private void closeCase(CourtCaseEntity courtCase) {
        log.debug("About to close court case id {}", courtCase.getId());
        List<EventEntity> eventList = new ArrayList<>();
        for (HearingEntity hearingEntity : courtCase.getHearings()) {
            eventList.addAll(hearingEntity.getEventList());
        }
        if (CollectionUtils.isNotEmpty(eventList)) {
            eventList.sort(Comparator.comparing(EventEntity::getCreatedDateTime).reversed());
            //find latest closed event
            Optional<EventEntity> closedEvent =
                eventList.stream().filter(eventEntity -> closeEvents.contains(eventEntity.getEventType().getEventName())).findFirst();

            if (closedEvent.isPresent()) {
                closeCaseInDbAndAddRetention(courtCase, closedEvent.get().getCreatedDateTime());
            } else {
                //look for the last event and use that date
                closeCaseInDbAndAddRetention(courtCase, eventList.getFirst().getCreatedDateTime());
            }
        } else if (courtCase.getHearings().isEmpty()) {
            //set to created date
            closeCaseInDbAndAddRetention(courtCase, courtCase.getCreatedDateTime());
        } else {
            //look for the last audio and use its recorded date
            List<MediaEntity> mediaList = new ArrayList<>();
            for (HearingEntity hearingEntity : courtCase.getHearings()) {
                mediaList.addAll(hearingEntity.getMediaList());
            }
            if (mediaList.isEmpty()) {
                //look for the last hearing date and use that
                courtCase.getHearings().sort(Comparator.comparing(HearingEntity::getHearingDate).reversed());
                HearingEntity lastHearingEntity = courtCase.getHearings().getFirst();
                closeCaseInDbAndAddRetention(courtCase, OffsetDateTime.of(lastHearingEntity.getHearingDate().atStartOfDay(), ZoneOffset.UTC));
            } else {
                mediaList.sort(Comparator.comparing(MediaEntity::getCreatedDateTime).reversed());
                closeCaseInDbAndAddRetention(courtCase, mediaList.getFirst().getCreatedDateTime());
            }
        }
        log.info("Successfully closed case with ID: {}", courtCase.getId());
    }

    private void closeCaseInDbAndAddRetention(CourtCaseEntity courtCase, OffsetDateTime caseClosedDate) {
        courtCase.setClosed(TRUE);
        courtCase.setCaseClosedTimestamp(caseClosedDate);
        courtCase.setRetConfReason(AGED_CASE);
        courtCase.setRetConfScore(CASE_NOT_PERFECTLY_CLOSED);
        caseRepository.save(courtCase);
        log.info("Closed court case id {}", courtCase.getId());

        LocalDate retentionDate = retentionDateHelper.getRetentionDateForPolicy(courtCase, RetentionPolicyEnum.DEFAULT);

        PostRetentionRequest postRetentionRequest = new PostRetentionRequest();
        postRetentionRequest.setComments(CLOSE_CASE_RETENTION_COMMENT);
        postRetentionRequest.setRetentionDate(retentionDate);

        CaseRetentionEntity retentionEntity = retentionApi.createRetention(postRetentionRequest, courtCase, retentionDate, userAccount,
                                                                           CaseRetentionStatus.PENDING);
        retentionEntity.setConfidenceCategory(RetentionConfidenceCategoryEnum.AGED_CASE);
        caseRetentionRepository.save(retentionEntity);
        log.info("Set retention date {} and confidence category {} for case id {}", retentionDate, RetentionConfidenceCategoryEnum.AGED_CASE,
                 courtCase.getId());
    }
}