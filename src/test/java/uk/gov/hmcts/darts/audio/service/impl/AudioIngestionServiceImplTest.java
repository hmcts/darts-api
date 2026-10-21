
package uk.gov.hmcts.darts.audio.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audio.config.AudioIngestionConfigurationProperties;
import uk.gov.hmcts.darts.audio.entity.AudioIngestionEntity;
import uk.gov.hmcts.darts.common.repository.AudioIngestionRepository;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AudioIngestionServiceImplTest {

    @Mock
    private AudioIngestionRepository audioIngestionRepository;

    private AudioIngestionServiceImpl audioIngestionService;

    @BeforeEach
    void setUp() {
        AudioIngestionConfigurationProperties configurationProperties =
            new AudioIngestionConfigurationProperties();

        configurationProperties.setBatchSize(5);
        configurationProperties.setProcessingTimeout(Duration.ofMinutes(10));

        audioIngestionService = new AudioIngestionServiceImpl(
            audioIngestionRepository,
            configurationProperties
        );
    }

    @Test
    void shouldReturnWhenNoItemsAreAvailable() {
        when(audioIngestionRepository.claimForProcessing(
            anyString(),
            any(OffsetDateTime.class),
            eq(5)
        )).thenReturn(List.of());

        audioIngestionService.process();

        verify(audioIngestionRepository).claimForProcessing(
            anyString(),
            any(OffsetDateTime.class),
            eq(5)
        );

        verify(audioIngestionRepository, never()).findAllById(any());
        verify(audioIngestionRepository, never())
            .deleteProcessed(anyLong(), anyString());
    }

    @Test
    void shouldRetrieveClaimedItemsWhenItemsAreAvailable() {
        List<Long> claimedIds = List.of(1L);

        when(audioIngestionRepository.claimForProcessing(
            anyString(),
            any(OffsetDateTime.class),
            eq(5)
        )).thenReturn(claimedIds);

        when(audioIngestionRepository.findAllById(claimedIds))
            .thenReturn(List.of(createItem(1)));

        audioIngestionService.process();

        verify(audioIngestionRepository).claimForProcessing(
            anyString(),
            any(OffsetDateTime.class),
            eq(5)
        );

        verify(audioIngestionRepository).findAllById(claimedIds);
    }

    @Test
    void shouldDeleteSuccessfullyProcessedItems() {
        List<Long> claimedIds =
            List.of(1L, 2L, 3L, 4L, 5L);

        when(audioIngestionRepository.claimForProcessing(
            anyString(),
            any(OffsetDateTime.class),
            eq(5)
        )).thenReturn(claimedIds);

        when(audioIngestionRepository.findAllById(claimedIds))
            .thenReturn(List.of(
                createItem(1),
                createItem(2),
                createItem(3),
                createItem(4),
                createItem(5)
            ));

        audioIngestionService.process();

        verify(audioIngestionRepository)
            .deleteProcessed(eq(1L), anyString());

        verify(audioIngestionRepository)
            .deleteProcessed(eq(2L), anyString());

        verify(audioIngestionRepository)
            .deleteProcessed(eq(3L), anyString());

        verify(audioIngestionRepository)
            .deleteProcessed(eq(4L), anyString());

        verify(audioIngestionRepository)
            .deleteProcessed(eq(5L), anyString());
    }

    @Test
    void shouldLeaveAllItemsProcessingWhenWholeJobTemporarilyFails() {
        List<Long> claimedIds =
            List.of(1L, 2L, 3L, 4L, 5L);

        when(audioIngestionRepository.claimForProcessing(
            anyString(),
            any(OffsetDateTime.class),
            eq(5)
        )).thenReturn(claimedIds);

        when(audioIngestionRepository.findAllById(claimedIds))
            .thenReturn(List.of(
                createItem(1, "FAIL-1.wav"),
                createItem(2, "FAIL-2.wav"),
                createItem(3, "FAIL-3.wav"),
                createItem(4, "FAIL-4.wav"),
                createItem(5, "FAIL-5.wav")
            ));

        audioIngestionService.process();

        verify(audioIngestionRepository, never())
            .deleteProcessed(anyLong(), anyString());
    }

    @Test
    void shouldLeaveFirstTwoItemsProcessingAndDeleteRemainingItems() {
        List<Long> claimedIds =
            List.of(1L, 2L, 3L, 4L, 5L);

        when(audioIngestionRepository.claimForProcessing(
            anyString(),
            any(OffsetDateTime.class),
            eq(5)
        )).thenReturn(claimedIds);

        when(audioIngestionRepository.findAllById(claimedIds))
            .thenReturn(List.of(
                createItem(1, "FAIL-1.wav"),
                createItem(2, "FAIL-2.wav"),
                createItem(3, "audio-3.wav"),
                createItem(4, "audio-4.wav"),
                createItem(5, "audio-5.wav")
            ));

        audioIngestionService.process();

        verify(audioIngestionRepository, never())
            .deleteProcessed(eq(1L), anyString());

        verify(audioIngestionRepository, never())
            .deleteProcessed(eq(2L), anyString());

        verify(audioIngestionRepository)
            .deleteProcessed(eq(3L), anyString());

        verify(audioIngestionRepository)
            .deleteProcessed(eq(4L), anyString());

        verify(audioIngestionRepository)
            .deleteProcessed(eq(5L), anyString());
    }

    private AudioIngestionEntity createItem(int id) {
        return createItem(id, "audio-" + id + ".wav");
    }

    private AudioIngestionEntity createItem(int id, String filename) {
        AudioIngestionEntity item = new AudioIngestionEntity();

        item.setId(id);
        item.setSourceFullFilename("/spike/" + filename);

        return item;
    }
}
