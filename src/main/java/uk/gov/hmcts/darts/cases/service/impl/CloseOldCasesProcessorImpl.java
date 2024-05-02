package uk.gov.hmcts.darts.cases.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
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
    public void closeCases() {
        List<CourtCaseEntity> courtCaseEntityList = caseRepository.findOpenCasesToClose(OffsetDateTime.now().minusYears(years));

        userAccount = authorisationApi.getCurrentUser();
        courtCaseEntityList.forEach(this::closeCase);
    }

    private void closeCase(CourtCaseEntity courtCase) {
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
                closeCaseInDbAndAddRetention(courtCase, eventList.get(0).getCreatedDateTime());
            }
        } else if (!courtCase.getHearings().isEmpty()) {
            //look for the last audio and use its recorded date
            List<MediaEntity> mediaList = new ArrayList<>();
            for (HearingEntity hearingEntity : courtCase.getHearings()) {
                mediaList.addAll(hearingEntity.getMediaList());
            }
            if (!mediaList.isEmpty()) {
                mediaList.sort(Comparator.comparing(MediaEntity::getCreatedDateTime).reversed());
                closeCaseInDbAndAddRetention(courtCase, mediaList.get(0).getCreatedDateTime());
            } else {
                //look for the last hearing date and use that
                courtCase.getHearings().sort(Comparator.comparing(HearingEntity::getHearingDate).reversed());
                HearingEntity lastHearingEntity = courtCase.getHearings().get(0);
                closeCaseInDbAndAddRetention(courtCase, OffsetDateTime.of(lastHearingEntity.getHearingDate().atStartOfDay(), ZoneOffset.UTC));
            }
        } else {
            //set to created date
            closeCaseInDbAndAddRetention(courtCase, courtCase.getCreatedDateTime());
        }


    }

    private void closeCaseInDbAndAddRetention(CourtCaseEntity courtCase, OffsetDateTime caseClosedDate) {
        courtCase.setClosed(TRUE);
        courtCase.setCaseClosedTimestamp(caseClosedDate);
        caseRepository.save(courtCase);

        LocalDate retentionDate = retentionDateHelper.getRetentionDateForPolicy(courtCase, RetentionPolicyEnum.DEFAULT);

        PostRetentionRequest postRetentionRequest = new PostRetentionRequest();
        postRetentionRequest.setComments(CLOSE_CASE_RETENTION_COMMENT);
        postRetentionRequest.setRetentionDate(retentionDate);

        CaseRetentionEntity retentionEntity = retentionApi.createRetention(postRetentionRequest, courtCase, retentionDate, userAccount,
                                                                           CaseRetentionStatus.PENDING);
        caseRetentionRepository.save(retentionEntity);
    }
}
