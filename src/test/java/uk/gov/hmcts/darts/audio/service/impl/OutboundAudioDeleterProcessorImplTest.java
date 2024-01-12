package uk.gov.hmcts.darts.audio.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.helper.SystemUserHelper;
import uk.gov.hmcts.darts.common.repository.MediaRequestRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.repository.TransformedMediaRepository;
import uk.gov.hmcts.darts.common.repository.TransientObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    private ObjectRecordStatusRepository objectRecordStatusRepository;
    private OutboundAudioDeleterProcessorImpl outboundAudioDeleterProcessorImpl;

    @Mock
    private SystemUserHelper systemUserHelper;

    @Mock
    private TransformedMediaRepository transformedMediaRepository;

    @BeforeEach
    void setUp() {
        this.outboundAudioDeleterProcessorImpl = new OutboundAudioDeleterProcessorImpl(
            mediaRequestRepository,
            transientObjectDirectoryRepository,
            userAccountRepository,
            objectRecordStatusRepository, lastAccessedDeletionDayCalculator,
            systemUserHelper, transformedMediaRepository
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
        when(transientObjectDirectoryRepository.findByMediaRequestIds(any())).thenReturn(List.of(new TransientObjectDirectoryEntity()));

        when(systemUserHelper.findSystemUserGuid(anyString())).thenReturn(null);
        assertThrows(DartsApiException.class, () ->
            outboundAudioDeleterProcessorImpl.markForDeletion());
    }


    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void testDeleteWhenSystemReturnsIdsForMediaRequestButDoNotExist(CapturedOutput capture) throws InterruptedException {
        List<Integer> value = new ArrayList<>();
        value.add(1);

        when(mediaRequestRepository.findAllIdsByLastAccessedTimeBeforeAndStatus(any(), any())).thenReturn(value);
        when(mediaRequestRepository.findAllByCreatedDateTimeBeforeAndStatusNotAndLastAccessedDateTimeIsNull(
            any(),
            any()
        )).thenReturn(Collections.emptyList());
        when(transientObjectDirectoryRepository.findByMediaRequestIds(any())).thenReturn(Collections.emptyList());
        when(userAccountRepository.findSystemUser(anyString())).thenReturn(new UserAccountEntity());
        outboundAudioDeleterProcessorImpl.markForDeletion();
        Thread.sleep(2000);
        assertTrue(capture.getOut().contains("Media request with id: 1 was found to be soft deleted but has gone missing when trying to mark it as expired"));
    }

}

