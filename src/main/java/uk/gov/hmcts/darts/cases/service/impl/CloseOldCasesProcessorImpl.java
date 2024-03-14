package uk.gov.hmcts.darts.cases.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.cases.service.CloseOldCasesProcessor;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;

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
    private final CaseRepository caseRepository;

    private final EventRepository eventRepository;

    private final MediaRepository mediaRepository;


    @Transactional
    @Override
    public void closeCases() {
        //need a query to get everything over 6 years old not set to closed and no retention
        //what does no retention mean, entry in which table?
        List<CourtCaseEntity> courtCaseEntityList = caseRepository.findOpenCaseNumbersToClose(OffsetDateTime.now().minusYears(6));
        //need a method to get the case closed date, based on criteria
        courtCaseEntityList.forEach(this::closeCase);
    }

    private void closeCase(CourtCaseEntity courtCase) {
        List<EventEntity> events = eventRepository.findAllByCaseNumberOrderByCreatedDate(courtCase.getCaseNumber());
        if (events != null && events.size() > 1) {
            //find latest closed event, but what are the closed events?
            Optional<EventEntity> closedEvent =
                events.stream().filter(eventEntity -> eventEntity.getEventType().getEventName().equals("Case closed")).findFirst();
            closedEvent.ifPresent(eventEntity -> closeCaseInDb(courtCase, eventEntity.getCreatedDateTime()));

            //look for the last event and use that date
            closeCaseInDb(courtCase, events.get(0).getCreatedDateTime());
        } else if (courtCase.getHearings() != null && !courtCase.getHearings().isEmpty()) {
            //look for the last audio and use its recorded date
            List<MediaEntity> mediaList = new ArrayList<>();
            for (HearingEntity hearingEntity: courtCase.getHearings()) {
                //check for null
                mediaList.addAll(hearingEntity.getMediaList());
            }
            if (!mediaList.isEmpty()) {
                mediaList.sort(Comparator.comparing(MediaEntity::getCreatedDateTime).reversed());
                closeCaseInDb(courtCase, mediaList.get(0).getCreatedDateTime());
            } else {
                //look for the last hearing date and use that
                if (courtCase.getHearings() != null && !courtCase.getHearings().isEmpty()) {
                    courtCase.getHearings().sort(Comparator.comparing(HearingEntity::getHearingDate).reversed());
                    HearingEntity lastHearingEntity = courtCase.getHearings().get(0);
                    closeCaseInDb(courtCase, OffsetDateTime.of(lastHearingEntity.getHearingDate().atStartOfDay(), ZoneOffset.UTC));
                }
            }
        } else {
            //set to created date
            closeCaseInDb(courtCase, courtCase.getCreatedDateTime());
        }


    }

    private void closeCaseInDb(CourtCaseEntity courtCase, OffsetDateTime caseClosedDate) {
        courtCase.setClosed(TRUE);
        courtCase.setCaseClosedTimestamp(caseClosedDate);
        caseRepository.save(courtCase);
        // ?? system user ?? courtCase.setLastModifiedBy(authorisationApi.getCurrentUser());
    }
}
