package uk.gov.hmcts.darts.common.datamanagement.component.impl;

import lombok.Getter;
import uk.gov.hmcts.darts.common.datamanagement.StorageConfiguration;
import uk.gov.hmcts.darts.common.datamanagement.enums.DatastoreContainerType;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A general download response that can be extended. Always use in combination with a try resources to ensure the file resources are cleaned up
 */
public abstract class DownloadResponseMetaData implements Closeable {
    protected InputStream inputStream;

    protected OutputStream outputStream;

    @Getter
    private boolean processedByContainer;

    @Getter
    private boolean successfulDownload;

    @Getter
    private DatastoreContainerType containerTypeUsedToDownload;

    public void markSuccess(DatastoreContainerType containerType) {
        successfulDownload = true;
        processedByContainer = true;
        this.containerTypeUsedToDownload = containerType;
    }

    public void markFailure(DatastoreContainerType containerType) {
        successfulDownload = false;
        processedByContainer = true;
        this.containerTypeUsedToDownload = containerType;
    }

    public InputStream getInputStream()  throws IOException {
        return inputStream;
    }

    public abstract OutputStream getOutputStream(StorageConfiguration configuration) throws IOException;

    public void markInputStream(InputStream is) {
        this.inputStream = is;
    }

    @Override
    public void close() throws IOException {

        if (inputStream != null) {
            inputStream.close();
        }

        if (outputStream != null) {
            outputStream.close();
        }
    }

}