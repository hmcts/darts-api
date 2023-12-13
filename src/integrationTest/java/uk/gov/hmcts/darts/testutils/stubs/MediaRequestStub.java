package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.enums.MediaRequestStatus;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestType;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.MediaRequestRepository;
import uk.gov.hmcts.darts.testutils.data.AudioTestData;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class MediaRequestStub {

    private final HearingStub hearingStub;
    private final MediaRequestRepository mediaRequestRepository;


    @Transactional
    public MediaRequestEntity createAndLoadMediaRequestEntity(UserAccountEntity requestor, AudioRequestType audioRequestType, MediaRequestStatus status) {

        HearingEntity hearing = hearingStub.createHearing("NEWCASTLE", "Int Test Courtroom 2", "2", LocalDate.of(2023, 6, 10));

        return mediaRequestRepository.save(
            AudioTestData.createCurrentMediaRequest(
                hearing,
                requestor,
                OffsetDateTime.parse("2023-06-26T13:00:00Z"),
                OffsetDateTime.parse("2023-06-26T13:45:00Z"),
                OffsetDateTime.parse("2023-06-30T13:00:00Z"),
                audioRequestType, status
            ));
    }
}
