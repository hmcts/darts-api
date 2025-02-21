package uk.gov.hmcts.darts.datamanagement.exception;

import lombok.NoArgsConstructor;
import lombok.ToString;


@ToString
@NoArgsConstructor
public class FileNotDownloadedException extends Exception {
    public FileNotDownloadedException(String message) {
        super(message);
    }

    public FileNotDownloadedException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileNotDownloadedException(String blobId, String containerName, String message) {
        super("BlobId: " + blobId + ", container: " + containerName + ", message: " + message);
    }

    public FileNotDownloadedException(String blobId, String containerName, String message, Throwable cause) {
        super("BlobId: " + blobId + ", container: " + containerName + ", message: " + message, cause);
    }
}
