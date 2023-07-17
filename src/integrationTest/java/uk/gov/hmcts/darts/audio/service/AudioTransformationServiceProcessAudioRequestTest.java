package uk.gov.hmcts.darts.audio.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.service.impl.AudioTransformationServiceImpl;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.HearingMediaEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.SystemCommandExecutorStubImpl;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.hmcts.darts.audio.enums.AudioRequestStatus.COMPLETED;
import static uk.gov.hmcts.darts.audio.enums.AudioRequestStatus.FAILED;
import static uk.gov.hmcts.darts.common.entity.ObjectDirectoryStatusEnum.STORED;

@SpringBootTest
@ActiveProfiles({"intTest", "h2db"})
@Import(SystemCommandExecutorStubImpl.class)
@AutoConfigureWireMock
@ExtendWith(MockitoExtension.class)
class AudioTransformationServiceProcessAudioRequestTest extends IntegrationBase {

    private static final int SOME_REQUESTOR = 666;
    public static final OffsetDateTime TIME_12_00 = OffsetDateTime.parse("2023-01-01T12:00Z");
    public static final OffsetDateTime TIME_12_10 = OffsetDateTime.parse("2023-01-01T12:10Z");
    public static final OffsetDateTime TIME_13_00 = OffsetDateTime.parse("2023-01-01T13:00Z");

    @Autowired
    private AudioTransformationServiceImpl audioTransformationService;

    private MediaRequestEntity mediaRequestEntity;
    private HearingEntity hearingEntity;

    @BeforeEach
    void setUp() {
        hearingEntity = dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            "1",
            "some-courthouse",
            "some-courtroom",
            LocalDate.now()
        );

        var mediaRequestEntity = new MediaRequestEntity();
        mediaRequestEntity.setHearing(hearingEntity);
        mediaRequestEntity.setRequestor(SOME_REQUESTOR);
        mediaRequestEntity.setStartTime(TIME_12_00);
        mediaRequestEntity.setEndTime(TIME_13_00);
        this.mediaRequestEntity = dartsDatabase.getMediaRequestRepository()
            .saveAndFlush(mediaRequestEntity);
    }

    @Test
    void processAudioRequestShouldSucceedAndUpdateRequestStatusToCompleted() {
        provisionDatabaseForHappyPath();

        UUID blobId = audioTransformationService.processAudioRequest(mediaRequestEntity.getId());

        assertNotNull(blobId);

        mediaRequestEntity = dartsDatabase.getMediaRequestRepository()
            .findById(mediaRequestEntity.getId())
            .orElseThrow();
        assertEquals(COMPLETED, mediaRequestEntity.getStatus());
    }

    @Test
    void processAudioRequestShouldFailAndUpdateRequestStatusToFailed() {
        var exception = assertThrows(
            DartsApiException.class,
            () -> audioTransformationService.processAudioRequest(mediaRequestEntity.getId())
        );

        assertEquals("Failed to process audio request", exception.getMessage());

        mediaRequestEntity = dartsDatabase.getMediaRequestRepository()
            .findById(mediaRequestEntity.getId())
            .orElseThrow();
        assertEquals(FAILED, mediaRequestEntity.getStatus());
    }

    private void provisionDatabaseForHappyPath() {
        var mediaEntity = dartsDatabase.createMediaEntity(
            TIME_12_00,
            TIME_12_10,
            1
        );

        var hearingMediaEntity = new HearingMediaEntity();
        hearingMediaEntity.setMedia(mediaEntity);
        hearingMediaEntity.setHearing(hearingEntity);

        dartsDatabase.getHearingMediaRepository()
            .saveAndFlush(hearingMediaEntity);

        var externalLocationTypeEntity = dartsDatabase.getExternalLocationTypeEntity(
            ExternalLocationTypeEnum.UNSTRUCTURED);
        var objectDirectoryStatusEntity = dartsDatabase.getObjectDirectoryStatusEntity(STORED);

        var externalObjectDirectoryEntity = CommonTestDataUtil.createExternalObjectDirectory(
            mediaEntity,
            objectDirectoryStatusEntity,
            externalLocationTypeEntity,
            UUID.randomUUID()
        );
        dartsDatabase.getExternalObjectDirectoryRepository()
            .saveAndFlush(externalObjectDirectoryEntity);
    }

}
