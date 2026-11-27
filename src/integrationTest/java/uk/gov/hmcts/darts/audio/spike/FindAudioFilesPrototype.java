package uk.gov.hmcts.darts.audio.spike;

import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.models.ListBlobsOptions;
import uk.gov.hmcts.darts.common.datamanagement.component.DataManagementAzureClientFactory;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DataManagementAzureClientFactoryImpl;

import java.time.Duration;
import java.util.List;

/**
 * Read-only prototype. Run main in IntelliJ with AZURE_STORAGE_CONNECTION_STRING set securely.
 * Supply one container name as a program argument for a separate call, or two names for a combined call.
 * With no arguments, scans darts-inbound-container and darts-unstructured sequentially.
 * AUDIO_SPIKE_PREFIX defaults to 000, the small staging sample used for this spike.
 * Fetches blob metadata only; does not download audio or update storage or a database.
 */
public class FindAudioFilesPrototype {

    private final DataManagementAzureClientFactory factory = new DataManagementAzureClientFactoryImpl();
    private final BlobServiceClient storage;

    public FindAudioFilesPrototype(String connectionString) {

        storage = factory.getBlobServiceClient(connectionString);
    }

    /**
     * Returns a lazy, paginated listing for one container. Azure requests occur when the result is iterated.
     */
    public PagedIterable<BlobItem> fetchFiles(String containerName, String prefix) {
        return factory.getBlobContainerClient(containerName, storage)
            .listBlobs(new ListBlobsOptions().setPrefix(prefix), Duration.ofSeconds(30));
    }

    @SuppressWarnings("PMD.SystemPrintln") // Standalone prototype output, independent of test logging configuration.
    public void scanContainer(String containerName, String prefix) {
        long start = System.nanoTime();
        long count = 0;
        try {
            for (BlobItem file : fetchFiles(containerName, prefix)) {
                System.out.printf("Container=%s file=%s%n", containerName, file.getName());
                count++;
            }
            System.out.printf("Container=%s files=%d elapsed_ms=%d%n", containerName, count, (System.nanoTime() - start) / 1_000_000);
        } catch (BlobStorageException exception) {
            System.err.printf(
                "Container=%s status=%d error=%s message=%s%n",
                containerName,
                exception.getStatusCode(),
                exception.getErrorCode(),
                exception.getServiceMessage()
            );
        }
    }

    @SuppressWarnings("PMD.SystemPrintln") // Display the combined duration when running main from the IDE.
    public static void main(String[] args) {
        String connectionString = System.getenv("AZURE_STORAGE_CONNECTION_STRING");
        if (connectionString == null || connectionString.isBlank()) {
            throw new IllegalArgumentException("Set AZURE_STORAGE_CONNECTION_STRING in the run environment");
        }
        String prefix = System.getenv().getOrDefault("AUDIO_SPIKE_PREFIX", "000");
        FindAudioFilesPrototype prototype = new FindAudioFilesPrototype(connectionString);
        List<String> containers = args.length == 0
            ? List.of("darts-inbound-container", "darts-unstructured") : List.of(args);
        long start = System.nanoTime();
        for (String container : containers) {
            prototype.scanContainer(container, prefix);
        }
        System.out.printf("Finished containers=%d total_elapsed_ms=%d%n", containers.size(), (System.nanoTime() - start) / 1_000_000);
    }
}
