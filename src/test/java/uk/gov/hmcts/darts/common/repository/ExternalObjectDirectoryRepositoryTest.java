package uk.gov.hmcts.darts.common.repository;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Limit;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExternalObjectDirectoryRepositoryTest {

    @Test
    void findEodsForTransfer_mediaItemsArelessThanLimit_shouldReturnBothMediaAndNonMedia() {
        ExternalObjectDirectoryRepository externalObjectDirectoryRepository = spy(ExternalObjectDirectoryRepository.class);
        ExternalObjectDirectoryEntity mediaEod1 = new ExternalObjectDirectoryEntity();
        ExternalObjectDirectoryEntity mediaEod2 = new ExternalObjectDirectoryEntity();
        ExternalObjectDirectoryEntity nonMediaEod1 = new ExternalObjectDirectoryEntity();
        doReturn(List.of(mediaEod1, mediaEod2))
            .when(externalObjectDirectoryRepository)
            .findEodsForTransferOnlyMedia(any(), any(), any(), any(), any(), any());
        doReturn(List.of(nonMediaEod1))
            .when(externalObjectDirectoryRepository)
            .findEodsForTransferExcludingMedia(any(), any(), any(), any(), any(), any());

        ObjectRecordStatusEntity status = mock(ObjectRecordStatusEntity.class);
        ExternalLocationTypeEntity type = mock(ExternalLocationTypeEntity.class);
        ObjectRecordStatusEntity notExistsStatus = mock(ObjectRecordStatusEntity.class);
        ExternalLocationTypeEntity notExistsType = mock(ExternalLocationTypeEntity.class);
        int maxTransferAttempts = 5;

        List<ExternalObjectDirectoryEntity> eods = externalObjectDirectoryRepository.findEodsForTransfer(
            status, type, notExistsStatus, notExistsType, maxTransferAttempts, Limit.of(5));

        assertThat(eods)
            .hasSize(3)
            .containsExactlyInAnyOrder(mediaEod1, mediaEod2, nonMediaEod1);

        verify(externalObjectDirectoryRepository)
            .findEodsForTransferOnlyMedia(status, type, notExistsStatus, notExistsType, maxTransferAttempts, Limit.of(5));
        verify(externalObjectDirectoryRepository)
            .findEodsForTransferExcludingMedia(status, type, notExistsStatus, notExistsType, maxTransferAttempts, Limit.of(3));
    }

    @Test
    void findEodsForTransfer_mediaItemsAreEqualToLimit_shouldReturnOnlyMedia() {
        ExternalObjectDirectoryRepository externalObjectDirectoryRepository = spy(ExternalObjectDirectoryRepository.class);
        ExternalObjectDirectoryEntity mediaEod1 = new ExternalObjectDirectoryEntity();
        ExternalObjectDirectoryEntity mediaEod2 = new ExternalObjectDirectoryEntity();
        doReturn(List.of(mediaEod1, mediaEod2))
            .when(externalObjectDirectoryRepository)
            .findEodsForTransferOnlyMedia(any(), any(), any(), any(), any(), any());

        ObjectRecordStatusEntity status = mock(ObjectRecordStatusEntity.class);
        ExternalLocationTypeEntity type = mock(ExternalLocationTypeEntity.class);
        ObjectRecordStatusEntity notExistsStatus = mock(ObjectRecordStatusEntity.class);
        ExternalLocationTypeEntity notExistsType = mock(ExternalLocationTypeEntity.class);
        int maxTransferAttempts = 5;

        List<ExternalObjectDirectoryEntity> eods = externalObjectDirectoryRepository.findEodsForTransfer(
            status, type, notExistsStatus, notExistsType, maxTransferAttempts, Limit.of(2));

        assertThat(eods)
            .hasSize(2)
            .containsExactlyInAnyOrder(mediaEod1, mediaEod2);

        verify(externalObjectDirectoryRepository)
            .findEodsForTransferOnlyMedia(status, type, notExistsStatus, notExistsType, maxTransferAttempts, Limit.of(2));
        verify(externalObjectDirectoryRepository, never())
            .findEodsForTransferExcludingMedia(any(), any(), any(), any(), any(), any());
    }


    @Test
    void findEodsNotInOtherStorage_mediaItemsArelessThanLimit_shouldReturnBothMediaAndNonMedia() {
        final int mediaEod1Id = 1;
        final int mediaEod2Id = 2;
        final int nonMediaEod1Id = 3;
        final int statusId = 4;
        final int typeId = 5;
        final int notExistsTypeId = 6;
        final int limitRecords = 5;
        final int numberOfMediaEods = 2;

        ExternalObjectDirectoryRepository externalObjectDirectoryRepository = spy(ExternalObjectDirectoryRepository.class);
        doReturn(List.of(mediaEod1Id, mediaEod2Id))
            .when(externalObjectDirectoryRepository)
            .findEodsNotInOtherStorageOnlyMedia(any(), any(), any(), any());
        doReturn(List.of(nonMediaEod1Id))
            .when(externalObjectDirectoryRepository)
            .findEodsNotInOtherStorageExcludingMedia(any(), any(), any(), any());


        ObjectRecordStatusEntity status = mock(ObjectRecordStatusEntity.class);
        when(status.getId()).thenReturn(statusId);
        ExternalLocationTypeEntity type = mock(ExternalLocationTypeEntity.class);
        when(type.getId()).thenReturn(typeId);
        ExternalLocationTypeEntity notExistsType = mock(ExternalLocationTypeEntity.class);
        when(notExistsType.getId()).thenReturn(notExistsTypeId);

        List<Integer> eods = externalObjectDirectoryRepository.findEodsNotInOtherStorage(
            status, type, notExistsType, limitRecords);

        assertThat(eods)
            .hasSize(3)
            .containsExactlyInAnyOrder(mediaEod1Id, mediaEod2Id, nonMediaEod1Id);

        verify(externalObjectDirectoryRepository)
            .findEodsNotInOtherStorageOnlyMedia(statusId, typeId, notExistsTypeId, limitRecords);
        verify(externalObjectDirectoryRepository)
            .findEodsNotInOtherStorageExcludingMedia(statusId, typeId, notExistsTypeId, limitRecords - numberOfMediaEods);
    }

    @Test
    void findEodsNotInOtherStorage_mediaItemsAreEqualToLimit_shouldReturnOnlyMedia() {
        final int mediaEod1Id = 1;
        final int mediaEod2Id = 2;
        final int statusId = 4;
        final int typeId = 5;
        final int notExistsTypeId = 6;
        final int limitRecords = 2;
        ExternalObjectDirectoryRepository externalObjectDirectoryRepository = spy(ExternalObjectDirectoryRepository.class);
        doReturn(List.of(mediaEod1Id, mediaEod2Id))
            .when(externalObjectDirectoryRepository)
            .findEodsNotInOtherStorageOnlyMedia(any(), any(), any(), any());
        ObjectRecordStatusEntity status = mock(ObjectRecordStatusEntity.class);
        when(status.getId()).thenReturn(statusId);
        ExternalLocationTypeEntity type = mock(ExternalLocationTypeEntity.class);
        when(type.getId()).thenReturn(typeId);
        ExternalLocationTypeEntity notExistsType = mock(ExternalLocationTypeEntity.class);
        when(notExistsType.getId()).thenReturn(notExistsTypeId);

        List<Integer> eods = externalObjectDirectoryRepository.findEodsNotInOtherStorage(
            status, type, notExistsType, limitRecords);

        assertThat(eods)
            .hasSize(2)
            .containsExactlyInAnyOrder(mediaEod1Id, mediaEod2Id);

        verify(externalObjectDirectoryRepository)
            .findEodsNotInOtherStorageOnlyMedia(statusId, typeId, notExistsTypeId, limitRecords);
        verify(externalObjectDirectoryRepository, never())
            .findEodsNotInOtherStorageExcludingMedia(any(), any(), any(), any());
    }
}
