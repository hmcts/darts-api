package uk.gov.hmcts.darts.datamanagement.model;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.models.BlobProperties;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;


@ExtendWith(MockitoExtension.class)
class BlobClientUploadResponseImplTest {

    private BlobClientUploadResponseImpl blobClientUploadResponse;

    @Mock
    private BlobClient blobClient;
    @Mock
    private BlobProperties blobProperties;

    @BeforeEach
    void setUp() {
        blobClientUploadResponse = new BlobClientUploadResponseImpl(blobClient);
    }

    @Test
    void shouldReturnExpectedBlobName() {
        // Given
        final String someName = UUID.randomUUID().toString();
        Mockito.when(blobClient.getBlobName()).thenReturn(someName);

        // When
        String blobName = blobClientUploadResponse.getBlobName();

        // Then
        assertEquals(someName, blobName);
    }

    @Test
    void shouldReturnExpectedBlobSizeAndOnlyInvokeClientOnce() {
        // Given
        Mockito.when(blobClient.getProperties())
            .thenReturn(blobProperties);

        final long someSize = 1000L;
        Mockito.when(blobProperties.getBlobSize())
            .thenReturn(someSize);

        // When invoked multiple times
        for (int i = 0; i < 2; i++) {
            Long blobSize = blobClientUploadResponse.getBlobSize();

            // Then
            assertEquals(someSize, blobSize);
        }

        // And
        Mockito.verify(blobClient, Mockito.times(1))
            .getProperties();
    }

    @Test
    void shouldAppendMetadataToExistingMetadataAndInvokeClient() {
        // Given
        Mockito.when(blobClient.getProperties())
            .thenReturn(blobProperties);

        Mockito.when(blobProperties.getMetadata())
            .thenReturn(Map.of("someExistingKey", "someExistingValue"));

        // When
        Map<String, String> metadata = blobClientUploadResponse.addMetadata(Map.of("someAdditionalKey", "someAdditionalValue"));

        // Then
        Mockito.verify(blobClient, Mockito.times(1))
            .setMetadata(metadata);

        assertEquals(2, metadata.size());
        MatcherAssert.assertThat(metadata, Matchers.hasEntry("someExistingKey", "someExistingValue"));
        MatcherAssert.assertThat(metadata, Matchers.hasEntry("someAdditionalKey", "someAdditionalValue"));
    }

}
