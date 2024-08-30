package uk.gov.hmcts.darts.event.validation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.component.validation.Validator;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.event.exception.EventError;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class EventIdValidator implements Validator<Integer> {

    private final EventRepository eventRepository;

    @Override
    public void validate(Integer eventId) {
        Optional<EventEntity> eventEntity = eventRepository.findById(eventId);

        if (!eventEntity.isPresent()) {
            throw new DartsApiException(EventError.EVENT_ID_NOT_FOUND_RESULTS);
        }
    }
}