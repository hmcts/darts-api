package uk.gov.hmcts.darts.common.datamanagement.component.impl;

import uk.gov.hmcts.darts.common.datamanagement.StorageConfiguration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

/**
 *The response download data. Always use in combination with a try resources to ensure the file resources are cleaned up
 */
public class FileBasedDownloadResponseMetaData extends DownloadResponseMetaData {
    private File fileToBeDownloadedTo;

    @Override
    public InputStream getInputStream()  throws IOException {
        if (inputStream == null) {
            inputStream = Files.newInputStream(Path.of(fileToBeDownloadedTo.toURI()), StandardOpenOption.READ);
        }
        return inputStream;
    }

    @Override
    @SuppressWarnings("PMD.AvoidFileStream")
    public OutputStream getOutputStream(StorageConfiguration configuration) throws IOException {
        if (outputStream == null) {
            Files.createDirectories(Path.of(configuration.getTempBlobWorkspace()));
            fileToBeDownloadedTo = Files.createFile(Path.of(
                configuration.getTempBlobWorkspace(),
                UUID.randomUUID().toString())).toFile();
            outputStream = new FileOutputStream(fileToBeDownloadedTo);
        }

        return outputStream;
    }

    @Override
    public void close() throws IOException {
        super.close();

        if (fileToBeDownloadedTo != null) {
            Files.delete(Path.of(fileToBeDownloadedTo.toURI()));
        }
    }
}