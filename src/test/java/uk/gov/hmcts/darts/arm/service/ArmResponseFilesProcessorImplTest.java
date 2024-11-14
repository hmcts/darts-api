package uk.gov.hmcts.darts.arm.service;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;
import uk.gov.hmcts.darts.arm.component.ArmResponseFilesProcessSingleElement;
import uk.gov.hmcts.darts.arm.service.impl.ArmResponseFilesProcessorImpl;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.service.impl.EodHelperMocks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.util.EodHelper.armDropZoneStatus;
import static uk.gov.hmcts.darts.common.util.EodHelper.armLocation;

@ExtendWith(MockitoExtension.class)
class ArmResponseFilesProcessorImplTest {

    @Mock
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;

    @Mock
    private UserIdentity userIdentity;

    @Mock
    private ExternalObjectDirectoryEntity externalObjectDirectoryArmDropZone;

    @Mock
    private ArmResponseFilesProcessSingleElement armResponseFilesProcessSingleElement;

    @Captor
    private ArgumentCaptor<ExternalObjectDirectoryEntity> externalObjectDirectoryEntityCaptor;

    private ArmResponseFilesProcessor armResponseFilesProcessor;

    private static final EodHelperMocks EOD_HELPER_MOCKS = new EodHelperMocks();

    @BeforeEach
    void setupData() {

        armResponseFilesProcessor = new ArmResponseFilesProcessorImpl(
            externalObjectDirectoryRepository,
            userIdentity,
            armResponseFilesProcessSingleElement
        );
    }

    @AfterAll
    public static void close() {
        EOD_HELPER_MOCKS.close();
    }

    @Test
    void processResponseFilesUnableToFindInputUploadFile() {
        when(externalObjectDirectoryArmDropZone.getId())
            .thenReturn(1);
        doReturn(armDropZoneStatus()).when(externalObjectDirectoryArmDropZone).getStatus();

        List<ExternalObjectDirectoryEntity> inboundList = new ArrayList<>(Collections.singletonList(externalObjectDirectoryArmDropZone));
        when(externalObjectDirectoryRepository.findByExternalLocationTypeAndObjectStatus(armLocation(), armDropZoneStatus(), Limit.of(100)))
            .thenReturn(inboundList);

        armResponseFilesProcessor.processResponseFiles(100);

        verify(externalObjectDirectoryRepository).findByExternalLocationTypeAndObjectStatus(armLocation(), armDropZoneStatus(), Limit.of(100));
        verify(externalObjectDirectoryRepository).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());
        verify(armResponseFilesProcessSingleElement).processResponseFilesFor(1);

        verifyNoMoreInteractions(
            externalObjectDirectoryRepository,
            armResponseFilesProcessSingleElement
        );
    }

}
