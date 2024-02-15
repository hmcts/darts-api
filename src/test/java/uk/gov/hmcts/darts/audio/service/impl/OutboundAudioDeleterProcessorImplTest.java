package uk.gov.hmcts.darts.audio.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audio.service.OutboundAudioDeleterProcessorSingleElement;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.helper.SystemUserHelper;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.repository.TransformedMediaRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboundAudioDeleterProcessorImplTest {
    @Mock
    LastAccessedDeletionDayCalculator lastAccessedDeletionDayCalculator;
    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private UserAccountEntity userAccountEntity;
    @Mock
    private ObjectRecordStatusRepository objectRecordStatusRepository;
    @Mock
    private OutboundAudioDeleterProcessorSingleElement singleElementProcessor;
    private OutboundAudioDeleterProcessorImpl outboundAudioDeleterProcessorImpl;

    @Mock
    private SystemUserHelper systemUserHelper;

    @Mock
    private TransformedMediaRepository transformedMediaRepository;

    @BeforeEach
    void setUp() {
        this.outboundAudioDeleterProcessorImpl = new OutboundAudioDeleterProcessorImpl(
            userAccountRepository, lastAccessedDeletionDayCalculator,
            systemUserHelper, transformedMediaRepository,
            singleElementProcessor
        );
        when(systemUserHelper.findSystemUserGuid(anyString())).thenReturn("value");
        when(userAccountRepository.findSystemUser(any())).thenReturn(userAccountEntity);
    }

    @Test
    void testDeleteWhenSystemUserDoesNotExist() {
        when(userAccountRepository.findSystemUser(any())).thenReturn(null);

        assertThrows(DartsApiException.class, () ->
            outboundAudioDeleterProcessorImpl.markForDeletion());
    }

    @Test
    void testContinuesProcessingNextIterationOnException() {
        // given
        List<TransformedMediaEntity> transformedMediaEntities = List.of(new TransformedMediaEntity(), new TransformedMediaEntity());
        when(transformedMediaRepository.findAllDeletableTransformedMedia(any())).thenReturn(transformedMediaEntities);

        var deletedValues = List.of(new TransientObjectDirectoryEntity());
        when(singleElementProcessor.markForDeletion(any(), any()))
                .thenThrow(new RuntimeException("Some error"))
                .thenReturn(deletedValues);

        // when
        List<TransientObjectDirectoryEntity> result = outboundAudioDeleterProcessorImpl.markForDeletion();

        // then
        assertThat(result).isEqualTo(deletedValues);
    }
}

