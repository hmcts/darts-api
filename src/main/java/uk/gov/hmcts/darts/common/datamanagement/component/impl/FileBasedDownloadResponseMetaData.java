package uk.gov.hmcts.darts.common.datamanagement.component.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

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
        if (is == null) {
            is = Files.newInputStream(Path.of(fileToBeDownloadedTo.toURI()), StandardOpenOption.READ);
        }

        return is;
    }

    @Override
    public void close() throws IOException {
        super.close();
        fileToBeDownloadedTo.delete();
    }
}