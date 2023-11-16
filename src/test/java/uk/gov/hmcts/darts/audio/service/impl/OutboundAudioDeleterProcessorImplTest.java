package uk.gov.hmcts.darts.audio.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.helper.SystemUserHelper;
import uk.gov.hmcts.darts.common.repository.MediaRequestRepository;
import uk.gov.hmcts.darts.common.repository.ObjectDirectoryStatusRepository;
import uk.gov.hmcts.darts.common.repository.TransientObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboundAudioDeleterProcessorImplTest {
    @Mock
    LastAccessedDeletionDayCalculator lastAccessedDeletionDayCalculator;
    @Mock
    private MediaRequestRepository mediaRequestRepository;
    @Mock
    private TransientObjectDirectoryRepository transientObjectDirectoryRepository;

    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private ObjectDirectoryStatusRepository objectDirectoryStatusRepository;
    private OutboundAudioDeleterProcessorImpl outboundAudioDeleterProcessorImpl;

    @Mock
    private SystemUserHelper systemUserHelper;

    @BeforeEach
    void setUp() {
        this.outboundAudioDeleterProcessorImpl = new OutboundAudioDeleterProcessorImpl(
            mediaRequestRepository,
            transientObjectDirectoryRepository,
            userAccountRepository,
            objectDirectoryStatusRepository, lastAccessedDeletionDayCalculator,
            systemUserHelper
        );
        when(systemUserHelper.findSystemUserGuid(anyString())).thenReturn("value");

    }

    @Test
    void testDeleteWhenSystemUserDoesNotExist() {
        List<Integer> value = new ArrayList<>();
        value.add(0);

        when(mediaRequestRepository.findAllIdsByLastAccessedTimeBeforeAndStatus(any(), any())).thenReturn(value);
        when(mediaRequestRepository.findAllByCreatedDateTimeBeforeAndStatusNotAndLastAccessedDateTimeIsNull(
            any(),
            any()
        )).thenReturn(value);
        when(transientObjectDirectoryRepository.findByMediaRequest_idIn(any())).thenReturn(List.of(new TransientObjectDirectoryEntity()));

        when(systemUserHelper.findSystemUserGuid(anyString())).thenReturn(null);
        assertThrows(DartsApiException.class, () ->
            outboundAudioDeleterProcessorImpl.delete());
    }
}

