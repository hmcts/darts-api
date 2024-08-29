package uk.gov.hmcts.darts.datamanagement.exception;

public class FileNotDownloadReadingBodyException extends FileNotDownloadedException {
    public FileNotDownloadReadingBodyException(String message) {
        super(message);
    }

    public FileNotDownloadReadingBodyException(String message, Throwable cause) {
        super(message, cause);
    }
}