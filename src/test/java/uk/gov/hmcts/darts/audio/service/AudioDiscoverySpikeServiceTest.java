package uk.gov.hmcts.darts.audio.service;

import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.ListBlobsOptions;
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
        config.setPrefix("dma16/");
        service = new AudioDiscoverySpikeService(factory, config);
    }

    @Test
    void given_missingConnection_when_scan_then_rejectBeforeAccessingStorage() {
        config.setConnectionString("");
        assertThatThrownBy(() -> service.scan(true, false, 10)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(factory);
    }

    @ParameterizedTest
    @CsvSource({"true,false,cvp", "false,true,car"})
    void given_singleSource_when_scan_then_limitConsumption(boolean cvp, boolean car, String name) {
        when(factory.getBlobServiceClient("test-connection")).thenReturn(storage);
        when(factory.getBlobContainerClient(name, storage)).thenReturn(container);
        when(container.listBlobs(any(ListBlobsOptions.class), any(Duration.class))).thenReturn(blobs);
        java.util.Iterator<BlobItem> iterator = List.of(new BlobItem(), new BlobItem()).iterator();
        when(blobs.iterator()).thenReturn(iterator);
        service.scan(cvp, car, 1);
        org.assertj.core.api.Assertions.assertThat(iterator.next()).isNotNull();
        org.assertj.core.api.Assertions.assertThat(iterator.hasNext()).isFalse();
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
    void given_combinedScan_when_successful_then_scanBothContainersWithPrefix() {
        when(factory.getBlobServiceClient("test-connection")).thenReturn(storage);
        when(factory.getBlobContainerClient("cvp", storage)).thenReturn(container);
        when(factory.getBlobContainerClient("car", storage)).thenReturn(container);
        when(container.listBlobs(any(ListBlobsOptions.class), eq(Duration.ofSeconds(30)))).thenAnswer(invocation -> {
            org.assertj.core.api.Assertions.assertThat(((ListBlobsOptions) invocation.getArgument(0)).getPrefix()).isEqualTo("dma16/");
            return blobs;
        });
        when(blobs.iterator()).thenAnswer(invocation -> List.of(new BlobItem()).iterator());
        service.scan(true, true, 10);
        verify(factory).getBlobContainerClient("cvp", storage);
        verify(factory).getBlobContainerClient("car", storage);
    }
}
