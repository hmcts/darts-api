package uk.gov.hmcts.darts.cases.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${darts.retention.close-open-cases-older-than-years}")
    long years;

    @Value("#{'${darts.retention.close-events}'.split(',')}")
    private List<String> closeEvents;

    @Transactional
    @Override
    public void closeCases() {
        List<CourtCaseEntity> courtCaseEntityList = caseRepository.findOpenCasesToClose(OffsetDateTime.now().minusYears(years));

        courtCaseEntityList.forEach(this::closeCase);
    }

    private void closeCase(CourtCaseEntity courtCase) {
        List<EventEntity> eventList = new ArrayList<>();
        for (HearingEntity hearingEntity: courtCase.getHearings()) {
            eventList.addAll(hearingEntity.getEventList());
        }
        if (!eventList.isEmpty()) {
            eventList.sort(Comparator.comparing(EventEntity::getCreatedDateTime).reversed());
            //find latest closed event
            Optional<EventEntity> closedEvent =
                eventList.stream().filter(eventEntity -> closeEvents.contains(eventEntity.getEventType().getEventName())).findFirst();

            if (closedEvent.isPresent()) {
                closeCaseInDb(courtCase, closedEvent.get().getCreatedDateTime());
            } else {
                //look for the last event and use that date
                closeCaseInDb(courtCase, eventList.get(0).getCreatedDateTime());
            }
        } else if (!courtCase.getHearings().isEmpty()) {
            //look for the last audio and use its recorded date
            List<MediaEntity> mediaList = new ArrayList<>();
            for (HearingEntity hearingEntity: courtCase.getHearings()) {
                mediaList.addAll(hearingEntity.getMediaList());
            }
            if (!mediaList.isEmpty()) {
                mediaList.sort(Comparator.comparing(MediaEntity::getCreatedDateTime).reversed());
                closeCaseInDb(courtCase, mediaList.get(0).getCreatedDateTime());
            } else {
                //look for the last hearing date and use that
                courtCase.getHearings().sort(Comparator.comparing(HearingEntity::getHearingDate).reversed());
                HearingEntity lastHearingEntity = courtCase.getHearings().get(0);
                closeCaseInDb(courtCase, OffsetDateTime.of(lastHearingEntity.getHearingDate().atStartOfDay(), ZoneOffset.UTC));
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
    }
}
