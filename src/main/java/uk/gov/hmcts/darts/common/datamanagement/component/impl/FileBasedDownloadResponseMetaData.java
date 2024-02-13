package uk.gov.hmcts.darts.common.datamanagement.component.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 *The response download data. Always use in combination with a try resources to ensure the file resources are cleaned up
 */
public class FileBasedDownloadResponseMetaData extends DownloadResponseMetaData {
    private File fileToBeDownloadedTo;

    public FileBasedDownloadResponseMetaData(File fileToBeDownloadedTo) throws IOException {
        super(Files.newOutputStream(Path.of(fileToBeDownloadedTo.toURI()), StandardOpenOption.CREATE));
        this.fileToBeDownloadedTo = fileToBeDownloadedTo;
    }

    public FileBasedDownloadResponseMetaData() throws IOException {
        this(File.createTempFile("transfer-", ".transfer"));
    }

    public InputStream getInputStream()  throws IOException {
        if (inputStream == null) {
            inputStream = Files.newInputStream(Path.of(fileToBeDownloadedTo.toURI()), StandardOpenOption.READ);
        }

        return inputStream;
    }

    @Override
    public void close() throws IOException {
        super.close();
        fileToBeDownloadedTo.delete();
    }
}