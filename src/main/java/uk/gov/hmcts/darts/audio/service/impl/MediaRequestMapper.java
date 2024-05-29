package uk.gov.hmcts.darts.audio.service.impl;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audiorequests.model.MediaRequest;
import uk.gov.hmcts.darts.audiorequests.model.MediaRequestCourtroom;
import uk.gov.hmcts.darts.audiorequests.model.MediaRequestHearing;

@Component
class MediaRequestMapper {

    public MediaRequest mediaRequestFrom(MediaRequestEntity mediaRequestEntity) {
        var hearing = mediaRequestEntity.getHearing();
        var mediaRequestHearing = new MediaRequestHearing()
            .id(hearing.getId())
            .hearingDate(hearing.getHearingDate());

        var courtroom = hearing.getCourtroom();
        var mediaRequestCourtroom = new MediaRequestCourtroom()
            .id(courtroom.getId())
            .name(courtroom.getName());

        return new MediaRequest()
            .id(mediaRequestEntity.getId())
            .startAt(mediaRequestEntity.getStartTime())
            .endAt(mediaRequestEntity.getEndTime())
            .requestedAt(mediaRequestEntity.getCreatedDateTime())
            .hearing(mediaRequestHearing)
            .courtroom(mediaRequestCourtroom)
            .requestedById(mediaRequestEntity.getRequestor().getId())
            .ownerId(mediaRequestEntity.getCurrentOwner().getId());
    }
}
