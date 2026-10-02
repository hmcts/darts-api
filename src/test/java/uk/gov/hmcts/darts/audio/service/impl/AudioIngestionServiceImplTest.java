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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AudioIngestionServiceImplTest {

    @Mock
    private AudioIngestionRepository audioIngestionRepository;

    @Mock
    private AudioIngestionEntity audioIngestionEntity;

    private AudioIngestionServiceImpl audioIngestionService;

    @BeforeEach
    void setUp() {
        AudioIngestionConfigurationProperties configurationProperties =
            new AudioIngestionConfigurationProperties();

        configurationProperties.setBatchSize(5);
        configurationProperties.setProcessingTimeout(Duration.ofMinutes(15));

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
            .thenReturn(List.of(audioIngestionEntity));

        when(audioIngestionEntity.getId()).thenReturn(1);
        when(audioIngestionEntity.getSourceFullFilename())
            .thenReturn("/spike/audio-1.wav");

        audioIngestionService.process();

        verify(audioIngestionRepository).claimForProcessing(
            anyString(),
            any(OffsetDateTime.class),
            eq(5)
        );

        verify(audioIngestionRepository).findAllById(claimedIds);
    }
}