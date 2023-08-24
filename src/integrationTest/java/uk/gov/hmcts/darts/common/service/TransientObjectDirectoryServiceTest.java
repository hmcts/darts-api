package uk.gov.hmcts.darts.common.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.repository.MediaRequestRepository;
import uk.gov.hmcts.darts.audio.service.MediaRequestService;
import uk.gov.hmcts.darts.audio.util.AudioTestDataUtil;
import uk.gov.hmcts.darts.cases.repository.CaseRepository;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.repository.CourtroomRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.courthouse.CourthouseRepository;
import uk.gov.hmcts.darts.testutils.data.CourtroomTestData;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.darts.common.enums.ObjectDirectoryStatusEnum.STORED;
import static uk.gov.hmcts.darts.testutils.data.CaseTestData.createCaseWithCaseNumber;
import static uk.gov.hmcts.darts.testutils.data.CourthouseTestData.createCourthouse;
import static uk.gov.hmcts.darts.testutils.data.HearingTestData.createHearingWith;

@SpringBootTest
@ActiveProfiles({"intTest", "h2db"})
class TransientObjectDirectoryServiceTest {

    @Autowired
    private CaseRepository caseRepository;
    @Autowired
    private CourthouseRepository courthouseRepository;
    @Autowired
    private CourtroomRepository courtroomRepository;
    @Autowired
    private HearingRepository hearingRepository;
    @Autowired
    private MediaRequestRepository mediaRequestRepository;

    @Autowired
    private MediaRequestService mediaRequestService;
    @Autowired
    private TransientObjectDirectoryService transientObjectDirectoryService;

    private MediaRequestEntity mediaRequestEntity1;

    @Test
    @Transactional
    void shouldSaveTransientDataLocation() {
        createAndLoadMediaRequestEntity();

        MediaRequestEntity mediaRequestEntity = mediaRequestService.getMediaRequestById(mediaRequestEntity1.getId());
        UUID externalLocation = UUID.fromString("f744a74f-83c0-47e4-8bb2-2fd4d2b68647");

        TransientObjectDirectoryEntity transientObjectDirectoryEntity = transientObjectDirectoryService.saveTransientDataLocation(
            mediaRequestEntity,
            externalLocation
        );

        assertNotNull(transientObjectDirectoryEntity);
        assertTrue(transientObjectDirectoryEntity.getId() > 0);
        assertEquals(mediaRequestEntity1.getId(), transientObjectDirectoryEntity.getMediaRequest().getId());
        assertEquals(STORED.getId(), transientObjectDirectoryEntity.getStatus().getId());
        assertEquals(externalLocation, transientObjectDirectoryEntity.getExternalLocation());
        assertNull(transientObjectDirectoryEntity.getChecksum());
        assertTrue(transientObjectDirectoryEntity.getCreatedDateTime()
                       .isAfter(OffsetDateTime.parse("2023-07-06T16:00:00.000Z")));
        assertTrue(transientObjectDirectoryEntity.getLastModifiedDateTime()
                       .isAfter(OffsetDateTime.parse("2023-07-06T16:05:00.000Z")));
        assertNotNull(transientObjectDirectoryEntity.getLastModifiedBy());
    }

    private void createAndLoadMediaRequestEntity() {

        var caseEntity = createCaseWithCaseNumber(UUID.randomUUID().toString());
        caseRepository.saveAndFlush(caseEntity);

        var courthouseEntity = createCourthouse("C1");
        courthouseEntity = courthouseRepository.saveAndFlush(courthouseEntity);

        var courtroomEntity = CourtroomTestData.createCourtRoomWithNameAtCourthouse(courthouseEntity, "R1");
        courtroomRepository.saveAndFlush(courtroomEntity);

        var hearingEntityWithMediaRequest1 = createHearingWith(caseEntity, courtroomEntity);
        hearingEntityWithMediaRequest1 = hearingRepository.saveAndFlush(hearingEntityWithMediaRequest1);

        mediaRequestEntity1 = AudioTestDataUtil.createMediaRequest(
            hearingEntityWithMediaRequest1,
            -2,
            OffsetDateTime.parse("2023-06-26T13:00:00Z"),
            OffsetDateTime.parse("2023-06-26T13:45:00Z")
        );

        mediaRequestEntity1 = mediaRequestRepository.saveAndFlush(mediaRequestEntity1);
        assertNotNull(mediaRequestEntity1);
    }

}
