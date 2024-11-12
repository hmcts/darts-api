package uk.gov.hmcts.darts.common.datamanagement.component.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.core.io.FileUrlResource;
import org.springframework.core.io.Resource;
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
 * The response download data. Always use in combination with a try resources to ensure the file resources are cleaned up
 */
@Slf4j
public class FileBasedDownloadResponseMetaData extends DownloadResponseMetaData {

    private File fileToBeDownloadedTo;

    /**
     * gets a spring file resource. The file is cleaned up after the closure of the associated input stream.
     * To that end this method call is single use
     *
     * @return The resource
     */
    @Override
    public Resource getResource() throws IOException {

        return new FileUrlResource(fileToBeDownloadedTo.toURI().toURL()) {

            @Override
            public InputStream getInputStream() throws IOException {
                InputStream inputStream = Files.newInputStream(Path.of(fileToBeDownloadedTo.toURI()), StandardOpenOption.READ);

                return new FileInputStreamWrapper(inputStream);
            }
        };
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

    public void setInputStream(InputStream inputStream, StorageConfiguration configuration) throws IOException {
        Files.createDirectories(Path.of(configuration.getTempBlobWorkspace()));
        fileToBeDownloadedTo = Files.createFile(Path.of(configuration.getTempBlobWorkspace(), UUID.randomUUID().toString())).toFile();
        FileUtils.copyInputStreamToFile(inputStream, fileToBeDownloadedTo);
    }

    @Override
    public void close() throws IOException {
        super.close();

        try {
            if (fileToBeDownloadedTo != null && fileToBeDownloadedTo.exists()) {
                Files.delete(Path.of(fileToBeDownloadedTo.toURI()));
            }
        } catch (IOException ioException) {
            log.error("Could not clean up file %s. Please manually delete it", ioException);
        }
    }

    class FileInputStreamWrapper extends InputStream {

        private final InputStream inputStream;

        public FileInputStreamWrapper(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        public int read() throws IOException {
            return inputStream.read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return inputStream.read(b, off, len);
        }

        /**
         * Delete the file when the input stream close is called.
         */
        @Override
        public void close() throws IOException {
            inputStream.close();
            FileBasedDownloadResponseMetaData.this.close();
        }
    }
}