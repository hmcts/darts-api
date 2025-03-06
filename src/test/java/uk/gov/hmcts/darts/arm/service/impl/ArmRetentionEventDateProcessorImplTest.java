package uk.gov.hmcts.darts.arm.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;
import uk.gov.hmcts.darts.arm.component.ArmRetentionEventDateCalculator;
import uk.gov.hmcts.darts.arm.service.ArmRetentionEventDateProcessor;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.testutils.ExternalObjectDirectoryTestData;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.ARM;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;
import static uk.gov.hmcts.darts.common.util.EodHelper.armLocation;
import static uk.gov.hmcts.darts.testutils.ExternalObjectDirectoryTestData.TEST_EXTERNAL_OBJECT_DIRECTORY_ID;

@ExtendWith(MockitoExtension.class)
class ArmRetentionEventDateProcessorImplTest {

    private static final OffsetDateTime MEDIA_RETENTION_DATE_TIME =
        OffsetDateTime.of(2023, 6, 20, 10, 0, 0, 0, ZoneOffset.UTC);

    @Mock
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    @Mock
    private ArmRetentionEventDateCalculator armRetentionEventDateCalculator;

    @Mock
    private ExternalObjectDirectoryEntity externalObjectDirectoryEntity;

    private ArmRetentionEventDateProcessor armRetentionEventDateProcessor;


    @BeforeEach
    void setupData() {

        MediaEntity media = new MediaEntity();
        media.setId(123);
        media.setRetainUntilTs(MEDIA_RETENTION_DATE_TIME);

        armRetentionEventDateProcessor = new ArmRetentionEventDateProcessorImpl(
            externalObjectDirectoryRepository, armRetentionEventDateCalculator);

        externalObjectDirectoryEntity = new ExternalObjectDirectoryTestData().createExternalObjectDirectory(
            media,
            ARM,
            STORED,
            UUID.randomUUID().toString());
        externalObjectDirectoryEntity.setUpdateRetention(true);

    }

    @Test
    void calculateEventDates() {
        // given
        List<Integer> eods = List.of(TEST_EXTERNAL_OBJECT_DIRECTORY_ID);
        when(externalObjectDirectoryRepository.findByExternalLocationTypeAndUpdateRetention(armLocation(), true, Limit.of(10_000))).thenReturn(eods);

        externalObjectDirectoryEntity.setEventDateTs(MEDIA_RETENTION_DATE_TIME);

        // when
        armRetentionEventDateProcessor.calculateEventDates(10_000);

        // then
        verify(externalObjectDirectoryRepository).findByExternalLocationTypeAndUpdateRetention(armLocation(), true, Limit.of(10_000));
        verify(armRetentionEventDateCalculator).calculateRetentionEventDate(TEST_EXTERNAL_OBJECT_DIRECTORY_ID);

        verifyNoMoreInteractions(
            externalObjectDirectoryRepository,
            armRetentionEventDateCalculator
        );
    }

    @Test
    void calculateEventDates_NoRowsToProcess() {
        // given
        List<Integer> eods = new ArrayList<>();
        when(externalObjectDirectoryRepository.findByExternalLocationTypeAndUpdateRetention(armLocation(), true, Limit.of(10_000))).thenReturn(eods);

        // when
        armRetentionEventDateProcessor.calculateEventDates(10_000);

        // then
        verify(externalObjectDirectoryRepository).findByExternalLocationTypeAndUpdateRetention(armLocation(), true, Limit.of(10_000));

        verifyNoMoreInteractions(
            externalObjectDirectoryRepository,
            armRetentionEventDateCalculator
        );
    }
}