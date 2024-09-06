package uk.gov.hmcts.darts.common.datamanagement.component.impl;

import lombok.Getter;
import org.springframework.core.io.FileUrlResource;
import org.springframework.core.io.Resource;
import reactor.util.annotation.NonNullApi;
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
import java.util.UUID;

/**
 *The response download data. Always use in combination with a try resources to ensure the file resources are cleaned up
 */
public class FileBasedDownloadResponseMetaData extends DownloadResponseMetaData {

    @Getter
    private File fileToBeDownloadedTo;

    public Resource getResource()  throws IOException {
        return new FileUrlResource(fileToBeDownloadedTo.toURI().toURL()) {
            public InputStream getInputStream() throws IOException {
                if (inputStream == null) {
                    inputStream = Files.newInputStream(Path.of(fileToBeDownloadedTo.toURI()), StandardOpenOption.READ);
                }

                return new FileInputStreamWrapper(inputStream);
            }
        };
    }

    @Override
    @SuppressWarnings("PMD.AvoidFileStream")
    public OutputStream getOutputStream(StorageConfiguration configuration) throws IOException {
        if (outputStream == null) {
            fileToBeDownloadedTo = RequestFileStore.getFileCreatedForThread().create(configuration.getTempBlobWorkspace());
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

    class FileInputStreamWrapper extends InputStream {

        private InputStream inputStream;

        public FileInputStreamWrapper(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        public int read() throws IOException {
            return inputStream.read();
        }

        @Override
        public void close() throws IOException {
            FileBasedDownloadResponseMetaData.this.close();
            FileBasedDownloadResponseMetaData.this.inputStream = null;
        }
    }
}