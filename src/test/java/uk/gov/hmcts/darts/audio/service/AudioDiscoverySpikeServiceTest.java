package uk.gov.hmcts.darts.audio.service;

import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.ListBlobsOptions;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audio.config.AudioDiscoverySpikeConfig;
import uk.gov.hmcts.darts.common.datamanagement.component.DataManagementAzureClientFactory;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AudioDiscoverySpikeServiceTest {
    @Mock
    private DataManagementAzureClientFactory factory;
    @Mock
    private BlobServiceClient storage;
    @Mock
    private BlobContainerClient container;
    @Mock
    private PagedIterable<BlobItem> blobs;
    private AudioDiscoverySpikeConfig config;
    private AudioDiscoverySpikeService service;

    @BeforeEach
    void setup() {
        config = new AudioDiscoverySpikeConfig();
        config.setConnectionString("test-connection");
        config.setCvpContainer("cvp");
        config.setCarContainer("car");
        config.setCarPrefix("");
        config.setPrefix("");
        service = new AudioDiscoverySpikeService(factory, config);
    }

    @Test
    void scanCvpExternal_listsNewSourceWithIdentity_whenConfigured() {
        config.setCvpAccountUrl("https://cvp-test.blob.core.windows.net");
        config.setCvpSourceContainer("crime-recordings");
        config.setCvpPrefix("2026/");
        when(factory.getBlobServiceClientWithIdentity(config.getCvpAccountUrl())).thenReturn(storage);
        when(factory.getBlobContainerClient("crime-recordings", storage)).thenReturn(container);
        when(container.listBlobs(any(ListBlobsOptions.class), any(Duration.class))).thenAnswer(invocation -> {
            assertThat(((ListBlobsOptions) invocation.getArgument(0)).getPrefix()).isEqualTo("2026/");
            return blobs;
        });
        when(blobs.iterator()).thenReturn(List.of(new BlobItem()).iterator());

        service.scanCvpExternal(10);

        verify(factory).getBlobServiceClientWithIdentity(config.getCvpAccountUrl());
        verify(factory, never()).getBlobServiceClient(any());
        verify(factory, never()).getBlobContainerClient("car", storage);
    }

    @ParameterizedTest
    @CsvSource({
        ",crime-recordings,2026/,10",
        "https://cvp-test.blob.core.windows.net,,2026/,10",
        "https://cvp-test.blob.core.windows.net,crime-recordings,,10",
        "https://cvp-test.blob.core.windows.net,crime-recordings,2026/,0"
    })
    void scanCvpExternal_rejectsBeforeAccessingStorage_whenSettingsInvalid(String accountUrl, String sourceContainer, String prefix, int batchSize) {
        config.setCvpAccountUrl(accountUrl);
        config.setCvpSourceContainer(sourceContainer);
        config.setCvpPrefix(prefix);

        assertThatThrownBy(() -> service.scanCvpExternal(batchSize)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(factory);
    }

    @Test
    void scanCvp_listsEveryInboundBlobWithoutPrefix_whenBatchSizeIsSmallerThanListing() {
        when(factory.getBlobServiceClient("test-connection")).thenReturn(storage);
        when(factory.getBlobContainerClient("cvp", storage)).thenReturn(container);
        when(container.listBlobs(any(ListBlobsOptions.class), any(Duration.class))).thenAnswer(invocation -> {
            assertThat(((ListBlobsOptions) invocation.getArgument(0)).getPrefix()).isEmpty();
            return blobs;
        });
        when(blobs.iterator()).thenReturn(java.util.Collections.nCopies(3, new BlobItem()).iterator());

        try (LogCaptor logs = LogCaptor.forClass(AudioDiscoverySpikeService.class)) {
            service.scanCvp(1);

            assertThat(logs.getInfoLogs()).anySatisfy(message -> assertThat(message)
                .contains("source=CVP", "container=cvp", "count=3", "limitReached=false", "elapsedMs=", "status=SUCCESS"));
        }
        verify(factory, never()).getBlobServiceClientWithIdentity(any());
    }

    @Test
    void scanCar_listsEveryBlobWithoutPrefix_whenBatchSizeIsSmallerThanListing() {
        when(factory.getBlobServiceClient("test-connection")).thenReturn(storage);
        when(factory.getBlobContainerClient("car", storage)).thenReturn(container);
        when(container.listBlobs(any(ListBlobsOptions.class), any(Duration.class))).thenAnswer(invocation -> {
            assertThat(((ListBlobsOptions) invocation.getArgument(0)).getPrefix()).isEmpty();
            return blobs;
        });
        when(blobs.iterator()).thenReturn(java.util.Collections.nCopies(3, new BlobItem()).iterator());

        try (LogCaptor logs = LogCaptor.forClass(AudioDiscoverySpikeService.class)) {
            service.scanCar(1);

            assertThat(logs.getInfoLogs()).anySatisfy(message -> assertThat(message)
                .contains("source=CAR", "container=car", "count=3", "limitReached=false", "elapsedMs=", "status=SUCCESS"));
        }
    }

    @Test
    void scanCombinedFaulty_logsFailedCvpAndUnattemptedCar_whenCvpContainerDoesNotExist() {
        when(factory.getBlobServiceClient("test-connection")).thenReturn(storage);
        when(factory.getBlobContainerClient("darts-container", storage)).thenReturn(container);
        var failure = new IllegalStateException("Container unavailable");
        when(container.listBlobs(any(ListBlobsOptions.class), any(Duration.class))).thenThrow(failure);

        try (LogCaptor logs = LogCaptor.forClass(AudioDiscoverySpikeService.class)) {
            assertThatThrownBy(() -> service.scanCombinedFaulty(10)).isSameAs(failure);

            assertThat(logs.getWarnLogs()).anySatisfy(message -> assertThat(message)
                .contains("source=CVP", "container=darts-container", "count=0", "elapsedMs=", "status=FAILED"));
            assertThat(logs.getWarnLogs()).anySatisfy(message -> assertThat(message)
                .contains("source=CAR", "container=car", "count=unknown", "status=NOT_ATTEMPTED", "failedSource=CVP"));
            assertThat(logs.getWarnLogs()).anySatisfy(message -> assertThat(message)
                .contains("mode=COMBINED_FAULTY", "attempted=1", "elapsedMs=", "status=FAILED"));
        }
        verify(factory, never()).getBlobContainerClient("car", storage);
    }

    @Test
    void scan_logsSuccessfulCvpAndFailedCar_whenSecondContainerFails() {
        when(factory.getBlobServiceClient("test-connection")).thenReturn(storage);
        when(factory.getBlobContainerClient("cvp", storage)).thenReturn(container);
        when(factory.getBlobContainerClient("car", storage)).thenReturn(container);
        var failure = new IllegalStateException("CAR unavailable");
        when(container.listBlobs(any(ListBlobsOptions.class), any(Duration.class)))
            .thenReturn(blobs).thenThrow(failure);
        when(blobs.iterator()).thenReturn(List.of(new BlobItem()).iterator());

        try (LogCaptor logs = LogCaptor.forClass(AudioDiscoverySpikeService.class)) {
            assertThatThrownBy(() -> service.scan(true, true, 10)).isSameAs(failure);

            assertThat(logs.getInfoLogs()).anySatisfy(message -> assertThat(message)
                .contains("source=CVP", "count=1", "status=SUCCESS"));
            assertThat(logs.getWarnLogs()).anySatisfy(message -> assertThat(message)
                .contains("source=CAR", "count=0", "elapsedMs=", "status=FAILED"));
            assertThat(logs.getWarnLogs()).anySatisfy(message -> assertThat(message)
                .contains("attempted=2", "status=FAILED"));
            assertThat(logs.getWarnLogs()).noneMatch(message -> message.contains("status=NOT_ATTEMPTED"));
        }
    }

    @Test
    void given_missingConnection_when_scan_then_rejectBeforeAccessingStorage() {
        config.setConnectionString("");
        assertThatThrownBy(() -> service.scan(true, false, 10)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(factory);
    }

    @ParameterizedTest
    @CsvSource({"true,false,cvp", "false,true,car"})
    void scan_readsAllBlobs_whenBatchSizeIsSmallerThanListing(boolean cvp, boolean car, String name) {
        when(factory.getBlobServiceClient("test-connection")).thenReturn(storage);
        when(factory.getBlobContainerClient(name, storage)).thenReturn(container);
        when(container.listBlobs(any(ListBlobsOptions.class), any(Duration.class))).thenReturn(blobs);
        java.util.Iterator<BlobItem> iterator = List.of(new BlobItem(), new BlobItem()).iterator();
        when(blobs.iterator()).thenReturn(iterator);
        service.scan(cvp, car, 1);
        assertThat(iterator.hasNext()).isFalse();
        verify(factory, never()).getBlobContainerClient(cvp ? "car" : "cvp", storage);
    }

    @Test
    void given_combinedScan_when_cvpFails_then_propagateFailureAndDoNotScanCar() {
        when(factory.getBlobServiceClient("test-connection")).thenReturn(storage);
        when(factory.getBlobContainerClient("cvp", storage)).thenReturn(container);
        when(container.listBlobs(any(ListBlobsOptions.class), any(Duration.class)))
            .thenThrow(new IllegalStateException("unavailable"));
        assertThatThrownBy(() -> service.scan(true, true, 10)).isInstanceOf(IllegalStateException.class);
        verify(factory, never()).getBlobContainerClient("car", storage);
    }

    @Test
    void scan_scansBothContainersWithoutPrefix_whenSuccessful() {
        when(factory.getBlobServiceClient("test-connection")).thenReturn(storage);
        when(factory.getBlobContainerClient("cvp", storage)).thenReturn(container);
        when(factory.getBlobContainerClient("car", storage)).thenReturn(container);
        when(container.listBlobs(any(ListBlobsOptions.class), eq(Duration.ofSeconds(30)))).thenAnswer(invocation -> {
            assertThat(((ListBlobsOptions) invocation.getArgument(0)).getPrefix()).isEmpty();
            return blobs;
        });
        when(blobs.iterator()).thenAnswer(invocation -> List.of(new BlobItem()).iterator());
        service.scan(true, true, 10);
        verify(factory).getBlobContainerClient("cvp", storage);
        verify(factory).getBlobContainerClient("car", storage);
    }
}
