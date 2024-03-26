package uk.gov.hmcts.darts.arm.service.impl;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.service.impl.EodHelperMocks;
import uk.gov.hmcts.darts.common.util.EodHelper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExternalObjectDirectoryServiceImplTest {

    @Mock
    ExternalObjectDirectoryRepository eodRepository;
    @Mock
    ArmDataManagementConfiguration armConfig;
    @Mock
    ExternalObjectDirectoryEntity eod;

    private static final EodHelperMocks EOD_HELPER_MOCKS = new EodHelperMocks();

    ExternalObjectDirectoryServiceImpl eodService;

    @AfterAll
    public static void close() {
        EOD_HELPER_MOCKS.close();
    }

    @BeforeEach
    void setup() {
        eodService = new ExternalObjectDirectoryServiceImpl(eodRepository, armConfig);
    }

    @Test
    void testFindFailedStillRetriableArmEodsInvokesRepositoryCorrectly() {
        var eods = List.of(eod);
        when(eodRepository.findNotFinishedAndNotExceededRetryInStorageLocation(any(), any(), any(), any())).thenReturn(eods);

        var pageable = Pageable.ofSize(3);
        var result = eodService.findFailedStillRetriableArmEods(pageable);

        assertThat(result).isEqualTo(eods);
        verify(eodRepository).findNotFinishedAndNotExceededRetryInStorageLocation(
            EodHelper.failedArmStatuses(),
            EodHelper.armLocation(),
            armConfig.getMaxRetryAttempts(),
            pageable);
    }
}