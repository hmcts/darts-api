package uk.gov.hmcts.darts.common.service.impl;

import lombok.SneakyThrows;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.util.EodHelper;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.ARM;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.DETS;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.INBOUND;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.UNSTRUCTURED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_DROP_ZONE;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_INGESTION;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_MANIFEST_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_MISSING_RESPONSE;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_PROCESSING_RESPONSE_FILES;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RAW_DATA_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_REPLAY;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RESPONSE_CHECKSUM_VERIFICATION_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RESPONSE_MANIFEST_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RESPONSE_PROCESSING_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RPO_PENDING;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.AWAITING_VERIFICATION;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.FAILURE;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.MARKED_FOR_DELETION;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;

/**
 * Helper test class that mocks {@link EodHelper} entities and methods so that they can be used in unit tests.
 */
@SuppressWarnings("PMD.NcssCount")
public class EodHelperMocks {

    @Mock
    private ExternalLocationTypeEntity armLocation;
    @Mock
    private ExternalLocationTypeEntity unstructuredLocation;
    @Mock
    private ExternalLocationTypeEntity inboundLocation;
    @Mock
    private ExternalLocationTypeEntity detsLocation;

    @Mock
    private ObjectRecordStatusEntity storedStatus;
    @Mock
    private ObjectRecordStatusEntity failureStatus;
    @Mock
    private ObjectRecordStatusEntity markForDeletionStatus;
    @Mock
    private ObjectRecordStatusEntity armIngestionStatus;
    @Mock
    private ObjectRecordStatusEntity armProcessingResponseFilesStatus;
    @Mock
    private ObjectRecordStatusEntity armDropZoneStatus;
    @Mock
    private ObjectRecordStatusEntity failedArmRawDataStatus;
    @Mock
    private ObjectRecordStatusEntity failedArmManifestFileStatus;
    @Mock
    private ObjectRecordStatusEntity awaitingVerificationStatus;
    @Mock
    private ObjectRecordStatusEntity armRpoPendingStatus;
    @Mock
    private ObjectRecordStatusEntity armResponseManifestFailedStatus;

    @Mock
    private ObjectRecordStatusEntity failedArmResponseManifestFileStatus;
    @Mock
    private ObjectRecordStatusEntity armResponseProcessingFailedStatus;
    @Mock
    private ObjectRecordStatusEntity armResponseChecksumVerificationFailedStatus;
    @Mock
    private ObjectRecordStatusEntity armReplayStatus;
    @Mock
    private ObjectRecordStatusEntity armMissingResponseStatus;


    private MockedStatic<EodHelper> mockedEodHelper;
    private AutoCloseable closeable;

    public EodHelperMocks() {
        mockEodHelper();
    }

    /**
     * Please note.
     * When mocking an EOD to return a certain location or status entity use the form
     * <pre>
     * doReturn(armLocation()).when(eod).getExternalLocationType();
     * </pre>
     * rather than the classic form
     * <pre>
     * when(eod.getExternalLocationType()).thenReturn(armLocation())
     * </pre>
     */
    public final void mockEodHelper() {
        closeable = MockitoAnnotations.openMocks(this);

        mockedEodHelper = Mockito.mockStatic(EodHelper.class);

        mockedEodHelper.when(EodHelper::inboundLocation).thenReturn(inboundLocation);
        lenient().when(inboundLocation.getId()).thenReturn(INBOUND.getId());
        lenient().when(inboundLocation.getDescription()).thenReturn("inbound");
        mockedEodHelper.when(EodHelper::armLocation).thenReturn(armLocation);
        lenient().when(armLocation.getId()).thenReturn(ARM.getId());
        lenient().when(armLocation.getDescription()).thenReturn("arm");
        mockedEodHelper.when(EodHelper::unstructuredLocation).thenReturn(unstructuredLocation);
        lenient().when(unstructuredLocation.getId()).thenReturn(UNSTRUCTURED.getId());
        lenient().when(unstructuredLocation.getDescription()).thenReturn("unstructured");
        mockedEodHelper.when(EodHelper::detsLocation).thenReturn(detsLocation);
        lenient().when(detsLocation.getId()).thenReturn(DETS.getId());
        lenient().when(detsLocation.getDescription()).thenReturn("dets");

        mockedEodHelper.when(EodHelper::storedStatus).thenReturn(storedStatus);
        lenient().when(storedStatus.getId()).thenReturn(STORED.getId());
        lenient().when(storedStatus.getDescription()).thenReturn("Stored");
        mockedEodHelper.when(EodHelper::failureStatus).thenReturn(failureStatus);
        lenient().when(failureStatus.getId()).thenReturn(FAILURE.getId());
        lenient().when(failureStatus.getDescription()).thenReturn("Failure");
        mockedEodHelper.when(EodHelper::markForDeletionStatus).thenReturn(markForDeletionStatus);
        lenient().when(markForDeletionStatus.getId()).thenReturn(MARKED_FOR_DELETION.getId());
        lenient().when(markForDeletionStatus.getDescription()).thenReturn("marked for Deletion");
        mockedEodHelper.when(EodHelper::armProcessingResponseFilesStatus).thenReturn(armProcessingResponseFilesStatus);
        lenient().when(armProcessingResponseFilesStatus.getId()).thenReturn(ARM_PROCESSING_RESPONSE_FILES.getId());
        lenient().when(armProcessingResponseFilesStatus.getDescription()).thenReturn("Arm Processing Response Files");
        mockedEodHelper.when(EodHelper::armIngestionStatus).thenReturn(armIngestionStatus);
        lenient().when(armIngestionStatus.getId()).thenReturn(ARM_INGESTION.getId());
        lenient().when(armIngestionStatus.getDescription()).thenReturn("Arm Ingestion");
        mockedEodHelper.when(EodHelper::armDropZoneStatus).thenReturn(armDropZoneStatus);
        lenient().when(armDropZoneStatus.getId()).thenReturn(ARM_DROP_ZONE.getId());
        lenient().when(armDropZoneStatus.getDescription()).thenReturn("Arm Drop Zone");
        mockedEodHelper.when(EodHelper::failedArmRawDataStatus).thenReturn(failedArmRawDataStatus);
        lenient().when(failedArmRawDataStatus.getId()).thenReturn(ARM_RAW_DATA_FAILED.getId());
        lenient().when(failedArmRawDataStatus.getDescription()).thenReturn("Arm Raw Data Failed");
        mockedEodHelper.when(EodHelper::failedArmManifestFileStatus).thenReturn(failedArmManifestFileStatus);
        lenient().when(failedArmManifestFileStatus.getId()).thenReturn(ARM_MANIFEST_FAILED.getId());
        lenient().when(failedArmManifestFileStatus.getDescription()).thenReturn("Arm Manifest Failed");
        mockedEodHelper.when(EodHelper::awaitingVerificationStatus).thenReturn(awaitingVerificationStatus);
        lenient().when(awaitingVerificationStatus.getId()).thenReturn(AWAITING_VERIFICATION.getId());
        lenient().when(awaitingVerificationStatus.getDescription()).thenReturn("Awaiting Verification");
        mockedEodHelper.when(EodHelper::armRpoPendingStatus).thenReturn(armRpoPendingStatus);
        lenient().when(armRpoPendingStatus.getId()).thenReturn(ARM_RPO_PENDING.getId());
        lenient().when(armRpoPendingStatus.getDescription()).thenReturn("Arm RPO Pending");
        mockedEodHelper.when(EodHelper::armResponseManifestFailedStatus).thenReturn(armResponseManifestFailedStatus);
        lenient().when(armResponseManifestFailedStatus.getId()).thenReturn(ARM_RESPONSE_MANIFEST_FAILED.getId());
        lenient().when(armResponseManifestFailedStatus.getDescription()).thenReturn("Arm Response Manifest Failed");
        mockedEodHelper.when(EodHelper::armResponseProcessingFailedStatus).thenReturn(armResponseProcessingFailedStatus);
        lenient().when(armResponseProcessingFailedStatus.getId()).thenReturn(ARM_RESPONSE_PROCESSING_FAILED.getId());
        lenient().when(armResponseProcessingFailedStatus.getDescription()).thenReturn("Arm Response Processing Failed");
        mockedEodHelper.when(EodHelper::armResponseChecksumVerificationFailedStatus).thenReturn(armResponseChecksumVerificationFailedStatus);
        lenient().when(armResponseChecksumVerificationFailedStatus.getId()).thenReturn(ARM_RESPONSE_CHECKSUM_VERIFICATION_FAILED.getId());
        lenient().when(armResponseChecksumVerificationFailedStatus.getDescription()).thenReturn("Arm Response Checksum Verification Failed");
        mockedEodHelper.when(EodHelper::armReplayStatus).thenReturn(armReplayStatus);
        lenient().when(armReplayStatus.getId()).thenReturn(ARM_REPLAY.getId());
        lenient().when(armReplayStatus.getDescription()).thenReturn("Arm Replay");
        mockedEodHelper.when(EodHelper::armMissingResponseStatus).thenReturn(armMissingResponseStatus);
        lenient().when(armMissingResponseStatus.getId()).thenReturn(ARM_MISSING_RESPONSE.getId());
        lenient().when(armMissingResponseStatus.getDescription()).thenReturn("Arm Missing Response");

    }

    public void givenIsEqualLocationReturns(boolean result) {
        mockedEodHelper.when(() -> EodHelper.isEqual(any(ExternalLocationTypeEntity.class), any(ExternalLocationTypeEntity.class)))
            .thenReturn(result);
    }

    public void givenIsEqualStatusReturns(boolean result) {
        mockedEodHelper.when(() -> EodHelper.isEqual(any(ObjectRecordStatusEntity.class), any(ObjectRecordStatusEntity.class)))
            .thenReturn(result);
    }

    @SneakyThrows
    public void close() {
        mockedEodHelper.close();
        closeable.close();
    }

    //This is needed to bypass static mock limitations with multi threaded operations
    public void simulateInitWithMockedData(ObjectRecordStatusEntity objectRecordStatusEntity,
                                           ExternalLocationTypeEntity externalLocationTypeEntity) {
        ExternalLocationTypeRepository eltRepository = mock(ExternalLocationTypeRepository.class);
        ObjectRecordStatusRepository orsRepository = mock(ObjectRecordStatusRepository.class);

        EodHelper eodHelper = spy(new EodHelper(null, eltRepository, orsRepository));

        when(eltRepository.findById(anyInt())).thenReturn(Optional.of(externalLocationTypeEntity));
        when(orsRepository.findById(anyInt())).thenReturn(Optional.of(objectRecordStatusEntity));
        eodHelper.init();
    }
}