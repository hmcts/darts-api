package uk.gov.hmcts.darts.common.service.impl;

import lombok.Getter;
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

import java.io.Closeable;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
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
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RAW_DATA_PUSHED;
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
@Getter
public class EodHelperMocks implements Closeable {

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
    private ObjectRecordStatusEntity armRawDataPushedStatus;

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
        this(true);
    }

    public EodHelperMocks(boolean setupMocks) {
        closeable = MockitoAnnotations.openMocks(this);
        if (setupMocks) {
            mockEodHelper();
        }
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
        mockedEodHelper = Mockito.mockStatic(EodHelper.class);
        setupVariables();
        setupLocationTypes();
        setupObjectRecordStatuses();
    }

    private void setupVariables() {
        lenient().when(storedStatus.getId()).thenReturn(STORED.getId());
        lenient().when(storedStatus.getDescription()).thenReturn("Stored");
        lenient().when(failureStatus.getId()).thenReturn(FAILURE.getId());
        lenient().when(failureStatus.getDescription()).thenReturn("Failure");
        lenient().when(markForDeletionStatus.getId()).thenReturn(MARKED_FOR_DELETION.getId());
        lenient().when(markForDeletionStatus.getDescription()).thenReturn("marked for Deletion");
        lenient().when(armProcessingResponseFilesStatus.getId()).thenReturn(ARM_PROCESSING_RESPONSE_FILES.getId());
        lenient().when(armProcessingResponseFilesStatus.getDescription()).thenReturn("Arm Processing Response Files");
        lenient().when(armIngestionStatus.getId()).thenReturn(ARM_INGESTION.getId());
        lenient().when(armIngestionStatus.getDescription()).thenReturn("Arm Ingestion");
        lenient().when(armDropZoneStatus.getId()).thenReturn(ARM_DROP_ZONE.getId());
        lenient().when(armDropZoneStatus.getDescription()).thenReturn("Arm Drop Zone");
        lenient().when(failedArmRawDataStatus.getId()).thenReturn(ARM_RAW_DATA_FAILED.getId());
        lenient().when(failedArmRawDataStatus.getDescription()).thenReturn("Arm Raw Data Failed");
        lenient().when(failedArmManifestFileStatus.getId()).thenReturn(ARM_MANIFEST_FAILED.getId());
        lenient().when(failedArmManifestFileStatus.getDescription()).thenReturn("Arm Manifest Failed");
        lenient().when(awaitingVerificationStatus.getId()).thenReturn(AWAITING_VERIFICATION.getId());
        lenient().when(awaitingVerificationStatus.getDescription()).thenReturn("Awaiting Verification");
        lenient().when(armRpoPendingStatus.getId()).thenReturn(ARM_RPO_PENDING.getId());
        lenient().when(armRpoPendingStatus.getDescription()).thenReturn("Arm RPO Pending");
        lenient().when(armResponseManifestFailedStatus.getId()).thenReturn(ARM_RESPONSE_MANIFEST_FAILED.getId());
        lenient().when(armResponseManifestFailedStatus.getDescription()).thenReturn("Arm Response Manifest Failed");
        lenient().when(armResponseProcessingFailedStatus.getId()).thenReturn(ARM_RESPONSE_PROCESSING_FAILED.getId());
        lenient().when(armResponseProcessingFailedStatus.getDescription()).thenReturn("Arm Response Processing Failed");
        lenient().when(armResponseChecksumVerificationFailedStatus.getId()).thenReturn(ARM_RESPONSE_CHECKSUM_VERIFICATION_FAILED.getId());
        lenient().when(armResponseChecksumVerificationFailedStatus.getDescription()).thenReturn("Arm Response Checksum Verification Failed");
        lenient().when(armReplayStatus.getId()).thenReturn(ARM_REPLAY.getId());
        lenient().when(armReplayStatus.getDescription()).thenReturn("Arm Replay");
        lenient().when(armMissingResponseStatus.getId()).thenReturn(ARM_MISSING_RESPONSE.getId());
        lenient().when(armMissingResponseStatus.getDescription()).thenReturn("Arm Missing Response");
        lenient().when(armRawDataPushedStatus.getId()).thenReturn(ARM_RAW_DATA_PUSHED.getId());
        lenient().when(armRawDataPushedStatus.getDescription()).thenReturn("Arm Raw Data Pushed");

        lenient().when(unstructuredLocation.getId()).thenReturn(UNSTRUCTURED.getId());
        lenient().when(unstructuredLocation.getDescription()).thenReturn("unstructured");
        lenient().when(inboundLocation.getId()).thenReturn(INBOUND.getId());
        lenient().when(inboundLocation.getDescription()).thenReturn("inbound");
        lenient().when(armLocation.getId()).thenReturn(ARM.getId());
        lenient().when(armLocation.getDescription()).thenReturn("arm");
        lenient().when(detsLocation.getId()).thenReturn(DETS.getId());
        lenient().when(detsLocation.getDescription()).thenReturn("dets");
    }

    private void setupObjectRecordStatuses() {
        mockedEodHelper.when(EodHelper::storedStatus).thenReturn(storedStatus);
        mockedEodHelper.when(EodHelper::failureStatus).thenReturn(failureStatus);
        mockedEodHelper.when(EodHelper::markForDeletionStatus).thenReturn(markForDeletionStatus);
        mockedEodHelper.when(EodHelper::armProcessingResponseFilesStatus).thenReturn(armProcessingResponseFilesStatus);
        mockedEodHelper.when(EodHelper::armIngestionStatus).thenReturn(armIngestionStatus);
        mockedEodHelper.when(EodHelper::armDropZoneStatus).thenReturn(armDropZoneStatus);
        mockedEodHelper.when(EodHelper::failedArmRawDataStatus).thenReturn(failedArmRawDataStatus);
        mockedEodHelper.when(EodHelper::failedArmManifestFileStatus).thenReturn(failedArmManifestFileStatus);
        mockedEodHelper.when(EodHelper::awaitingVerificationStatus).thenReturn(awaitingVerificationStatus);
        mockedEodHelper.when(EodHelper::armRpoPendingStatus).thenReturn(armRpoPendingStatus);
        mockedEodHelper.when(EodHelper::armResponseManifestFailedStatus).thenReturn(armResponseManifestFailedStatus);
        mockedEodHelper.when(EodHelper::armResponseProcessingFailedStatus).thenReturn(armResponseProcessingFailedStatus);
        mockedEodHelper.when(EodHelper::armResponseChecksumVerificationFailedStatus).thenReturn(armResponseChecksumVerificationFailedStatus);
        mockedEodHelper.when(EodHelper::armReplayStatus).thenReturn(armReplayStatus);
        mockedEodHelper.when(EodHelper::armMissingResponseStatus).thenReturn(armMissingResponseStatus);
        mockedEodHelper.when(EodHelper::armRawDataPushedStatus).thenReturn(armRawDataPushedStatus);
    }

    private void setupLocationTypes() {
        mockedEodHelper.when(EodHelper::inboundLocation).thenReturn(inboundLocation);
        mockedEodHelper.when(EodHelper::armLocation).thenReturn(armLocation);
        mockedEodHelper.when(EodHelper::unstructuredLocation).thenReturn(unstructuredLocation);
        mockedEodHelper.when(EodHelper::detsLocation).thenReturn(detsLocation);
    }

    public void givenIsEqualLocationReturns(boolean result) {
        mockedEodHelper.when(() -> EodHelper.isEqual(any(ExternalLocationTypeEntity.class), any(ExternalLocationTypeEntity.class)))
            .thenReturn(result);
    }

    @SneakyThrows
    @Override
    public void close() {
        mockedEodHelper.close();
        closeable.close();
    }

    //This is needed to bypass static mock limitations with multi threaded operations
    public void simulateInitWithMockedData() {
        setupVariables();
        ExternalLocationTypeRepository eltRepository = mock(ExternalLocationTypeRepository.class);
        ObjectRecordStatusRepository orsRepository = mock(ObjectRecordStatusRepository.class);

        final EodHelper eodHelper = spy(new EodHelper(null, eltRepository, orsRepository));

        lenient().when(orsRepository.findById(STORED.getId())).thenReturn(Optional.of(storedStatus));
        lenient().when(orsRepository.findById(FAILURE.getId())).thenReturn(Optional.of(failureStatus));
        lenient().when(orsRepository.findById(MARKED_FOR_DELETION.getId())).thenReturn(Optional.of(markForDeletionStatus));
        lenient().when(orsRepository.findById(ARM_PROCESSING_RESPONSE_FILES.getId())).thenReturn(Optional.of(armProcessingResponseFilesStatus));
        lenient().when(orsRepository.findById(ARM_INGESTION.getId())).thenReturn(Optional.of(armIngestionStatus));
        lenient().when(orsRepository.findById(ARM_DROP_ZONE.getId())).thenReturn(Optional.of(armDropZoneStatus));
        lenient().when(orsRepository.findById(ARM_RAW_DATA_FAILED.getId())).thenReturn(Optional.of(failedArmRawDataStatus));
        lenient().when(orsRepository.findById(ARM_MANIFEST_FAILED.getId())).thenReturn(Optional.of(failedArmManifestFileStatus));
        lenient().when(orsRepository.findById(AWAITING_VERIFICATION.getId())).thenReturn(Optional.of(awaitingVerificationStatus));
        lenient().when(orsRepository.findById(ARM_RPO_PENDING.getId())).thenReturn(Optional.of(armRpoPendingStatus));
        lenient().when(orsRepository.findById(ARM_RESPONSE_MANIFEST_FAILED.getId())).thenReturn(Optional.of(armResponseManifestFailedStatus));
        lenient().when(orsRepository.findById(ARM_RESPONSE_PROCESSING_FAILED.getId())).thenReturn(Optional.of(armResponseProcessingFailedStatus));
        lenient().when(orsRepository.findById(ARM_RESPONSE_CHECKSUM_VERIFICATION_FAILED.getId())).thenReturn(
            Optional.of(armResponseChecksumVerificationFailedStatus));
        lenient().when(orsRepository.findById(ARM_REPLAY.getId())).thenReturn(Optional.of(armReplayStatus));
        lenient().when(orsRepository.findById(ARM_MISSING_RESPONSE.getId())).thenReturn(Optional.of(armMissingResponseStatus));
        lenient().when(orsRepository.findById(ARM_RAW_DATA_PUSHED.getId())).thenReturn(Optional.of(armRawDataPushedStatus));

        lenient().when(eltRepository.findById(INBOUND.getId())).thenReturn(Optional.of(inboundLocation));
        lenient().when(eltRepository.findById(ARM.getId())).thenReturn(Optional.of(armLocation));
        lenient().when(eltRepository.findById(UNSTRUCTURED.getId())).thenReturn(Optional.of(unstructuredLocation));
        lenient().when(eltRepository.findById(DETS.getId())).thenReturn(Optional.of(detsLocation));

        eodHelper.init();
    }
}