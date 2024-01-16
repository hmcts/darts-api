package uk.gov.hmcts.darts.audio.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.helper.SystemUserHelper;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.repository.TransformedMediaRepository;
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
            transientObjectDirectoryRepository,
            userAccountRepository,
            objectRecordStatusRepository, lastAccessedDeletionDayCalculator,
            systemUserHelper, transformedMediaRepository
        );
        when(systemUserHelper.findSystemUserGuid(anyString())).thenReturn("value");

    }

    @Test
    void testDeleteWhenSystemUserDoesNotExist() {
        List<TransformedMediaEntity> value = new ArrayList<>();
        value.add(new TransformedMediaEntity());

        when(transformedMediaRepository.findAllDeletableTransformedMedia(any())).thenReturn(value);

        when(systemUserHelper.findSystemUserGuid(anyString())).thenReturn(null);
        assertThrows(DartsApiException.class, () ->
            outboundAudioDeleterProcessorImpl.markForDeletion());
    }

}

