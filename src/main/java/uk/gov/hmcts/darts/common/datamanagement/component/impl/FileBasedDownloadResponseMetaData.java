package uk.gov.hmcts.darts.common.datamanagement.component.impl;

import lombok.Getter;
import org.apache.commons.io.FileUtils;
import org.springframework.core.io.FileUrlResource;
import org.springframework.core.io.Resource;
import uk.gov.hmcts.darts.common.datamanagement.StorageConfiguration;
import uk.gov.hmcts.darts.common.util.RequestFileStore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 *The response download data. Always use in combination with a try resources to ensure the file resources are cleaned up
 */
public class FileBasedDownloadResponseMetaData extends DownloadResponseMetaData {

    @Getter
    private File fileToBeDownloadedTo;

    /**
     * gets a spring file resource. The file is cleaned up after the closure of the associated input stream.
     * To that end this method call is single use
     * @return The resource
     */
    public Resource getResource()  throws IOException {

        return new FileUrlResource(fileToBeDownloadedTo.toURI().toURL()) {
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
            fileToBeDownloadedTo = RequestFileStore.getFileStore().create(configuration.getTempBlobWorkspace());
            outputStream = new FileOutputStream(fileToBeDownloadedTo);
        }

        return outputStream;
    }

    public void setInputStream(InputStream inputStream, StorageConfiguration configuration)  throws IOException {
        fileToBeDownloadedTo = RequestFileStore.getFileStore().create(configuration.getTempBlobWorkspace());
        FileUtils.copyInputStreamToFile(inputStream, fileToBeDownloadedTo);
    }

    @Override
    public void close() throws IOException {
        super.close();

        if (fileToBeDownloadedTo != null && fileToBeDownloadedTo.exists()) {
            Files.delete(Path.of(fileToBeDownloadedTo.toURI()));
        }
    }

    class FileInputStreamWrapper extends InputStream {

        private InputStream inputStream;

        public FileInputStreamWrapper(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        public int read() throws IOException {
            return inputStream.read();
        }

        /**
         * Delete the file when the input stream close is called.
         */
        @Override
        public void close() throws IOException {
            FileBasedDownloadResponseMetaData.this.close();
        }
    }
}