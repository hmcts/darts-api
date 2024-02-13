package uk.gov.hmcts.darts.common.datamanagement.component.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.darts.common.datamanagement.enums.DatastoreContainerType;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@RequiredArgsConstructor
@Getter
public class DownloadResponseMetaData implements Closeable {
    protected InputStream inputStream;

    private final OutputStream outputStream;

    private boolean processedByContainer;

    private boolean successfulDownload;

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

    /**
     ** override as appropriate to return your custom input stream null by default.
     */
    public InputStream getInputStream()  throws IOException {
        return is;
    }

    public void markInputStream(InputStream is) {
        this.is = is;
    }


    public boolean isSuccessfullyDownloaded() {
        return successfulDownload;
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