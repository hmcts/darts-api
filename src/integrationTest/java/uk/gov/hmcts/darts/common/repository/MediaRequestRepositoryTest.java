package uk.gov.hmcts.darts.common.repository;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.enums.MediaRequestStatus;
import uk.gov.hmcts.darts.audio.service.impl.MediaRequestServiceImpl;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestDetails;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.OPEN;
import static uk.gov.hmcts.darts.audiorequests.model.AudioRequestType.DOWNLOAD;

class MediaRequestRepositoryTest extends PostgresIntegrationBase {

    private AudioRequestDetails requestDetails;

    private static final String T_09_00_00_Z = "2023-05-31T09:00:00Z";
    private static final String T_12_00_00_Z = "2023-05-31T12:00:00Z";

    @Autowired
    private MediaRequestServiceImpl mediaRequestService;

    @Autowired
    private MediaRequestRepository mediaRequestRepository;

    private HearingEntity hearing;

    @BeforeEach
    void before() {
        hearing = dartsDatabase.hasSomeHearing();

        requestDetails = new AudioRequestDetails(null, null, null, null);
        requestDetails.setHearingId(hearing.getId());
        requestDetails.setRequestor(0);
    }

    @Test
    void updateAndRetrieveMediaRequestToProcessing() {
        requestDetails.setStartTime(OffsetDateTime.parse(T_09_00_00_Z));
        requestDetails.setEndTime(OffsetDateTime.parse(T_12_00_00_Z));

        MediaRequestEntity mediaRequestEntity = new MediaRequestEntity();
        UserAccountEntity requestor = dartsDatabase.getUserAccountRepository().getReferenceById(requestDetails.getRequestor());
        mediaRequestEntity.setHearing(hearing);
        mediaRequestEntity.setRequestor(requestor);
        mediaRequestEntity.setCurrentOwner(requestor);
        mediaRequestEntity.setStartTime(requestDetails.getStartTime());
        mediaRequestEntity.setEndTime(requestDetails.getEndTime());
        mediaRequestEntity.setRequestType(DOWNLOAD);
        mediaRequestEntity.setStatus(OPEN);
        mediaRequestEntity.setAttempts(0);
        mediaRequestEntity.setCreatedBy(requestor);
        mediaRequestEntity.setLastModifiedBy(requestor);


        MediaRequestEntity request = mediaRequestRepository.saveAndFlush(mediaRequestEntity);
        OffsetDateTime createdTime = request.getLastModifiedDateTime();

        mediaRequestService.getMediaRequestEntityById(request.getId());
        MediaRequestEntity mediaRequestEntityAfterUpdate = mediaRequestRepository.updateAndRetrieveMediaRequestToProcessing(request.getLastModifiedById(),
                                                                                                                 List.of(0));

        // prove an update happened on the date and time
        Assertions.assertNotEquals(createdTime.atZoneSameInstant(ZoneOffset.UTC),
                                   mediaRequestEntityAfterUpdate.getLastModifiedDateTime().atZoneSameInstant(ZoneOffset.UTC));
        Assertions.assertEquals(MediaRequestStatus.PROCESSING, mediaRequestEntityAfterUpdate.getStatus());
    }
}