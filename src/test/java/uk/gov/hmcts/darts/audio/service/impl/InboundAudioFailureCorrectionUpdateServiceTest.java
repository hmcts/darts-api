package uk.gov.hmcts.darts.audio.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.util.EodHelper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InboundAudioFailureCorrectionUpdateServiceTest {

    private static final long EOD_ID = 123L;
    private static final int USER_ID = 987;

    private ObjectRecordStatusEntity storedStatus;

    @Mock
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;

    @InjectMocks
    private InboundAudioFailureCorrectionUpdateService service;

    @BeforeEach
    void beforeEach() {
        storedStatus = new ObjectRecordStatusEntity();
        storedStatus.setId(ObjectRecordStatusEnum.STORED.getId());
        storedStatus.setDescription(ObjectRecordStatusEnum.STORED.name());

        ReflectionTestUtils.setField(EodHelper.class, "storedStatus", storedStatus);
    }

    @Test
    void markEodAsStored_updatesStatusAndTransferAttempts() {
        service.markEodAsStored(EOD_ID, USER_ID);

        verify(externalObjectDirectoryRepository).updateEodStatusAndTransferAttemptsWhereIdIn(
            storedStatus,
            0,
            USER_ID,
            List.of(EOD_ID)
        );
    }

    @Test
    void markEodAsStored_isTransactional() throws NoSuchMethodException {
        assertThat(InboundAudioFailureCorrectionUpdateService.class.getMethod("markEodAsStored", Long.class, Integer.class)
                       .isAnnotationPresent(Transactional.class))
            .isTrue();
    }
}


