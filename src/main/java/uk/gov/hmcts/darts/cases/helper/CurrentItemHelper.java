package uk.gov.hmcts.darts.cases.helper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CurrentItemHelper {

    private final EventRepository eventRepository;
    private final MediaRepository mediaRepository;

    public List<EventEntity> getCurrentEvents(CourtCaseEntity courtCase) {
        List<EventEntity> eventList = new ArrayList<>();
        for (HearingEntity hearingEntity : courtCase.getHearings()) {
            eventList.addAll(eventRepository.findCurrentEventsByHearingId(hearingEntity.getId()));
        }
        return eventList;
    }

    public List<MediaEntity> getCurrentMedia(CourtCaseEntity courtCase) {
        List<MediaEntity> mediaList = new ArrayList<>();
        for (HearingEntity hearingEntity : courtCase.getHearings()) {
            mediaList.addAll(mediaRepository.findAllCurrentMediaByHearingId(hearingEntity.getId(), false));
        }
        return mediaList;
    }



}